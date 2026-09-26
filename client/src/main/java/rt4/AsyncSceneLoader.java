package rt4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

// Not part of the original client. Builds the next area in the background
// while the current one keeps rendering, instead of freezing the game behind
// "Loading - please wait" (see ASYNC_SCENE_LOADING.md for the design).
//
// The rebuild (LoginManager.prepareMapFiles/buildMap) runs on a builder
// thread, but never at the same time as the main thread: the two hand a baton
// back and forth, so no state is ever touched by two threads at once. Each
// rendered frame the main thread gives the builder a few ms, swapping the
// builder's copy of the scene statics (SceneState) in and its own back out
// afterwards. The builder hands the baton back at checkpoint() calls once its
// slice is used up. GL calls the builder would make (vertex buffer uploads)
// are deferred and done on the main thread before the swap.
//
// While a load is in flight no packets are read (they're already in the new
// area's coordinates), but the game keeps running in the old scene. At the
// swap the new scene state goes live, entities are shifted into the new
// coordinates (LoginManager.shiftRegion) and the end of the original rebuild
// runs (LoginManager.finishMap), then packet reading resumes.
//
// Anything unexpected - an exception in the builder, logout, leaving HD -
// throws the new scene away (the old one was never touched) and falls back to
// the original synchronous load.
public final class AsyncSceneLoader {

	// Player's tile in the new area's coordinates, for buildMap (the player
	// entity itself is only moved at the swap)
	public static int targetTileX;
	public static int targetTileY;

	private static Thread builder;
	private static final Semaphore builderTurn = new Semaphore(0);
	private static final Semaphore mainTurn = new Semaphore(0);

	// Only ever read/written by whichever thread holds the baton, and the
	// semaphores order those accesses, so none of this needs to be volatile.
	private static boolean active;
	private static boolean buildDone;
	private static boolean abortRequested;
	private static Throwable failure;
	private static long turnDeadline;

	private static final SceneState mainState = new SceneState();
	private static final SceneState builderState = new SceneState();

	private static int plane;
	private static int zoneX;
	private static int zoneY;
	private static int localTileX;
	private static int localTileY;
	private static boolean forceSync;

	// Objects the rebuild modifies in place. The builder gets its own; after a
	// swap the old scene's become the spares for the next load.
	private static byte[][][] spareRenderFlags;
	private static CollisionMap[] spareCollisionMaps;
	private static Shadow[][] spareShadows;
	private static SoftwareIndexedSprite spareShadowMapImage;
	private static Light[] spareLights;
	private static int[][][] spareLightGrid;
	private static Environment[][] spareChunksAtmosphere;
	private static LinkedList spareLocSounds;
	// Spare shadows hold GL textures, which die with their GL context
	private static int spareContextId = -1;
	private static int beginContextId;

	private static final ArrayList<GlVertexBufferObject> pendingBuffers = new ArrayList<>();

	// Diagnostics
	private static long beginNanos;
	private static long builderNanos;
	private static long maxSliceNanos;
	private static int slices;

	private static long lastStepNanos;

	private AsyncSceneLoader() {
	}

	private static final class AbortedException extends RuntimeException {
		AbortedException() {
			super("area load aborted", null, false, false);
		}
	}

	public static boolean isLoading() {
		return active;
	}

	// Called by the rebuild packet handler (via LoginManager.loadRegion)
	// instead of the synchronous load. Returns false to use the original path.
	public static boolean begin(int plane, int zoneY, int zoneX, int localTileY, int localTileX) {
		boolean fromPacket = rebuildPacketRead;
		rebuildPacketRead = false;
		if (forceSync || active || !GlobalConfig.ASYNC_SCENE_LOADING || !GlRenderer.enabled || client.gameState != 30) {
			return false;
		}
		if (ShadowManager.shadows == null || ShadowManager.shadowMapImage == null || LightingManager.lights == null || LightingManager.lightGrid == null) {
			return false;
		}
		AsyncSceneLoader.plane = plane;
		AsyncSceneLoader.zoneX = zoneX;
		AsyncSceneLoader.zoneY = zoneY;
		AsyncSceneLoader.localTileX = localTileX;
		AsyncSceneLoader.localTileY = localTileY;
		targetTileX = localTileX;
		targetTileY = localTileY;

		mainState.capture();
		builderState.copyFrom(mainState);

		// The first half of LoginManager.loadRegion, applied to the builder's
		// copy only
		builderState.centralZoneX = zoneX;
		builderState.centralZoneY = zoneY;
		builderState.centralPlane = SceneGraph.allLevelsAreVisible() ? 0 : plane;
		builderState.originY = zoneY * 8 - 48;
		builderState.originX = (zoneX - 6) * 8;
		builderState.map = MapList.getContainingSource(zoneX * 8, zoneY * 8);
		builderState.mapElementList = null;
		// readRebuildPacket already set dynamicMapRegion for the new area;
		// the old scene keeps what it had
		builderState.dynamicMapRegion = SceneGraph.dynamicMapRegion;
		if (fromPacket) {
			mainState.dynamicMapRegion = previousDynamicMapRegion;
		}

		giveBuilderOwnObjects();

		// What setGameState(25) resets for a load
		LoginManager.missingLocModelCount = 0;
		LoginManager.loadingScreenState = 0;
		LoginManager.mapFilesMissingCount = 0;

		pendingBuffers.clear();
		beginContextId = GlCleaner.contextId;
		active = true;
		buildDone = false;
		abortRequested = false;
		failure = null;
		beginNanos = System.nanoTime();
		lastStepNanos = beginNanos;
		builderNanos = 0L;
		maxSliceNanos = 0L;
		slices = 0;
		mainState.apply();
		startBuilder();
		return true;
	}

	// readRebuildPacket overwrites SceneGraph.dynamicMapRegion for the new
	// area before calling loadRegion; this is the old scene's value
	private static boolean previousDynamicMapRegion;
	private static boolean rebuildPacketRead;

	public static void onRebuildPacket() {
		previousDynamicMapRegion = SceneGraph.dynamicMapRegion;
		rebuildPacketRead = true;
	}

	// loadRegion returned early (same area): forget the packet's flag
	public static void onRegionUnchanged() {
		rebuildPacketRead = false;
	}

	public static boolean isBuilderThread() {
		return Thread.currentThread() == builder;
	}

	private static void giveBuilderOwnObjects() {
		if (spareContextId != GlCleaner.contextId) {
			spareShadows = null;
			spareContextId = GlCleaner.contextId;
		}
		if (spareRenderFlags == null) {
			spareRenderFlags = new byte[4][104][104];
		}
		builderState.renderFlags = spareRenderFlags;
		spareRenderFlags = null;

		if (spareCollisionMaps == null) {
			spareCollisionMaps = new CollisionMap[4];
			for (int i = 0; i < 4; i++) {
				spareCollisionMaps[i] = new CollisionMap(104, 104);
			}
		}
		builderState.collisionMaps = spareCollisionMaps;
		spareCollisionMaps = null;

		if (spareShadows == null) {
			// Shadow() creates a GL texture, so this has to happen here on the
			// main thread
			Shadow[][] current = mainState.shadows;
			spareShadows = new Shadow[current.length][];
			for (int col = 0; col < current.length; col++) {
				spareShadows[col] = new Shadow[current[col].length];
				for (int row = 0; row < current[col].length; row++) {
					spareShadows[col][row] = new Shadow();
				}
			}
		}
		builderState.shadows = spareShadows;
		spareShadows = null;

		if (spareShadowMapImage == null) {
			SoftwareIndexedSprite current = mainState.shadowMapImage;
			spareShadowMapImage = new SoftwareIndexedSprite(current.width, current.height, 0);
		}
		builderState.shadowMapImage = spareShadowMapImage;
		spareShadowMapImage = null;

		if (spareLights == null) {
			spareLights = new Light[mainState.lights.length];
		}
		builderState.lights = spareLights;
		spareLights = null;

		if (spareLightGrid == null) {
			int[][][] current = mainState.lightGrid;
			spareLightGrid = new int[current.length][current[0].length][current[0][0].length];
		}
		builderState.lightGrid = spareLightGrid;
		spareLightGrid = null;

		if (spareChunksAtmosphere == null) {
			spareChunksAtmosphere = new Environment[13][13];
		}
		builderState.chunksAtmosphere = spareChunksAtmosphere;
		spareChunksAtmosphere = null;

		if (spareLocSounds == null) {
			spareLocSounds = new LinkedList();
		}
		builderState.locSounds = spareLocSounds;
		spareLocSounds = null;

		// Small, and not always rewritten by the rebuild: start from the
		// current values, like the synchronous load would
		builderState.light0Position = mainState.light0Position.clone();
		builderState.light1Position = mainState.light1Position.clone();
	}

	private static void startBuilder() {
		if (builder != null && builder.isAlive()) {
			return;
		}
		builder = new Thread(AsyncSceneLoader::builderLoop, "scene-builder");
		builder.setDaemon(true);
		builder.start();
	}

	private static void builderLoop() {
		while (true) {
			builderTurn.acquireUninterruptibly();
			try {
				while (!LoginManager.prepareMapFiles()) {
					// Map files or loc models still downloading: let the main
					// thread (which drives the cache) run a frame and retry
					yieldToMain();
				}
				LoginManager.buildMap(true);
			} catch (Throwable ex) {
				failure = ex;
			}
			buildDone = true;
			mainTurn.release();
		}
	}

	// Called from the builder's heavy loops. Hands the baton back to the main
	// thread once this slice's time is used up.
	public static void checkpoint() {
		if (Thread.currentThread() != builder) {
			return;
		}
		if (abortRequested) {
			throw new AbortedException();
		}
		if (System.nanoTime() >= turnDeadline) {
			yieldToMain();
		}
	}

	private static void yieldToMain() {
		mainTurn.release();
		builderTurn.acquireUninterruptibly();
		if (abortRequested) {
			throw new AbortedException();
		}
	}

	public static void addPendingBuffer(GlVertexBufferObject buffer) {
		pendingBuffers.add(buffer);
	}

	// Main thread, once per rendered frame: give the builder a slice, or
	// upload finished buffers.
	public static void step() {
		if (!active) {
			return;
		}
		lastStepNanos = System.nanoTime();
		if (client.gameState != 30) {
			// Logged out or connection lost: the next login rebuilds anyway
			abort(false);
			return;
		}
		if (!GlRenderer.enabled || GlCleaner.contextId != beginContextId) {
			// Left HD or the GL context was recreated: the builder's GL objects
			// are unusable
			abort(true);
			return;
		}
		if (!buildDone) {
			runBuilderSlice(GlobalConfig.ASYNC_LOAD_SLICE_MS * 1000000L);
			return;
		}
		if (failure == null) {
			long deadline = System.nanoTime() + GlobalConfig.ASYNC_LOAD_SLICE_MS * 1000000L;
			while (!pendingBuffers.isEmpty() && System.nanoTime() < deadline) {
				pendingBuffers.remove(pendingBuffers.size() - 1).uploadPending();
			}
		}
	}

	private static void runBuilderSlice(long budgetNanos) {
		mainState.capture();
		builderState.apply();
		long start = System.nanoTime();
		turnDeadline = start + budgetNanos;
		builderTurn.release();
		awaitMainTurn();
		long spent = System.nanoTime() - start;
		builderNanos += spent;
		slices++;
		if (spent > maxSliceNanos) {
			maxSliceNanos = spent;
		}
		builderState.capture();
		mainState.apply();
	}

	private static void awaitMainTurn() {
		try {
			while (!mainTurn.tryAcquire(2, java.util.concurrent.TimeUnit.SECONDS)) {
				// Shouldn't happen: the builder only runs cache/scene code
				// that never waits on the main thread. Keep waiting (running
				// on concurrently would corrupt the scene) but leave a trace.
				log("builder slow to hand back", System.nanoTime(), null);
			}
		} catch (InterruptedException ex) {
			mainTurn.acquireUninterruptibly();
		}
	}

	// Main thread, once per logic tick before packets are read: swap the new
	// scene in once it's fully built and uploaded.
	public static void finishIfReady() {
		if (active && System.nanoTime() - lastStepNanos > 100000000L) {
			// No frames being drawn (e.g. minimized): keep the load moving from
			// the logic tick, or packets would stay paused
			step();
		}
		if (!active || !buildDone) {
			return;
		}
		if (failure != null) {
			fallBackToSync(failure);
			return;
		}
		if (!pendingBuffers.isEmpty()) {
			return;
		}
		long swapStart = System.nanoTime();

		// Old scene's ambient loc sounds, like the synchronous rebuild's
		// AreaSoundManager.clear(false)
		AreaSoundManager.clear(false);
		// What setGameState(25) did for an in-game load
		WorldMap.clear(true);
		client.topBannerRefresh();

		keepSpares(mainState);
		int deltaX = builderState.originX - mainState.originX;
		int deltaY = builderState.originY - mainState.originY;
		builderState.apply();
		active = false;

		// Drop the old scene's references so it can be collected
		mainState.copyFrom(builderState);

		LoginManager.shiftRegion(deltaX, deltaY, false, plane, localTileX, localTileY);
		LoginManager.finishMap(true);

		log("async", swapStart, null);
	}

	private static void keepSpares(SceneState old) {
		spareRenderFlags = old.renderFlags;
		spareCollisionMaps = old.collisionMaps;
		spareShadows = old.shadows;
		spareShadowMapImage = old.shadowMapImage;
		spareLights = old.lights;
		spareLightGrid = old.lightGrid;
		spareChunksAtmosphere = old.chunksAtmosphere;
		spareLocSounds = old.locSounds;
	}

	private static void abort(boolean reloadSynchronously) {
		if (!buildDone) {
			abortRequested = true;
			runBuilderSlice(Long.MAX_VALUE / 4);
		}
		if (reloadSynchronously) {
			fallBackToSync(null);
			return;
		}
		discard();
		log("aborted", System.nanoTime(), null);
	}

	private static void discard() {
		// The builder's objects were only ever referenced by the new scene,
		// so the spares go back to being fresh ones next time
		builderState.copyFrom(mainState);
		pendingBuffers.clear();
		active = false;
		abortRequested = false;
		// Statics already hold the untouched old scene (mainState)
	}

	private static void fallBackToSync(Throwable ex) {
		// The statics hold the old scene, but readRebuildPacket had already
		// set dynamicMapRegion for the new area
		boolean newDynamic = builderState.dynamicMapRegion;
		discard();
		SceneGraph.dynamicMapRegion = newDynamic;
		log(ex == null ? "left HD, falling back to synchronous load" : "failed, falling back to synchronous load", System.nanoTime(), ex);
		forceSync = true;
		try {
			LoginManager.loadRegion(plane, zoneY, zoneX, localTileY, false, localTileX);
		} finally {
			forceSync = false;
		}
	}

	private static void log(String result, long swapStart, Throwable ex) {
		String homeDir = SignLink.getHomeDir();
		if (homeDir == null) {
			return;
		}
		long now = System.nanoTime();
		StringBuilder line = new StringBuilder();
		line.append(result)
			.append(": wall=").append((now - beginNanos) / 1000000L).append("ms")
			.append(" builder=").append(builderNanos / 1000000L).append("ms")
			.append(" slices=").append(slices)
			.append(" maxSlice=").append(maxSliceNanos / 1000000L).append("ms")
			.append(" swap=").append((now - swapStart) / 1000000L).append("ms")
			.append(" | dynamic=").append(SceneGraph.dynamicMapRegion)
			.append('\n');
		if (ex != null) {
			StringWriter trace = new StringWriter();
			ex.printStackTrace(new PrintWriter(trace));
			line.append(trace);
		}
		try (FileWriter out = new FileWriter(new File(homeDir, "area_load_timing.log"), true)) {
			out.write(line.toString());
		} catch (IOException ignored) {
		}
	}
}
