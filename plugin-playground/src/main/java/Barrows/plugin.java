package Barrows;

import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.API;
import plugin.api.FontColor;
import plugin.api.FontType;
import plugin.api.TextModifier;
import KondoKit.Exposed;
import rt4.Chat;
import rt4.Npc;
import rt4.VarpDomain;

import java.awt.*;
import java.io.*;
import java.util.HashMap;
import java.util.Map;

@PluginMeta(
        author = "YourName",
        description = "Tracks Barrows brothers killed during a run",
        version = 1.0
)
public class plugin extends Plugin {

    // Brother names
    private static final String[] BROTHER_NAMES = {
            "Ahrim the Blighted",
            "Dharok the Wretched",
            "Guthan the Infested",
            "Karil the Tainted",
            "Torag the Corrupted",
            "Verac the Defiled"
    };

    // Track which brothers are killed
    private Map<String, Boolean> brothersKilled = new HashMap<>();

    // UI settings
    @Exposed
    public boolean showTracker = true;

    @Exposed
    public int windowX = 15;

    @Exposed
    public boolean autoShow = true;

    @Exposed
    public boolean compactMode = true;

    private boolean trackerVisible = false;
    private int brothersKilledCount = 0;

    // State persistence
    private static final String STATE_FILE = System.getProperty("user.home") + File.separator + "2009scape" + File.separator + "barrows_state.dat";

    // Track the on-screen killcount display (the accurate one)
    private int displayedKillcount = 0;
    private int lastCheckedKillcount = 0;
    private int lastProcessedKillcount = -1;
    private boolean ignoreKillcountUntilZero = false;

    // Combat tracking
    private Map<String, Long> lastInCombatTime = new HashMap<>();
    private Map<String, Boolean> deathTimerActive = new HashMap<>();

    @Override
    public void Init() {
        LoadConfig();
        loadState();
        API.SendMessage("<col=ffff00>Barrows Tracker loaded! Use ::barrows for commands</col>");
    }

    private void saveState() {
        try {
            File stateFile = new File(STATE_FILE);
            stateFile.getParentFile().mkdirs();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(stateFile))) {
                writer.write(System.currentTimeMillis() + "\n");
                writer.write(lastCheckedKillcount + "\n");

                for (String brother : BROTHER_NAMES) {
                    boolean killed = brothersKilled.getOrDefault(brother, false);
                    writer.write(brother + "," + killed + "\n");
                }

                writer.write("COMBAT_HISTORY\n");
                for (Map.Entry<String, Long> entry : lastInCombatTime.entrySet()) {
                    writer.write(entry.getKey() + "," + entry.getValue() + "\n");
                }

                // DON'T save DEATH_TIMERS - they should reset each session
            }
        } catch (IOException e) {
            // Silently fail
        }
    }

    private void loadState() {
        try {
            File stateFile = new File(STATE_FILE);
            if (!stateFile.exists()) {
                return;
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(stateFile))) {
                String timestampStr = reader.readLine();
                if (timestampStr == null) return;

                long savedTime = Long.parseLong(timestampStr);
                long currentTime = System.currentTimeMillis();
                long timePassed = currentTime - savedTime;

                if (timePassed > 86400000) {
                    API.SendMessage("<col=ffff00>Barrows state too old (24h+), starting fresh</col>");
                    return;
                }

                String kcStr = reader.readLine();
                if (kcStr != null) {
                    lastCheckedKillcount = Integer.parseInt(kcStr);
                }

                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("COMBAT_HISTORY")) break;

                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String brother = parts[0];
                        boolean killed = Boolean.parseBoolean(parts[1]);
                        brothersKilled.put(brother, killed);
                        if (killed) brothersKilledCount++;
                    }
                }

                while ((line = reader.readLine()) != null) {
                    if (line.equals("DEATH_TIMERS")) break;

                    String[] parts = line.split(",");
                    if (parts.length == 2) {
                        String brother = parts[0];
                        long timestamp = Long.parseLong(parts[1]);
                        lastInCombatTime.put(brother, timestamp);
                    }
                }

                // Skip DEATH_TIMERS section if it exists (legacy)
                // deathTimerActive always starts empty

                if (brothersKilledCount > 0) {
                    trackerVisible = true;
                    API.SendMessage("<col=00ff00>Barrows state restored: " + brothersKilledCount + "/6 brothers</col>");
                }
            }
        } catch (IOException | NumberFormatException e) {
            // Silently fail
        }
    }

    public void OnLogout() {
        saveState();
    }

    public void ComponentDraw(int componentIndex, rt4.Component component, int screenX, int screenY) {
        if (component != null && component.text != null) {
            String text = component.text.toString();

            if (text.contains("Kill Count") || text.contains("killcount")) {
                try {
                    String[] parts = text.split(":");
                    if (parts.length >= 2) {
                        String numberPart = parts[1].trim();
                        int kc = Integer.parseInt(numberPart);
                        // Ignore stale killcount after reset until it returns to 0
                        if (ignoreKillcountUntilZero) {
                            if (kc == 0) {
                                ignoreKillcountUntilZero = false;
                                lastCheckedKillcount = 0;
                                lastProcessedKillcount = 0;
                            }
                            return;
                        }

                        if (kc == lastProcessedKillcount) {
                            return;
                        }

                        if (kc > lastCheckedKillcount) {
                            int kills = kc - lastCheckedKillcount;

                            for (int i = 0; i < kills; i++) {
                                checkForNewKill();
                            }

                            lastCheckedKillcount = kc;
                        }

                        lastProcessedKillcount = kc;
                        displayedKillcount = kc;
                    }
                } catch (Exception e) {
                    // Failed to parse - ignore
                }
            }
        }
    }

    @Override
    public void Draw(long timeDelta) {
        lastProcessedKillcount = -1;
        checkChatMessages();

        if (!showTracker || !trackerVisible) {
            return;
        }

        drawTrackerWindow();
    }

    private void checkForNewKill() {
        String mostRecentBrother = null;
        long mostRecentTime = 0;

        for (String brother : BROTHER_NAMES) {
            Boolean alreadyDead = deathTimerActive.getOrDefault(brother, false);
            Long lastCombat = lastInCombatTime.get(brother);

            if (!alreadyDead && lastCombat != null) {
                if (lastCombat > mostRecentTime) {
                    mostRecentTime = lastCombat;
                    mostRecentBrother = brother;
                }
            }
        }

        if (mostRecentBrother != null) {
            onBrotherDeath(mostRecentBrother);
            deathTimerActive.put(mostRecentBrother, true);
        } else {
            // DEBUG: Show why no brother was found
            API.SendMessage("<col=ff8800>[DEBUG] No valid brother. CombatHist=" + lastInCombatTime.size() + " DeadTimer=" + deathTimerActive.size() + "</col>");
        }
    }

    public void OnXPUpdate(int skillId, int xp) {
        // Not needed
    }

    @Override
    public void NPCOverheadDraw(Npc npc, int screenX, int screenY) {
        if (npc == null || npc.type == null || npc.type.name == null) return;

        String npcName = npc.type.name.toString();
        long currentTime = System.currentTimeMillis();

        for (String brother : BROTHER_NAMES) {
            if (npcName.equals(brother)) {
                if (npc.faceEntity != -1) {
                    lastInCombatTime.put(brother, currentTime);
                }
                break;
            }
        }
    }

    private int getBarrowsKillcount() {
        try {
            int varpValue = VarpDomain.varp[453];
            int count = 0;
            int temp = varpValue;
            while (temp != 0) {
                count += temp & 1;
                temp >>>= 1;
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }

    private void onBrotherDeath(String brother) {
        if (!brothersKilled.getOrDefault(brother, false)) {
            brothersKilled.put(brother, true);
            brothersKilledCount++;

            if (autoShow && brothersKilledCount == 1) {
                trackerVisible = true;
            }

            API.SendMessage("<col=00ff00>" + getShortName(brother) + " defeated! (" +
                    brothersKilledCount + "/6)</col>");

            if (brothersKilledCount == 6) {
                API.SendMessage("<col=ffff00>All brothers defeated! Loot the chest!</col>");
            }

            saveState();
        }
    }

    private void checkChatMessages() {
        try {
            if (Chat.size <= 0) {
                return;
            }

            int messagesToCheck = Math.min(10, Chat.size);

            for (int i = 0; i < messagesToCheck; i++) {
                if (Chat.messages[i] == null) {
                    continue;
                }

                String message = Chat.messages[i].toString();
                String lowerMsg = message.toLowerCase();

                if (lowerMsg.contains("your barrows chest count is:") ||
                        lowerMsg.contains("your chest count is") ||
                        lowerMsg.contains("chest count")) {

                    if (brothersKilledCount > 0) {
                        API.SendMessage("<col=ffff00>Barrows run completed! Tracker reset.</col>");
                        resetTracker();

                        if (autoShow) {
                            trackerVisible = false;
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            // Silently fail
        }
    }

    private void drawTrackerWindow() {
        int width = compactMode ? 60 : 110;
        int lineHeight = compactMode ? 14 : 16;
        int padding = compactMode ? 6 : 12;

        int contentLines = BROTHER_NAMES.length;
        if (!compactMode) contentLines++;

        int height = (contentLines * lineHeight) + (padding * 2);

        int boxX = windowX;

        int screenHeight = 503;
        try {
            screenHeight = rt4.GameShell.canvasHeight;
        } catch (Exception e) {
            // Use default
        }

        int boxY = screenHeight - 237 - height;

        if (boxY < 50) boxY = 50;
        if (boxY > screenHeight - height - 10) boxY = screenHeight - height - 10;

        int bgColor = new Color(20, 18, 16, 240).getRGB();
        API.FillRect(boxX, boxY, width, height, bgColor, 240);

        int borderColor = new Color(180, 140, 60).getRGB();
        API.DrawRect(boxX, boxY, width, height, borderColor);

        int shadowColor = new Color(10, 10, 10, 200).getRGB();
        API.DrawRect(boxX + 1, boxY + 1, width - 2, height - 2, shadowColor);

        int y = boxY + padding + 10;
        int textX = boxX + (compactMode ? 8 : 10);

        for (String brother : BROTHER_NAMES) {
            boolean killed = brothersKilled.getOrDefault(brother, false);
            String shortName = getShortName(brother);

            int iconX = textX;
            int iconY = y - 7;

            if (killed) {
                int skullColor = new Color(100, 255, 100).getRGB();
                API.FillRect(iconX + 1, iconY, 5, 1, skullColor, 255);
                API.FillRect(iconX, iconY + 1, 7, 3, skullColor, 255);
                API.FillRect(iconX + 1, iconY + 4, 5, 1, skullColor, 255);

                int eyeColor = new Color(0, 0, 0).getRGB();
                API.FillRect(iconX + 1, iconY + 2, 2, 1, eyeColor, 255);
                API.FillRect(iconX + 4, iconY + 2, 2, 1, eyeColor, 255);

                API.FillRect(iconX + 2, iconY + 5, 3, 2, skullColor, 255);

                Color nameColor = new Color(220, 220, 220);
                API.DrawText(FontType.SMALL, FontColor.fromColor(nameColor), TextModifier.LEFT,
                        shortName, textX + 10, y);
            } else {
                int tombColor = new Color(140, 50, 50).getRGB();
                API.FillRect(iconX + 2, iconY, 3, 1, tombColor, 255);
                API.FillRect(iconX + 1, iconY + 1, 5, 1, tombColor, 255);
                API.FillRect(iconX + 1, iconY + 2, 5, 5, tombColor, 255);

                int crossColor = new Color(80, 30, 30).getRGB();
                API.FillRect(iconX + 3, iconY + 3, 1, 3, crossColor, 255);
                API.FillRect(iconX + 2, iconY + 4, 3, 1, crossColor, 255);

                Color nameColor = new Color(100, 100, 100);
                API.DrawText(FontType.SMALL, FontColor.fromColor(nameColor), TextModifier.LEFT,
                        shortName, textX + 10, y);
            }

            y += lineHeight;
        }

        if (!compactMode) {
            String footer = brothersKilledCount + " / 6 killed";
            API.DrawText(FontType.SMALL, FontColor.fromColor(Color.WHITE), TextModifier.LEFT,
                    footer, textX, y);
        }
    }

    private String getShortName(String fullName) {
        return fullName.split(" ")[0];
    }

    private void resetTracker() {
        brothersKilled.clear();
        for (String brother : BROTHER_NAMES) {
            brothersKilled.put(brother, false);
        }

        brothersKilledCount = 0;

        lastInCombatTime.clear();
        deathTimerActive.clear();

        displayedKillcount = 0;
        lastCheckedKillcount = 0;
        lastProcessedKillcount = -1;

        // Prevent stale killcount re-processing after chest
        ignoreKillcountUntilZero = true;

        saveState();
    }

    public void ProcessCommand(String commandStr, String[] args) {
        if (commandStr.equalsIgnoreCase("::barrows")) {
            if (args.length == 0) {
                API.SendMessage("<col=ffff00>=== BARROWS TRACKER ===</col>");
                API.SendMessage("Status: " + (showTracker ? "ENABLED" : "DISABLED"));
                API.SendMessage("Window visible: " + (trackerVisible ? "YES" : "NO"));
                API.SendMessage("Brothers killed: " + brothersKilledCount + "/6");
                API.SendMessage("");
                API.SendMessage("<col=ffffff>Commands:</col>");
                API.SendMessage("::barrows toggle - Toggle tracker on/off");
                API.SendMessage("::barrows show - Show tracker window");
                API.SendMessage("::barrows hide - Hide tracker window");
                API.SendMessage("::barrows reset - Reset current run");
                API.SendMessage("::barrows status - Show current progress");
            } else {
                String subCommand = args[0].toLowerCase();

                switch (subCommand) {
                    case "toggle":
                        showTracker = !showTracker;
                        API.SendMessage("<col=ffff00>Barrows tracker " +
                                (showTracker ? "enabled" : "disabled") + "</col>");
                        SaveConfig();
                        break;

                    case "show":
                        trackerVisible = true;
                        API.SendMessage("<col=00ff00>Tracker window shown</col>");
                        break;

                    case "hide":
                        trackerVisible = false;
                        API.SendMessage("<col=ffff00>Tracker window hidden</col>");
                        break;

                    case "reset":
                        resetTracker();
                        API.SendMessage("<col=ffff00>Barrows tracker reset</col>");
                        break;

                    case "status":
                        API.SendMessage("<col=ffff00>Barrows Progress:</col>");
                        for (String brother : BROTHER_NAMES) {
                            boolean killed = brothersKilled.getOrDefault(brother, false);
                            String status = killed ? "<col=00ff00>[KILLED]</col>" : "<col=ff0000>[ALIVE]</col>";
                            API.SendMessage(status + " " + getShortName(brother));
                        }
                        API.SendMessage("<col=ffffff>Total: " + brothersKilledCount + "/6</col>");
                        break;

                    default:
                        API.SendMessage("<col=ff0000>Unknown command. Use ::barrows for help</col>");
                        break;
                }
            }
        } else if (commandStr.equalsIgnoreCase("::barrowsdebug")) {
            API.SendMessage("<col=ffff00>=== BARROWS DEBUG ===</col>");
            API.SendMessage("showTracker: " + showTracker);
            API.SendMessage("trackerVisible: " + trackerVisible);
            API.SendMessage("autoShow: " + autoShow);
            API.SendMessage("Displayed KC: " + displayedKillcount);
            API.SendMessage("Tracked count: " + brothersKilledCount);
            API.SendMessage("Combat history: " + lastInCombatTime.size());
            API.SendMessage("Death timers: " + deathTimerActive.size());

            // Show which brothers are in each map
            API.SendMessage("<col=ffffff>Combat history contains:</col>");
            for (String brother : BROTHER_NAMES) {
                if (lastInCombatTime.containsKey(brother)) {
                    API.SendMessage("  " + getShortName(brother));
                }
            }
            API.SendMessage("<col=ffffff>Death timers contains:</col>");
            for (String brother : BROTHER_NAMES) {
                if (deathTimerActive.getOrDefault(brother, false)) {
                    API.SendMessage("  " + getShortName(brother));
                }
            }
        } else if (commandStr.equalsIgnoreCase("::barrowstest")) {
            API.SendMessage("<col=ffff00>Simulating kill...</col>");
            for (String brother : BROTHER_NAMES) {
                if (!brothersKilled.getOrDefault(brother, false)) {
                    onBrotherDeath(brother);
                    deathTimerActive.put(brother, true);
                    API.SendMessage("<col=00ff00>Test: Marked " + getShortName(brother) + " as killed</col>");
                    break;
                }
            }
        }
    }

    public void OnKondoValueUpdated() {
        SaveConfig();
    }

    private void LoadConfig() {
        if (windowX < 0) windowX = 15;
    }

    private void SaveConfig() {
        // Auto-saves via KondoKit
    }
}