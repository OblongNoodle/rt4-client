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
		"void main() {",
		"	vec4 eye = gl_ModelViewMatrix * gl_Vertex;",
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
		"void main() {",
		"	vec4 colour = gl_Color;",
		"	if (uLighting) {",
		"		vec3 n = vNormalLength > 0.0 ? normalize(vNormal) * vNormalLength : vec3(0.0);",
		"		vec3 light = gl_LightModel.ambient.rgb;",
		"		light += directional(gl_LightSource[0], n);",
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
