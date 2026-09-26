package rt4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Not part of the original client. Diagnostic: logs every logic tick or
// rendered frame that takes longer than THRESHOLD_MS, with how long after the
// last area swap it happened and how much of it was the minimap redraw or the
// background area builder, to <home>/area_load_timing.log.
public final class HitchLog {

	private static final long THRESHOLD_MS = 25;

	// Only hitches this soon after an area change are interesting
	private static final long WINDOW_MS = 3000;

	private static long lastSwapNanos = Long.MIN_VALUE / 2;

	public static long minimapNanos;
	public static long builderNanos;
	public static long shadowNanos;
	public static long textureNanos;
	public static long lateBufferNanos;

	private HitchLog() {
	}

	public static boolean timeMinimap(int plane) {
		long start = System.nanoTime();
		boolean rendered = MiniMap.renderMap(plane);
		minimapNanos += System.nanoTime() - start;
		return rendered;
	}

	public static void onAreaSwapped() {
		lastSwapNanos = System.nanoTime();
	}

	public static void begin() {
		minimapNanos = 0L;
		builderNanos = 0L;
		shadowNanos = 0L;
		textureNanos = 0L;
		lateBufferNanos = 0L;
	}

	public static void end(String what, long startNanos) {
		long now = System.nanoTime();
		long ms = (now - startNanos) / 1000000L;
		long sinceSwapMs = (startNanos - lastSwapNanos) / 1000000L;
		if (ms < THRESHOLD_MS || sinceSwapMs > WINDOW_MS && !AsyncSceneLoader.isLoading()) {
			return;
		}
		String homeDir = SignLink.getHomeDir();
		if (homeDir == null) {
			return;
		}
		String line = "  hitch: " + what + " " + ms + "ms, " + sinceSwapMs + "ms after swap, minimap=" + minimapNanos / 1000000L + "ms builder=" + builderNanos / 1000000L + "ms shadowTex=" + shadowNanos / 1000000L + "ms textures=" + textureNanos / 1000000L + "ms lateBuffers=" + lateBufferNanos / 1000000L + "ms\n";
		try (FileWriter out = new FileWriter(new File(homeDir, "area_load_timing.log"), true)) {
			out.write(line);
		} catch (IOException ignored) {
		}
	}
}
