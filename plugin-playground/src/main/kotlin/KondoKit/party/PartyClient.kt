package KondoKit.party

import KondoKit.util.JsonParser
import com.google.gson.JsonObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

/**
 * Talks to the party relay (party-server/public/party.php).
 *
 * Threading: the game thread only calls [queuePatch] / [drainUpdates] /
 * [join] / [leave]; every HTTP request happens on [executor]. The relay only
 * ever carries snapshots of each member's own state, so nothing received
 * here can act on the local client - it is only drawn by PartyView.
 */
object PartyClient {
    const val DEFAULT_SERVER = "https://oblongnoodle.com/party/party.php"
    private const val POLL_MS = 1000L
    private const val TIMEOUT_MS = 5000
    // Resend everything this often, so a member whose row the relay expired
    // (e.g. after a long network drop) comes back complete, not as a diff.
    private const val FULL_RESYNC_MS = 60_000L
    // Mixed into the password before hashing so the room id is not just a
    // plain SHA-256 of a common word.
    private const val ROOM_SALT = "rt4-party-v1:"

    data class RemoteMember(val id: String, val name: String, val state: JsonObject)

    /** One poll's worth of news for the game thread. */
    class Update(val present: Set<String>, val changed: List<RemoteMember>)

    enum class Status { IDLE, CONNECTING, CONNECTED, ERROR, ROOM_FULL }

    @Volatile var serverUrl = DEFAULT_SERVER
    @Volatile var status = Status.IDLE
        private set
    @Volatile var statusDetail = ""
        private set
    /** Name to publish; set by the game thread whenever it is known. */
    @Volatile var displayName = ""

    val inParty: Boolean get() = room != null

    @Volatile private var room: String? = null
    @Volatile private var member = ""
    @Volatile private var needsFullSync = false
    private var since = 0
    private var lastFullSync = 0L

    private val patchLock = Any()
    private val pendingPatch = LinkedHashMap<String, Any>()
    private val updates = ConcurrentLinkedQueue<Update>()

    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { r ->
        Thread(r, "KondoKit-Party").apply { isDaemon = true }
    }
    private var pollTask: ScheduledFuture<*>? = null

    fun join(password: String) {
        leave()
        room = roomId(password)
        member = randomHex(16)
        since = 0
        needsFullSync = true
        lastFullSync = 0L
        status = Status.CONNECTING
        statusDetail = ""
        pollTask = executor.scheduleWithFixedDelay({ pollSafely() }, 0, POLL_MS, TimeUnit.MILLISECONDS)
    }

    fun leave() {
        val oldRoom = room ?: return
        val oldMember = member
        pollTask?.cancel(false)
        pollTask = null
        room = null
        synchronized(patchLock) { pendingPatch.clear() }
        updates.clear()
        status = Status.IDLE
        statusDetail = ""
        executor.execute {
            try {
                post(mapOf("room" to oldRoom, "member" to oldMember, "name" to safeName(),
                    "since" to 0, "leave" to true))
            } catch (_: Exception) {
                // Best effort: the relay expires silent members on its own.
            }
        }
    }

    /**
     * True once after joining, and periodically after that: the caller should
     * pass its whole state to [queuePatch] instead of only what changed.
     */
    fun consumeFullSyncRequest(): Boolean {
        val now = System.currentTimeMillis()
        if (needsFullSync || now - lastFullSync >= FULL_RESYNC_MS) {
            needsFullSync = false
            lastFullSync = now
            return true
        }
        return false
    }

    /** Merge changed state keys into the next request. Game thread. */
    fun queuePatch(patch: Map<String, Any>) {
        if (patch.isEmpty() || room == null) return
        synchronized(patchLock) { pendingPatch.putAll(patch) }
    }

    /** Everything received since the last call. Game thread. */
    fun drainUpdates(): List<Update> {
        val out = ArrayList<Update>()
        while (true) out.add(updates.poll() ?: break)
        return out
    }

    private fun pollSafely() {
        try {
            poll()
        } catch (e: Exception) {
            status = Status.ERROR
            statusDetail = e.javaClass.simpleName + (e.message?.let { ": $it" } ?: "")
        }
    }

    private fun poll() {
        val currentRoom = room ?: return
        val patch = synchronized(patchLock) {
            if (pendingPatch.isEmpty()) null else LinkedHashMap(pendingPatch).also { pendingPatch.clear() }
        }
        val body = linkedMapOf<String, Any?>(
            "room" to currentRoom, "member" to member, "name" to safeName(),
            "since" to since, "patch" to patch
        )

        val (code, response) = try {
            post(body)
        } catch (e: Exception) {
            requeue(patch)
            throw e
        }
        if (room != currentRoom) return // left or rejoined while in flight

        if (code == 403 && response?.get("error")?.asString == "room_full") {
            status = Status.ROOM_FULL
            statusDetail = "Party is full"
            return
        }
        if (code != 200 || response == null || response.get("ok")?.asBoolean != true) {
            requeue(patch)
            status = Status.ERROR
            statusDetail = "HTTP $code" + (response?.get("error")?.asString?.let { " ($it)" } ?: "")
            return
        }

        val present = response.getAsJsonArray("present").mapTo(HashSet()) { it.asString }
        val changed = response.getAsJsonArray("members").map {
            val m = it.asJsonObject
            RemoteMember(m.get("member").asString, m.get("name").asString, m.getAsJsonObject("state"))
        }
        since = response.get("seq").asInt
        updates.add(Update(present, changed))
        status = Status.CONNECTED
        statusDetail = ""
    }

    /** Put a failed patch back, without clobbering anything newer. */
    private fun requeue(patch: Map<String, Any>?) {
        if (patch == null) return
        synchronized(patchLock) {
            for ((k, v) in patch) if (k !in pendingPatch) pendingPatch[k] = v
        }
    }

    private fun post(body: Map<String, Any?>): Pair<Int, JsonObject?> {
        val conn = URL(serverUrl).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "RT4-PartyPanel/1.0")
            conn.outputStream.use { it.write(JsonParser.gson.toJson(body).toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val stream = if (code < 400) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
            val json = try {
                text?.let { JsonParser.gson.fromJson(it, JsonObject::class.java) }
            } catch (_: Exception) {
                null // e.g. an HTML error page from the host
            }
            return code to json
        } finally {
            conn.disconnect()
        }
    }

    // The relay only accepts [A-Za-z0-9 _-]{1,12}.
    private fun safeName(): String {
        val cleaned = displayName.replace(Regex("[^A-Za-z0-9 _-]"), " ").trim().take(12)
        return if (cleaned.isEmpty()) "Player" else cleaned
    }

    private fun roomId(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest((ROOM_SALT + password).toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun randomHex(bytes: Int): String {
        val buf = ByteArray(bytes)
        SecureRandom().nextBytes(buf)
        return buf.joinToString("") { "%02x".format(it) }
    }
}
