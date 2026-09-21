package GWDBrightness;

import plugin.Plugin;
import plugin.annotations.PluginMeta;
import plugin.api.API;
import rt4.Preferences;
import rt4.FogManager;
import rt4.GlRenderer;
import com.jogamp.opengl.GL2;
import KondoKit.Exposed;

@PluginMeta(
        author = "YourName",
        description = "Aggressive brightness - forces lighting changes every frame",
        version = 1.0
)
public class plugin extends Plugin {

    @Exposed
    public boolean enableBrightness = false;

    @Exposed
    public float ambientMultiplier = 3.0f;  // How much to boost ambient (1.0-10.0)

    @Exposed
    public boolean useBrightFog = true;  // Use bright white fog

    private int originalGameBrightness = 3;
    private boolean isCurrentlyBoosted = false;
    private int frameCounter = 0;

    @Override
    public void Init() {
        LoadConfig();
        originalGameBrightness = Preferences.brightness;

        API.SendMessage("<col=ffff00>Aggressive Brightness Plugin loaded!</col>");
        API.SendMessage("Forces lighting changes every frame");
        API.SendMessage("Ambient multiplier: " + ambientMultiplier + "x");
    }

    @Override
    public void Draw(long timeDelta) {
        if (enableBrightness && !isCurrentlyBoosted) {
            originalGameBrightness = Preferences.brightness;
            Preferences.brightness = 4;
            isCurrentlyBoosted = true;
            API.SendMessage("<col=00ff00>Aggressive brightness enabled</col>");
        } else if (!enableBrightness && isCurrentlyBoosted) {
            Preferences.brightness = originalGameBrightness;
            isCurrentlyBoosted = false;
            API.SendMessage("<col=ffff00>Brightness disabled</col>");
        }

        // Apply brightness adjustments EVERY SINGLE FRAME
        if (enableBrightness) {
            frameCounter++;
            forceMaxBrightness();
        }
    }

    private void forceMaxBrightness() {
        try {
            // 1. Force fog to bright white
            if (useBrightFog) {
                FogManager.setFogParams(0xFFFFFF, -1000);  // Bright white fog, very close
            }

            // 2. Force lighting to maximum
            float baseAmbient = 1.1523438f;
            float boostedAmbient = baseAmbient * ambientMultiplier;
            if (boostedAmbient > 10.0f) boostedAmbient = 10.0f;

            FogManager.setLightParams(
                    0xFFFFFF,           // Pure white light color
                    boostedAmbient,     // Boosted ambient
                    2.0f,               // Max diffuse light 0
                    2.0f                // Max diffuse light 1
            );

            // 3. Force OpenGL ambient lighting directly (in case FogManager gets overwritten)
            if (GlRenderer.enabled && GlRenderer.gl != null) {
                GL2 gl = GlRenderer.gl;

                // Set global ambient to bright
                float ambient = Math.min(ambientMultiplier / 3.0f, 1.0f);
                gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, new float[]{ambient, ambient, ambient, 1.0f}, 0);

                // Set all 4 lights to have bright ambient
                for (int i = 0; i < 4; i++) {
                    int lightId = GL2.GL_LIGHT4 + i;
                    gl.glLightfv(lightId, GL2.GL_AMBIENT, new float[]{ambient, ambient, ambient, 1.0f}, 0);
                }
            }

            // 4. Every 60 frames, log that we're still applying
            if (frameCounter % 60 == 0 && frameCounter < 180) {
                API.SendMessage("Applying brightness... (frame " + frameCounter + ")");
            }

        } catch (Exception e) {
            // Fail silently
        }
    }

    @Override
    public void ProcessCommand(String commandStr, String[] args) {
        if (commandStr.equalsIgnoreCase("::aggressivebright")) {
            if (args.length == 0) {
                API.SendMessage("<col=ffff00>=== AGGRESSIVE BRIGHTNESS ===</col>");
                API.SendMessage("Enabled: " + (enableBrightness ? "<col=00ff00>YES</col>" : "<col=ff0000>NO</col>"));
                API.SendMessage("Ambient multiplier: " + ambientMultiplier + "x");
                API.SendMessage("Bright fog: " + (useBrightFog ? "ON" : "OFF"));
                API.SendMessage("Frames processed: " + frameCounter);
                API.SendMessage("");
                API.SendMessage("Commands:");
                API.SendMessage("  ::aggressivebright on/off");
                API.SendMessage("  ::aggressivebright ambient <1.0-10.0>");
                API.SendMessage("  ::aggressivebright fog on/off");
            } else if (args[0].equalsIgnoreCase("on")) {
                enableBrightness = true;
                frameCounter = 0;
                SaveConfig();
            } else if (args[0].equalsIgnoreCase("off")) {
                enableBrightness = false;
                SaveConfig();
            } else if (args[0].equalsIgnoreCase("ambient") && args.length > 1) {
                try {
                    float mult = Float.parseFloat(args[1]);
                    if (mult >= 1.0f && mult <= 10.0f) {
                        ambientMultiplier = mult;
                        SaveConfig();
                        API.SendMessage("<col=00ff00>Ambient set to " + mult + "x</col>");
                    } else {
                        API.SendMessage("<col=ff0000>Must be 1.0-10.0</col>");
                    }
                } catch (NumberFormatException e) {
                    API.SendMessage("<col=ff0000>Invalid number</col>");
                }
            } else if (args[0].equalsIgnoreCase("fog")) {
                if (args.length > 1) {
                    useBrightFog = args[1].equalsIgnoreCase("on");
                    SaveConfig();
                    API.SendMessage("Bright fog: " + (useBrightFog ? "ON" : "OFF"));
                }
            }
        }
    }

    public void OnKondoValueUpdated() {
        SaveConfig();

        // Clamp values
        if (ambientMultiplier < 1.0f) ambientMultiplier = 1.0f;
        if (ambientMultiplier > 10.0f) ambientMultiplier = 10.0f;
    }

    private void LoadConfig() {
        if (ambientMultiplier < 1.0f) ambientMultiplier = 1.0f;
        if (ambientMultiplier > 10.0f) ambientMultiplier = 10.0f;
    }

    private void SaveConfig() {
        // Auto-saves
    }

    @Override
    public void OnLogout() {
        if (isCurrentlyBoosted) {
            Preferences.brightness = originalGameBrightness;
            isCurrentlyBoosted = false;
        }
    }
}