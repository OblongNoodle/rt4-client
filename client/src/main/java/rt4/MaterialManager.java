package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.nio.ByteBuffer;

public class MaterialManager {
	public static final int WATER = 4;

	@OriginalMember(owner = "client!dl", name = "c", descriptor = "I")
	public static int currentType = 0;
	@OriginalMember(owner = "client!je", name = "R", descriptor = "Z")
	public static boolean renderingUnderwater = false;
	@OriginalMember(owner = "client!aa", name = "t", descriptor = "[Lclient!pc;")
	public static MaterialRenderer[] renderers;
	@OriginalMember(owner = "client!mh", name = "eb", descriptor = "I")
	public static int currentArg = 0;
	@OriginalMember(owner = "client!sj", name = "D", descriptor = "I")
	public static int cameraRenderY;
	@OriginalMember(owner = "client!ej", name = "X", descriptor = "I")
	public static int cameraYaw;
	@OriginalMember(owner = "client!uj", name = "H", descriptor = "I")
	public static int cameraPitch;
	@OriginalMember(owner = "client!bb", name = "M", descriptor = "I")
	public static int cameraRenderX;
	@OriginalMember(owner = "client!qc", name = "cb", descriptor = "I")
	public static int cameraRenderZ;
	@OriginalMember(owner = "client!lm", name = "e", descriptor = "Z")
	public static boolean allows3DTextureMapping;
	@OriginalMember(owner = "client!lm", name = "a", descriptor = "[I")
	public static int[] waterfallTextures = null;
	@OriginalMember(owner = "client!lm", name = "b", descriptor = "[I")
	public static int[] noise2DTextures = null;
	@OriginalMember(owner = "client!lm", name = "f", descriptor = "I")
	public static int texture3D = -1;
	@OriginalMember(owner = "client!lm", name = "g", descriptor = "I")
	public static int waterfallTextureId = -1;
	@OriginalMember(owner = "client!lm", name = "c", descriptor = "Ljava/nio/ByteBuffer;")
	private static ByteBuffer waterfallNoiseBuffer;
	@OriginalMember(owner = "client!lm", name = "d", descriptor = "Ljava/nio/ByteBuffer;")
	private static ByteBuffer textureBuffer;

	@OriginalMember(owner = "client!cb", name = "b", descriptor = "(III)V")
	public static void setMaterial(@OriginalArg(1) int arg, @OriginalArg(2) int type) {
		if (type == 4 && !Preferences.highWaterDetail) {
			type = 2;
			arg = 2;
		}
		if (currentType != type) {
			if (renderingUnderwater) {
				return;
			}
			if (currentType != 0) {
				renderers[currentType].unbind();
			}
			if (type != 0) {
				@Pc(61) MaterialRenderer renderer = renderers[type];
				renderer.bind();
				renderer.setArgument(arg);
			}
			currentType = type;
			currentArg = arg;
		} else if (type != 0 && arg != currentArg) {
			renderers[type].setArgument(arg);
			currentArg = arg;
		}
	}

	@OriginalMember(owner = "client!ka", name = "b", descriptor = "(II)V")
	public static void resetArgument(@OriginalArg(1) int arg0) {
		if (arg0 == currentType) {
			@Pc(12) MaterialRenderer local12 = renderers[arg0];
			local12.setArgument(currentArg);
		}
	}

	@OriginalMember(owner = "client!ef", name = "a", descriptor = "(I)I")
	public static int getFlags() {
		return currentType == 0 ? 0 : renderers[currentType].getFlags();
	}

	@OriginalMember(owner = "client!nj", name = "a", descriptor = "(I)V")
	public static void quit() {
		renderers = null;
		deleteNoiseTextures();
	}

	@OriginalMember(owner = "client!te", name = "e", descriptor = "(I)V")
	public static void init() {
		createNoiseTextures();
		renderers = new MaterialRenderer[7];
		renderers[1] = new SpecularMaterialRenderer();
		renderers[2] = new LiquidMaterialRenderer();
		renderers[3] = new UnderwaterMaterialRenderer();
		renderers[4] = new WaterMaterialRenderer();
		renderers[5] = new WaterfallMaterialRenderer();
		renderers[6] = new UnlitMaterialRenderer();
	}

	@OriginalMember(owner = "client!ld", name = "a", descriptor = "(IIIIZI)V")
	public static void setCameraTransform(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4) {
		cameraRenderY = arg1;
		cameraYaw = arg4;
		cameraPitch = arg0;
		cameraRenderX = arg3;
		cameraRenderZ = arg2;
	}

	@OriginalMember(owner = "client!lm", name = "a", descriptor = "()V")
	public static void initNoiseTextures() {
		@Pc(11) byte[] local11;
		if (textureBuffer == null) {
			@Pc(5) RidgedNoiseTexture local5 = new RidgedNoiseTexture();
			local11 = local5.generateTexture();
			textureBuffer = ByteBuffer.allocateDirect(local11.length);
			textureBuffer.position(0);
			textureBuffer.put(local11);
			textureBuffer.flip();
		}
		if (waterfallNoiseBuffer == null) {
			@Pc(32) FractalNoiseTexture local32 = new FractalNoiseTexture();
			local11 = local32.generateTexture();
			waterfallNoiseBuffer = ByteBuffer.allocateDirect(local11.length);
			waterfallNoiseBuffer.position(0);
			waterfallNoiseBuffer.put(local11);
			waterfallNoiseBuffer.flip();
		}
	}

	@OriginalMember(owner = "client!lm", name = "b", descriptor = "()V")
	public static void deleteNoiseTextures() {
		@Pc(4) GL2 gl;
		@Pc(11) int[] local11;
		if (texture3D != -1) {
			gl = GlRenderer.gl;
			local11 = new int[]{texture3D};
			gl.glDeleteTextures(1, local11, 0);
			texture3D = -1;
			GlCleaner.onCardTexture -= textureBuffer.limit() * 2;
		}
		if (noise2DTextures != null) {
			gl = GlRenderer.gl;
			gl.glDeleteTextures(64, noise2DTextures, 0);
			noise2DTextures = null;
			GlCleaner.onCardTexture -= textureBuffer.limit() * 2;
		}
		if (waterfallTextureId != -1) {
			gl = GlRenderer.gl;
			local11 = new int[]{waterfallTextureId};
			gl.glDeleteTextures(1, local11, 0);
			waterfallTextureId = -1;
			GlCleaner.onCardTexture -= waterfallNoiseBuffer.limit() * 2;
		}
		if (waterfallTextures != null) {
			gl = GlRenderer.gl;
			gl.glDeleteTextures(64, waterfallTextures, 0);
			waterfallTextures = null;
			GlCleaner.onCardTexture -= waterfallNoiseBuffer.limit() * 2;
		}
	}

	@OriginalMember(owner = "client!lm", name = "c", descriptor = "()V")
	public static void createNoiseTextures() {
		allows3DTextureMapping = GlRenderer.extTexture3dSupported;
		initNoiseTextures();
		uploadNoiseTexture();
		uploadWaterfallNoiseTexture();
	}

	@OriginalMember(owner = "client!lm", name = "e", descriptor = "()V")
	private static void uploadNoiseTexture() {
		@Pc(1) GL2 gl = GlRenderer.gl;
		if (allows3DTextureMapping) {
			@Pc(6) int[] local6 = new int[1];
			gl.glGenTextures(1, local6, 0);
			gl.glBindTexture(GL2.GL_TEXTURE_3D, local6[0]);
			textureBuffer.position(0);
			gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL2.GL_LUMINANCE_ALPHA, 64, 64, 64, 0, GL2.GL_LUMINANCE_ALPHA, GL2.GL_UNSIGNED_BYTE, textureBuffer);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			texture3D = local6[0];
			GlCleaner.onCardTexture += textureBuffer.limit() * 2;
		} else {
			noise2DTextures = new int[64];
			gl.glGenTextures(64, noise2DTextures, 0);
			for (@Pc(65) int local65 = 0; local65 < 64; local65++) {
				GlRenderer.setTextureId(noise2DTextures[local65]);
				textureBuffer.position(local65 * 64 * 64 * 2);
				gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE_ALPHA, 64, 64, 0, GL2.GL_LUMINANCE_ALPHA, GL2.GL_UNSIGNED_BYTE, textureBuffer);
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
				gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			}
			GlCleaner.onCardTexture += textureBuffer.limit() * 2;
		}
	}

	@OriginalMember(owner = "client!lm", name = "f", descriptor = "()V")
	private static void uploadWaterfallNoiseTexture() {
		@Pc(1) GL2 gl = GlRenderer.gl;
		if (allows3DTextureMapping) {
			@Pc(6) int[] local6 = new int[1];
			gl.glGenTextures(1, local6, 0);
			gl.glBindTexture(GL2.GL_TEXTURE_3D, local6[0]);
			waterfallNoiseBuffer.position(0);
			gl.glTexImage3D(GL2.GL_TEXTURE_3D, 0, GL2.GL_LUMINANCE_ALPHA, 64, 64, 64, 0, GL2.GL_LUMINANCE_ALPHA, GL2.GL_UNSIGNED_BYTE, waterfallNoiseBuffer);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_3D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
			waterfallTextureId = local6[0];
			GlCleaner.onCardTexture += waterfallNoiseBuffer.limit() * 2;
			return;
		}
		waterfallTextures = new int[64];
		gl.glGenTextures(64, waterfallTextures, 0);
		for (@Pc(65) int local65 = 0; local65 < 64; local65++) {
			GlRenderer.setTextureId(waterfallTextures[local65]);
			waterfallNoiseBuffer.position(local65 * 64 * 64 * 2);
			gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_LUMINANCE_ALPHA, 64, 64, 0, GL2.GL_LUMINANCE_ALPHA, GL2.GL_UNSIGNED_BYTE, waterfallNoiseBuffer);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
			gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		}
		GlCleaner.onCardTexture += waterfallNoiseBuffer.limit() * 2;
	}

	@OriginalMember(owner = "client!lh", name = "b", descriptor = "(II)V")
	public static void setUnderwaterFogRange(@OriginalArg(0) int arg0) {
		UnderwaterMaterialRenderer.underwaterDepthRange = arg0;
		resetArgument(3);
		resetArgument(4);
	}
}
