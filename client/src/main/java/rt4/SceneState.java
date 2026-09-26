package rt4;

// Not part of the original client. A copy of every static field an area
// rebuild writes (LoginManager.buildMap and everything it calls), so
// AsyncSceneLoader can keep one set in the statics for the main thread (the
// scene currently on screen) while its builder thread fills in another (the
// next scene), swapping them on every hand-off. The field list came from a
// bytecode call-graph analysis of all methods buildMap can reach, plus the
// objects it modifies in place. If buildMap ever starts writing a new scene
// field, it must be added here, or the old scene will see half of the new one.
public final class SceneState {

	// SceneGraph
	int activeOccluderCount;
	Occluder[] activeOccluders;
	int firstVisibleLevel;
	int hueJitter;
	int length;
	int levels;
	int lightnessJitter;
	int[][][] occludeFlags;
	int occluderCount;
	Occluder[] occluders;
	int[][][] occlusionCache;
	byte[][][] occlusionHeights;
	int[] rowChroma;
	int[] rowCount;
	int[] rowLightness;
	int[] rowSaturation;
	int[] rowWeightedHue;
	Scenery[] scenery;
	Scenery[] sceneryBuffer;
	int sceneryLen;
	byte[][][] shadowmap;
	int startLevel;
	Tile[][][] surfaceGroundTiles;
	GlTile[][] surfaceHdTiles;
	int[][][] surfaceTileHeights;
	byte[][][] tileAngles;
	int[][][] tileHeights;
	byte[][][] tileOverlays;
	byte[][][] tileShapes;
	byte[][][] tileUnderlays;
	int[][] tileWaterData;
	Tile[][][] tiles;
	Tile[][][] underWaterGroundTiles;
	GlTile[][] underWaterHdTiles;
	GlTile[][] underwaterHdTiles;
	int[][][] underwaterTileHeights;
	int visibility;
	boolean[][] visibilityPoints;
	boolean[][] visibleTiles;
	int width;
	byte[][][] renderFlags;
	int centralZoneX;
	int centralZoneY;
	int centralPlane;
	boolean dynamicMapRegion;

	// Camera
	int originX;
	int originY;

	// LoginManager
	Map map;
	MapElementList mapElementList;

	// LightingManager
	Light[] lights;
	int lightCount;
	int[][][] lightGrid;

	// FogManager
	int currentLightX;
	int currentLightY;
	int currentLightZ;
	int lightX;
	int lightZ;
	boolean instantScreenFade;
	float[] light0Position;
	float[] light1Position;
	Environment[][] chunksAtmosphere;

	// ShadowManager
	Shadow[][] shadows;
	SoftwareIndexedSprite shadowMapImage;

	// PathFinder
	CollisionMap[] collisionMaps;

	// AreaSoundManager
	LinkedList locSounds;

	public void capture() {
		this.activeOccluderCount = SceneGraph.activeOccluderCount;
		this.activeOccluders = SceneGraph.activeOccluders;
		this.firstVisibleLevel = SceneGraph.firstVisibleLevel;
		this.hueJitter = SceneGraph.hueJitter;
		this.length = SceneGraph.length;
		this.levels = SceneGraph.levels;
		this.lightnessJitter = SceneGraph.lightnessJitter;
		this.occludeFlags = SceneGraph.occludeFlags;
		this.occluderCount = SceneGraph.occluderCount;
		this.occluders = SceneGraph.occluders;
		this.occlusionCache = SceneGraph.occlusionCache;
		this.occlusionHeights = SceneGraph.occlusionHeights;
		this.rowChroma = SceneGraph.rowChroma;
		this.rowCount = SceneGraph.rowCount;
		this.rowLightness = SceneGraph.rowLightness;
		this.rowSaturation = SceneGraph.rowSaturation;
		this.rowWeightedHue = SceneGraph.rowWeightedHue;
		this.scenery = SceneGraph.scenery;
		this.sceneryBuffer = SceneGraph.sceneryBuffer;
		this.sceneryLen = SceneGraph.sceneryLen;
		this.shadowmap = SceneGraph.shadowmap;
		this.startLevel = SceneGraph.startLevel;
		this.surfaceGroundTiles = SceneGraph.surfaceGroundTiles;
		this.surfaceHdTiles = SceneGraph.surfaceHdTiles;
		this.surfaceTileHeights = SceneGraph.surfaceTileHeights;
		this.tileAngles = SceneGraph.tileAngles;
		this.tileHeights = SceneGraph.tileHeights;
		this.tileOverlays = SceneGraph.tileOverlays;
		this.tileShapes = SceneGraph.tileShapes;
		this.tileUnderlays = SceneGraph.tileUnderlays;
		this.tileWaterData = SceneGraph.tileWaterData;
		this.tiles = SceneGraph.tiles;
		this.underWaterGroundTiles = SceneGraph.underWaterGroundTiles;
		this.underWaterHdTiles = SceneGraph.underWaterHdTiles;
		this.underwaterHdTiles = SceneGraph.underwaterHdTiles;
		this.underwaterTileHeights = SceneGraph.underwaterTileHeights;
		this.visibility = SceneGraph.visibility;
		this.visibilityPoints = SceneGraph.visibilityPoints;
		this.visibleTiles = SceneGraph.visibleTiles;
		this.width = SceneGraph.width;
		this.renderFlags = SceneGraph.renderFlags;
		this.centralZoneX = SceneGraph.centralZoneX;
		this.centralZoneY = SceneGraph.centralZoneY;
		this.centralPlane = SceneGraph.centralPlane;
		this.dynamicMapRegion = SceneGraph.dynamicMapRegion;

		this.originX = Camera.originX;
		this.originY = Camera.originY;

		this.map = LoginManager.map;
		this.mapElementList = LoginManager.mapElementList;

		this.lights = LightingManager.lights;
		this.lightCount = LightingManager.lightCount;
		this.lightGrid = LightingManager.lightGrid;

		this.currentLightX = FogManager.currentLightX;
		this.currentLightY = FogManager.currentLightY;
		this.currentLightZ = FogManager.currentLightZ;
		this.lightX = FogManager.lightX;
		this.lightZ = FogManager.lightZ;
		this.instantScreenFade = FogManager.instantScreenFade;
		this.light0Position = FogManager.light0Position;
		this.light1Position = FogManager.light1Position;
		this.chunksAtmosphere = FogManager.chunksAtmosphere;

		this.shadows = ShadowManager.shadows;
		this.shadowMapImage = ShadowManager.shadowMapImage;

		this.collisionMaps = PathFinder.collisionMaps;

		this.locSounds = AreaSoundManager.locSounds;
	}

	public void apply() {
		SceneGraph.activeOccluderCount = this.activeOccluderCount;
		SceneGraph.activeOccluders = this.activeOccluders;
		SceneGraph.firstVisibleLevel = this.firstVisibleLevel;
		SceneGraph.hueJitter = this.hueJitter;
		SceneGraph.length = this.length;
		SceneGraph.levels = this.levels;
		SceneGraph.lightnessJitter = this.lightnessJitter;
		SceneGraph.occludeFlags = this.occludeFlags;
		SceneGraph.occluderCount = this.occluderCount;
		SceneGraph.occluders = this.occluders;
		SceneGraph.occlusionCache = this.occlusionCache;
		SceneGraph.occlusionHeights = this.occlusionHeights;
		SceneGraph.rowChroma = this.rowChroma;
		SceneGraph.rowCount = this.rowCount;
		SceneGraph.rowLightness = this.rowLightness;
		SceneGraph.rowSaturation = this.rowSaturation;
		SceneGraph.rowWeightedHue = this.rowWeightedHue;
		SceneGraph.scenery = this.scenery;
		SceneGraph.sceneryBuffer = this.sceneryBuffer;
		SceneGraph.sceneryLen = this.sceneryLen;
		SceneGraph.shadowmap = this.shadowmap;
		SceneGraph.startLevel = this.startLevel;
		SceneGraph.surfaceGroundTiles = this.surfaceGroundTiles;
		SceneGraph.surfaceHdTiles = this.surfaceHdTiles;
		SceneGraph.surfaceTileHeights = this.surfaceTileHeights;
		SceneGraph.tileAngles = this.tileAngles;
		SceneGraph.tileHeights = this.tileHeights;
		SceneGraph.tileOverlays = this.tileOverlays;
		SceneGraph.tileShapes = this.tileShapes;
		SceneGraph.tileUnderlays = this.tileUnderlays;
		SceneGraph.tileWaterData = this.tileWaterData;
		SceneGraph.tiles = this.tiles;
		SceneGraph.underWaterGroundTiles = this.underWaterGroundTiles;
		SceneGraph.underWaterHdTiles = this.underWaterHdTiles;
		SceneGraph.underwaterHdTiles = this.underwaterHdTiles;
		SceneGraph.underwaterTileHeights = this.underwaterTileHeights;
		SceneGraph.visibility = this.visibility;
		SceneGraph.visibilityPoints = this.visibilityPoints;
		SceneGraph.visibleTiles = this.visibleTiles;
		SceneGraph.width = this.width;
		SceneGraph.renderFlags = this.renderFlags;
		SceneGraph.centralZoneX = this.centralZoneX;
		SceneGraph.centralZoneY = this.centralZoneY;
		SceneGraph.centralPlane = this.centralPlane;
		SceneGraph.dynamicMapRegion = this.dynamicMapRegion;

		Camera.originX = this.originX;
		Camera.originY = this.originY;

		LoginManager.map = this.map;
		LoginManager.mapElementList = this.mapElementList;

		LightingManager.lights = this.lights;
		LightingManager.lightCount = this.lightCount;
		LightingManager.lightGrid = this.lightGrid;

		FogManager.currentLightX = this.currentLightX;
		FogManager.currentLightY = this.currentLightY;
		FogManager.currentLightZ = this.currentLightZ;
		FogManager.lightX = this.lightX;
		FogManager.lightZ = this.lightZ;
		FogManager.instantScreenFade = this.instantScreenFade;
		FogManager.light0Position = this.light0Position;
		FogManager.light1Position = this.light1Position;
		FogManager.chunksAtmosphere = this.chunksAtmosphere;

		ShadowManager.shadows = this.shadows;
		ShadowManager.shadowMapImage = this.shadowMapImage;

		PathFinder.collisionMaps = this.collisionMaps;

		AreaSoundManager.locSounds = this.locSounds;
	}

	public void copyFrom(SceneState other) {
		this.activeOccluderCount = other.activeOccluderCount;
		this.activeOccluders = other.activeOccluders;
		this.firstVisibleLevel = other.firstVisibleLevel;
		this.hueJitter = other.hueJitter;
		this.length = other.length;
		this.levels = other.levels;
		this.lightnessJitter = other.lightnessJitter;
		this.occludeFlags = other.occludeFlags;
		this.occluderCount = other.occluderCount;
		this.occluders = other.occluders;
		this.occlusionCache = other.occlusionCache;
		this.occlusionHeights = other.occlusionHeights;
		this.rowChroma = other.rowChroma;
		this.rowCount = other.rowCount;
		this.rowLightness = other.rowLightness;
		this.rowSaturation = other.rowSaturation;
		this.rowWeightedHue = other.rowWeightedHue;
		this.scenery = other.scenery;
		this.sceneryBuffer = other.sceneryBuffer;
		this.sceneryLen = other.sceneryLen;
		this.shadowmap = other.shadowmap;
		this.startLevel = other.startLevel;
		this.surfaceGroundTiles = other.surfaceGroundTiles;
		this.surfaceHdTiles = other.surfaceHdTiles;
		this.surfaceTileHeights = other.surfaceTileHeights;
		this.tileAngles = other.tileAngles;
		this.tileHeights = other.tileHeights;
		this.tileOverlays = other.tileOverlays;
		this.tileShapes = other.tileShapes;
		this.tileUnderlays = other.tileUnderlays;
		this.tileWaterData = other.tileWaterData;
		this.tiles = other.tiles;
		this.underWaterGroundTiles = other.underWaterGroundTiles;
		this.underWaterHdTiles = other.underWaterHdTiles;
		this.underwaterHdTiles = other.underwaterHdTiles;
		this.underwaterTileHeights = other.underwaterTileHeights;
		this.visibility = other.visibility;
		this.visibilityPoints = other.visibilityPoints;
		this.visibleTiles = other.visibleTiles;
		this.width = other.width;
		this.renderFlags = other.renderFlags;
		this.centralZoneX = other.centralZoneX;
		this.centralZoneY = other.centralZoneY;
		this.centralPlane = other.centralPlane;
		this.dynamicMapRegion = other.dynamicMapRegion;
		this.originX = other.originX;
		this.originY = other.originY;
		this.map = other.map;
		this.mapElementList = other.mapElementList;
		this.lights = other.lights;
		this.lightCount = other.lightCount;
		this.lightGrid = other.lightGrid;
		this.currentLightX = other.currentLightX;
		this.currentLightY = other.currentLightY;
		this.currentLightZ = other.currentLightZ;
		this.lightX = other.lightX;
		this.lightZ = other.lightZ;
		this.instantScreenFade = other.instantScreenFade;
		this.light0Position = other.light0Position;
		this.light1Position = other.light1Position;
		this.chunksAtmosphere = other.chunksAtmosphere;
		this.shadows = other.shadows;
		this.shadowMapImage = other.shadowMapImage;
		this.collisionMaps = other.collisionMaps;
		this.locSounds = other.locSounds;
	}
}
