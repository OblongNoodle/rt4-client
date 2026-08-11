package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

public class LightingManager {
	@OriginalMember(owner = "client!jf", name = "b", descriptor = "[F")
	private static final float[] lightPositionTemp = new float[]{0.0F, 0.0F, 0.0F, 1.0F};
	@OriginalMember(owner = "client!jf", name = "l", descriptor = "I")
	public static int lightCount = 0;
	@OriginalMember(owner = "client!jf", name = "a", descriptor = "[Lclient!gi;")
	public static Light[] lights;
	@OriginalMember(owner = "client!rh", name = "d", descriptor = "I")
	public static int visibleMaxY;
	@OriginalMember(owner = "client!aa", name = "m", descriptor = "I")
	public static int visibleMaxX;
	@OriginalMember(owner = "client!gf", name = "M", descriptor = "I")
	public static int visibleMinY;
	@OriginalMember(owner = "client!ch", name = "w", descriptor = "I")
	public static int visibleMinX;
	@OriginalMember(owner = "client!id", name = "b", descriptor = "I")
	public static int minimapRenderedPlane = -1;
	@OriginalMember(owner = "client!jf", name = "c", descriptor = "[I")
	private static int[] candidateLightIndices;
	@OriginalMember(owner = "client!jf", name = "d", descriptor = "I")
	private static int cachedStartTileY;
	@OriginalMember(owner = "client!jf", name = "e", descriptor = "I")
	private static int cachedEndTileY;
	@OriginalMember(owner = "client!jf", name = "f", descriptor = "[Z")
	private static boolean[] enabledLights;
	@OriginalMember(owner = "client!jf", name = "g", descriptor = "[[[I")
	private static int[][][] lightGrid;
	@OriginalMember(owner = "client!jf", name = "h", descriptor = "[I")
	private static int[] activeLightIndices;
	@OriginalMember(owner = "client!jf", name = "i", descriptor = "I")
	private static int cachedPlane;
	@OriginalMember(owner = "client!jf", name = "j", descriptor = "I")
	private static int planeCount;
	@OriginalMember(owner = "client!jf", name = "k", descriptor = "I")
	private static int cachedStartTileX;
	@OriginalMember(owner = "client!jf", name = "m", descriptor = "[Z")
	private static boolean[] lightSlotRetained;
	@OriginalMember(owner = "client!jf", name = "n", descriptor = "I")
	private static int cachedEndTileX;
	@OriginalMember(owner = "client!jf", name = "o", descriptor = "I")
	private static int length;
	@OriginalMember(owner = "client!jf", name = "p", descriptor = "I")
	private static int width;

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(IIIIIII)V")
	public static void updateLightsForWallPiece(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6) {
		if (!Preferences.highDetailLighting) {
			return;
		}
		if (arg0 == 1 && arg5 > 0) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 - 1, arg6);
		} else if (arg0 == 4 && arg5 < width - 1) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 + 1, arg6);
		} else if (arg0 == 8 && arg6 > 0) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5, arg6 - 1);
		} else if (arg0 == 2 && arg6 < length - 1) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5, arg6 + 1);
		} else if (arg0 == 16 && arg5 > 0 && arg6 < length - 1) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 - 1, arg6 + 1);
		} else if (arg0 == 32 && arg5 < width - 1 && arg6 < length - 1) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 + 1, arg6 + 1);
		} else if (arg0 == 128 && arg5 > 0 && arg6 > 0) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 - 1, arg6 - 1);
		} else if (arg0 == 64 && arg5 < width - 1 && arg6 > 0) {
			updateLightsForTile(arg1, arg2, arg3, arg4, arg5 + 1, arg6 - 1);
		}
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(Lclient!gi;)V")
	public static void addLight(@OriginalArg(0) Light arg0) {
		if (lightCount >= 255) {
			System.out.println("Number of lights added exceeds maximum!");
		} else {
			lights[lightCount++] = arg0;
		}
	}

	private static final int[][] STATIC_LIGHT_OVERRIDES = {
			{ 8005, 3063, 3046, 2 }
	};

	public static boolean matchesStaticLightOverride(int regionId, int x, int y, int plane) {
		for (int[] override : STATIC_LIGHT_OVERRIDES) {
			if (override[0] == regionId
			&& override[1] == x
			&& override[2] == y
			&& override[3] == plane) {
				return true;
			}
		}
		return false;
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "()V")
	public static void resetActiveLights() {
		for (@Pc(1) int local1 = 0; local1 < 4; local1++) {
			activeLightIndices[local1] = -1;
			disableLight(local1);
		}
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(IIIIIIII)V")
	public static void updateLightsForRegion(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6, @OriginalArg(7) int arg7) {
		if (!Preferences.highDetailLighting || cachedPlane == arg3 && cachedStartTileX == arg4 && cachedStartTileY == arg5 && cachedEndTileX == arg6 && cachedEndTileY == arg7) {
			return;
		}
		@Pc(20) int local20;
		for (local20 = 0; local20 < 4; local20++) {
			lightSlotRetained[local20] = false;
		}
		local20 = 0;
		@Pc(33) int local33 = 0;
		@Pc(35) int local35;
		@Pc(40) int local40;
		label112:
		for (local35 = arg4; local35 <= arg6; local35++) {
			label110:
			for (local40 = arg5; local40 <= arg7; local40++) {
				@Pc(51) int local51 = lightGrid[arg3][local35][local40];
				while (true) {
					while (true) {
						label96:
						while (true) {
							if (local51 == 0) {
								continue label110;
							}
							@Pc(59) int local59 = (local51 & 0xFF) - 1;
							local51 >>>= 0x8;
							@Pc(65) int local65;
							for (local65 = 0; local65 < local33; local65++) {
								if (local59 == candidateLightIndices[local65]) {
									continue label96;
								}
							}
							for (local65 = 0; local65 < 4; local65++) {
								if (local59 == activeLightIndices[local65]) {
									if (!lightSlotRetained[local65]) {
										lightSlotRetained[local65] = true;
										local20++;
										if (local20 == 4) {
											break label112;
										}
									}
									continue label96;
								}
							}
							candidateLightIndices[local33++] = local59;
							local20++;
							if (local20 == 4) {
								break label112;
							}
						}
					}
				}
			}
		}
		for (local35 = 0; local35 < local33; local35++) {
			for (local40 = 0; local40 < 4; local40++) {
				if (!lightSlotRetained[local40]) {
					activeLightIndices[local40] = candidateLightIndices[local35];
					lightSlotRetained[local40] = true;
					enableGlLight(local40, lights[candidateLightIndices[local35]], arg0, arg1, arg2);
					break;
				}
			}
		}
		for (local35 = 0; local35 < 4; local35++) {
			if (!lightSlotRetained[local35]) {
				activeLightIndices[local35] = -1;
				disableLight(local35);
			}
		}
		cachedPlane = arg3;
		cachedStartTileX = arg4;
		cachedStartTileY = arg5;
		cachedEndTileX = arg6;
		cachedEndTileY = arg7;
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(III)V")
	public static void initLightGrid() {
		planeCount = 4;
		width = 104;
		length = 104;
		lightGrid = new int[planeCount][width][length];
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(IIIIII)V")
	public static void updateLightsForTile(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5) {
		if (!Preferences.highDetailLighting || cachedPlane == arg3 && cachedStartTileX == arg4 && cachedStartTileY == arg5 && cachedEndTileX == arg4 && cachedEndTileY == arg5) {
			return;
		}
		@Pc(20) int local20;
		for (local20 = 0; local20 < 4; local20++) {
			lightSlotRetained[local20] = false;
		}
		local20 = 0;
		@Pc(39) int local39 = lightGrid[arg3][arg4][arg5];
		while (true) {
			@Pc(47) int local47;
			@Pc(53) int local53;
			label72:
			while (local39 != 0) {
				local47 = (local39 & 0xFF) - 1;
				local39 >>>= 0x8;
				for (local53 = 0; local53 < 4; local53++) {
					if (local47 == activeLightIndices[local53]) {
						lightSlotRetained[local53] = true;
						continue label72;
					}
				}
				candidateLightIndices[local20++] = local47;
			}
			for (local47 = 0; local47 < local20; local47++) {
				for (local53 = 0; local53 < 4; local53++) {
					if (!lightSlotRetained[local53]) {
						activeLightIndices[local53] = candidateLightIndices[local47];
						lightSlotRetained[local53] = true;
						enableGlLight(local53, lights[candidateLightIndices[local47]], arg0, arg1, arg2);
						break;
					}
				}
			}
			for (local47 = 0; local47 < 4; local47++) {
				if (!lightSlotRetained[local47]) {
					activeLightIndices[local47] = -1;
					disableLight(local47);
				}
			}
			cachedPlane = arg3;
			cachedStartTileX = arg4;
			cachedStartTileY = arg5;
			cachedEndTileX = arg4;
			cachedEndTileY = arg5;
			return;
		}
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(IZ)V")
	public static void updateAllLightAnimations(@OriginalArg(0) int arg0, @OriginalArg(1) boolean arg1) {
		for (@Pc(1) int local1 = 0; local1 < lightCount; local1++) {
			lights[local1].updateAnimation(arg1, arg0);
		}
		cachedPlane = -1;
		cachedStartTileX = -1;
		cachedStartTileY = -1;
		cachedEndTileX = -1;
		cachedEndTileY = -1;
	}

	@OriginalMember(owner = "client!jf", name = "b", descriptor = "()V")
	public static void buildLightGrid() {
		for (@Pc(1) int local1 = 0; local1 < lightCount; local1++) {
			@Pc(8) Light local8 = lights[local1];
			@Pc(11) int local11 = local8.level;
			if (local8.extendsDown) {
				local11 = 0;
			}
			@Pc(19) int local19 = local8.level;
			if (local8.extendsUp) {
				local19 = 3;
			}
			for (@Pc(26) int local26 = local11; local26 <= local19; local26++) {
				@Pc(31) int local31 = 0;
				@Pc(39) int local39 = (local8.y >> 7) - local8.radius;
				if (local39 < 0) {
					local31 = -local39;
					local39 = 0;
				}
				@Pc(55) int local55 = (local8.y >> 7) + local8.radius;
				if (local55 > length - 1) {
					local55 = length - 1;
				}
				for (@Pc(66) int local66 = local39; local66 <= local55; local66++) {
					@Pc(75) short local75 = local8.spanProfile[local31++];
					@Pc(87) int local87 = (local8.x >> 7) + (local75 >> 8) - local8.radius;
					@Pc(95) int local95 = local87 + (local75 & 0xFF) - 1;
					if (local87 < 0) {
						local87 = 0;
					}
					if (local95 > width - 1) {
						local95 = width - 1;
					}
					for (@Pc(110) int local110 = local87; local110 <= local95; local110++) {
						@Pc(121) int local121 = lightGrid[local26][local110][local66];
						if ((local121 & 0xFF) == 0) {
							lightGrid[local26][local110][local66] = local121 | local1 + 1;
						} else if ((local121 & 0xFF00) == 0) {
							lightGrid[local26][local110][local66] = local121 | local1 + 1 << 8;
						} else if ((local121 & 0xFF0000) == 0) {
							lightGrid[local26][local110][local66] = local121 | local1 + 1 << 16;
						} else if ((local121 & 0xFF000000) == 0) {
							lightGrid[local26][local110][local66] = local121 | local1 + 1 << 24;
						}
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(I)V")
	private static void disableLight(@OriginalArg(0) int i) {
		if (enabledLights[i]) {
			enabledLights[i] = false;
			@Pc(14) int light = i + 16384 + 4;
			@Pc(16) GL2 gl = GlRenderer.gl;
			gl.glDisable(light);
		}
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(IIIII)V")
	public static void pruneInactiveLights(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
		if (!Preferences.highDetailLighting) {
			return;
		}
		label43:
		for (@Pc(4) int local4 = 0; local4 < 4; local4++) {
			if (activeLightIndices[local4] != -1) {
				@Pc(20) int local20 = lightGrid[arg0][arg1][arg2];
				@Pc(28) int local28;
				while (local20 != 0) {
					local28 = (local20 & 0xFF) - 1;
					local20 >>>= 0x8;
					if (local28 == activeLightIndices[local4]) {
						continue label43;
					}
				}
				local20 = lightGrid[arg0][arg3][arg4];
				while (local20 != 0) {
					local28 = (local20 & 0xFF) - 1;
					local20 >>>= 0x8;
					if (local28 == activeLightIndices[local4]) {
						continue label43;
					}
				}
			}
			activeLightIndices[local4] = -1;
			disableLight(local4);
		}
	}

	@OriginalMember(owner = "client!jf", name = "c", descriptor = "()V")
	public static void destroy() {
		lights = null;
		activeLightIndices = null;
		enabledLights = null;
		candidateLightIndices = null;
		lightSlotRetained = null;
		lightGrid = null;
	}

	@OriginalMember(owner = "client!jf", name = "e", descriptor = "()V")
	public static void initGlLightParameters() {
		@Pc(1) GL2 gl = GlRenderer.gl;
		@Pc(3) int local3;
		for (local3 = 0; local3 < 4; local3++) {
			@Pc(10) int local10 = local3 + 16388;
			gl.glLightfv(local10, GL2.GL_AMBIENT, new float[]{0.0F, 0.0F, 0.0F, 1.0F}, 0);
			gl.glLightf(local10, GL2.GL_LINEAR_ATTENUATION, 0.0F);
			gl.glLightf(local10, GL2.GL_CONSTANT_ATTENUATION, 0.0F);
		}
		for (local3 = 0; local3 < 4; local3++) {
			activeLightIndices[local3] = -1;
			disableLight(local3);
		}
	}

	@OriginalMember(owner = "client!jf", name = "f", descriptor = "()V")
	public static void allocateLightArrays() {
		lights = new Light[255];
		activeLightIndices = new int[4];
		enabledLights = new boolean[4];
		candidateLightIndices = new int[4];
		lightSlotRetained = new boolean[4];
		lightGrid = new int[planeCount][width][length];
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(II[[[Lclient!bj;)V")
	public static void renderLightMeshes(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) Tile[][][] arg2) {
		if (!Preferences.highDetailLighting) {
			return;
		}
		@Pc(4) GL2 gl = GlRenderer.gl;
		MaterialManager.setMaterial(0, 0);
		GlRenderer.setTextureCombineRgbMode(0);
		GlRenderer.resetTextureMatrix();
		GlRenderer.setTextureId(GlRenderer.defaultTextureId);
		gl.glDepthMask(false);
		GlRenderer.setLightingEnabled(false);
		gl.glBlendFunc(GL2.GL_DST_COLOR, GL2.GL_ONE);
		gl.glFogfv(GL2.GL_FOG_COLOR, new float[]{0.0F, 0.0F, 0.0F, 0.0F}, 0);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_CONSTANT);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_RGB, GL2.GL_SRC_ALPHA);
		label71:
		for (@Pc(56) int local56 = 0; local56 < lightCount; local56++) {
			@Pc(63) Light local63 = lights[local56];
			@Pc(66) int local66 = local63.level;
			if (local63.onBridge) {
				local66--;
			}
			if (local63.mesh != null) {
				@Pc(76) int local76 = 0;
				@Pc(84) int local84 = (local63.y >> 7) - local63.radius;
				@Pc(92) int local92 = (local63.y >> 7) + local63.radius;
				if (local92 >= visibleMaxY) {
					local92 = visibleMaxY - 1;
				}
				if (local84 < visibleMinY) {
					local76 = visibleMinY - local84;
					local84 = visibleMinY;
				}
				for (@Pc(112) int local112 = local84; local112 <= local92; local112++) {
					@Pc(121) short local121 = local63.spanProfile[local76++];
					@Pc(133) int local133 = (local63.x >> 7) + (local121 >> 8) - local63.radius;
					@Pc(141) int local141 = local133 + (local121 & 0xFF) - 1;
					if (local133 < visibleMinX) {
						local133 = visibleMinX;
					}
					if (local141 >= visibleMaxX) {
						local141 = visibleMaxX - 1;
					}
					for (@Pc(155) int local155 = local133; local155 <= local141; local155++) {
						@Pc(160) Tile local160 = null;
						if (local66 >= 0) {
							local160 = arg2[local66][local155][local112];
						}
						if (local66 < 0 || local160 != null && local160.visible) {
							GlRenderer.setDepthLayer(201.5F - (float) local63.level * 50.0F - 1.5F);
							gl.glTexEnvfv(GL2.GL_TEXTURE_ENV, GL2.GL_TEXTURE_ENV_COLOR, new float[]{0.0F, 0.0F, 0.0F, local63.alpha}, 0);
							local63.mesh.draw();
							continue label71;
						}
					}
				}
			}
		}
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_SRC0_RGB, GL2.GL_TEXTURE);
		gl.glTexEnvi(GL2.GL_TEXTURE_ENV, GL2.GL_OPERAND0_RGB, GL2.GL_SRC_COLOR);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
		gl.glDepthMask(true);
		gl.glFogfv(GL2.GL_FOG_COLOR, FogManager.fogColor, 0);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);
		GlRenderer.restoreLighting();
	}

	@OriginalMember(owner = "client!jf", name = "a", descriptor = "(ILclient!gi;III)V")
	private static void enableGlLight(@OriginalArg(0) int arg0, @OriginalArg(1) Light arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
		@Pc(5) int local5 = arg0 + 16384 + 4;
		@Pc(7) GL2 gl = GlRenderer.gl;
		if (!enabledLights[arg0]) {
			gl.glEnable(local5);
			enabledLights[arg0] = true;
		}
		gl.glLightf(local5, GL2.GL_QUADRATIC_ATTENUATION, arg1.quadraticAttenuation);
		gl.glLightfv(local5, GL2.GL_DIFFUSE, arg1.diffuse, 0);
		lightPositionTemp[0] = arg1.x - arg2;
		lightPositionTemp[1] = arg1.z - arg3;
		lightPositionTemp[2] = arg1.y - arg4;
		gl.glLightfv(local5, GL2.GL_POSITION, lightPositionTemp, 0);
	}

	@OriginalMember(owner = "client!jf", name = "g", descriptor = "()V")
	public static void clearLightGrid() {
		lightCount = 0;
		for (@Pc(3) int local3 = 0; local3 < planeCount; local3++) {
			for (@Pc(8) int local8 = 0; local8 < width; local8++) {
				for (@Pc(13) int local13 = 0; local13 < length; local13++) {
					lightGrid[local3][local8][local13] = 0;
				}
			}
		}
	}

}
