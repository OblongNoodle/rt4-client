package TimerPlugin;

import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.API;
import plugin.api.FontColor;
import plugin.api.FontType;
import plugin.api.TextModifier;
import KondoKit.Exposed;
import rt4.Chat;
import rt4.JagString;
import rt4.Npc;
import rt4.PlayerSkillXpTable;

import java.awt.Color;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@PluginMeta(
        author = "OblongNoodle",
        description = "Timer system with auto-tracking for GWD bosses (Graardor, Kree'arra, Zilyana, K'ril), GWD altars, KBD, and potions (Antifire, Antipoison variants). Also supports custom timers and farming presets.",
        version = 1.1
)
public class plugin extends Plugin {

    // =====================================================================
    // CONFIGURATION - Exposed to KondoKit UI
    // =====================================================================

    // Alert Settings
    @Exposed
    public int soundId = 2266; // Quest complete sound (distinctive alert)

    @Exposed
    public int soundVolume = 255; // 0-255

    @Exposed
    public boolean showInactiveTimers = false;

    @Exposed
    public boolean enableThirtySecondWarning = true;

    // Preset Timers - Farming only (others are reactive)
    @Exposed
    public boolean presetHerbPatch = false; // 80 minutes

    // GWD Prayer Altar Timer
    @Exposed
    public boolean enableAltarTimer = true; // 10 minute cooldown

    // Stat Boost Display
    @Exposed
    public boolean showStatBoosts = true; // Show boosted stats display

    // Custom Timers - Format: "Name:Seconds" (e.g. "Barrage:20")
    // Leave empty to disable. Click checkmark to start/stop.
    @Exposed
    public String A = "";

    @Exposed
    public String B = "";

    @Exposed
    public String C = "";

    // =====================================================================
    // INTERNAL STATE
    // =====================================================================

    // GWD Boss and Altar settings (not exposed to UI)
    private final boolean enableBandosTimer = true;
    private final boolean enableArmadylTimer = true;
    private final boolean enableSaradominTimer = true;
    private final boolean enableZamorakTimer = true;
    private final boolean enableKbdTimer = true;

    // Default respawn times (not configurable)
    private final int bandosRespawnSeconds = 90;
    private final int armadylRespawnSeconds = 90;
    private final int saradominRespawnSeconds = 90;
    private final int zamorakRespawnSeconds = 90;
    private final int kbdRespawnSeconds = 30;
    private final int altarCooldownSeconds = 600; // 10 minutes

    // Fixed display settings
    private static final int DISPLAY_X = 10;
    private static final int DISPLAY_Y = 250; // User preference
    private static final int ALERT_REPEAT_TIMES = 3;
    private static final int ALERT_REPEAT_INTERVAL = 1000;

    // Timer state persistence
    private static final String TIMER_STATE_FILE = System.getProperty("user.home") + File.separator + "2009scape" + File.separator + "timer_state.dat";

    private List<Timer> activeTimers = new ArrayList<>();
    private long lastAlertTime = 0;
    private int alertsRemaining = 0;
    private long lastStateSaveTime = 0;
    private static final long STATE_SAVE_INTERVAL = 5000; // Save every 5 seconds
    private java.util.Set<String> warned30Seconds = new java.util.HashSet<>(); // Track which timers have warned at 30s

    // Stat boost display update tracking
    private static final long STAT_UPDATE_INTERVAL = 2000; // Update every 2 seconds
    private long lastStatUpdateTime = 0;
    private List<String> currentStatBoosts = new ArrayList<>(); // Cache boosted stats for display

    // GWD Boss death tracking
    private long lastBandosSeenTime = 0;
    private boolean bandosTimerActive = false;

    private long lastArmadylSeenTime = 0;
    private boolean armadylTimerActive = false;

    private long lastSaradominSeenTime = 0;
    private boolean saradominTimerActive = false;

    private long lastZamorakSeenTime = 0;
    private boolean zamorakTimerActive = false;

    private long lastKbdSeenTime = 0;
    private boolean kbdTimerActive = false;

    private static final long NPC_DEATH_TIMEOUT = 500; // 0.5 seconds without seeing NPC = dead

    // Altar usage tracking to prevent multiple triggers
    private long lastAltarUseTime = 0;
    private static final long ALTAR_USE_COOLDOWN = 2000; // 2 seconds between altar uses

    // Prayer altar tracking - now uses chat messages instead of stat monitoring
    // (kept for potential future use)

    // Timer class to track individual timers
    private static class Timer {
        String name;
        long startTime;
        long duration; // milliseconds
        boolean hasAlerted;

        Timer(String name, long durationSeconds) {
            this.name = name;
            this.startTime = System.currentTimeMillis();
            this.duration = durationSeconds * 1000;
            this.hasAlerted = false;
        }

        long getRemainingTime() {
            long elapsed = System.currentTimeMillis() - startTime;
            return Math.max(0, duration - elapsed);
        }

        boolean isExpired() {
            return getRemainingTime() == 0;
        }

        String getDisplayText() {
            long remaining = getRemainingTime();
            long seconds = (remaining / 1000) % 60;
            long minutes = (remaining / 1000) / 60;

            if (minutes > 0) {
                return String.format("%s: %d:%02d", name, minutes, seconds);
            } else {
                return String.format("%s: %ds", name, seconds);
            }
        }
    }

    // =====================================================================
    // TIMER STATE PERSISTENCE
    // =====================================================================

    private void saveTimerState() {
        try {
            File stateFile = new File(TIMER_STATE_FILE);
            stateFile.getParentFile().mkdirs(); // Create directory if it doesn't exist

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(stateFile))) {
                // Write current time for reference
                writer.write(System.currentTimeMillis() + "\n");

                // Write each timer: name,startTime,duration,hasAlerted
                for (Timer timer : activeTimers) {
                    writer.write(timer.name + "," + timer.startTime + "," + timer.duration + "," + timer.hasAlerted + "\n");
                }
            }
        } catch (IOException e) {
            // Silently fail - not critical
        }
    }

    private void loadTimerState() {
        try {
            File stateFile = new File(TIMER_STATE_FILE);
            if (!stateFile.exists()) {
                return; // No saved state
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(stateFile))) {
                // Read saved time
                String savedTimeStr = reader.readLine();
                if (savedTimeStr == null) return;

                long savedTime = Long.parseLong(savedTimeStr);
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - savedTime;

                // Track expired timers for notification
                List<String> expiredWhileAway = new ArrayList<>();

                // Read timers
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(",");
                    if (parts.length != 4) continue;

                    String name = parts[0];
                    long startTime = Long.parseLong(parts[1]);
                    long duration = Long.parseLong(parts[2]);
                    boolean hasAlerted = Boolean.parseBoolean(parts[3]);

                    // Create timer with original start time (don't adjust)
                    // The Timer.getRemainingTime() method will calculate based on current time
                    Timer timer = new Timer(name, 0);
                    timer.startTime = startTime; // Use original start time
                    timer.duration = duration;
                    timer.hasAlerted = hasAlerted;

                    // Check if timer expired while away
                    long remainingTime = timer.getRemainingTime();
                    if (remainingTime <= 0) {
                        // Timer expired while logged out
                        long expiredAgoMs = Math.abs(remainingTime);
                        long expiredAgoMinutes = expiredAgoMs / 60000;
                        long expiredAgoSeconds = (expiredAgoMs / 1000) % 60;

                        String expiredMsg;
                        if (expiredAgoMinutes > 0) {
                            expiredMsg = name + " finished " + expiredAgoMinutes + " minute" + (expiredAgoMinutes == 1 ? "" : "s") + " ago";
                        } else {
                            expiredMsg = name + " finished " + expiredAgoSeconds + " second" + (expiredAgoSeconds == 1 ? "" : "s") + " ago";
                        }
                        expiredWhileAway.add(expiredMsg);
                    } else {
                        // Timer still active, restore it
                        activeTimers.add(timer);
                    }
                }

                // Send notifications for expired timers
                if (!expiredWhileAway.isEmpty()) {
                    API.SendMessage("<col=ffff00>=== Timers Expired While Away ===</col>");
                    for (String msg : expiredWhileAway) {
                        API.SendMessage("<col=ff8800>" + msg + "</col>");
                    }
                }
            }

            // Don't delete - let it get overwritten on next save

        } catch (IOException | NumberFormatException e) {
            // Silently fail - not critical
        }
    }

    // =====================================================================
    // LIFECYCLE METHODS
    // =====================================================================

    @Override
    public void Init() {
        API.SendMessage("<col=ffff00>Timer Plugin loaded!</col>");
        API.SendMessage("Auto-timers enabled for:");
        API.SendMessage("  - GWD bosses (90s respawn)");
        API.SendMessage("  - GWD altars (10min cooldown)");
        API.SendMessage("  - Potions (antifire, antipoison variants)");
        API.SendMessage("Use ::timer help for commands.");
        LoadConfig();
        loadTimerState();
    }

    @Override
    public void Draw(long timeDelta) {
        // Check for new chat messages
        checkChatMessages();

        // Check prayer altar usage
        checkPrayerAltar();

        // Update all active timers
        updateTimers();

        // Check for 30-second warnings
        check30SecondWarnings();

        // Handle alert repeats
        handleAlertRepeats();

        // Draw timer display
        drawTimers();

        // Draw stat boosts display
        drawStatBoosts();

        // Periodically save timer state
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStateSaveTime >= STATE_SAVE_INTERVAL) {
            saveTimerState();
            lastStateSaveTime = currentTime;
        }

        // Clean up dead NPC tracking
        cleanupDeadNpcs();
    }

    @Override
    public void NPCOverheadDraw(Npc npc, int screenX, int screenY) {
        if (npc == null || npc.type == null || npc.type.name == null) return;

        String npcName = npc.type.name.toString();
        long currentTime = System.currentTimeMillis();

        // Track each GWD boss separately
        if (enableBandosTimer && npcName.equalsIgnoreCase("General Graardor")) {
            lastBandosSeenTime = currentTime;
        }

        if (enableArmadylTimer && npcName.equalsIgnoreCase("Kree'arra")) {
            lastArmadylSeenTime = currentTime;
        }

        if (enableSaradominTimer && npcName.equalsIgnoreCase("Commander Zilyana")) {
            lastSaradominSeenTime = currentTime;
        }

        if (enableZamorakTimer && npcName.equalsIgnoreCase("K'ril Tsutsaroth")) {
            lastZamorakSeenTime = currentTime;
        }

        if (enableKbdTimer && npcName.equalsIgnoreCase("King Black Dragon")) {
            lastKbdSeenTime = currentTime;
        }
    }

    @Override
    public void OnLogout() {
        // Save timer state before logout
        saveTimerState();

        // Clear all boss tracking
        lastBandosSeenTime = 0;
        bandosTimerActive = false;
        lastArmadylSeenTime = 0;
        armadylTimerActive = false;
        lastSaradominSeenTime = 0;
        saradominTimerActive = false;
        lastZamorakSeenTime = 0;
        zamorakTimerActive = false;
        lastKbdSeenTime = 0;
        kbdTimerActive = false;
    }

    // =====================================================================
    // NPC DEATH DETECTION (visual tracking)
    // =====================================================================

    private void cleanupDeadNpcs() {
        long currentTime = System.currentTimeMillis();

        // Check Bandos
        if (enableBandosTimer && lastBandosSeenTime > 0 && !bandosTimerActive) {
            if (currentTime - lastBandosSeenTime > NPC_DEATH_TIMEOUT) {
                onBossDeath("General Graardor", bandosRespawnSeconds);
                bandosTimerActive = true;
                lastBandosSeenTime = 0;
            }
        }

        // Check Armadyl
        if (enableArmadylTimer && lastArmadylSeenTime > 0 && !armadylTimerActive) {
            if (currentTime - lastArmadylSeenTime > NPC_DEATH_TIMEOUT) {
                onBossDeath("Kree'arra", armadylRespawnSeconds);
                armadylTimerActive = true;
                lastArmadylSeenTime = 0;
            }
        }

        // Check Saradomin
        if (enableSaradominTimer && lastSaradominSeenTime > 0 && !saradominTimerActive) {
            if (currentTime - lastSaradominSeenTime > NPC_DEATH_TIMEOUT) {
                onBossDeath("Commander Zilyana", saradominRespawnSeconds);
                saradominTimerActive = true;
                lastSaradominSeenTime = 0;
            }
        }

        // Check Zamorak
        if (enableZamorakTimer && lastZamorakSeenTime > 0 && !zamorakTimerActive) {
            if (currentTime - lastZamorakSeenTime > NPC_DEATH_TIMEOUT) {
                onBossDeath("K'ril Tsutsaroth", zamorakRespawnSeconds);
                zamorakTimerActive = true;
                lastZamorakSeenTime = 0;
            }
        }

        if (enableKbdTimer && lastKbdSeenTime > 0 && !kbdTimerActive) {
            if (currentTime - lastKbdSeenTime > NPC_DEATH_TIMEOUT) {
                onBossDeath("King Black Dragon", kbdRespawnSeconds);
                kbdTimerActive = true;
                lastKbdSeenTime = 0;
            }
        }
    }

    private void onBossDeath(String bossName, int respawnSeconds) {
        // Start respawn timer for this boss
        String timerName = bossName + " Respawn";

        // Remove old timer if it exists and start new one
        activeTimers.removeIf(t -> t.name.equals(timerName));
        startNpcTimer(timerName, respawnSeconds);

        API.SendMessage("<col=00ff00>" + bossName + " died - respawn timer started</col>");
    }

    private void startNpcTimer(String name, int seconds) {
        activeTimers.add(new Timer(name, seconds));
        saveTimerState();
    }

    // =====================================================================
    // PRAYER ALTAR DETECTION (via chat messages)
    // =====================================================================

    private void checkPrayerAltar() {
        // Prayer altar detection now handled in handleChatMessage()
        // Kept as separate method in case we need additional logic later
    }

    private void onAltarUsed() {
        // Prevent multiple triggers in quick succession (chat message appears multiple times)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAltarUseTime < ALTAR_USE_COOLDOWN) {
            return; // Ignore, too soon after last altar use
        }
        lastAltarUseTime = currentTime;

        String timerName = "Altar Cooldown";

        // Remove existing altar timer if present
        activeTimers.removeIf(t -> t.name.equals(timerName));

        // Start new altar cooldown timer
        activeTimers.add(new Timer(timerName, altarCooldownSeconds));
        saveTimerState();
    }

    // =====================================================================
    // CHAT MESSAGE MONITORING
    // =====================================================================

    private void checkChatMessages() {
        try {
            if (Chat.size <= 0) {
                return;
            }

            // Process the 10 most recent messages to catch game messages
            int messagesToCheck = Math.min(10, Chat.size);

            for (int i = 0; i < messagesToCheck; i++) {
                if (Chat.messages[i] == null) {
                    continue;
                }

                String message = Chat.messages[i].toString();

                // For game event messages (altars, potions), we DON'T want to skip duplicates
                // because the same message legitimately appears multiple times
                // Only skip if we've seen this exact message in THIS batch of 10
                boolean skipThisMessage = false;
                for (int j = 0; j < i; j++) {
                    if (Chat.messages[j] != null && Chat.messages[j].toString().equals(message)) {
                        skipThisMessage = true; // Already processed this message in this batch
                        break;
                    }
                }

                if (skipThisMessage) continue;

                handleChatMessage(message);
            }
        } catch (Exception e) {
            // Silently fail - chat monitoring is non-critical
        }
    }

    private void handleChatMessage(String message) {
        if (message == null) return;

        // Ignore messages from this plugin to prevent loops
        if (message.startsWith("Started timer:") ||
                message.startsWith("Started custom timer:") ||
                message.contains("TIMER EXPIRED") ||
                message.contains("WARNING:") ||
                message.contains("respawn timer started")) {
            return;
        }

        String lowerMsg = message.toLowerCase();

        // Check for prayer altar usage - "You recharge your prayer points."
        if (enableAltarTimer && lowerMsg.contains("you recharge your prayer points")) {
            onAltarUsed();
            return;
        }

        // Only process "drink" messages for potions
        if (!lowerMsg.contains("drink")) {
            return;
        }

        // Detect antifire - "You drink some of your antifire."
        if (lowerMsg.contains("antifire")) {
            activeTimers.removeIf(t -> t.name.equals("Antifire"));
            startPresetTimer("Antifire", 360); // 6 minutes
            return;
        }

        // Detect antipoison++ - "You drink some of your antipoison++"
        if (lowerMsg.contains("antipoison++")) {
            activeTimers.removeIf(t -> t.name.equals("Antipoison++"));
            startPresetTimer("Antipoison++", 720); // 12 minutes
            return;
        }

        // Detect antipoison+ - "You drink some of your antipoison+"
        if (lowerMsg.contains("antipoison+")) {
            activeTimers.removeIf(t -> t.name.equals("Antipoison+"));
            startPresetTimer("Antipoison+", 540); // 9 minutes
            return;
        }

        // Detect super antipoison - "You drink some of your super antipoison."
        if (lowerMsg.contains("super antipoison")) {
            activeTimers.removeIf(t -> t.name.equals("Super Antipoison"));
            startPresetTimer("Super Antipoison", 360); // 6 minutes
            return;
        }

        // Detect regular antipoison - "You drink some of your antipoison."
        if (lowerMsg.contains("antipoison")) {
            activeTimers.removeIf(t -> t.name.equals("Antipoison"));
            startPresetTimer("Antipoison", 90); // 90 seconds
            return;
        }
    }

    @Override
    public void ProcessCommand(String commandStr, String[] args) {
        String cmd = commandStr.toLowerCase();

        // Help command
        if (cmd.equals("::timer") || cmd.equals("::timer help")) {
            showHelp();
            return;
        }

        // Start preset timers
        if (cmd.equals("::herbpatch")) {
            startPresetTimer("Herb Patch", 4800); // 80 minutes
            return;
        }

        // Start custom timer with command
        if (cmd.equals("::timer start") && args.length >= 2) {
            try {
                int seconds = Integer.parseInt(args[1]);
                String name = args.length > 2 ? String.join(" ", java.util.Arrays.copyOfRange(args, 2, args.length)) : "Timer";
                startCustomTimer(name, seconds);
            } catch (NumberFormatException e) {
                API.SendMessage("Usage: ::timer start <seconds> [name]");
            }
            return;
        }

        // Clear all timers
        if (cmd.equals("::timer clear")) {
            activeTimers.clear();
            // Reset all boss timer flags
            bandosTimerActive = false;
            armadylTimerActive = false;
            saradominTimerActive = false;
            zamorakTimerActive = false;
            saveTimerState(); // Save immediately
            API.SendMessage("All timers cleared.");
            return;
        }

        // List active timers
        if (cmd.equals("::timer list")) {
            if (activeTimers.isEmpty()) {
                API.SendMessage("No active timers.");
            } else {
                API.SendMessage("Active timers:");
                for (Timer timer : activeTimers) {
                    API.SendMessage("  " + timer.getDisplayText());
                }
            }
            return;
        }
    }

    // =====================================================================
    // KONDO CONFIG
    // =====================================================================

    public void OnKondoValueUpdated() {
        SaveConfig();

        // Start/stop preset timers based on config changes
        updatePresetTimers();
    }

    private void LoadConfig() {
        // Config auto-loads from KondoKit
        updatePresetTimers();
    }

    private void SaveConfig() {
        // Config auto-saves via KondoKit
    }

    // =====================================================================
    // TIMER MANAGEMENT
    // =====================================================================

    private void updatePresetTimers() {
        // Start herb patch timer if enabled and not already running
        if (presetHerbPatch && !hasTimerNamed("Herb Patch")) {
            startPresetTimer("Herb Patch", 4800); // 80 minutes = 4800 seconds
        }

        // Auto-start custom timers if they have text (format "Name:Seconds")
        // Check hasTimerNamed to avoid overwriting loaded timers
        if (!A.trim().isEmpty()) {
            String name = getCustomTimerName(A);
            if (!name.isEmpty() && !hasTimerNamed(name)) {
                parseAndStartCustomTimer(A);
            }
        }
        if (!B.trim().isEmpty()) {
            String name = getCustomTimerName(B);
            if (!name.isEmpty() && !hasTimerNamed(name)) {
                parseAndStartCustomTimer(B);
            }
        }
        if (!C.trim().isEmpty()) {
            String name = getCustomTimerName(C);
            if (!name.isEmpty() && !hasTimerNamed(name)) {
                parseAndStartCustomTimer(C);
            }
        }
    }

    private void parseAndStartCustomTimer(String timerConfig) {
        if (timerConfig == null || timerConfig.trim().isEmpty()) {
            return;
        }

        String[] parts = timerConfig.split(":");
        if (parts.length != 2) {
            return; // Invalid format, skip
        }

        String name = parts[0].trim();
        try {
            int seconds = Integer.parseInt(parts[1].trim());
            if (!hasTimerNamed(name)) {
                startCustomTimer(name, seconds);
            }
        } catch (NumberFormatException e) {
            // Invalid number, skip
        }
    }

    private void startPresetTimer(String name, int seconds) {
        if (!hasTimerNamed(name)) {
            activeTimers.add(new Timer(name, seconds));
            API.SendMessage("Started timer: " + name + " (" + seconds + "s)");
            saveTimerState(); // Save immediately
        }
    }

    private void startCustomTimer(String name, int seconds) {
        if (!hasTimerNamed(name)) {
            activeTimers.add(new Timer(name, seconds));
            API.SendMessage("Started custom timer: " + name + " (" + seconds + "s)");
            saveTimerState(); // Save immediately
        }
    }

    private boolean hasTimerNamed(String name) {
        for (Timer timer : activeTimers) {
            if (timer.name.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private void updateTimers() {
        Iterator<Timer> iterator = activeTimers.iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();

            // Check if timer expired and needs alert
            if (timer.isExpired() && !timer.hasAlerted) {
                triggerAlert(timer.name);
                timer.hasAlerted = true;

                // Reset boss timer flags when timers expire
                if (timer.name.equals("General Graardor Respawn")) {
                    bandosTimerActive = false;
                } else if (timer.name.equals("Kree'arra Respawn")) {
                    armadylTimerActive = false;
                } else if (timer.name.equals("Commander Zilyana Respawn")) {
                    saradominTimerActive = false;
                } else if (timer.name.equals("K'ril Tsutsaroth Respawn")) {
                    zamorakTimerActive = false;
                } else if (timer.name.equals("King Black Dragon Respawn")) {
                    kbdTimerActive = false;
                }

                // Remove timer after alerting (unless showInactiveTimers is true)
                if (!showInactiveTimers) {
                    iterator.remove();
                }
            }
        }

        // Remove inactive preset timers that are disabled
        removeDisabledPresets();
    }

    private void removeDisabledPresets() {
        Iterator<Timer> iterator = activeTimers.iterator();
        while (iterator.hasNext()) {
            Timer timer = iterator.next();

            // Only remove timers that are expired AND not configured anymore
            if (!timer.isExpired()) {
                continue; // Don't remove active timers
            }

            // Check if this timer is still configured
            String timerAName = getCustomTimerName(A);
            String timerBName = getCustomTimerName(B);
            String timerCName = getCustomTimerName(C);

            // Reactive potion timers (never remove while active, only after expiry)
            boolean isReactivePotion = timer.name.equals("Antifire") ||
                    timer.name.equals("Antipoison") ||
                    timer.name.equals("Super Antipoison") ||
                    timer.name.equals("Antipoison+") ||
                    timer.name.equals("Antipoison++");

            // Don't remove reactive potions (they stay red until re-drunk)
            if (isReactivePotion) {
                continue;
            }

            // Don't remove boss timers or altar timers (they clean themselves up)
            if (timer.name.equals("General Graardor Respawn") ||
                    timer.name.equals("Kree'arra Respawn") ||
                    timer.name.equals("Commander Zilyana Respawn") ||
                    timer.name.equals("K'ril Tsutsaroth Respawn") ||
                    timer.name.equals("King Black Dragon Respawn") ||
                    timer.name.equals("Altar Cooldown")) {
                continue;
            }

            // Check configured timers
            boolean isConfiguredPreset = (timer.name.equals("Herb Patch") && presetHerbPatch);

            boolean isConfiguredCustom = (timer.name.equals(timerAName) && !A.trim().isEmpty()) ||
                    (timer.name.equals(timerBName) && !B.trim().isEmpty()) ||
                    (timer.name.equals(timerCName) && !C.trim().isEmpty());

            boolean isAnyPreset = timer.name.equals("Herb Patch");

            boolean isAnyCustom = timer.name.equals(timerAName) ||
                    timer.name.equals(timerBName) ||
                    timer.name.equals(timerCName);

            // Remove expired timers that are no longer configured
            if (isAnyPreset && !isConfiguredPreset) {
                iterator.remove();
            } else if (isAnyCustom && !isConfiguredCustom) {
                iterator.remove();
            } else if (!isAnyPreset && !isAnyCustom && !isReactivePotion) {
                // Remove command-created timers that expired
                iterator.remove();
            }
        }
    }

    private String getCustomTimerName(String timerConfig) {
        if (timerConfig == null || timerConfig.trim().isEmpty()) {
            return "";
        }
        String[] parts = timerConfig.split(":");
        return parts.length >= 1 ? parts[0].trim() : "";
    }

    // =====================================================================
    // ALERT SYSTEM
    // =====================================================================

    private void triggerAlert(String timerName) {
        API.SendMessage("<col=ff0000>TIMER EXPIRED: " + timerName + "</col>");
        API.PlaySound(soundVolume, soundId, 0);

        // Setup repeat alerts
        alertsRemaining = ALERT_REPEAT_TIMES - 1; // Already played once
        lastAlertTime = System.currentTimeMillis();
    }

    private void handleAlertRepeats() {
        if (alertsRemaining > 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAlertTime >= ALERT_REPEAT_INTERVAL) {
                API.PlaySound(soundVolume, soundId, 0);
                alertsRemaining--;
                lastAlertTime = currentTime;
            }
        }
    }

    private void check30SecondWarnings() {
        if (!enableThirtySecondWarning) return;

        for (Timer timer : activeTimers) {
            long remaining = timer.getRemainingTime();

            // Check if timer is between 30-31 seconds and hasn't warned yet
            if (remaining > 30000 && remaining <= 31000 && !warned30Seconds.contains(timer.name)) {
                // Give 30-second warning
                API.SendMessage("<col=ffff00>WARNING: " + timer.name + " expires in 30 seconds!</col>");
                API.PlaySound(soundVolume, soundId, 0);
                warned30Seconds.add(timer.name);
            }

            // Reset warning flag if timer is above 31 seconds (in case it restarted)
            if (remaining > 31000) {
                warned30Seconds.remove(timer.name);
            }

            // Clean up warning flag when timer expires
            if (timer.isExpired()) {
                warned30Seconds.remove(timer.name);
            }
        }
    }

    // =====================================================================
    // DISPLAY
    // =====================================================================

    private void drawBox(int x, int y, int width, int height) {
        // Draw a RuneScape-themed box using FillRect and DrawRect

        // Background - dark brown semi-transparent
        int bgColor = new Color(42, 34, 26).getRGB(); // Dark brown
        API.FillRect(x, y, width, height, bgColor, 200); // 200 alpha for semi-transparency

        // Border - lighter brown for contrast
        int borderColor = new Color(100, 80, 60).getRGB(); // Medium brown
        API.DrawRect(x, y, width, height, borderColor);

        // Inner highlight for 3D effect (1px inset)
        int highlightColor = new Color(130, 105, 80).getRGB(); // Light brown
        API.DrawRect(x + 1, y + 1, width - 2, height - 2, highlightColor);
    }

    private void drawTimers() {
        if (activeTimers.isEmpty()) return;

        int lineHeight = 16;
        int padding = 20; // Vertical padding inside box
        int boxX = DISPLAY_X - 6; // Fixed horizontal offset
        int boxWidth = 120; // User preference

        // Calculate how many lines we need
        int contentLines = 1; // Header
        for (Timer timer : activeTimers) {
            boolean isReactivePotion = timer.name.equals("Antifire") ||
                    timer.name.equals("Antipoison") ||
                    timer.name.equals("Super Antipoison") ||
                    timer.name.equals("Antipoison+") ||
                    timer.name.equals("Antipoison++");

            if (showInactiveTimers || isReactivePotion || !timer.isExpired()) {
                contentLines++;
            }
        }

        int boxHeight = (contentLines * lineHeight) + (padding * 2);

        // Draw the box background starting at DISPLAY_Y
        drawBox(boxX, DISPLAY_Y, boxWidth, boxHeight);

        // Start drawing text INSIDE the box (after top padding)
        int y = DISPLAY_Y + padding;

        // Draw header
        API.DrawText(FontType.SMALL, FontColor.fromColor(Color.ORANGE), TextModifier.LEFT, "TIMERS", DISPLAY_X, y);
        y += lineHeight;

        // Draw each timer
        for (Timer timer : activeTimers) {
            boolean isReactivePotion = timer.name.equals("Antifire") ||
                    timer.name.equals("Antipoison") ||
                    timer.name.equals("Super Antipoison") ||
                    timer.name.equals("Antipoison+") ||
                    timer.name.equals("Antipoison++");

            if (!showInactiveTimers && !isReactivePotion && timer.isExpired()) {
                continue;
            }

            FontColor color;
            long remaining = timer.getRemainingTime();

            if (remaining == 0) {
                color = FontColor.fromColor(Color.RED);
            } else if (remaining < 10000) {
                color = FontColor.fromColor(new Color(255, 165, 0));
            } else if (remaining < 30000) {
                color = FontColor.YELLOW;
            } else {
                color = FontColor.fromColor(Color.GREEN);
            }

            API.DrawText(FontType.SMALL, color, TextModifier.LEFT, timer.getDisplayText(), DISPLAY_X, y);
            y += lineHeight;
        }
    }

    private void drawStatBoosts() {
        if (!showStatBoosts) return;

        // Update cached stat values every 2 seconds
        updateStatBoostsCache();

        // Draw every frame (using cached values)
        int lineHeight = 16;
        int padding = 20; // Vertical padding inside box
        int boxX = DISPLAY_X - 6; // Fixed horizontal offset
        int boxWidth = 100; // User preference

        // Calculate Y position for box start (where stat boosts box begins)
        int boxStartY = DISPLAY_Y;

        // Calculate Y position to be below timers
        if (!activeTimers.isEmpty()) {
            boxStartY += lineHeight + padding; // Timers header with padding
            for (Timer timer : activeTimers) {
                boolean isReactivePotion = timer.name.equals("Antifire") ||
                        timer.name.equals("Antipoison") ||
                        timer.name.equals("Super Antipoison") ||
                        timer.name.equals("Antipoison+") ||
                        timer.name.equals("Antipoison++");

                if (showInactiveTimers || isReactivePotion || !timer.isExpired()) {
                    boxStartY += lineHeight;
                }
            }
            boxStartY += padding + lineHeight; // Bottom padding of timers box + space between boxes
        }

        // Calculate box height
        int contentLines = 1; // Header
        if (currentStatBoosts.isEmpty()) {
            contentLines++; // "No active boosts" line
        } else {
            contentLines += currentStatBoosts.size();
        }
        int boxHeight = (contentLines * lineHeight) + (padding * 2);

        // Draw the box background
        drawBox(boxX, boxStartY, boxWidth, boxHeight);

        // Start drawing text INSIDE the box (after top padding)
        int y = boxStartY + padding;

        // Draw header
        API.DrawText(FontType.SMALL, FontColor.fromColor(Color.ORANGE), TextModifier.LEFT, "STAT BOOSTS", DISPLAY_X, y);
        y += lineHeight;

        // Draw cached stat boosts
        if (currentStatBoosts.isEmpty()) {
            API.DrawText(FontType.SMALL, FontColor.fromColor(Color.GRAY), TextModifier.LEFT, "No active boosts", DISPLAY_X, y);
        } else {
            for (String statLine : currentStatBoosts) {
                // Parse the stat line to determine color
                String[] parts = statLine.split(": ");
                if (parts.length == 2) {
                    String[] levels = parts[1].split("/");
                    if (levels.length == 2) {
                        try {
                            int boosted = Integer.parseInt(levels[0]);
                            int base = Integer.parseInt(levels[1]);

                            FontColor color = boosted > base ?
                                    FontColor.fromColor(Color.GREEN) :
                                    FontColor.fromColor(Color.RED);

                            API.DrawText(FontType.SMALL, color, TextModifier.LEFT, statLine, DISPLAY_X, y);
                            y += lineHeight;
                        } catch (NumberFormatException e) {
                            // Skip malformed line
                        }
                    }
                }
            }
        }
    }

    private void updateStatBoostsCache() {
        if (PlayerSkillXpTable.boostedLevels == null || PlayerSkillXpTable.baseLevels == null) return;

        // Only update cache every 2 seconds (not every frame)
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastStatUpdateTime < STAT_UPDATE_INTERVAL) {
            return;
        }
        lastStatUpdateTime = currentTime;

        // Clear old cached boosts
        currentStatBoosts.clear();

        // Check all combat stats for boosts (excluding Prayer and HP - already displayed elsewhere)
        String[] statNames = {"Att", "Def", "Str", "Rng", "Mag"};
        int[] statIds = {0, 1, 2, 4, 6}; // Skip 3 (HP) and 5 (Prayer)

        for (int i = 0; i < statIds.length; i++) {
            int statId = statIds[i];
            if (statId >= PlayerSkillXpTable.boostedLevels.length ||
                    statId >= PlayerSkillXpTable.baseLevels.length) {
                continue;
            }

            int boosted = PlayerSkillXpTable.boostedLevels[statId];
            int base = PlayerSkillXpTable.baseLevels[statId];

            // Only cache if boosted or drained
            if (boosted != base) {
                String statText = String.format("%s: %d/%d", statNames[i], boosted, base);
                currentStatBoosts.add(statText);
            }
        }
    }

    // =====================================================================
    // HELP COMMAND
    // =====================================================================

    private void showHelp() {
        API.SendMessage("=== Timer Plugin Commands ===");
        API.SendMessage("Reactive potions (auto-start when drunk):");
        API.SendMessage("  Antifire, Antipoison, Super Antipoison,");
        API.SendMessage("  Antipoison+, Antipoison++");
        API.SendMessage("Reactive events:");
        API.SendMessage("  GWD boss deaths (if enabled in config)");
        API.SendMessage("    - General Graardor (Bandos)");
        API.SendMessage("    - Kree'arra (Armadyl)");
        API.SendMessage("    - Commander Zilyana (Saradomin)");
        API.SendMessage("    - K'ril Tsutsaroth (Zamorak)");
        API.SendMessage("  Prayer altar use (if enabled in config)");
        API.SendMessage("Manual timers:");
        API.SendMessage("  ::herbpatch - Start 80 minute herb patch timer");
        API.SendMessage("  ::timer start <seconds> [name] - Custom timer");
        API.SendMessage("Management:");
        API.SendMessage("  ::timer list - Show all active timers");
        API.SendMessage("  ::timer clear - Clear all timers");
    }
}