package KondoKit.party

import plugin.api.API
import rt4.Inv
import rt4.PlayerList
import rt4.PlayerSkillXpTable
import rt4.VarpDomain

/**
 * Reads the local player's own inventory, gear, stats and prayers each game
 * tick and hands PartyClient only the keys that changed. Read-only: nothing
 * here writes to client state. Game thread only.
 */
object LocalPartyState {
    const val INVENTORY_INV_ID = 93
    const val EQUIPMENT_INV_ID = 94
    private const val INVENTORY_SIZE = 28
    private const val EQUIPMENT_SIZE = 14
    private const val SKILL_COUNT = 25
    private const val PRAYER_SKILL = 5
    // Special attack energy x10 (1000 = 100%). The server sends it on every
    // change whatever is equipped; the client just hides the bar.
    private const val SPECIAL_ENERGY_VARP = 300

    // The 2009scape server gives every prayer its own on/off varp (see its
    // PrayerType.java). Listed in prayer-tab order, which is also the bit
    // order of the "prayers" bitmask sent to the party (PartyView.PRAYER_NAMES).
    private val PRAYER_VARPS = intArrayOf(
        83, 84, 85, 862, 863, 86, 87, 88, 89, 90, 91, 864, 865, 92,
        93, 94, 1168, 95, 96, 97, 866, 867, 98, 99, 100, 1052, 1053
    )

    // Last values handed to PartyClient, to diff against.
    private val lastQueued = HashMap<String, Any>()

    fun tick() {
        if (!PartyClient.inParty) {
            lastQueued.clear()
            return
        }
        PartyClient.displayName = PlayerList.self?.username?.toString() ?: PartyClient.displayName
        if (!API.IsLoggedIn()) return

        val current = snapshot()
        val full = PartyClient.consumeFullSyncRequest()
        val patch = if (full) current else current.filter { (k, v) -> lastQueued[k] != v }
        if (patch.isNotEmpty()) {
            PartyClient.queuePatch(patch)
            lastQueued.putAll(patch)
        }
    }

    private fun snapshot(): Map<String, Any> {
        val skills = (0 until SKILL_COUNT).map {
            listOf(PlayerSkillXpTable.baseLevels[it], PlayerSkillXpTable.boostedLevels[it], PlayerSkillXpTable.experience[it])
        }
        val prayer = listOf(listOf(
            PlayerSkillXpTable.boostedLevels[PRAYER_SKILL],
            PlayerSkillXpTable.baseLevels[PRAYER_SKILL]
        ))
        var prayers = 0
        for ((bit, varp) in PRAYER_VARPS.withIndex()) {
            if (VarpDomain.activeVarps[varp] != 0) prayers = prayers or (1 shl bit)
        }
        return mapOf(
            "inv" to readInv(INVENTORY_INV_ID, INVENTORY_SIZE),
            "gear" to readInv(EQUIPMENT_INV_ID, EQUIPMENT_SIZE),
            "skills" to skills,
            "prayer" to prayer,
            "prayers" to prayers,
            "spec" to (VarpDomain.activeVarps[SPECIAL_ENERGY_VARP] / 10).coerceIn(0, 100)
        )
    }

    /** [objId, qty] per slot; -1 id for an empty slot. */
    private fun readInv(invId: Int, size: Int): List<List<Int>> {
        val inv = Inv.objectContainerCache.get(invId.toLong()) as? Inv
        return (0 until size).map { slot ->
            val id = inv?.objectIds?.getOrNull(slot) ?: -1
            val qty = inv?.objectStackSizes?.getOrNull(slot) ?: 0
            if (id < 0) listOf(-1, 0) else listOf(id, qty)
        }
    }
}
