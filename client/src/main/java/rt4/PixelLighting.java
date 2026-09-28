package rt4;

import com.jogamp.opengl.GL2;

// Not part of the original client. Stage 5 step 1: per-pixel lighting for the
// 3D scene (GlobalConfig.PIXEL_LIGHTING, toggled in game with ::pixellight).
//
// A GLSL program that does exactly what the fixed-function pipeline does for
// the scene (vertex colour as ambient + diffuse material, the sun and its
// negative back light, up to four local point lights, texture modulate or
// replace, linear fog), except that the lighting is evaluated per pixel from
// interpolated normals instead of per vertex. It reads the same GL light and
// fog state the client already sets (gl_LightSource, gl_LightModel, gl_Fog),
// so nothing else about lighting changes.
//
// The client's normals aren't unit length (their length carries each model's
// contrast, see GlModel.packInterleavedVertexData) and GL_NORMALIZE is off, so
// the length is interpolated separately and reapplied per pixel.
//
// Step 2: sun shadows. Before the scene, renderShadowMap draws it again from
// the sun into a depth texture (GlobalConfig.SHADOWS, ::shadows); the sun's
// diffuse light is then removed per pixel where that map says it's blocked.
//
// It only runs while the scene is drawn (beginScene/endScene) and only for
// state it reproduces: plain materials, modulate/replace texturing. Anything
// else (water, specular and other materials, the additive light-mesh pass,
// shadows) falls back to fixed function automatically.
public final class PixelLighting {

	private static final String VERTEX = String.join("\n",
		"#version 120",
		"varying vec3 vNormal;",
		"varying float vNormalLength;",
		"varying vec3 vEyePos;",
		"varying float vFogDepth;",
		"varying vec4 vShadowCoord;",
		"uniform mat4 uShadowMatrix;",
		"void main() {",
		"	vec4 eye = gl_ModelViewMatrix * gl_Vertex;",
		"	vShadowCoord = uShadowMatrix * eye;",
		"	vEyePos = eye.xyz;",
		"	vec3 n = gl_NormalMatrix * gl_Normal;",
		"	vNormalLength = length(n);",
		"	vNormal = n;",
		"	vFogDepth = abs(eye.z);",
		"	gl_FrontColor = gl_Color;",
		"	gl_TexCoord[0] = gl_TextureMatrix[0] * gl_MultiTexCoord0;",
		"	gl_Position = gl_ProjectionMatrix * eye;",
		"}");

	private static final String FRAGMENT = String.join("\n",
		"#version 120",
		"uniform sampler2D uTexture;",
		"uniform bool uTextured;",
		"uniform bool uLighting;",
		"uniform int uRgbMode;",
		"uniform int uAlphaMode;",
		"uniform bool uFog;",
		"uniform bool uLocal0;",
		"uniform bool uLocal1;",
		"uniform bool uLocal2;",
		"uniform bool uLocal3;",
		"uniform bool uShadows;",
		"uniform sampler2D uShadowMap;",
		"uniform float uShadowTexel;",
		"uniform bool uShadowDebug;",
		"varying vec4 vShadowCoord;",
		"varying vec3 vNormal;",
		"varying float vNormalLength;",
		"varying vec3 vEyePos;",
		"varying float vFogDepth;",
		"vec3 directional(gl_LightSourceParameters light, vec3 n) {",
		"	vec3 l = normalize(light.position.xyz);",
		"	return light.diffuse.rgb * max(dot(n, l), 0.0);",
		"}",
		"vec3 point(gl_LightSourceParameters light, vec3 n) {",
		"	vec3 toLight = light.position.xyz - vEyePos;",
		"	float d = length(toLight);",
		"	float attenuation = 1.0 / max(light.constantAttenuation + light.linearAttenuation * d + light.quadraticAttenuation * d * d, 1e-6);",
		"	return light.diffuse.rgb * max(dot(n, toLight / d), 0.0) * attenuation;",
		"}",
		"float sunVisibility() {",
		"	vec3 c = vShadowCoord.xyz / vShadowCoord.w;",
		"	vec2 edge = abs(c.xy - 0.5) * 2.0;",
		"	float reach = max(edge.x, edge.y);",
		"	if (reach >= 1.0 || c.z >= 1.0) return 1.0;",
		"	float z = c.z - 0.00015;",
		"	float lit = 0.0;",
		"	for (int i = -1; i <= 1; i++) {",
		"		for (int j = -1; j <= 1; j++) {",
		"			float stored = texture2D(uShadowMap, c.xy + vec2(float(i), float(j)) * uShadowTexel).r;",
		"			lit += z <= stored ? 1.0 : 0.0;",
		"		}",
		"	}",
		"	return mix(lit / 9.0, 1.0, smoothstep(0.8, 1.0, reach));",
		"}",
		"void main() {",
		"	vec4 colour = gl_Color;",
		"	if (uLighting) {",
		"		vec3 n = vNormalLength > 0.0 ? normalize(vNormal) * vNormalLength : vec3(0.0);",
		"		vec3 light = gl_LightModel.ambient.rgb;",
		"		vec3 sun = directional(gl_LightSource[0], n);",
		"		if (uShadows && dot(sun, sun) > 0.0) sun *= sunVisibility();",
		"		light += sun;",
		"		light += directional(gl_LightSource[1], n);",
		"		if (uLocal0) light += point(gl_LightSource[4], n);",
		"		if (uLocal1) light += point(gl_LightSource[5], n);",
		"		if (uLocal2) light += point(gl_LightSource[6], n);",
		"		if (uLocal3) light += point(gl_LightSource[7], n);",
		"		colour.rgb = clamp(colour.rgb * light, 0.0, 1.0);",
		"	}",
		"	if (uTextured) {",
		"		vec4 texel = texture2D(uTexture, gl_TexCoord[0].st);",
		"		colour.rgb = uRgbMode == 1 ? texel.rgb : colour.rgb * texel.rgb;",
		"		if (uAlphaMode == 0) colour.a *= texel.a;",
		"		else if (uAlphaMode == 1) colour.a = texel.a;",
		"		else colour.a = min(colour.a + texel.a, 1.0);",
		"	}",
		"	if (uShadowDebug && uShadows) {",
		"		vec3 c = vShadowCoord.xyz / vShadowCoord.w;",
		"		vec2 edge = abs(c.xy - 0.5) * 2.0;",
		"		if (max(edge.x, edge.y) >= 1.0 || c.z >= 1.0 || c.z <= 0.0) colour.rgb = mix(colour.rgb, vec3(0.0, 0.0, 1.0), 0.5);",
		"		else if (sunVisibility() < 0.5) colour.rgb = mix(colour.rgb, vec3(1.0, 0.0, 0.0), 0.6);",
		"	}",
		"	if (uFog) {",
		"		float f = clamp((gl_Fog.end - vFogDepth) * gl_Fog.scale, 0.0, 1.0);",
		"		colour.rgb = mix(gl_Fog.color.rgb, colour.rgb, f);",
		"	}",
		"	gl_FragColor = colour;",
		"}");

	private static int program;
	private static boolean failed;
	private static int uTexture, uTextured, uLighting, uRgbMode, uAlphaMode, uFog;
	private static final int[] uLocal = new int[4];
	private static int uShadows, uShadowMap, uShadowMatrix, uShadowTexel, uShadowDebug;
	// ::shadowdebug: red = in shadow, blue = outside the shadow map
	public static boolean shadowDebug;

	// Shadow maps (depth textures, same size): the static one holds scenery
	// and is redrawn only now and then; each frame it's copied into the
	// sampled one (texture unit SHADOW_UNIT) and moving entities are added
	private static final int SHADOW_UNIT = 2;
	private static int shadowTexture;
	private static int shadowFramebuffer;
	private static int staticTexture;
	private static int staticFramebuffer;
	private static int shadowSize;
	private static boolean shadowFailed;
	// This frame's map is valid for the scene about to be drawn
	private static boolean shadowsReady;
	// Main pass eye space -> shadow map texture coordinates
	private static final float[] shadowMatrix = new float[16];

	// True while drawing into a shadow map (GlModel draws depth only, culled
	// against the shadow area; SceneGraph and LightingManager skip camera-only
	// work and, for the static map, moving entities)
	public static boolean shadowPass;
	// Light view and the shadow area in light space (fixed while the static
	// map is valid)
	private static final float[] lightView = new float[16];
	private static final float[] lightRight = new float[3];
	private static final float[] lightUp = new float[3];
	private static float[] lightProjection;
	private static float lightCenterX, lightCenterY, shadowHalfSize;
	private static float passCameraX, passCameraY, passCameraZ;
	private static int anchorTileX, anchorTileY;
	// What the static map was drawn for
	private static boolean staticValid;
	private static float staticSunX, staticSunY, staticSunZ;
	private static int staticOriginX, staticOriginY, staticPlane, staticDistance;
	private static long staticDrawnAt;

	// Mirrors of the GL state the program reproduces
	private static boolean textured;
	private static boolean lighting = true;
	private static int rgbMode;
	private static int alphaMode;
	private static boolean fog = true;
	private static int materialType;
	private static final boolean[] localLights = new boolean[4];

	private static boolean inScene;
	private static int suspended;
	private static boolean active;

	private PixelLighting() {
	}

	// --- scene scope ---

	public static void beginScene() {
		inScene = GlobalConfig.PIXEL_LIGHTING && ensureProgram();
		refresh();
	}

	public static void endScene() {
		inScene = false;
		refresh();
		shadowsReady = false;
	}

	// Real shadows replace the old baked scenery shadows
	public static boolean shadowsActive() {
		return shadowsReady;
	}

	// --- shadow map ---

	// Updates the shadow map for this frame. Call after the camera's view is
	// set up (the modelview holds the camera rotation) and before beginScene,
	// once this frame's entities are in the scene. cam* are the camera's scene
	// coordinates (x, height, y), focus* the point shadows are centred on,
	// drawScene draws the scene (setPlainTile) for the static map.
	public static void renderShadowMap(int camX, int camZ, int camY, int focusX, int focusZ, int focusY, Runnable drawScene) {
		shadowsReady = false;
		if (!GlobalConfig.PIXEL_LIGHTING || !GlobalConfig.SHADOWS || !ensureProgram() || !ensureShadowTargets()) {
			return;
		}
		float sx = FogManager.light0Position[0], sy = FogManager.light0Position[1], sz = FogManager.light0Position[2];
		if (sx == 0.0F && sy == 0.0F && sz == 0.0F) {
			return;
		}
		GL2 gl = GlRenderer.gl;
		float[] cameraRotation = new float[16];
		gl.glGetFloatv(GL2.GL_MODELVIEW_MATRIX, cameraRotation, 0);
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
		boolean scissor = gl.glIsEnabled(GL2.GL_SCISSOR_TEST);

		int focusTileX = focusX >> 7;
		int focusTileY = focusY >> 7;
		long now = System.nanoTime();
		boolean redrawStatic = !staticValid
			|| sx != staticSunX || sy != staticSunY || sz != staticSunZ
			|| Camera.originX != staticOriginX || Camera.originY != staticOriginY || Player.plane != staticPlane
			|| GlobalConfig.SHADOW_DISTANCE != staticDistance
			|| Math.abs(focusTileX - anchorTileX) > GlobalConfig.SHADOW_REANCHOR_TILES || Math.abs(focusTileY - anchorTileY) > GlobalConfig.SHADOW_REANCHOR_TILES
			|| now - staticDrawnAt > GlobalConfig.SHADOW_REFRESH_MS * 1000000L;
		if (redrawStatic) {
			setupLight(sx, sy, sz, focusX, focusZ, focusY);
			anchorTileX = focusTileX;
			anchorTileY = focusTileY;
		}

		// Models are drawn relative to the camera, the ground under a
		// translation to -camera (SceneGraph.setShapedTile), so a pass's base
		// modelview is lightView * translate(camera)
		float[] base = multiply(lightView, translation(camX, camZ, camY));
		passCameraX = camX;
		passCameraY = camZ;
		passCameraZ = camY;

		if (scissor) {
			gl.glDisable(GL2.GL_SCISSOR_TEST);
		}
		gl.glViewport(0, 0, shadowSize, shadowSize);
		gl.glColorMask(false, false, false, false);
		gl.glDepthMask(true);
		gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(2.0F, 4.0F);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadMatrixf(lightProjection, 0);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glLoadMatrixf(base, 0);
		boolean allowInput = RawModel.allowInput;
		boolean walkPending = MiniMenu.walkPending;
		RawModel.allowInput = false;
		MiniMenu.walkPending = false;
		shadowPass = true;
		try {
			if (redrawStatic) {
				gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, staticFramebuffer);
				gl.glClear(GL2.GL_DEPTH_BUFFER_BIT);
				drawScene.run();
				staticValid = true;
				staticSunX = sx;
				staticSunY = sy;
				staticSunZ = sz;
				staticOriginX = Camera.originX;
				staticOriginY = Camera.originY;
				staticPlane = Player.plane;
				staticDistance = GlobalConfig.SHADOW_DISTANCE;
				staticDrawnAt = now;
			}
			// This frame: the static map plus everything that moves
			gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, staticFramebuffer);
			gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, shadowFramebuffer);
			gl.glBlitFramebuffer(0, 0, shadowSize, shadowSize, 0, 0, shadowSize, shadowSize, GL2.GL_DEPTH_BUFFER_BIT, GL2.GL_NEAREST);
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, shadowFramebuffer);
			for (int i = 0; i < SceneGraph.sceneryLen; i++) {
				Scenery s = SceneGraph.scenery[i];
				if (s != null && s.entity != null) {
					s.entity.render(s.orientation, SceneGraph.sinPitch, SceneGraph.cosPitch, SceneGraph.sinYaw, SceneGraph.cosYaw, s.xFine - camX, s.zFine - camZ, s.yFine - camY, s.key, s.level, null);
				}
			}
		} finally {
			shadowPass = false;
			RawModel.allowInput = allowInput;
			MiniMenu.walkPending = walkPending;
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glPopMatrix();
			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glPopMatrix();
			gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);
			gl.glColorMask(true, true, true, true);
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			gl.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
			if (scissor) {
				gl.glEnable(GL2.GL_SCISSOR_TEST);
			}
		}

		// Main pass eye space -> camera-relative (the camera rotation is
		// orthonormal, so its inverse is its transpose) -> light clip space
		// -> [0, 1] texture space
		float[] inverseRotation = new float[16];
		for (int col = 0; col < 3; col++) {
			for (int row = 0; row < 3; row++) {
				inverseRotation[col * 4 + row] = cameraRotation[row * 4 + col];
			}
		}
		inverseRotation[15] = 1.0F;
		float[] bias = {
			0.5F, 0.0F, 0.0F, 0.0F,
			0.0F, 0.5F, 0.0F, 0.0F,
			0.0F, 0.0F, 0.5F, 0.0F,
			0.5F, 0.5F, 0.5F, 1.0F
		};
		float[] m = multiply(bias, multiply(lightProjection, multiply(base, inverseRotation)));
		System.arraycopy(m, 0, shadowMatrix, 0, 16);
		shadowsReady = true;
	}


	// Light view from the sun (light0 points towards it) and the shadow area
	// around the focus, snapped to whole texels in light space so shadows
	// don't shimmer when it moves
	private static void setupLight(float sx, float sy, float sz, int focusX, int focusZ, int focusY) {
		float length = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		float fx = -sx / length, fy = -sy / length, fz = -sz / length;
		float ux = 0.0F, uy = 1.0F, uz = 0.0F;
		if (Math.abs(fy) > 0.99F) {
			ux = 1.0F;
			uy = 0.0F;
		}
		float rx = fy * uz - fz * uy, ry = fz * ux - fx * uz, rz = fx * uy - fy * ux;
		float rl = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
		rx /= rl;
		ry /= rl;
		rz /= rl;
		ux = ry * fz - rz * fy;
		uy = rz * fx - rx * fz;
		uz = rx * fy - ry * fx;
		float[] view = {
			rx, ux, -fx, 0.0F,
			ry, uy, -fy, 0.0F,
			rz, uz, -fz, 0.0F,
			0.0F, 0.0F, 0.0F, 1.0F
		};
		System.arraycopy(view, 0, lightView, 0, 16);
		lightRight[0] = rx;
		lightRight[1] = ry;
		lightRight[2] = rz;
		lightUp[0] = ux;
		lightUp[1] = uy;
		lightUp[2] = uz;
		float half = GlobalConfig.SHADOW_DISTANCE * 128.0F;
		float texel = 2.0F * half / shadowSize;
		float cx = rx * focusX + ry * focusZ + rz * focusY;
		float cy = ux * focusX + uy * focusZ + uz * focusY;
		float cz = -fx * focusX - fy * focusZ - fz * focusY;
		cx = (float) Math.floor(cx / texel) * texel;
		cy = (float) Math.floor(cy / texel) * texel;
		lightCenterX = cx;
		lightCenterY = cy;
		shadowHalfSize = half;
		float depth = 16384.0F;
		lightProjection = ortho(cx - half, cx + half, cy - half, cy + half, -cz - depth, -cz + depth);
	}

	// Shadow pass: whether a model at this camera-relative position (with its
	// bounds) can cast into the shadow area
	public static boolean castsShadow(int sceneX, int sceneY, int sceneZ, int radius, int minY, int maxY) {
		// Ground clutter (grass, flowers, flat decorations): not worth a draw
		if (maxY - minY < GlobalConfig.SHADOW_MIN_HEIGHT) {
			return false;
		}
		float x = sceneX + passCameraX, y = sceneY + passCameraY, z = sceneZ + passCameraZ;
		float lx = lightRight[0] * x + lightRight[1] * y + lightRight[2] * z - lightCenterX;
		float ly = lightUp[0] * x + lightUp[1] * y + lightUp[2] * z - lightCenterY;
		float reach = radius + Math.max(Math.abs(minY), Math.abs(maxY));
		return Math.abs(lx) <= shadowHalfSize + reach && Math.abs(ly) <= shadowHalfSize + reach;
	}

	private static boolean ensureShadowTargets() {
		int size = GlobalConfig.SHADOW_MAP_SIZE;
		if (shadowFramebuffer != 0 && shadowSize == size) {
			return true;
		}
		if (shadowFailed) {
			return false;
		}
		GL2 gl = GlRenderer.gl;
		try {
			if (!gl.isFunctionAvailable("glGenFramebuffers") || !gl.isFunctionAvailable("glBlitFramebuffer")) {
				throw new IllegalStateException("framebuffers not supported");
			}
			deleteShadowTargets(gl);
			int[] sampled = createDepthTarget(gl, size, true);
			shadowTexture = sampled[0];
			shadowFramebuffer = sampled[1];
			int[] stat = createDepthTarget(gl, size, false);
			staticTexture = stat[0];
			staticFramebuffer = stat[1];
			shadowSize = size;
			staticValid = false;
			System.out.println("[PixelLighting] shadow maps " + size + "x" + size);
			return true;
		} catch (RuntimeException e) {
			shadowFailed = true;
			shadowFramebuffer = 0;
			System.err.println("[PixelLighting] shadows disabled: " + e.getMessage());
			return false;
		}
	}

	private static void deleteShadowTargets(GL2 gl) {
		int[] id = new int[1];
		for (int texture : new int[]{shadowTexture, staticTexture}) {
			if (texture != 0) {
				id[0] = texture;
				gl.glDeleteTextures(1, id, 0);
			}
		}
		for (int framebuffer : new int[]{shadowFramebuffer, staticFramebuffer}) {
			if (framebuffer != 0) {
				id[0] = framebuffer;
				gl.glDeleteFramebuffers(1, id, 0);
			}
		}
		shadowTexture = staticTexture = shadowFramebuffer = staticFramebuffer = 0;
	}

	// A depth texture and a framebuffer drawing into it; the sampled one is
	// left bound on SHADOW_UNIT
	private static int[] createDepthTarget(GL2 gl, int size, boolean sampled) {
		int[] id = new int[1];
		gl.glActiveTexture(GL2.GL_TEXTURE0 + SHADOW_UNIT);
		gl.glGenTextures(1, id, 0);
		int texture = id[0];
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_DEPTH_COMPONENT24, size, size, 0, GL2.GL_DEPTH_COMPONENT, GL2.GL_UNSIGNED_INT, null);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		// Compared in the shader (a hardware compare-mode lookup read raw
		// depth on this driver)
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_COMPARE_MODE, GL2.GL_NONE);
		gl.glGenFramebuffers(1, id, 0);
		int framebuffer = id[0];
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebuffer);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, GL2.GL_TEXTURE_2D, texture, 0);
		gl.glDrawBuffer(GL2.GL_NONE);
		gl.glReadBuffer(GL2.GL_NONE);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		if (!sampled) {
			// Leave the sampled map bound on the shadow unit
			gl.glBindTexture(GL2.GL_TEXTURE_2D, shadowTexture);
		}
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		if (status != GL2.GL_FRAMEBUFFER_COMPLETE) {
			throw new IllegalStateException("shadow framebuffer incomplete: 0x" + Integer.toHexString(status));
		}
		return new int[]{texture, framebuffer};
	}

	// Shadow pass: tiles worth walking - the shadow area plus the few tiles a
	// tall building's shadow can reach into it from outside
	public static boolean tileInShadowArea(int tileX, int tileY) {
		int reach = GlobalConfig.SHADOW_DISTANCE + 6;
		return Math.abs(tileX - anchorTileX) <= reach && Math.abs(tileY - anchorTileY) <= reach;
	}

	// Column-major 4x4 helpers
	private static float[] multiply(float[] a, float[] b) {
		float[] r = new float[16];
		for (int col = 0; col < 4; col++) {
			for (int row = 0; row < 4; row++) {
				float sum = 0.0F;
				for (int k = 0; k < 4; k++) {
					sum += a[k * 4 + row] * b[col * 4 + k];
				}
				r[col * 4 + row] = sum;
			}
		}
		return r;
	}

	private static float[] translation(float x, float y, float z) {
		return new float[]{1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, x, y, z, 1};
	}

	private static float[] ortho(float l, float r, float b, float t, float n, float f) {
		return new float[]{
			2.0F / (r - l), 0, 0, 0,
			0, 2.0F / (t - b), 0, 0,
			0, 0, -2.0F / (f - n), 0,
			-(r + l) / (r - l), -(t + b) / (t - b), -(f + n) / (f - n), 1
		};
	}


	// Around passes that set up GL state the program doesn't reproduce
	public static void suspend() {
		suspended++;
		refresh();
	}

	public static void resume() {
		suspended--;
		refresh();
	}

	// --- state mirrors (called from GlRenderer / MaterialManager / LightingManager on change) ---

	public static void setTextured(boolean value) {
		textured = value;
		if (active) {
			GlRenderer.gl.glUniform1i(uTextured, value ? 1 : 0);
		}
	}

	public static void setLighting(boolean value) {
		lighting = value;
		if (active) {
			GlRenderer.gl.glUniform1i(uLighting, value ? 1 : 0);
		}
	}

	public static void setRgbMode(int mode) {
		rgbMode = mode;
		refresh();
		if (active) {
			GlRenderer.gl.glUniform1i(uRgbMode, mode);
		}
	}

	public static void setAlphaMode(int mode) {
		alphaMode = mode;
		refresh();
		if (active) {
			GlRenderer.gl.glUniform1i(uAlphaMode, mode);
		}
	}

	public static void setFog(boolean value) {
		fog = value;
		if (active) {
			GlRenderer.gl.glUniform1i(uFog, value ? 1 : 0);
		}
	}

	public static void setMaterialType(int type) {
		if (type != materialType) {
			materialType = type;
			refresh();
		}
	}

	public static void setLocalLight(int slot, boolean enabled) {
		localLights[slot] = enabled;
		if (active) {
			GlRenderer.gl.glUniform1i(uLocal[slot], enabled ? 1 : 0);
		}
	}

	// GL context (re)created: the old program is gone with it
	public static void reset() {
		program = 0;
		failed = false;
		shadowTexture = 0;
		shadowFramebuffer = 0;
		staticTexture = 0;
		staticFramebuffer = 0;
		staticValid = false;
		shadowFailed = false;
		shadowsReady = false;
		shadowPass = false;
		active = false;
		inScene = false;
		suspended = 0;
	}

	// --- internals ---

	private static void refresh() {
		boolean want = inScene && suspended == 0 && materialType == 0 && rgbMode <= 1 && alphaMode <= 2;
		if (want == active) {
			return;
		}
		GL2 gl = GlRenderer.gl;
		active = want;
		if (want) {
			gl.glUseProgram(program);
			gl.glUniform1i(uTexture, 0);
			gl.glUniform1i(uTextured, textured ? 1 : 0);
			gl.glUniform1i(uLighting, lighting ? 1 : 0);
			gl.glUniform1i(uRgbMode, rgbMode);
			gl.glUniform1i(uAlphaMode, alphaMode);
			gl.glUniform1i(uFog, fog ? 1 : 0);
			for (int i = 0; i < 4; i++) {
				gl.glUniform1i(uLocal[i], localLights[i] ? 1 : 0);
			}
			gl.glUniform1i(uShadowMap, SHADOW_UNIT);
			gl.glUniform1i(uShadows, shadowsReady ? 1 : 0);
			gl.glUniform1i(uShadowDebug, shadowDebug ? 1 : 0);
			if (shadowsReady) {
				gl.glUniformMatrix4fv(uShadowMatrix, 1, false, shadowMatrix, 0);
				gl.glUniform1f(uShadowTexel, 1.0F / shadowSize);
			}
		} else {
			gl.glUseProgram(0);
		}
	}

	private static boolean ensureProgram() {
		if (program != 0) {
			return true;
		}
		if (failed) {
			return false;
		}
		GL2 gl = GlRenderer.gl;
		try {
			if (!gl.isFunctionAvailable("glCreateShader") || !gl.isFunctionAvailable("glUseProgram")) {
				throw new IllegalStateException("GLSL not supported");
			}
			int vs = compile(gl, GL2.GL_VERTEX_SHADER, VERTEX);
			int fs = compile(gl, GL2.GL_FRAGMENT_SHADER, FRAGMENT);
			int p = gl.glCreateProgram();
			gl.glAttachShader(p, vs);
			gl.glAttachShader(p, fs);
			gl.glLinkProgram(p);
			int[] status = new int[1];
			gl.glGetProgramiv(p, GL2.GL_LINK_STATUS, status, 0);
			if (status[0] == 0) {
				throw new IllegalStateException("link failed: " + programLog(gl, p));
			}
			uTexture = gl.glGetUniformLocation(p, "uTexture");
			uTextured = gl.glGetUniformLocation(p, "uTextured");
			uLighting = gl.glGetUniformLocation(p, "uLighting");
			uRgbMode = gl.glGetUniformLocation(p, "uRgbMode");
			uAlphaMode = gl.glGetUniformLocation(p, "uAlphaMode");
			uFog = gl.glGetUniformLocation(p, "uFog");
			for (int i = 0; i < 4; i++) {
				uLocal[i] = gl.glGetUniformLocation(p, "uLocal" + i);
			}
			uShadows = gl.glGetUniformLocation(p, "uShadows");
			uShadowMap = gl.glGetUniformLocation(p, "uShadowMap");
			uShadowMatrix = gl.glGetUniformLocation(p, "uShadowMatrix");
			uShadowTexel = gl.glGetUniformLocation(p, "uShadowTexel");
			uShadowDebug = gl.glGetUniformLocation(p, "uShadowDebug");
			program = p;
			System.out.println("[PixelLighting] shader ready");
			return true;
		} catch (RuntimeException e) {
			// Fixed function carries on as before
			failed = true;
			System.err.println("[PixelLighting] disabled: " + e.getMessage());
			return false;
		}
	}

	private static int compile(GL2 gl, int type, String source) {
		int shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, 1, new String[]{source}, null, 0);
		gl.glCompileShader(shader);
		int[] status = new int[1];
		gl.glGetShaderiv(shader, GL2.GL_COMPILE_STATUS, status, 0);
		if (status[0] == 0) {
			int[] length = new int[1];
			gl.glGetShaderiv(shader, GL2.GL_INFO_LOG_LENGTH, length, 0);
			byte[] log = new byte[Math.max(1, length[0])];
			gl.glGetShaderInfoLog(shader, log.length, null, 0, log, 0);
			throw new IllegalStateException((type == GL2.GL_VERTEX_SHADER ? "vertex" : "fragment") + " shader: " + new String(log).trim());
		}
		return shader;
	}

	private static String programLog(GL2 gl, int p) {
		int[] length = new int[1];
		gl.glGetProgramiv(p, GL2.GL_INFO_LOG_LENGTH, length, 0);
		byte[] log = new byte[Math.max(1, length[0])];
		gl.glGetProgramInfoLog(p, log.length, null, 0, log, 0);
		return new String(log).trim();
	}
}
