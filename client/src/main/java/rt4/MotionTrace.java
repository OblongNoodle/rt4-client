package rt4;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Not part of the original client. Diagnostic: a frame-by-frame record of
// what the player sees around every area change, written to
// <home>/motion_trace.log. Frames are kept in memory and written once each
// capture window closes, so recording never touches the disk mid-capture.
//
// Captures 2s before to 3s after each area change (rebuild packet), and
// around every press of the Insert key (a marker for "that one felt bad").
public final class MotionTrace {

	private static final long BEFORE_NS = 2000000000L;
	private static final long AFTER_NS = 3000000000L;
	private static final int CAPACITY = 4096;

	private static final String[] lines = new String[CAPACITY];
	private static final long[] times = new long[CAPACITY];
	private static int head;
	private static int count;

	private static long captureUntil;
	private static long captureFrom;
	private static long anchor;
	private static String anchorLabel;

	private static long lastFrameNanos;
	private static double lastWorldX = Double.NaN;
	private static double lastWorldY = Double.NaN;
	private static int ticksSinceFrame;
	private static int packetsSinceFrame;
	private static boolean insertWasDown;

	private MotionTrace() {
	}

	private static void add(long now, String line) {
		lines[head] = line;
		times[head] = now;
		head = (head + 1) % CAPACITY;
		if (count < CAPACITY) {
			count++;
		}
	}

	public static void onPacket() {
		packetsSinceFrame++;
	}

	public static void onTick() {
		ticksSinceFrame++;
	}

	public static void event(String what) {
		long now = System.nanoTime();
		add(now, "EVENT " + what);
		if (captureUntil == 0L) {
			captureFrom = now - BEFORE_NS;
			anchor = now;
			anchorLabel = what;
		}
		captureUntil = now + AFTER_NS;
	}

	public static void onSelfUpdate(int type, String detail) {
		Player self = PlayerList.self;
		String head = self == null ? "?" : "queueHead=" + (Camera.originX + self.movementQueueX[0]) + "," + (Camera.originY + self.movementQueueY[0]) + " q=" + self.movementQueueSize;
		add(System.nanoTime(), "SERVER self update type=" + type + (type == 0 ? "(none)" : type == 1 ? "(walk)" : type == 2 ? "(run)" : "(place)") + " " + detail + " -> " + head);
	}

	// After each rendered frame
	public static void onFrame(long frameStart) {
		long now = System.nanoTime();
		boolean insertDown = Keyboard.getKey(KeyEvent.VK_INSERT);
		if (insertDown && !insertWasDown) {
			event("MARK (Insert pressed)");
		}
		insertWasDown = insertDown;

		Player self = PlayerList.self;
		if (self != null && client.gameState == 30) {
			double worldX = Camera.originX + self.xFine / 128.0;
			double worldY = Camera.originY + self.yFine / 128.0;
			double moved = Double.isNaN(lastWorldX) ? 0.0 : Math.hypot(worldX - lastWorldX, worldY - lastWorldY);
			lastWorldX = worldX;
			lastWorldY = worldY;
			int speed = self.movementQueueSize > 0 ? self.movementQueueSpeed[self.movementQueueSize - 1] : 0;
			add(now, String.format("frame gap=%5.1fms draw=%5.1fms ticks=%d pkts=%2d pos=%.2f,%.2f moved=%.3f q=%d spd=%d seq=%d catchup=%d cam=%.1f,%.1f",
				lastFrameNanos == 0L ? 0.0 : (now - lastFrameNanos) / 1e6, (now - frameStart) / 1e6, ticksSinceFrame, packetsSinceFrame,
				worldX, worldY, moved, self.movementQueueSize, speed, self.seqId, self.movementCatchupTicks,
				Camera.originX + Camera.renderX / 128.0, Camera.originY + Camera.renderY / 128.0));
		}
		lastFrameNanos = now;
		ticksSinceFrame = 0;
		packetsSinceFrame = 0;

		if (captureUntil != 0L && now > captureUntil) {
			flush(now);
		}
	}

	private static void flush(long now) {
		StringBuilder out = new StringBuilder();
		out.append("===== capture around ").append(anchorLabel).append(" =====\n");
		int start = (head - count + CAPACITY) % CAPACITY;
		for (int i = 0; i < count; i++) {
			int idx = (start + i) % CAPACITY;
			if (times[idx] < captureFrom || times[idx] > captureUntil) {
				continue;
			}
			out.append(String.format("%+7.1fms ", (times[idx] - anchor) / 1e6)).append(lines[idx]).append('\n');
		}
		captureUntil = 0L;
		String homeDir = SignLink.getHomeDir();
		if (homeDir == null) {
			return;
		}
		try (FileWriter writer = new FileWriter(new File(homeDir, "motion_trace.log"), true)) {
			writer.write(out.toString());
		} catch (IOException ignored) {
		}
	}
}
