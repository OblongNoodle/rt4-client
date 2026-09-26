package rt4;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

// Not part of the original client. Builds areas in the background while the
// current one keeps rendering, instead of freezing the game behind
// "Loading - please wait" (see ASYNC_SCENE_LOADING.md for the design).
//
// The rebuild (LoginManager.prepareMapFiles/buildMap) runs on a builder
// thread, but never at the same time as the main thread: the two hand a baton
// back and forth, so no state is ever touched by two threads at once. The main
// thread gives the builder a few ms per rendered frame plus the game loop's
// idle time, swapping the builder's copy of the scene statics (SceneState) in
// and its own back out around each turn. The builder hands the baton back at
// checkpoint() calls once its time is used up. GL calls the builder would make
// (vertex buffer uploads) are deferred and done on the main thread.
//
// Two kinds of build:
// - Predicted: the server rebuilds when the player's chunk gets 4 chunks from
//   the last rebuild's chunk, centred on the player's chunk. predict() watches
//   the player walk towards that line and builds the area it will ask for
//   ahead of time, while the game (and packet processing) carries on.
// - Requested: when a rebuild packet arrives. If a predicted build matches
//   it, that build is used; otherwise a new build starts. Packets after the
//   rebuild packet are already in the new area's coordinates, so they wait
//   until the new scene is swapped in (the game keeps running meanwhile).
//
// At the swap the new scene state goes live, entities are shifted into the new
// coordinates (LoginManager.shiftRegion) and the end of the original rebuild
// runs (LoginManager.finishMap), then packet reading resumes.
//
// Anything unexpected throws the new scene away (the old one was never
// touched); for a requested build it falls back to the original synchronous
// load.
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
	private static boolean speculative;
	private static boolean buildDone;
	private static boolean abortRequested;
	private static Throwable failure;
	private static long turnDeadline;

	private static final SceneState mainState = new SceneState();
	private static final SceneState builderState = new SceneState();

	// The area being built
	private static int plane;
	private static int zoneX;
	private static int zoneY;
	private static int centralPlane;
	// From the rebuild packet (requested builds only)
	private static int localTileX;
	private static int localTileY;
	private static boolean prebuilt;
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

	// After the build: shadow textures the first frame would otherwise
	// upload all at once, done ahead of the swap (see prewarm)
	private static boolean prewarmDone;
	private static boolean shadowsPrewarmed;
	// Next tile column to prepare models on: level * 104 + x
	private static int prewarmColumn;

	// Diagnostics
	private static long beginNanos;
	private static long requestNanos;
	private static long builderNanos;
	private static long maxSliceNanos;
	private static int slices;

	private static long lastStepNanos;

	// Builder-thread only: longest stretch of work without a checkpoint
	private static long lastCheckpointNanos;
	private static long longestGapNanos;
	private static String longestGapEnd;

	// readRebuildPacket overwrites SceneGraph.dynamicMapRegion for the new
	// area before calling loadRegion; this is the old scene's value
	private static boolean previousDynamicMapRegion;
	private static boolean rebuildPacketRead;

	private AsyncSceneLoader() {
	}

	private static final class AbortedException extends RuntimeException {
		AbortedException() {
			super("area load aborted", null, false, false);
		}
	}

	// True while packets are held back for a requested build
	public static boolean isLoading() {
		return active && !speculative;
	}

	public static boolean isBuilderThread() {
		return Thread.currentThread() == builder;
	}

	public static void onRebuildPacket() {
		previousDynamicMapRegion = SceneGraph.dynamicMapRegion;
		rebuildPacketRead = true;
	}

	// loadRegion returned early (same area): forget the packet's flag
	public static void onRegionUnchanged() {
		rebuildPacketRead = false;
	}

	private static boolean canBuild() {
		return GlobalConfig.ASYNC_SCENE_LOADING && GlRenderer.enabled && client.gameState == 30 && ShadowManager.shadows != null && ShadowManager.shadowMapImage != null && LightingManager.lights != null && LightingManager.lightGrid != null;
	}

	// Called by LoginManager.loadRegion for a rebuild packet or plane change,
	// instead of the synchronous load. Returns false to use the original path.
	public static boolean begin(int plane, int zoneY, int zoneX, int localTileY, int localTileX) {
		boolean fromPacket = rebuildPacketRead;
		rebuildPacketRead = false;
		if (active && speculative) {
			if (fromPacket && !forceSync && canBuild() && matchesPrediction(plane, zoneY, zoneX)) {
				// The server asked for exactly the area already being built
				speculative = false;
				prebuilt = true;
				AsyncSceneLoader.plane = plane;
				AsyncSceneLoader.localTileX = localTileX;
				AsyncSceneLoader.localTileY = localTileY;
				requestNanos = System.nanoTime();
				MotionTrace.event("rebuild matches prebuilt area");
				// The packet set this for the new area; the old scene keeps its own
				SceneGraph.dynamicMapRegion = previousDynamicMapRegion;
				return true;
			}
			cancel("prediction missed");
		}
		if (forceSync || active || !canBuild()) {
			return false;
		}
		AsyncSceneLoader.localTileX = localTileX;
		AsyncSceneLoader.localTileY = localTileY;
		targetTileX = localTileX;
		targetTileY = localTileY;
		prebuilt = false;
		speculative = false;
		mainState.capture();
		// The packet already set dynamicMapRegion and the map file lists for
		// the new area: the builder takes those, the old scene keeps its own
		// dynamicMapRegion
		startBuild(plane, zoneY, zoneX, SceneGraph.dynamicMapRegion, null);
		if (fromPacket) {
			mainState.dynamicMapRegion = previousDynamicMapRegion;
			mainState.apply();
		}
		requestNanos = beginNanos;
		return true;
	}

	private static boolean matchesPrediction(int plane, int zoneY, int zoneX) {
		if (zoneX != AsyncSceneLoader.zoneX || zoneY != AsyncSceneLoader.zoneY || SceneGraph.dynamicMapRegion) {
			return false;
		}
		if ((SceneGraph.allLevelsAreVisible() ? 0 : plane) != centralPlane) {
			return false;
		}
		// Same regions with the same keys as the packet
		int[] regions = LoginManager.regionBitPacked;
		int[][] keys = LoginManager.regionsXteaKeys;
		if (regions == null || keys == null || regions.length != builderState.regionBitPacked.length || keys.length != regions.length) {
			return false;
		}
		for (int i = 0; i < regions.length; i++) {
			if (regions[i] != builderState.regionBitPacked[i]) {
				return false;
			}
			if (builderState.locationsMapFileIds[i] == -1) {
				continue;
			}
			int[] ours = builderState.regionsXteaKeys[i];
			for (int k = 0; k < 4; k++) {
				if (keys[i][k] != ours[k]) {
					return false;
				}
			}
		}
		return true;
	}

	// The turn points of the player's last walk, in world coordinates, as sent
	// to the server (ClientProt.sendMovePacket)
	private static final int[] walkX = new int[25];
	private static final int[] walkY = new int[25];
	private static int walkLength;

	public static void recordWalkPath(int pathLength, int sentLength) {
		walkLength = sentLength;
		for (int i = 0; i < sentLength; i++) {
			walkX[i] = Camera.originX + PathFinder.queueX[pathLength - 1 - i];
			walkY[i] = Camera.originY + PathFinder.queueY[pathLength - 1 - i];
		}
	}

	private static final int ROUTE_TILES = 256;

	// Run toggle
	private static final int RUN_VARP = 173;
	private static final int[] routeX = new int[ROUTE_TILES];
	private static final int[] routeY = new int[ROUTE_TILES];

	// Walks the recorded path the way the server does (diagonally, then
	// straight, towards each turn point) and returns the index in routeX/Y of
	// the first tile that triggers a rebuild, measured from the player's
	// current tile, or -1. Sets routeLength.
	private static int routeLength;

	private static int findRouteCrossing(int worldX, int worldY) {
		if (walkLength < 1) {
			return -1;
		}
		int x = walkX[0];
		int y = walkY[0];
		int n = 0;
		routeX[n] = x;
		routeY[n] = y;
		n++;
		for (int i = 1; i < walkLength && n < ROUTE_TILES; i++) {
			while ((x != walkX[i] || y != walkY[i]) && n < ROUTE_TILES) {
				x += Integer.signum(walkX[i] - x);
				y += Integer.signum(walkY[i] - y);
				routeX[n] = x;
				routeY[n] = y;
				n++;
			}
		}
		routeLength = n;
		int current = -1;
		for (int i = 0; i < n; i++) {
			if (routeX[i] == worldX && routeY[i] == worldY) {
				current = i;
				break;
			}
		}
		if (current == -1) {
			return -1;
		}
		for (int i = current + 1; i < n; i++) {
			if (Math.abs((routeX[i] >> 3) - SceneGraph.centralZoneX) >= 4 || Math.abs((routeY[i] >> 3) - SceneGraph.centralZoneY) >= 4) {
				if (i - current > GlobalConfig.PREBUILD_LEAD_TILES) {
					return -1;
				}
				// Running moves 2 tiles a tick and the server only checks after
				// both, from a tick boundary (the player's current tile), so an
				// odd step count means it rebuilds one tile further on
				boolean running = VarpDomain.activeVarps[RUN_VARP] == 1 && Player.runEnergy > 0;
				if (running && ((i - current) & 1) == 1 && i + 1 < n) {
					return i + 1;
				}
				return i;
			}
		}
		return -1;
	}

	public static boolean isEnabled() {
		return GlobalConfig.ASYNC_SCENE_LOADING && GlRenderer.enabled;
	}

	// For MovementSmoother: steps from the given world tile along the player's
	// route (or straight on, if not on it) to the first tile that makes the
	// server rebuild, or -1 if that's more than 12 steps away or unknown
	public static int stepsUntilRebuild(int worldX, int worldY) {
		if (findRouteCrossing(worldX, worldY) != -1 || routeLength > 0) {
			int current = -1;
			for (int i = 0; i < routeLength; i++) {
				if (routeX[i] == worldX && routeY[i] == worldY) {
					current = i;
					break;
				}
			}
			if (current != -1) {
				for (int i = current + 1; i < routeLength && i - current <= 12; i++) {
					if (Math.abs((routeX[i] >> 3) - SceneGraph.centralZoneX) >= 4 || Math.abs((routeY[i] >> 3) - SceneGraph.centralZoneY) >= 4) {
						return i - current;
					}
				}
				return -1;
			}
		}
		Player self = PlayerList.self;
		if (self == null || self.movementQueueSize == 0) {
			return -1;
		}
		int stepX = Integer.signum(self.movementQueueX[0] - self.movementQueueX[1]);
		int stepY = Integer.signum(self.movementQueueY[0] - self.movementQueueY[1]);
		if (stepX == 0 && stepY == 0) {
			return -1;
		}
		for (int i = 1; i <= 12; i++) {
			int x = worldX + stepX * i;
			int y = worldY + stepY * i;
			if (Math.abs((x >> 3) - SceneGraph.centralZoneX) >= 4 || Math.abs((y >> 3) - SceneGraph.centralZoneY) >= 4) {
				return i;
			}
		}
		return -1;
	}

	// Main thread, once per logic tick after packets: if the player is heading
	// for the server's rebuild line, start building the area it will send.
	public static void predict() {
		if (!GlobalConfig.ASYNC_PREBUILD || isLoading() || !canBuild() || SceneGraph.dynamicMapRegion) {
			return;
		}
		Player self = PlayerList.self;
		if (self == null || self.movementQueueSize == 0) {
			return;
		}
		int stepX = Integer.signum(self.movementQueueX[0] - self.movementQueueX[1]);
		int stepY = Integer.signum(self.movementQueueY[0] - self.movementQueueY[1]);
		int worldX = Camera.originX + self.movementQueueX[0];
		int worldY = Camera.originY + self.movementQueueY[0];

		int bestDistance = Integer.MAX_VALUE;
		int predictedZoneX = 0;
		int predictedZoneY = 0;
		int crossingX = 0;
		int crossingY = 0;
		// Rebuild happens once the player's chunk is 4 chunks from the last one.
		// Best guess: follow the path the player clicked, which is exactly
		// what the server walks.
		int crossing = findRouteCrossing(worldX, worldY);
		if (crossing != -1) {
			bestDistance = 0;
			crossingX = routeX[crossing];
			crossingY = routeY[crossing];
			predictedZoneX = crossingX >> 3;
			predictedZoneY = crossingY >> 3;
		}
		// Otherwise (following, pushed, ...) extrapolate the last step
		if (stepX != 0 && bestDistance != 0) {
			int triggerChunk = SceneGraph.centralZoneX + 4 * stepX;
			int distance = stepX > 0 ? triggerChunk * 8 - worldX : worldX - (triggerChunk * 8 + 7);
			if (distance > 0 && distance <= GlobalConfig.PREBUILD_LEAD_TILES) {
				bestDistance = distance;
				crossingX = worldX + stepX * distance;
				crossingY = worldY + stepY * distance;
				predictedZoneX = triggerChunk;
				predictedZoneY = crossingY >> 3;
			}
		}
		if (stepY != 0 && bestDistance != 0) {
			int triggerChunk = SceneGraph.centralZoneY + 4 * stepY;
			int distance = stepY > 0 ? triggerChunk * 8 - worldY : worldY - (triggerChunk * 8 + 7);
			if (distance > 0 && distance <= GlobalConfig.PREBUILD_LEAD_TILES && distance < bestDistance) {
				bestDistance = distance;
				crossingX = worldX + stepX * distance;
				crossingY = worldY + stepY * distance;
				predictedZoneX = crossingX >> 3;
				predictedZoneY = triggerChunk;
			}
		}
		if (bestDistance == Integer.MAX_VALUE) {
			candidateTicks = 0;
			return;
		}
		if (active && predictedZoneX == zoneX && predictedZoneY == zoneY && Player.plane == plane) {
			candidateTicks = 0;
			return;
		}
		// A path around obstacles can flip the prediction back and forth, so
		// only act on one that holds for a few ticks
		if (predictedZoneX != candidateZoneX || predictedZoneY != candidateZoneY) {
			candidateZoneX = predictedZoneX;
			candidateZoneY = predictedZoneY;
			candidateTicks = 0;
		}
		candidateTicks++;
		if (candidateTicks < (active ? 10 : 2)) {
			return;
		}
		candidateTicks = 0;
		if (active) {
			cancel("prediction changed");
		}

		mainState.capture();
		if (!predictMapFiles(predictedZoneX, predictedZoneY)) {
			return;
		}
		prebuilt = false;
		speculative = true;
		int originX = (predictedZoneX - 6) * 8;
		int originY = predictedZoneY * 8 - 48;
		targetTileX = crossingX - originX;
		targetTileY = crossingY - originY;
		startBuild(Player.plane, predictedZoneY, predictedZoneX, false, predictedFiles);
		predictedFiles = null;
	}

	private static int candidateZoneX;
	private static int candidateZoneY;
	private static int candidateTicks;

	// The map file lists a rebuild packet for this zone would give, in the
	// same order (see Protocol.readRebuildPacket); false if any region's keys
	// aren't known yet.
	private static SceneState predictedFiles;

	private static boolean predictMapFiles(int zoneX, int zoneY) {
		int minX = (zoneX - 6) / 8;
		int maxX = (zoneX + 6) / 8;
		int minY = (zoneY - 6) / 8;
		int maxY = (zoneY + 6) / 8;
		int count = (maxX - minX + 1) * (maxY - minY + 1);
		SceneState files = new SceneState();
		files.regionsXteaKeys = new int[count][];
		files.regionBitPacked = new int[count];
		files.mapFileIds = new int[count];
		files.locationsMapFileIds = new int[count];
		files.underWaterMapFileIds = new int[count];
		files.underWaterLocationsMapFileIds = new int[count];
		files.mapFilesBuffer = new byte[count][];
		files.locationMapFilesBuffer = new byte[count][];
		files.underWaterMapFilesBuffer = new byte[count][];
		files.underWaterLocationsMapFilesBuffer = new byte[count][];
		files.npcSpawnsFileIds = null;
		files.npcSpawnsFilesBuffer = null;
		boolean isBlacklisted = (zoneX / 8 == 48 || zoneX / 8 == 49) && zoneY / 8 == 48;
		if (zoneX / 8 == 48 && zoneY / 8 == 148) {
			isBlacklisted = true;
		}
		int i = 0;
		for (int regionX = minX; regionX <= maxX; regionX++) {
			for (int regionY = minY; regionY <= maxY; regionY++) {
				int region = (regionX << 8) + regionY;
				files.regionBitPacked[i] = region;
				if (isBlacklisted && (regionY == 49 || regionY == 149 || regionY == 147 || regionX == 50 || regionX == 49 && regionY == 47)) {
					files.mapFileIds[i] = -1;
					files.locationsMapFileIds[i] = -1;
					files.underWaterMapFileIds[i] = -1;
					files.underWaterLocationsMapFileIds[i] = -1;
				} else {
					files.mapFileIds[i] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.MAP_PREFIX, JagString.parseInt(regionX), LoginManager.UNDERSCORE, JagString.parseInt(regionY)}));
					files.locationsMapFileIds[i] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.LOC_PREFIX, JagString.parseInt(regionX), LoginManager.UNDERSCORE, JagString.parseInt(regionY)}));
					files.underWaterMapFileIds[i] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.UNDERWATER_MAP_PREFIX, JagString.parseInt(regionX), LoginManager.UNDERSCORE, JagString.parseInt(regionY)}));
					files.underWaterLocationsMapFileIds[i] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.UNDERWATER_LOC_PREFIX, JagString.parseInt(regionX), LoginManager.UNDERSCORE, JagString.parseInt(regionY)}));
				}
				int[] key;
				if (files.locationsMapFileIds[i] == -1) {
					key = new int[4];
				} else {
					key = XteaKeyStore.get(region, files.locationsMapFileIds[i]);
					if (key == null || key == XteaKeyStore.NOT_READY) {
						// Unknown key, or its loc file is still downloading (the
						// check requested it, so a later tick can try again)
						return false;
					}
				}
				files.regionsXteaKeys[i] = key.clone();
				i++;
			}
		}
		predictedFiles = files;
		return true;
	}

	// Main thread. mainState must hold the current statics.
	private static void startBuild(int plane, int zoneY, int zoneX, boolean dynamic, SceneState files) {
		AsyncSceneLoader.plane = plane;
		AsyncSceneLoader.zoneX = zoneX;
		AsyncSceneLoader.zoneY = zoneY;
		builderState.copyFrom(mainState);

		// The first half of LoginManager.loadRegion, applied to the builder's
		// copy only
		centralPlane = SceneGraph.allLevelsAreVisible() ? 0 : plane;
		builderState.centralZoneX = zoneX;
		builderState.centralZoneY = zoneY;
		builderState.centralPlane = centralPlane;
		builderState.originY = zoneY * 8 - 48;
		builderState.originX = (zoneX - 6) * 8;
		builderState.map = MapList.getContainingSource(zoneX * 8, zoneY * 8);
		builderState.mapElementList = null;
		builderState.dynamicMapRegion = dynamic;
		if (files != null) {
			builderState.regionsXteaKeys = files.regionsXteaKeys;
			builderState.regionBitPacked = files.regionBitPacked;
			builderState.mapFileIds = files.mapFileIds;
			builderState.locationsMapFileIds = files.locationsMapFileIds;
			builderState.underWaterMapFileIds = files.underWaterMapFileIds;
			builderState.underWaterLocationsMapFileIds = files.underWaterLocationsMapFileIds;
			builderState.npcSpawnsFileIds = files.npcSpawnsFileIds;
			builderState.mapFilesBuffer = files.mapFilesBuffer;
			builderState.locationMapFilesBuffer = files.locationMapFilesBuffer;
			builderState.underWaterMapFilesBuffer = files.underWaterMapFilesBuffer;
			builderState.underWaterLocationsMapFilesBuffer = files.underWaterLocationsMapFilesBuffer;
			builderState.npcSpawnsFilesBuffer = files.npcSpawnsFilesBuffer;
		}

		giveBuilderOwnObjects();

		// What setGameState(25) resets for a load
		LoginManager.missingLocModelCount = 0;
		LoginManager.loadingScreenState = 0;
		LoginManager.mapFilesMissingCount = 0;

		pendingBuffers.clear();
		prewarmDone = false;
		shadowsPrewarmed = false;
		prewarmColumn = 0;
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
		longestGapNanos = 0L;
		longestGapEnd = null;
		startBuilder();
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
			lastCheckpointNanos = System.nanoTime();
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
	// thread once this turn's time is used up.
	public static void checkpoint() {
		if (Thread.currentThread() != builder) {
			return;
		}
		if (abortRequested) {
			throw new AbortedException();
		}
		long now = System.nanoTime();
		long gap = now - lastCheckpointNanos;
		if (gap > longestGapNanos) {
			// Rare (only on a new maximum), so the stack walk is affordable
			longestGapNanos = gap;
			StackTraceElement[] stack = new Throwable().getStackTrace();
			longestGapEnd = stack.length > 1 ? stack[1].toString() : "?";
		}
		if (now >= turnDeadline) {
			yieldToMain();
		}
		lastCheckpointNanos = System.nanoTime();
	}

	private static void yieldToMain() {
		mainTurn.release();
		builderTurn.acquireUninterruptibly();
		lastCheckpointNanos = System.nanoTime();
		if (abortRequested) {
			throw new AbortedException();
		}
	}

	public static void addPendingBuffer(GlVertexBufferObject buffer) {
		pendingBuffers.add(buffer);
	}

	// Checks that the build can carry on; false if it was thrown away
	private static boolean stillValid() {
		if (client.gameState != 30) {
			// Logged out or connection lost: the next login rebuilds anyway
			abort(false);
			return false;
		}
		if (!GlRenderer.enabled || GlCleaner.contextId != beginContextId) {
			// Left HD or the GL context was recreated: the builder's GL objects
			// are unusable
			abort(true);
			return false;
		}
		if (speculative && buildDone && failure != null) {
			cancel("prediction build failed");
			return false;
		}
		return true;
	}

	// Main thread, once per rendered frame: give the builder a turn, or
	// upload finished buffers.
	public static void step() {
		if (!active) {
			return;
		}
		lastStepNanos = System.nanoTime();
		if (!stillValid()) {
			return;
		}
		// A requested build holds packets back, so it gets a real share of
		// every frame; a predicted one mostly runs in idle time
		long budget = (speculative ? GlobalConfig.ASYNC_LOAD_SLICE_MS : GlobalConfig.ASYNC_REQUESTED_SLICE_MS) * 1000000L;
		if (!buildDone) {
			runBuilderSlice(budget);
		} else if (failure == null) {
			finishOnMainThread(System.nanoTime() + budget);
		}
	}

	// Main thread, from the game loop in place of its sleep between frames
	// and ticks. Returns false if there's nothing to do (the caller sleeps).
	public static boolean runDuringIdle(long idleMs) {
		if (!active || idleMs < 1 || (buildDone && (failure != null || prewarmDone))) {
			return false;
		}
		if (!stillValid()) {
			return false;
		}
		// Leave a little slack so the next frame isn't late
		long deadline = System.nanoTime() + idleMs * 1000000L - 500000L;
		if (!buildDone) {
			runBuilderSlice(Math.max(deadline - System.nanoTime(), 1000000L));
		} else if (failure == null) {
			finishOnMainThread(deadline);
		}
		long left = (deadline - System.nanoTime()) / 1000000L;
		if (left > 0) {
			ThreadUtils.sleep(left);
		}
		return true;
	}

	// The GL work for a finished build, spread over frames: the deferred
	// vertex buffers, then the shadow textures (the renderer uploads those the
	// first time each shadow tile is drawn, up to 169 of them in the first
	// frame after a swap)
	private static void finishOnMainThread(long deadline) {
		while (!pendingBuffers.isEmpty() && System.nanoTime() < deadline) {
			pendingBuffers.remove(pendingBuffers.size() - 1).uploadPending();
		}
		if (!pendingBuffers.isEmpty() || prewarmDone) {
			return;
		}
		if (!shadowsPrewarmed) {
			Shadow[][] shadows = builderState.shadows;
			for (int col = 0; col < shadows.length; col++) {
				for (int row = 0; row < shadows[col].length; row++) {
					Shadow shadow = shadows[col][row];
					if (shadow.outputToSprite) {
						if (System.nanoTime() >= deadline) {
							return;
						}
						shadow.updateShadowTexture(builderState.shadowMapImage, col, row);
						shadow.outputToSprite = false;
					}
				}
			}
			shadowsPrewarmed = true;
		}
		// Static scenery models pack their vertex data on first draw, which
		// would otherwise all happen in the new area's first frame
		Tile[][][] tiles = builderState.surfaceGroundTiles;
		int columns = tiles.length * 104;
		while (prewarmColumn < columns) {
			if (System.nanoTime() >= deadline) {
				return;
			}
			Tile[] column = tiles[prewarmColumn / 104][prewarmColumn % 104];
			prewarmColumn++;
			for (Tile tile : column) {
				if (tile == null) {
					continue;
				}
				for (int i = 0; i < tile.sceneryLen; i++) {
					Scenery scenery = tile.scenery[i];
					if (scenery != null) {
						prepareModel(scenery.entity);
					}
				}
				if (tile.wall != null) {
					prepareModel(tile.wall.primary);
					prepareModel(tile.wall.secondary);
				}
				if (tile.wallDecor != null) {
					prepareModel(tile.wallDecor.primary);
					prepareModel(tile.wallDecor.secondary);
				}
				if (tile.groundDecor != null) {
					prepareModel(tile.groundDecor.entity);
				}
			}
		}
		prewarmDone = true;
	}

	private static void prepareModel(Entity entity) {
		if (entity instanceof GlModel) {
			((GlModel) entity).prepareBuffers();
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
		HitchLog.builderNanos += spent;
		slices++;
		if (spent > maxSliceNanos) {
			maxSliceNanos = spent;
		}
		builderState.capture();
		mainState.apply();
	}

	private static void awaitMainTurn() {
		try {
			while (!mainTurn.tryAcquire(2, TimeUnit.SECONDS)) {
				// Shouldn't happen: the builder only runs cache/scene code
				// that never waits on the main thread. Keep waiting (running
				// on concurrently would corrupt the scene) but leave a trace.
				log("builder slow to hand back", null);
			}
		} catch (InterruptedException ex) {
			mainTurn.acquireUninterruptibly();
		}
	}

	// Main thread, once per logic tick before packets are read: swap the new
	// scene in once a requested build is fully built and uploaded.
	public static void finishIfReady() {
		if (active && System.nanoTime() - lastStepNanos > 100000000L) {
			// No frames being drawn (e.g. minimized): keep the load moving from
			// the logic tick, or packets would stay paused
			step();
		}
		if (!active || speculative || !buildDone) {
			return;
		}
		if (failure != null) {
			fallBackToSync(failure);
			return;
		}
		if (!prewarmDone) {
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
		if (prebuilt) {
			// Built before the player's tile was known: redo buildMap's
			// light position for where the player actually is
			FogManager.setLightPosition(localTileY >> 3, localTileX >> 3);
		}
		// Drop the old scene's references so it can be collected
		mainState.copyFrom(builderState);

		MotionTrace.event("swapping in " + (prebuilt ? "prebuilt" : "background-built") + " area");
		LoginManager.shiftRegion(deltaX, deltaY, false, plane, localTileX, localTileY);
		LoginManager.finishMap(true);

		log(prebuilt ? "prebuilt" : "async", null, swapStart);
		HitchLog.onAreaSwapped();
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

	// Throws away a predicted build
	private static void cancel(String reason) {
		stopBuilder();
		discard();
		log(reason, null);
	}

	private static void abort(boolean reloadSynchronously) {
		boolean wasSpeculative = speculative;
		stopBuilder();
		if (reloadSynchronously && !wasSpeculative) {
			fallBackToSync(null);
			return;
		}
		discard();
		log("aborted", null);
	}

	private static void stopBuilder() {
		if (!buildDone) {
			abortRequested = true;
			runBuilderSlice(Long.MAX_VALUE / 4);
		}
	}

	private static void discard() {
		// The builder's objects were only ever referenced by the new scene,
		// so the spares go back to being fresh ones next time
		builderState.copyFrom(mainState);
		pendingBuffers.clear();
		active = false;
		speculative = false;
		prebuilt = false;
		abortRequested = false;
		// Statics already hold the untouched old scene (mainState)
	}

	private static void fallBackToSync(Throwable ex) {
		// The statics hold the old scene, but readRebuildPacket had already
		// set dynamicMapRegion for the new area
		boolean newDynamic = builderState.dynamicMapRegion;
		discard();
		SceneGraph.dynamicMapRegion = newDynamic;
		log(ex == null ? "left HD, falling back to synchronous load" : "failed, falling back to synchronous load", ex);
		forceSync = true;
		try {
			LoginManager.loadRegion(plane, zoneY, zoneX, localTileY, false, localTileX);
		} finally {
			forceSync = false;
		}
	}

	private static void log(String result, Throwable ex) {
		log(result, ex, System.nanoTime());
	}

	private static void log(String result, Throwable ex, long swapStart) {
		String homeDir = SignLink.getHomeDir();
		if (homeDir == null) {
			return;
		}
		long now = System.nanoTime();
		StringBuilder line = new StringBuilder();
		line.append(result)
			.append(": packetsPaused=").append(requestNanos == 0L ? 0L : (now - requestNanos) / 1000000L).append("ms")
			.append(" wall=").append((now - beginNanos) / 1000000L).append("ms")
			.append(" builder=").append(builderNanos / 1000000L).append("ms")
			.append(" slices=").append(slices)
			.append(" maxSlice=").append(maxSliceNanos / 1000000L).append("ms")
			.append(" swap=").append((now - swapStart) / 1000000L).append("ms")
			.append(" longestStep=").append(longestGapNanos / 1000000L).append("ms before ").append(longestGapEnd)
			.append(" | zone=").append(zoneX).append(',').append(zoneY)
			.append('\n');
		requestNanos = 0L;
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
