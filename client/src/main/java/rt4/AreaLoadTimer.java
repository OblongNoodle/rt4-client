package rt4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Not part of the original client. Diagnostic: times each phase of
// LoginManager.rebuildMap() (the "Loading - please wait" freeze when
// entering a new area) and appends one line per load to
// <home>/area_load_timing.log.
public final class AreaLoadTimer {

	private static long startNanos;

	private static long lastNanos;

	private static StringBuilder phases;

	private AreaLoadTimer() {
	}

	public static void start() {
		if (AsyncSceneLoader.isBuilderThread()) {
			// AsyncSceneLoader logs its own timings for background loads
			phases = null;
			return;
		}
		startNanos = lastNanos = System.nanoTime();
		phases = new StringBuilder();
	}

	public static void mark(String phase) {
		if (phases == null) {
			return;
		}
		long now = System.nanoTime();
		phases.append(' ').append(phase).append('=').append((now - lastNanos) / 1000000L).append("ms");
		lastNanos = now;
	}

	public static void finish() {
		if (phases == null) {
			return;
		}
		long totalMs = (System.nanoTime() - startNanos) / 1000000L;
		int regions = LoginManager.regionsXteaKeys == null ? 0 : LoginManager.regionsXteaKeys.length;
		int keyedRegions = 0;
		for (int i = 0; i < regions; i++) {
			int[] key = LoginManager.regionsXteaKeys[i];
			if (key != null && (key[0] | key[1] | key[2] | key[3]) != 0) {
				keyedRegions++;
			}
		}
		String line = "total=" + totalMs + "ms" + phases
			+ " | hd=" + GlRenderer.enabled + " dynamic=" + SceneGraph.dynamicMapRegion
			+ " regions=" + regions + " nonZeroXteaKeys=" + keyedRegions + "\n";
		phases = null;
		String homeDir = SignLink.getHomeDir();
		if (homeDir == null) {
			return;
		}
		try (FileWriter out = new FileWriter(new File(homeDir, "area_load_timing.log"), true)) {
			out.write(line);
		} catch (IOException ex) {
		}
	}
}
