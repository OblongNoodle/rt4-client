package rt4;

// Not part of the original client. Hides the 2009scape server's skipped tick
// at area boundaries.
//
// On the tick the server decides to send a new area it returns from
// WalkingQueue.update() before moving the player, so the player stands still
// on the server for one whole tick (600ms). The client used to hide that
// behind its loading freeze; with background loading there's no freeze, so the
// player visibly runs into a dead stop. The stall is predictable: it happens
// on the tick after the player's chunk gets 4 chunks from the last rebuild's
// chunk. So in the last few ticks before it, the player's own movement
// is slowed just enough that the steps already received last until movement
// resumes - an ease-off instead of a stop. The client stays exactly in step
// with the server; only the animation of steps it already has is stretched.
public final class MovementSmoother {

	private static final long TICK_MS = 600L;

	// How many server ticks before the stall to start easing off: the one lost
	// tick is spread over this many + 2, so more = a smaller slowdown
	private static final int MAX_TICKS_AHEAD = 5;

	// Fine units (1/128 tile) per logic tick (20ms); 0 when not smoothing
	private static double pace;
	private static double carry;
	private static long activeUntilNanos;

	// Tiles per server tick of the player's last movement: 2 running, 1 walking
	private static int stepsPerTick = 2;

	private MovementSmoother() {
	}

	public static boolean isActive() {
		return pace > 0.0 && System.nanoTime() < activeUntilNanos;
	}

	public static double getPace() {
		return isActive() ? pace : 0.0;
	}

	// After each server update about the player's own movement
	public static void onSelfUpdate(int type, boolean ran) {
		if (!GlobalConfig.SMOOTH_AREA_STALLS || !AsyncSceneLoader.isEnabled()) {
			return;
		}
		if (type == 1) {
			stepsPerTick = 1;
		} else if (type == 2) {
			stepsPerTick = ran ? 2 : 1;
		}
		Player self = PlayerList.self;
		if (self == null || SceneGraph.dynamicMapRegion) {
			return;
		}
		int headX = Camera.originX + self.movementQueueX[0];
		int headY = Camera.originY + self.movementQueueY[0];
		int ticksBeforeStall;
		int stepsBeforeStall;
		if (Math.abs((headX >> 3) - SceneGraph.centralZoneX) >= 4 || Math.abs((headY >> 3) - SceneGraph.centralZoneY) >= 4) {
			// Already on the rebuild line: the next server tick is the stall
			ticksBeforeStall = 0;
			stepsBeforeStall = 0;
		} else {
			stepsBeforeStall = AsyncSceneLoader.stepsUntilRebuild(headX, headY);
			if (stepsBeforeStall < 0) {
				return;
			}
			ticksBeforeStall = (stepsBeforeStall + stepsPerTick - 1) / stepsPerTick;
			if (ticksBeforeStall > MAX_TICKS_AHEAD) {
				return;
			}
		}
		// Distance still to cover before movement resumes: what's left to the
		// queued head, plus the steps the server will still send before the stall
		int size = self.getSize() * 64;
		int remaining = Math.max(Math.abs(self.movementQueueX[0] * 128 + size - self.xFine), Math.abs(self.movementQueueY[0] * 128 + size - self.yFine));
		double distance = remaining + 128.0 * stepsBeforeStall;
		// Movement resumes with the update after the stall; aim to arrive just
		// before it so the new steps continue without a gap
		long timeMs = (ticksBeforeStall + 2) * TICK_MS - 30L;
		double normal = stepsPerTick == 2 ? 8.0 : 4.0;
		double wanted = distance / (timeMs / 20.0);
		if (wanted >= normal * 0.97) {
			return;
		}
		pace = Math.max(wanted, normal * 0.4);
		activeUntilNanos = System.nanoTime() + (timeMs + 50L) * 1000000L;
	}

	// NpcList.processMovement: the step for this logic tick
	public static int limitStep(PathingEntity entity, int speed) {
		if (entity != PlayerList.self) {
			return speed;
		}
		if (!isActive()) {
			carry = 0.0;
			return speed;
		}
		carry += pace;
		int step = (int) carry;
		carry -= step;
		return Math.min(speed, step);
	}
}
