package rt4;

import com.jogamp.opengl.GL2;

// Not part of the original client. Stage 5 step 4: a colour grade over the 3D
// view (GlobalConfig.COLOR_GRADING, strength GlobalConfig.COLOR_GRADING_STRENGTH;
// ::grade toggles, ::grade <0-100> sets the strength).
//
// After the scene is drawn and before overheads and the interface, the view
// is resolved (it's multisampled) into a texture with one blit and drawn
// back through a small shader: a gentle contrast curve, more saturation and
// a little warmth. The scene shader (PixelLighting) also shifts the light
// balance by the same strength: slightly less flat ambient light and more
// sun, so shaded sides read darker than lit ones.
public final class ColorGrading {

	private static final String VERTEX = String.join("\n",
		"#version 120",
		"void main() {",
		"	gl_TexCoord[0] = gl_MultiTexCoord0;",
		"	gl_Position = gl_Vertex;",
		"}");

	private static final String FRAGMENT = String.join("\n",
		"#version 120",
		"uniform sampler2D uScene;",
		"uniform float uStrength;",
		"void main() {",
		"	vec3 c = texture2D(uScene, gl_TexCoord[0].st).rgb;",
		"	vec3 graded = c;",
		"	// Contrast: a soft S-curve around the middle",
		"	vec3 s = graded * graded * (3.0 - 2.0 * graded);",
		"	graded = mix(graded, s, 0.35);",
		"	// Saturation",
		"	float luma = dot(graded, vec3(0.299, 0.587, 0.114));",
		"	graded = mix(vec3(luma), graded, 1.25);",
		"	// Warmth",
		"	graded *= vec3(1.03, 1.0, 0.95);",
		"	gl_FragColor = vec4(clamp(mix(c, graded, uStrength), 0.0, 1.0), 1.0);",
		"}");

	// Scene shader light balance at full strength
	public static final float AMBIENT_AT_FULL = 0.85F;
	public static final float SUN_AT_FULL = 1.25F;

	private static final int UNIT = 3;
	private static int program;
	private static boolean failed;
	private static int uScene;
	private static int uStrength;
	private static int texture;
	private static int framebuffer;
	private static int width;
	private static int height;
	private static boolean verified;

	private ColorGrading() {
	}

	public static float strength() {
		return GlobalConfig.COLOR_GRADING && GlobalConfig.PIXEL_LIGHTING ? GlobalConfig.COLOR_GRADING_STRENGTH / 100.0F : 0.0F;
	}

	// GL context (re)created
	public static void reset() {
		program = 0;
		failed = false;
		texture = 0;
		framebuffer = 0;
		width = 0;
		height = 0;
		verified = false;
	}

	// After the 3D scene, while its viewport is still set
	public static void apply() {
		float strength = strength();
		if (strength <= 0.0F || !ensureProgram()) {
			return;
		}
		GL2 gl = GlRenderer.gl;
		int[] viewport = new int[4];
		gl.glGetIntegerv(GL2.GL_VIEWPORT, viewport, 0);
		int w = viewport[2];
		int h = viewport[3];
		if (w <= 0 || h <= 0 || !ensureTarget(gl, w, h)) {
			return;
		}
		// Resolve the (multisampled) view into the texture
		gl.glBindFramebuffer(GL2.GL_READ_FRAMEBUFFER, 0);
		gl.glBindFramebuffer(GL2.GL_DRAW_FRAMEBUFFER, framebuffer);
		if (!verified) {
			while (gl.glGetError() != GL2.GL_NO_ERROR) {
			}
		}
		gl.glBlitFramebuffer(viewport[0], viewport[1], viewport[0] + w, viewport[1] + h, 0, 0, w, h, GL2.GL_COLOR_BUFFER_BIT, GL2.GL_NEAREST);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		if (!verified) {
			// A driver that can't resolve the view this way would leave the
			// texture empty: give up rather than draw it
			int error = gl.glGetError();
			if (error != GL2.GL_NO_ERROR) {
				failed = true;
				program = 0;
				System.err.println("[ColorGrading] disabled: resolve failed, GL error 0x" + Integer.toHexString(error));
				return;
			}
			verified = true;
			System.out.println("[ColorGrading] ready");
		}

		// Draw it back graded
		boolean depthTest = gl.glIsEnabled(GL2.GL_DEPTH_TEST);
		boolean cull = gl.glIsEnabled(GL2.GL_CULL_FACE);
		boolean blend = gl.glIsEnabled(GL2.GL_BLEND);
		gl.glDisable(GL2.GL_DEPTH_TEST);
		gl.glDisable(GL2.GL_CULL_FACE);
		gl.glDisable(GL2.GL_BLEND);
		gl.glDepthMask(false);
		gl.glActiveTexture(GL2.GL_TEXTURE0 + UNIT);
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		gl.glUseProgram(program);
		gl.glUniform1i(uScene, UNIT);
		gl.glUniform1f(uStrength, strength);
		gl.glBegin(GL2.GL_QUADS);
		gl.glTexCoord2f(0.0F, 0.0F);
		gl.glVertex2f(-1.0F, -1.0F);
		gl.glTexCoord2f(1.0F, 0.0F);
		gl.glVertex2f(1.0F, -1.0F);
		gl.glTexCoord2f(1.0F, 1.0F);
		gl.glVertex2f(1.0F, 1.0F);
		gl.glTexCoord2f(0.0F, 1.0F);
		gl.glVertex2f(-1.0F, 1.0F);
		gl.glEnd();
		gl.glUseProgram(0);
		gl.glDepthMask(true);
		if (depthTest) {
			gl.glEnable(GL2.GL_DEPTH_TEST);
		}
		if (cull) {
			gl.glEnable(GL2.GL_CULL_FACE);
		}
		if (blend) {
			gl.glEnable(GL2.GL_BLEND);
		}
	}

	private static boolean ensureTarget(GL2 gl, int w, int h) {
		if (framebuffer != 0 && w == width && h == height) {
			return true;
		}
		int[] id = new int[1];
		if (texture != 0) {
			id[0] = texture;
			gl.glDeleteTextures(1, id, 0);
		}
		if (framebuffer != 0) {
			id[0] = framebuffer;
			gl.glDeleteFramebuffers(1, id, 0);
		}
		gl.glActiveTexture(GL2.GL_TEXTURE0 + UNIT);
		gl.glGenTextures(1, id, 0);
		texture = id[0];
		gl.glBindTexture(GL2.GL_TEXTURE_2D, texture);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA8, w, h, 0, GL2.GL_RGBA, GL2.GL_UNSIGNED_BYTE, null);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_NEAREST);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
		gl.glActiveTexture(GL2.GL_TEXTURE0);
		gl.glGenFramebuffers(1, id, 0);
		framebuffer = id[0];
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, framebuffer);
		gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, GL2.GL_COLOR_ATTACHMENT0, GL2.GL_TEXTURE_2D, texture, 0);
		int status = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
		if (status != GL2.GL_FRAMEBUFFER_COMPLETE) {
			failed = true;
			System.err.println("[ColorGrading] disabled: framebuffer incomplete 0x" + Integer.toHexString(status));
			return false;
		}
		width = w;
		height = h;
		return true;
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
			if (!gl.isFunctionAvailable("glBlitFramebuffer") || !gl.isFunctionAvailable("glCreateShader")) {
				throw new IllegalStateException("not supported");
			}
			int p = PixelLighting.buildProgram(gl, VERTEX, FRAGMENT);
			uScene = gl.glGetUniformLocation(p, "uScene");
			uStrength = gl.glGetUniformLocation(p, "uStrength");
			program = p;
			return true;
		} catch (RuntimeException e) {
			failed = true;
			System.err.println("[ColorGrading] disabled: " + e.getMessage());
			return false;
		}
	}
}
