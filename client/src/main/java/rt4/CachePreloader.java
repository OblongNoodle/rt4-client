package rt4;

// Not part of the original client. Cache groups are read from disk on a
// background thread, so the first time after launch that a clientscript or
// interface is asked for it comes back null. ScriptRunner drops a script it
// can't get, and a gosub to one throws partway through the caller, which left
// the bank empty, unsorted or missing tabs on its first open. Once in game this
// reads every clientscript group (and the bank interfaces) into memory, a
// window at a time so it doesn't crowd out other reads. Archive 12 keeps its
// packed groups, so they stay loaded for the rest of the session.
public final class CachePreloader {

	private static final int[] INTERFACES = {762, 763};
	private static final int WINDOW = 64;
	// Frames to wait on one group before skipping it, so a group that never
	// arrives can't hold up the rest
	private static final int STALL_FRAMES = 500;

	private static int cursor;
	private static int stalledFrames;
	private static boolean done;
	private static long startTime;

	public static void tick() {
		if (done) {
			return;
		}
		Js5 scripts = client.js5Archive12;
		Js5 interfaces = InterfaceList.interfaceProvider;
		if (scripts == null || interfaces == null) {
			return;
		}
		int capacity = scripts.capacity();
		if (capacity < 0) {
			return;
		}
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
		}
		boolean interfacesReady = true;
		for (int id : INTERFACES) {
			if (!interfaces.isGroupReady(id)) {
				interfacesReady = false;
			}
		}
		int start = cursor;
		int end = Math.min(capacity, cursor + WINDOW);
		for (int group = cursor; group < end; group++) {
			// isGroupReady queues the read; groups past the cursor are picked up
			// once the cursor reaches them
			boolean ready = scripts.getGroupCapacity(group) == 0 || scripts.isGroupReady(group);
			if (ready && group == cursor) {
				cursor++;
			}
		}
		if (cursor < capacity) {
			if (cursor == start && ++stalledFrames > STALL_FRAMES) {
				cursor++;
				stalledFrames = 0;
			} else if (cursor != start) {
				stalledFrames = 0;
			}
			return;
		}
		if (interfacesReady) {
			done = true;
			System.out.println("[CachePreloader] " + capacity + " clientscript groups loaded in " + (System.currentTimeMillis() - startTime) + "ms");
		}
	}
}
