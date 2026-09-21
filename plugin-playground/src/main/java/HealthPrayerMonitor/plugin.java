package HealthPrayerMonitor;

import KondoKit.Exposed;
import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.*;
import rt4.PlayerSkillXpTable;
import java.awt.Color;

@PluginMeta(
        author = "YourName",
        description = "Monitors health and prayer points and provides audio/visual alerts when they are low",
        version = 2.0
)
public class plugin extends Plugin {

    // Configurable settings - these create the UI automatically!
    @Exposed(description = "Alert when health drops to or below this value (Default: 30)")
    private int healthThreshold = 30;

    @Exposed(description = "Alert when prayer drops to or below this value (Default: 20)")
    private int prayerThreshold = 20;

    @Exposed(description = "Seconds between repeated alerts while still low (Default: 10)")
    private int alertIntervalSeconds = 10;

    @Exposed(description = "Sound ID to play for alerts (Default: 2266)")
    private int alertSoundId = 2266;

    @Exposed(description = "Sound volume 0-255 (Default: 255)")
    private int soundVolume = 255;

    @Exposed(description = "Show warning text on screen (Default: true)")
    private boolean showWarningText = true;

    @Exposed(description = "Enable health alerts (Default: true)")
    private boolean enableHealthAlerts = true;

    @Exposed(description = "Enable prayer alerts (Default: true)")
    private boolean enablePrayerAlerts = true;

    // Internal state tracking
    private boolean healthWarningActive = false;
    private boolean prayerWarningActive = false;
    private long lastHealthAlert = 0;
    private long lastPrayerAlert = 0;

    @Override
    public void Init() {
        // Load saved config values
        Object healthThresholdObj = API.GetData("hp-mon-health-threshold");
        if (healthThresholdObj instanceof Integer) {
            healthThreshold = (Integer) healthThresholdObj;
        }

        Object prayerThresholdObj = API.GetData("hp-mon-prayer-threshold");
        if (prayerThresholdObj instanceof Integer) {
            prayerThreshold = (Integer) prayerThresholdObj;
        }

        Object intervalObj = API.GetData("hp-mon-alert-interval");
        if (intervalObj instanceof Integer) {
            alertIntervalSeconds = (Integer) intervalObj;
        }

        Object soundIdObj = API.GetData("hp-mon-sound-id");
        if (soundIdObj instanceof Integer) {
            alertSoundId = (Integer) soundIdObj;
        }

        Object volumeObj = API.GetData("hp-mon-sound-volume");
        if (volumeObj instanceof Integer) {
            soundVolume = (Integer) volumeObj;
        }

        Object showTextObj = API.GetData("hp-mon-show-text");
        if (showTextObj instanceof Boolean) {
            showWarningText = (Boolean) showTextObj;
        }

        Object enableHealthObj = API.GetData("hp-mon-enable-health");
        if (enableHealthObj instanceof Boolean) {
            enableHealthAlerts = (Boolean) enableHealthObj;
        }

        Object enablePrayerObj = API.GetData("hp-mon-enable-prayer");
        if (enablePrayerObj instanceof Boolean) {
            enablePrayerAlerts = (Boolean) enablePrayerObj;
        }

        API.SendMessage("Health & Prayer Monitor loaded!");
        API.SendMessage("Configure settings in the plugin panel");
        API.SendMessage("Type ::hpmontest to test alerts");
    }

    public void OnKondoValueUpdated() {
        // Save config when user changes settings in UI
        API.StoreData("hp-mon-health-threshold", healthThreshold);
        API.StoreData("hp-mon-prayer-threshold", prayerThreshold);
        API.StoreData("hp-mon-alert-interval", alertIntervalSeconds);
        API.StoreData("hp-mon-sound-id", alertSoundId);
        API.StoreData("hp-mon-sound-volume", soundVolume);
        API.StoreData("hp-mon-show-text", showWarningText);
        API.StoreData("hp-mon-enable-health", enableHealthAlerts);
        API.StoreData("hp-mon-enable-prayer", enablePrayerAlerts);
    }

    @Override
    public void Draw(long timeDelta) {
        if (!API.IsLoggedIn()) return;

        // Get current health and prayer levels
        int currentHealth = PlayerSkillXpTable.boostedLevels[3];  // Hitpoints = skill 3
        int currentPrayer = PlayerSkillXpTable.boostedLevels[5];  // Prayer = skill 5
        int maxHealth = PlayerSkillXpTable.baseLevels[3];
        int maxPrayer = PlayerSkillXpTable.baseLevels[5];

        long now = System.currentTimeMillis();
        long alertInterval = alertIntervalSeconds * 1000L;  // Convert to milliseconds

        /* ---------- HEALTH ALERTS ---------- */
        if (enableHealthAlerts && currentHealth > 0 && currentHealth <= healthThreshold) {
            // Fire alert if: first time entering low HP OR interval elapsed
            if (!healthWarningActive || now - lastHealthAlert >= alertInterval) {
                API.PlaySound(soundVolume, alertSoundId, 0);
                API.SendMessage("<col=ff0000>WARNING: Low health! (" + currentHealth + "/" + maxHealth + ")</col>");
                lastHealthAlert = now;
                healthWarningActive = true;
            }

            // Draw on-screen warning text
            if (showWarningText) {
                API.DrawText(
                        FontType.LARGE,
                        FontColor.fromColor(Color.RED),
                        TextModifier.CENTER,
                        "!!! LOW HEALTH: " + currentHealth + " !!!",
                        API.GetWindowDimensions().width / 2,
                        100
                );
            }
        } else {
            // Reset when health recovers
            healthWarningActive = false;
            lastHealthAlert = 0;
        }

        /* ---------- PRAYER ALERTS ---------- */
        if (enablePrayerAlerts && currentPrayer > 0 && currentPrayer <= prayerThreshold) {
            // Fire alert if: first time entering low prayer OR interval elapsed
            if (!prayerWarningActive || now - lastPrayerAlert >= alertInterval) {
                API.PlaySound(soundVolume, alertSoundId, 0);
                API.SendMessage("<col=00ffff>WARNING: Low prayer! (" + currentPrayer + "/" + maxPrayer + ")</col>");
                lastPrayerAlert = now;
                prayerWarningActive = true;
            }

            // Draw on-screen warning text
            if (showWarningText) {
                int warningY = (enableHealthAlerts && currentHealth > 0 && currentHealth <= healthThreshold) ? 120 : 100;
                API.DrawText(
                        FontType.LARGE,
                        FontColor.fromColor(Color.CYAN),
                        TextModifier.CENTER,
                        "!!! LOW PRAYER: " + currentPrayer + " !!!",
                        API.GetWindowDimensions().width / 2,
                        warningY
                );
            }
        } else {
            // Reset when prayer recovers
            prayerWarningActive = false;
            lastPrayerAlert = 0;
        }
    }

    @Override
    public void ProcessCommand(String commandStr, String[] args) {
        if (commandStr.equalsIgnoreCase("::hpmontest")) {
            API.SendMessage("Testing Health & Prayer Monitor...");
            API.SendMessage("Sound ID: " + alertSoundId + " | Volume: " + soundVolume);
            API.PlaySound(soundVolume, alertSoundId, 0);
            API.SendMessage("<col=ff0000>This is a health warning test</col>");
            API.SendMessage("<col=00ffff>This is a prayer warning test</col>");
        }

        if (commandStr.equalsIgnoreCase("::hpmonstatus")) {
            API.SendMessage("=== Health & Prayer Monitor Status ===");
            API.SendMessage("Health Threshold: " + healthThreshold);
            API.SendMessage("Prayer Threshold: " + prayerThreshold);
            API.SendMessage("Alert Interval: " + alertIntervalSeconds + " seconds");
            API.SendMessage("Sound ID: " + alertSoundId);
            API.SendMessage("Volume: " + soundVolume);
            API.SendMessage("Show Text: " + showWarningText);
            API.SendMessage("Health Alerts: " + (enableHealthAlerts ? "ON" : "OFF"));
            API.SendMessage("Prayer Alerts: " + (enablePrayerAlerts ? "ON" : "OFF"));
        }
    }

    @Override
    public void OnLogout() {
        // Reset all state on logout
        healthWarningActive = false;
        prayerWarningActive = false;
        lastHealthAlert = 0;
        lastPrayerAlert = 0;
    }
}