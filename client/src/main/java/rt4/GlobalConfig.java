package rt4;

import org.openrs2.deob.annotation.OriginalMember;

import java.math.BigInteger;

public class GlobalConfig {
	// Jagex's RSA key:
	// public static final BigInteger RSA_MODULUS = new BigInteger("7162900525229798032761816791230527296329313291232324290237849263501208207972894053929065636522363163621000728841182238772712427862772219676577293600221789");
	// public static final BigInteger RSA_EXPONENT = new BigInteger("58778699976184461502525193738213253649000149147835990136706041084440742975821");

	//region 2009scape-compatibility Configuration
	@OriginalMember(owner = "client!um", name = "V", descriptor = "Ljava/math/BigInteger;")
	public static final BigInteger RSA_MODULUS = new BigInteger("96982303379631821170939875058071478695026608406924780574168393250855797534862289546229721580153879336741968220328805101128831071152160922518190059946555203865621183480223212969502122536662721687753974815205744569357388338433981424032996046420057284324856368815997832596174397728134370577184183004453899764051");

	@OriginalMember(owner = "client!gm", name = "X", descriptor = "Ljava/math/BigInteger;")
	public static final BigInteger RSA_EXPONENT = new BigInteger("65537");

	public static String EXTENDED_CONFIG_PATH = "config.json";

	// Server IP
	public static String DEFAULT_HOSTNAME = "test.2009scape.org";

	// Jagex had this at 40000+id
	public static int DEFAULT_PORT = 43594;

	// Jagex had this at 50000+id/443
	public static int ALTERNATE_PORT = 43593;

	// If this isn't set, the world server will need to send a default
	public static boolean SELECT_DEFAULT_WORLD = true;

	// Send strings instead of base37 for login/registration packets
	public static boolean LOGIN_USE_STRINGS = true;

	// Send additional information like user/serial/mac address
	public static boolean LOGIN_EXTRA_INFO = true;

	// Send an additional empty CRC for idx28 (not in this revision originally)
	public static boolean LOGIN_FAKE_IDX28 = true;

	// Packet opcode encryption
	public static boolean USE_ISAAC = false;
	//endregion

	// default distance is 28
	public static int TILE_DISTANCE = 56;
	public static int VIEW_DISTANCE = TILE_DISTANCE * 128;
	public static float VIEW_FADE_DISTANCE = ((float) TILE_DISTANCE / 28.0f) * 256.0f;

	// Not part of the original client. VIEW_DISTANCE is measured in
	// eye-space distance from the CAMERA, not ground distance from the
	// player, and it's what both FogManager's fog range and GlRenderer's
	// perspective far clip plane (setupPerspectiveView) are built from
	// every frame - so raising it pushes the real hard cutoff out, not
	// just the fog. Deliberately does NOT touch the fixed 104x104 loaded
	// area itself (SceneGraph.width/length) or re-run SceneGraph.init() -
	// those stay exactly as they are; this only widens how much of that
	// already-loaded area the camera is allowed to actually render before
	// clipping.
	public static void setTileDistance(int tiles) {
		TILE_DISTANCE = tiles;
		VIEW_DISTANCE = tiles * 128;
		VIEW_FADE_DISTANCE = ((float) tiles / 28.0f) * 256.0f;
	}

	// Not part of the original client. HD textures are procedural (built
	// from TextureOp graphs in fractional coordinates), so they can be
	// generated at any power-of-two resolution from the same cache data.
	// The original client always used 128 (64 in low detail). Only affects
	// the GL textures built in GlTexture.bind(); SD rendering is untouched.
	public static int HD_TEXTURE_SIZE = 256;

	// Not part of the original client. Max anisotropic filtering samples
	// for mipmapped HD textures (1 = off). Clamped to what the driver
	// supports; ignored if GL_EXT_texture_filter_anisotropic is missing.
	public static float ANISOTROPIC_FILTERING = 16.0F;

	// Not part of the original client. Build new areas in the background
	// while the current one keeps rendering, instead of freezing on
	// "Loading - please wait" (see AsyncSceneLoader). HD only; false restores
	// the original synchronous loading exactly.
	public static boolean ASYNC_SCENE_LOADING = true;

	// Not part of the original client. How long a background area build may
	// run per rendered frame, in ms (on top of the game loop's idle time):
	// predicted builds, which nothing waits on, and requested ones, which hold
	// packets back so the player stops until they finish.
	public static int ASYNC_LOAD_SLICE_MS = 3;
	public static int ASYNC_REQUESTED_SLICE_MS = 25;

	// Not part of the original client. Start building the next area this many
	// tiles before the player reaches the server's rebuild line, so it's ready
	// when the server asks (see AsyncSceneLoader.predict).
	public static boolean ASYNC_PREBUILD = true;

	// Not part of the original client. Ease the player's running off before
	// an area boundary instead of stopping dead on the server's skipped tick
	// (see MovementSmoother).
	public static boolean SMOOTH_AREA_STALLS = true;

	// Not part of the original client. How far from the camera, in tiles, to
	// draw distant terrain and scenery beyond the loaded 104x104 area (see
	// FarTerrain; the FarView plugin sets this in game); 0 = off.
	public static int FAR_TERRAIN_TILES = 200;

	// Not part of the original client. Texture the far terrain like the real
	// ground (false = flat average colours, cheaper).
	public static boolean FAR_TERRAIN_TEXTURES = true;

	// Not part of the original client. Walls, trees, buildings and roofs on
	// the far terrain (see FarScenery).
	public static boolean FAR_SCENERY = true;

	// Not part of the original client. Far centrepieces (trees, rocks,
	// plants, ...) shorter than this (fine units, 128 = 1 tile) are skipped.
	public static int FAR_SCENERY_MIN_HEIGHT = 96;

	// Not part of the original client. Far scenery vertices snap to a grid
	// this size (fine units, 128 = 1 tile) and collapsed triangles are
	// dropped, thinning out detail far away. 0 = full detail (the default:
	// grids large enough to matter also erase thin fence rails and branches).
	public static int FAR_SCENERY_SIMPLIFY = 0;

	// Not part of the original client (was 200). How many generated textures
	// HD keeps. The far view shows ~180 scenery textures on top of the nearby
	// scene's; with too small a cache, textures drawn in the same order every
	// frame evict each other and are regenerated every frame.
	public static int HD_TEXTURE_CACHE = 1000;

	// Not part of the original client. Per-pixel lighting for the 3D scene
	// (PixelLighting); ::pixellight toggles it in game.
	public static boolean PIXEL_LIGHTING = true;

	// Not part of the original client. Writes area load timings
	// (area_load_timing.log) and frame-by-frame captures around area changes
	// (motion_trace.log, Insert marks a moment) to the client's home folder.
	// Background load failures are logged either way.
	public static boolean DIAGNOSTICS = false;
	public static int PREBUILD_LEAD_TILES = 12;

	public static boolean USE_SHIFT_CLICK = true;
	public static boolean USE_TWEENING = true;
	public static boolean BILINEAR_MINIMAP = true;
	public static boolean MOUSEWHEEL_ZOOM = true;

	public static int JS5_RESPONSE_TIMEOUT = 5000;

        public static int AUDIO_SAMPLE_RATE = 22050; //default is 22050, buff is 44100

}
