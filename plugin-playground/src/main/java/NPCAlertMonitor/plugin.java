package NPCAlertMonitor;

import KondoKit.Exposed;
import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.*;
import rt4.Npc;
import rt4.NpcList;
import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

@PluginMeta(
        author = "YourName",
        description = "Alerts when specific NPCs appear nearby (random events, dangerous NPCs, etc.)",
        version = 1.0
)
public class plugin extends Plugin {

    // Common Random Events & Dangerous NPCs to monitor
    @Exposed(description = "Alert on Evil Chicken (random event)")
    private boolean alertEvilChicken = true;

    @Exposed(description = "Alert on Rock Golem (random event)")
    private boolean alertRockGolem = true;

    @Exposed(description = "Alert on River Troll (random event)")
    private boolean alertRiverTroll = true;

    @Exposed(description = "Alert on Zombie (random event)")
    private boolean alertZombie = false;

    @Exposed(description = "Alert on Shade (random event)")
    private boolean alertShade = false;

    @Exposed(description = "Alert on Tree spirit (random event)")
    private boolean alertTreeSpirit = true;

    @Exposed(description = "Alert on Swarm (random event)")
    private boolean alertSwarm = false;

    // Test NPCs (common ones for testing)
    @Exposed(description = "Alert on Chicken (for testing)")
    private boolean alertChicken = false;

    @Exposed(description = "Alert on Cow (for testing)")
    private boolean alertCow = false;

    @Exposed(description = "Alert on Goblin (for testing)")
    private boolean alertGoblin = false;

    @Exposed(description = "Alert on Guard (for testing)")
    private boolean alertGuard = false;

    // Alert settings
    @Exposed(description = "Sound ID to play for alerts (Default: 2863)")
    private int alertSoundId = 2863;

    @Exposed(description = "Sound volume 0-255 (Default: 255)")
    private int soundVolume = 255;

    @Exposed(description = "Show warning text on screen (Default: true)")
    private boolean showWarningText = true;

    @Exposed(description = "Repeat alert every X seconds while NPC is nearby (Default: 5)")
    private int alertIntervalSeconds = 5;

    @Exposed(description = "Debug mode - show all nearby NPC names in chat (Default: false)")
    private boolean debugMode = false;

    // Internal state tracking
    private Set<String> activeAlerts = new HashSet<>();
    private long lastAlertTime = 0;
    private long lastDebugTime = 0;

    @Override
    public void Init() {
        // Load saved config values
        loadConfig();

        API.SendMessage("NPC Alert Monitor loaded!");
        API.SendMessage("Configure alerts in the plugin panel");
        API.SendMessage("Type ::npctest to test alert sound");
    }

    private void loadConfig() {
        // Load all boolean settings
        Object obj;

        obj = API.GetData("npc-alert-evil-chicken");
        if (obj instanceof Boolean) alertEvilChicken = (Boolean) obj;

        obj = API.GetData("npc-alert-rock-golem");
        if (obj instanceof Boolean) alertRockGolem = (Boolean) obj;

        obj = API.GetData("npc-alert-river-troll");
        if (obj instanceof Boolean) alertRiverTroll = (Boolean) obj;

        obj = API.GetData("npc-alert-zombie");
        if (obj instanceof Boolean) alertZombie = (Boolean) obj;

        obj = API.GetData("npc-alert-shade");
        if (obj instanceof Boolean) alertShade = (Boolean) obj;

        obj = API.GetData("npc-alert-tree-spirit");
        if (obj instanceof Boolean) alertTreeSpirit = (Boolean) obj;

        obj = API.GetData("npc-alert-swarm");
        if (obj instanceof Boolean) alertSwarm = (Boolean) obj;

        obj = API.GetData("npc-alert-chicken");
        if (obj instanceof Boolean) alertChicken = (Boolean) obj;

        obj = API.GetData("npc-alert-cow");
        if (obj instanceof Boolean) alertCow = (Boolean) obj;

        obj = API.GetData("npc-alert-goblin");
        if (obj instanceof Boolean) alertGoblin = (Boolean) obj;

        obj = API.GetData("npc-alert-guard");
        if (obj instanceof Boolean) alertGuard = (Boolean) obj;

        obj = API.GetData("npc-alert-sound-id");
        if (obj instanceof Integer) alertSoundId = (Integer) obj;

        obj = API.GetData("npc-alert-sound-volume");
        if (obj instanceof Integer) soundVolume = (Integer) obj;

        obj = API.GetData("npc-alert-show-text");
        if (obj instanceof Boolean) showWarningText = (Boolean) obj;

        obj = API.GetData("npc-alert-interval");
        if (obj instanceof Integer) alertIntervalSeconds = (Integer) obj;

        obj = API.GetData("npc-alert-debug");
        if (obj instanceof Boolean) debugMode = (Boolean) obj;
    }

    public void OnKondoValueUpdated() {
        // Save config when user changes settings in UI
        API.StoreData("npc-alert-evil-chicken", alertEvilChicken);
        API.StoreData("npc-alert-rock-golem", alertRockGolem);
        API.StoreData("npc-alert-river-troll", alertRiverTroll);
        API.StoreData("npc-alert-zombie", alertZombie);
        API.StoreData("npc-alert-shade", alertShade);
        API.StoreData("npc-alert-tree-spirit", alertTreeSpirit);
        API.StoreData("npc-alert-swarm", alertSwarm);
        API.StoreData("npc-alert-chicken", alertChicken);
        API.StoreData("npc-alert-cow", alertCow);
        API.StoreData("npc-alert-goblin", alertGoblin);
        API.StoreData("npc-alert-guard", alertGuard);
        API.StoreData("npc-alert-sound-id", alertSoundId);
        API.StoreData("npc-alert-sound-volume", soundVolume);
        API.StoreData("npc-alert-show-text", showWarningText);
        API.StoreData("npc-alert-interval", alertIntervalSeconds);
        API.StoreData("npc-alert-debug", debugMode);
    }

    @Override
    public void NPCOverheadDraw(Npc npc, int screenX, int screenY) {
        if (npc == null || npc.type == null || npc.type.name == null) return;

        String npcName = npc.type.name.toString();

        // Debug - show the NPC name above it
        if (debugMode) {
            API.DrawText(
                    FontType.SMALL,
                    FontColor.YELLOW,
                    TextModifier.CENTER,
                    npcName,
                    screenX,
                    screenY - 20
            );
        }

        // Check if we should alert on this NPC
        if (shouldAlertOnNPC(npcName)) {
            long now = System.currentTimeMillis();
            long alertInterval = alertIntervalSeconds * 1000L;

            if (!activeAlerts.contains(npcName) || now - lastAlertTime >= alertInterval) {
                triggerAlert(npcName);
                activeAlerts.add(npcName);
                lastAlertTime = now;
            }
        }
    }

    private boolean shouldAlertOnNPC(String npcName) {
        String nameLower = npcName.toLowerCase();

        // Random Events
        if (alertEvilChicken && nameLower.contains("evil chicken")) return true;
        if (alertRockGolem && nameLower.contains("rock golem")) return true;
        if (alertRiverTroll && nameLower.contains("river troll")) return true;
        if (alertZombie && nameLower.contains("zombie")) return true;
        if (alertShade && nameLower.contains("shade")) return true;
        if (alertTreeSpirit && nameLower.contains("tree spirit")) return true;
        if (alertSwarm && nameLower.contains("swarm")) return true;

        // Test NPCs
        if (alertChicken && nameLower.contains("chicken") && !nameLower.contains("evil")) return true;
        if (alertCow && nameLower.contains("cow")) return true;
        if (alertGoblin && nameLower.contains("goblin")) return true;
        if (alertGuard && nameLower.contains("guard")) return true;

        return false;
    }

    private void triggerAlert(String npcName) {
        API.PlaySound(soundVolume, alertSoundId, 0);
        API.SendMessage("<col=ff0000>!!! WARNING: " + npcName + " detected nearby! !!!</col>");
    }

    private void showNearbyNPCs() {
        API.SendMessage("=== Nearby NPCs (Debug) ===");
        Set<String> uniqueNPCs = new HashSet<>();

        for (int i = 0; i < NpcList.size; i++) {
            Npc npc = NpcList.npcs[i];
            if (npc != null && npc.type != null && npc.type.name != null) {
                String npcName = npc.type.name.toString();
                if (!npcName.equals("null") && !uniqueNPCs.contains(npcName)) {
                    uniqueNPCs.add(npcName);
                    API.SendMessage("- " + npcName);
                }
            }
        }

        if (uniqueNPCs.isEmpty()) {
            API.SendMessage("No NPCs nearby");
        }
    }

    @Override
    public void ProcessCommand(String commandStr, String[] args) {
        if (commandStr.equalsIgnoreCase("::npctest")) {
            API.SendMessage("Testing NPC Alert...");
            API.SendMessage("Sound ID: " + alertSoundId + " | Volume: " + soundVolume);
            API.PlaySound(soundVolume, alertSoundId, 0);
            API.SendMessage("<col=ff0000>!!! This is an NPC alert test !!!</col>");
        }

        if (commandStr.equalsIgnoreCase("::npclist")) {
            debugMode = true;
            showNearbyNPCs();
            debugMode = false;
        }

        if (commandStr.equalsIgnoreCase("::npcstatus")) {
            API.SendMessage("=== NPC Alert Monitor Status ===");
            API.SendMessage("Evil Chicken: " + (alertEvilChicken ? "ON" : "OFF"));
            API.SendMessage("Rock Golem: " + (alertRockGolem ? "ON" : "OFF"));
            API.SendMessage("River Troll: " + (alertRiverTroll ? "ON" : "OFF"));
            API.SendMessage("Chicken (test): " + (alertChicken ? "ON" : "OFF"));
            API.SendMessage("Cow (test): " + (alertCow ? "ON" : "OFF"));
            API.SendMessage("Goblin (test): " + (alertGoblin ? "ON" : "OFF"));
            API.SendMessage("Sound ID: " + alertSoundId);
            API.SendMessage("Alert Interval: " + alertIntervalSeconds + " seconds");
            API.SendMessage("Debug Mode: " + (debugMode ? "ON" : "OFF"));
        }
    }

    @Override
    public void OnLogout() {
        // Clear all state on logout
        activeAlerts.clear();
        lastAlertTime = 0;
        lastDebugTime = 0;
    }

}