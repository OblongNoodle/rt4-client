package rt4;

import com.jogamp.opengl.GL2;

// Not part of the original client. Diagnostic (::profile, or
// GlobalConfig.DIAGNOSTICS): every 5 seconds logs to stdout the frame rate
// and, for a few named sections, the CPU time and the GPU time
// (GL_TIME_ELAPSED queries, read back a few frames later so nothing stalls).
public final class FrameProfiler {

	public static final int SCENE = 0;
	public static final int FAR = 1;
	public static final int GRADE = 2;
	private static final String[] NAMES = {"scene", "far view", "grade"};
	private static final int SECTIONS = NAMES.length;
	private static final int RING = 4;

	private static int[][] queries;
	private static final boolean[][] pending = new boolean[SECTIONS][RING];
	private static int frame;
	private static final long[] cpuStart = new long[SECTIONS];
	private static final long[] cpuTotal = new long[SECTIONS];
	private static final long[] gpuTotal = new long[SECTIONS];
	private static final int[] gpuCount = new int[SECTIONS];
	private static int frames;
	private static long lastFrame;
	private static long frameTimeTotal;
	private static long worstFrame;
	private static long reportAt;
	private static boolean disabled;
	private static int active = -1;

	public static boolean enabled;

	private static boolean on() {
		return enabled || GlobalConfig.DIAGNOSTICS;
	}

	private FrameProfiler() {
	}

	public static void reset() {
		queries = null;
		disabled = false;
		active = -1;
		for (boolean[] p : pending) {
			java.util.Arrays.fill(p, false);
		}
	}

	private static boolean ready() {
		if (disabled) {
			return false;
		}
		if (queries == null) {
			GL2 gl = GlRenderer.gl;
			if (!gl.isFunctionAvailable("glGenQueries") || !gl.isExtensionAvailable("GL_ARB_timer_query") && !gl.isFunctionAvailable("glGetQueryObjectui64v")) {
				disabled = true;
				return false;
			}
			queries = new int[SECTIONS][RING];
			for (int s = 0; s < SECTIONS; s++) {
				gl.glGenQueries(RING, queries[s], 0);
			}
		}
		return true;
	}

	// Once per rendered frame, before the scene
	public static void frame() {
		if (!on()) {
			lastFrame = 0L;
			reportAt = 0L;
			return;
		}
		long now = System.nanoTime();
		if (lastFrame != 0L) {
			long time = now - lastFrame;
			frameTimeTotal += time;
			worstFrame = Math.max(worstFrame, time);
			frames++;
		}
		lastFrame = now;
		frame++;
		if (ready()) {
			collect();
		}
		if (reportAt == 0L) {
			reportAt = now;
		}
		if (now - reportAt > 5000000000L && frames > 0) {
			StringBuilder line = new StringBuilder("[FrameProfiler] fps=").append(Math.round(frames * 1e9 / (now - reportAt)))
				.append(" frame=").append(ms(frameTimeTotal / frames)).append("ms worst=").append(ms(worstFrame)).append("ms |");
			for (int s = 0; s < SECTIONS; s++) {
				line.append(' ').append(NAMES[s]).append(" cpu=").append(ms(cpuTotal[s] / frames))
					.append(" gpu=").append(gpuCount[s] == 0 ? "-" : ms(gpuTotal[s] / gpuCount[s])).append(';');
				cpuTotal[s] = 0;
				gpuTotal[s] = 0;
				gpuCount[s] = 0;
			}
			System.out.println(line);
			frames = 0;
			frameTimeTotal = 0;
			worstFrame = 0;
			reportAt = now;
		}
	}

	public static void begin(int section) {
		if (!on()) {
			return;
		}
		cpuStart[section] = System.nanoTime();
		// GL_TIME_ELAPSED queries can't nest: only the outermost is timed
		if (active == -1 && ready()) {
			int slot = frame % RING;
			if (!pending[section][slot]) {
				GlRenderer.gl.glBeginQuery(GL2.GL_TIME_ELAPSED, queries[section][slot]);
				active = section;
			}
		}
	}

	public static void end(int section) {
		if (!on() && active != section) {
			return;
		}
		cpuTotal[section] += System.nanoTime() - cpuStart[section];
		if (active == section) {
			GlRenderer.gl.glEndQuery(GL2.GL_TIME_ELAPSED);
			pending[section][frame % RING] = true;
			active = -1;
		}
	}

	private static void collect() {
		GL2 gl = GlRenderer.gl;
		int[] available = new int[1];
		long[] result = new long[1];
		for (int s = 0; s < SECTIONS; s++) {
			for (int slot = 0; slot < RING; slot++) {
				if (!pending[s][slot] || slot == frame % RING) {
					continue;
				}
				gl.glGetQueryObjectiv(queries[s][slot], GL2.GL_QUERY_RESULT_AVAILABLE, available, 0);
				if (available[0] != 0) {
					gl.glGetQueryObjectui64v(queries[s][slot], GL2.GL_QUERY_RESULT, result, 0);
					gpuTotal[s] += result[0];
					gpuCount[s]++;
					pending[s][slot] = false;
				}
			}
		}
	}

	private static String ms(long nanos) {
		return String.format("%.2f", nanos / 1e6);
	}
}
