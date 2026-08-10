package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

public class Camera {
	@OriginalMember(owner = "client!id", name = "d", descriptor = "[[[I")
	public static final int[][][] cameraPathData = new int[2][][];
	@OriginalMember(owner = "client!sa", name = "Q", descriptor = "[I")
	public static final int[] cameraJitter = new int[5];
	@OriginalMember(owner = "client!wh", name = "m", descriptor = "[I")
	public static final int[] cameraAmplitude = new int[5];
	@OriginalMember(owner = "client!qg", name = "Y", descriptor = "[I")
	public static final int[] cameraFrequency = new int[5];
	@OriginalMember(owner = "client!ob", name = "a", descriptor = "[Z")
	public static final boolean[] customCameraActive = new boolean[5];
	@OriginalMember(owner = "client!fl", name = "s", descriptor = "I")
	public static double pitchTarget = 128;

	@OriginalMember(owner = "client!eg", name = "d", descriptor = "I")
	public static double yawTarget = 0;

	@OriginalMember(owner = "client!ef", name = "i", descriptor = "I")
	public static double pitchAccel = 0;

	@OriginalMember(owner = "client!ii", name = "a", descriptor = "I")
	public static int cameraY;
	@OriginalMember(owner = "client!gg", name = "bb", descriptor = "I")
	public static int cameraX;
	@OriginalMember(owner = "client!tg", name = "b", descriptor = "I")
	public static int cameraPitch;
	@OriginalMember(owner = "client!ol", name = "ib", descriptor = "I")
	public static int cameraYaw;
	@OriginalMember(owner = "client!sg", name = "o", descriptor = "I")
	public static int cameraType;
	@OriginalMember(owner = "client!g", name = "d", descriptor = "I")
	public static int lookAtSplineIndex = 0;
	@OriginalMember(owner = "client!eb", name = "t", descriptor = "I")
	public static int lookAtPathId = -1;
	@OriginalMember(owner = "client!j", name = "K", descriptor = "I")
	public static int moveSplineSpeedEnd = 0;
	@OriginalMember(owner = "client!sh", name = "c", descriptor = "I")
	public static int moveSplineSpeedStart = 0;
	@OriginalMember(owner = "client!t", name = "z", descriptor = "I")
	public static int splineProgress = 0;
	@OriginalMember(owner = "client!k", name = "i", descriptor = "I")
	public static int moveSplineIndex = 0;
	@OriginalMember(owner = "client!me", name = "k", descriptor = "I")
	public static int movePathId = -1;
	@OriginalMember(owner = "client!lc", name = "n", descriptor = "I")
	public static int originY;
	@OriginalMember(owner = "client!se", name = "a", descriptor = "I")
	public static int originX;
	@OriginalMember(owner = "client!bc", name = "I", descriptor = "Z")
	public static boolean splineJustFinished = false;
	@OriginalMember(owner = "client!km", name = "Pc", descriptor = "I")
	public static int renderX;
	@OriginalMember(owner = "client!kh", name = "f", descriptor = "I")
	public static int renderY;
	@OriginalMember(owner = "client!uc", name = "f", descriptor = "I")
	public static int lockedTargetY;
	@OriginalMember(owner = "client!ug", name = "h", descriptor = "I")
	public static int lockedLookAtX;
	@OriginalMember(owner = "client!vj", name = "d", descriptor = "I")
	public static int lockedLookAtY;
	@OriginalMember(owner = "client!tm", name = "g", descriptor = "I")
	public static int lockedTargetX;
	@OriginalMember(owner = "client!pa", name = "K", descriptor = "Z")
	public static boolean splineFinished = false;
	@OriginalMember(owner = "client!gk", name = "d", descriptor = "F")
	public static float yawRadians;
	public static int ZOOM = 600;
	@OriginalMember(owner = "client!il", name = "O", descriptor = "I")
	public static int lockedMoveSpeed;
	@OriginalMember(owner = "client!eg", name = "a", descriptor = "I")
	public static int lockedLookAtHeight;
	@OriginalMember(owner = "client!kf", name = "f", descriptor = "I")
	public static int lockedMinStep;
	@OriginalMember(owner = "client!ke", name = "U", descriptor = "I")
	public static int cameraOffsetX = 0;
	@OriginalMember(owner = "client!uc", name = "a", descriptor = "I")
	public static int anInt4229 = 2;
	@OriginalMember(owner = "client!ta", name = "B", descriptor = "I")
	public static int maxPitchDistance = 0;
	@OriginalMember(owner = "client!qk", name = "h", descriptor = "I")
	public static int cameraOffsetY = 0;
	@OriginalMember(owner = "client!sj", name = "H", descriptor = "I")
	public static int anInt5161 = 0;
	@OriginalMember(owner = "client!af", name = "d", descriptor = "I")
	public static int renderZ;
	@OriginalMember(owner = "client!lg", name = "d", descriptor = "F")
	public static float pitchRadians;
	@OriginalMember(owner = "client!sk", name = "jb", descriptor = "I")
	public static int lockedTargetHeight;
	@OriginalMember(owner = "client!t", name = "o", descriptor = "I")
	public static int lockedAngleSpeed;
	@OriginalMember(owner = "client!t", name = "A", descriptor = "I")
	public static int lockedMinMoveStep;

	public static double mod(double a, double b) {
		return ((a % b) + b) % b;
	}

	@OriginalMember(owner = "client!gn", name = "b", descriptor = "(B)V")
	public static void clampCameraAngle() {
		if (pitchTarget < 128) {
			pitchTarget = 128;
		}
		if (pitchTarget > 383) {
			pitchTarget = 383;
		}
		yawTarget = mod(yawTarget, 2047.0d);
		@Pc(33) int local33 = cameraX >> 7;
		@Pc(37) int local37 = cameraY >> 7;
		@Pc(43) int local43 = SceneGraph.getTileHeight(Player.plane, cameraX, cameraY);
		@Pc(45) int local45 = 0;
		@Pc(64) int local64;
		if (local33 > 3 && local37 > 3 && local33 < 100 && local37 < 100) {
			for (local64 = local33 - 4; local64 <= local33 + 4; local64++) {
				for (@Pc(73) int local73 = local37 - 4; local73 <= local37 + 4; local73++) {
					@Pc(80) int local80 = Player.plane;
					if (local80 < 3 && (SceneGraph.renderFlags[1][local64][local73] & 0x2) == 2) {
						local80++;
					}
					@Pc(117) int local117 = (SceneGraph.aByteArrayArrayArray13[local80][local64][local73] & 0xFF) * 8 + local43 - SceneGraph.tileHeights[local80][local64][local73];
					if (local117 > local45) {
						local45 = local117;
					}
				}
			}
		}
		local64 = local45 * 192;
		if (local64 > 98048) {
			local64 = 98048;
		}
		if (local64 < 32768) {
			local64 = 32768;
		}
		if (maxPitchDistance < local64) {
			maxPitchDistance += (local64 - maxPitchDistance) / 24;
		} else if (local64 < maxPitchDistance) {
			maxPitchDistance += (local64 - maxPitchDistance) / 80;
		}
	}

	@OriginalMember(owner = "client!jl", name = "c", descriptor = "(I)V")
	public static void updateLockedCamera() {
		@Pc(9) int local9 = lockedTargetY * 128 + 64;
		@Pc(15) int local15 = lockedTargetX * 128 + 64;
		@Pc(23) int local23 = SceneGraph.getTileHeight(Player.plane, local15, local9) - lockedTargetHeight;
		if (lockedMoveSpeed >= 100) {
			renderX = lockedTargetX * 128 + 64;
			renderY = lockedTargetY * 128 + 64;
			renderZ = SceneGraph.getTileHeight(Player.plane, renderX, renderY) - lockedTargetHeight;
		} else {
			if (renderX < local15) {
				renderX += lockedMinMoveStep + lockedMoveSpeed * (local15 - renderX) / 1000;
				if (renderX > local15) {
					renderX = local15;
				}
			}
			if (renderZ < local23) {
				renderZ += (local23 - renderZ) * lockedMoveSpeed / 1000 + lockedMinMoveStep;
				if (renderZ > local23) {
					renderZ = local23;
				}
			}
			if (renderX > local15) {
				renderX -= lockedMinMoveStep + (renderX - local15) * lockedMoveSpeed / 1000;
				if (renderX < local15) {
					renderX = local15;
				}
			}
			if (renderY < local9) {
				renderY += lockedMinMoveStep + lockedMoveSpeed * (local9 - renderY) / 1000;
				if (local9 < renderY) {
					renderY = local9;
				}
			}
			if (local23 < renderZ) {
				renderZ -= (renderZ - local23) * lockedMoveSpeed / 1000 + lockedMinMoveStep;
				if (local23 > renderZ) {
					renderZ = local23;
				}
			}
			if (renderY > local9) {
				renderY -= lockedMinMoveStep + (renderY - local9) * lockedMoveSpeed / 1000;
				if (local9 > renderY) {
					renderY = local9;
				}
			}
		}
		local9 = lockedLookAtY * 128 + 64;
		local15 = lockedLookAtX * 128 + 64;
		local23 = SceneGraph.getTileHeight(Player.plane, local15, local9) - lockedLookAtHeight;
		@Pc(236) int local236 = local23 - renderZ;
		@Pc(241) int local241 = local9 - renderY;
		@Pc(246) int local246 = local15 - renderX;
		@Pc(257) int local257 = (int) Math.sqrt(local246 * local246 + local241 * local241);
		@Pc(268) int local268 = (int) (Math.atan2(local236, local257) * 325.949D) & 0x7FF;
		if (local268 < 128) {
			local268 = 128;
		}
		if (local268 > 383) {
			local268 = 383;
		}
		@Pc(292) int local292 = (int) (-325.949D * Math.atan2(local246, local241)) & 0x7FF;
		if (cameraPitch < local268) {
			cameraPitch += lockedMinStep + lockedAngleSpeed * (local268 - cameraPitch) / 1000;
			if (cameraPitch > local268) {
				cameraPitch = local268;
			}
		}
		if (cameraPitch > local268) {
			cameraPitch -= (cameraPitch - local268) * lockedAngleSpeed / 1000 + lockedMinStep;
			if (cameraPitch < local268) {
				cameraPitch = local268;
			}
		}
		@Pc(350) int local350 = local292 - cameraYaw;
		if (local350 > 1024) {
			local350 -= 2048;
		}
		if (local350 < -1024) {
			local350 += 2048;
		}
		if (local350 > 0) {
			cameraYaw += local350 * lockedAngleSpeed / 1000 + lockedMinStep;
			cameraYaw &= 0x7FF;
		}
		if (local350 < 0) {
			cameraYaw -= lockedAngleSpeed * -local350 / 1000 + lockedMinStep;
			cameraYaw &= 0x7FF;
		}
		@Pc(404) int local404 = local292 - cameraYaw;
		if (local404 > 1024) {
			local404 -= 2048;
		}
		if (local404 < -1024) {
			local404 += 2048;
		}
		if (local404 < 0 && local350 > 0 || local404 > 0 && local350 < 0) {
			cameraYaw = local292;
		}
	}

	@OriginalMember(owner = "client!da", name = "d", descriptor = "(I)V")
	public static void updateLoginScreenCamera() {
		if (movePathId == -1 || lookAtPathId == -1) {
			return;
		}
		@Pc(27) int local27 = (splineProgress * (moveSplineSpeedEnd - moveSplineSpeedStart) >> 16) + moveSplineSpeedStart;
		@Pc(30) float[] renderCoordinates = new float[3];
		splineProgress += local27;
		if (splineProgress >= 65535) {
			splineProgress = 65535;
			splineJustFinished = !splineFinished;
			splineFinished = true;
		} else {
			splineFinished = false;
			splineJustFinished = false;
		}
		@Pc(66) float local66 = (float) splineProgress / 65535.0F;
		@Pc(70) int local70 = moveSplineIndex * 2;
		@Pc(141) int local141;
		@Pc(131) int local131;
		@Pc(111) int local111;
		@Pc(119) int local119;
		@Pc(146) int local146;
		@Pc(155) int local155;
		@Pc(173) int local173;
		for (@Pc(72) int local72 = 0; local72 < 3; local72++) {
			local111 = (cameraPathData[movePathId][local70 + 2][local72] + cameraPathData[movePathId][local70 + 2][local72] - cameraPathData[movePathId][local70 + 3][local72]) * 3;
			local119 = cameraPathData[movePathId][local70][local72];
			local131 = cameraPathData[movePathId][local70 + 1][local72] * 3;
			local141 = cameraPathData[movePathId][local70][local72] * 3;
			local146 = local131 - local141;
			local155 = local111 + local141 - local131 * 2;
			local173 = cameraPathData[movePathId][local70 + 2][local72] + local131 - local119 - local111;
			renderCoordinates[local72] = (float) local119 + (((float) local173 * local66 + (float) local155) * local66 + (float) local146) * local66;
		}
		renderZ = (int) renderCoordinates[1] * -1;
		renderX = (int) renderCoordinates[0] - originX * 128;
		renderY = (int) renderCoordinates[2] - originY * 128;
		@Pc(226) float[] local226 = new float[3];
		local141 = lookAtSplineIndex * 2;
		for (local131 = 0; local131 < 3; local131++) {
			local111 = cameraPathData[lookAtPathId][local141][local131] * 3;
			local146 = (cameraPathData[lookAtPathId][local141 + 2][local131] + cameraPathData[lookAtPathId][local141 + 2][local131] - cameraPathData[lookAtPathId][local141 + 3][local131]) * 3;
			local155 = cameraPathData[lookAtPathId][local141][local131];
			local119 = cameraPathData[lookAtPathId][local141 + 1][local131] * 3;
			local173 = local119 - local111;
			@Pc(313) int local313 = local146 + local111 - local119 * 2;
			@Pc(331) int local331 = cameraPathData[lookAtPathId][local141 + 2][local131] + local119 - local146 - local155;
			local226[local131] = (float) local155 + local66 * (local66 * (local66 * (float) local331 + (float) local313) + (float) local173);
		}
		@Pc(363) float local363 = local226[0] - renderCoordinates[0];
		@Pc(371) float local371 = local226[2] - renderCoordinates[2];
		@Pc(382) float local382 = (local226[1] - renderCoordinates[1]) * -1.0F;
		@Pc(392) double local392 = Math.sqrt(local371 * local371 + local363 * local363);
		pitchRadians = (float) Math.atan2(local382, local392);
		yawRadians = -((float) Math.atan2(local363, local371));
		cameraPitch = (int) ((double) pitchRadians * 325.949D) & 0x7FF;
		cameraYaw = (int) ((double) yawRadians * 325.949D) & 0x7FF;
	}

	@OriginalMember(owner = "client!vd", name = "a", descriptor = "(IIIIBI)V")
	public static void setLockedLookAt(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4) {
		lockedMinStep = arg2;
		lockedLookAtY = arg1;
		lockedAngleSpeed = arg4;
		lockedLookAtX = arg3;
		lockedLookAtHeight = arg0;
		if (lockedAngleSpeed >= 100) {
			@Pc(30) int local30 = lockedLookAtX * 128 + 64;
			@Pc(36) int local36 = lockedLookAtY * 128 + 64;
			@Pc(44) int local44 = SceneGraph.getTileHeight(Player.plane, local30, local36) - lockedLookAtHeight;
			@Pc(49) int local49 = local44 - renderZ;
			@Pc(54) int local54 = local30 - renderX;
			@Pc(59) int local59 = local36 - renderY;
			@Pc(70) int local70 = (int) Math.sqrt(local59 * local59 + local54 * local54);
			cameraPitch = (int) (Math.atan2(local49, local70) * 325.949D) & 0x7FF;
			cameraYaw = (int) (Math.atan2(local54, local59) * -325.949D) & 0x7FF;
			if (cameraPitch < 128) {
				cameraPitch = 128;
			}
			if (cameraPitch > 383) {
				cameraPitch = 383;
			}
		}
		cameraType = 2;
	}

	@OriginalMember(owner = "client!lb", name = "a", descriptor = "(ZIIIBII)V")
	public static void setLockedPosition(@OriginalArg(0) boolean arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5) {
		lockedMoveSpeed = arg3;
		lockedTargetHeight = arg2;
		lockedTargetX = arg5;
		lockedMinMoveStep = arg1;
		lockedTargetY = arg4;
		if (arg0 && lockedMoveSpeed >= 100) {
			renderX = lockedTargetX * 128 + 64;
			renderY = lockedTargetY * 128 + 64;
			renderZ = SceneGraph.getTileHeight(Player.plane, renderX, renderY) - lockedTargetHeight;
		}
		cameraType = 2;
	}

	@OriginalMember(owner = "client!cl", name = "e", descriptor = "(I)V")
	public static void resetCameraEffects() {
		for (@Pc(3) int local3 = 0; local3 < 5; local3++) {
			customCameraActive[local3] = false;
		}
		lockedMinStep = 0;
		lockedAngleSpeed = 0;
		movePathId = -1;
		lookAtPathId = -1;
		cameraType = 1;
	}

	@OriginalMember(owner = "client!uf", name = "a", descriptor = "(B)V")
	public static void updatePlayerCamera() {
		@Pc(14) int playerX = PlayerList.self.xFine + cameraOffsetX;
		@Pc(20) int playerY = PlayerList.self.yFine + cameraOffsetY;
		if (cameraX - playerX < -500 || cameraX - playerX > 500 || cameraY - playerY < -500 || cameraY - playerY > 500) {
			cameraX = playerX;
			cameraY = playerY;
		}
		if (cameraY != playerY) {
			cameraY += (playerY - cameraY) / 16;
		}
		if (cameraX != playerX) {
			cameraX += (playerX - cameraX) / 16;
		}
		if (Preferences.aBoolean63) {
			for (@Pc(93) int local93 = 0; local93 < InterfaceList.keyQueueSize; local93++) {
				@Pc(104) int code = InterfaceList.keyCodes[local93];
				if (code == Keyboard.KEY_UP) {
					pitchTarget += 47;
				} else if (code == Keyboard.KEY_DOWN) {
					pitchTarget -= 17;
				} else if (code == Keyboard.KEY_LEFT) {
					yawTarget -= 65;
				} else if (code == Keyboard.KEY_RIGHT) {
					yawTarget += 191;
				}
			}
			clampCameraAngle();
		}
	}

	@OriginalMember(owner = "client!bh", name = "a", descriptor = "(IIIIIIII)V")
	public static void calculateRenderPosition(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6) {
		@Pc(5) int local5;
		@Pc(29) int local29;
		if (GlRenderer.enabled) {
			local5 = arg1 - 334;
			if (local5 < 0) {
				local5 = 0;
			} else if (local5 > 100) {
				local5 = 100;
			}
			local29 = local5 * (ScriptRunner.aShort27 - ScriptRunner.aShort30) / 100 + ScriptRunner.aShort30;
			arg3 = local29 * arg3 >> 8;
		}
		local5 = 2048 - arg6 & 0x7FF;
		local29 = 2048 - arg4 & 0x7FF;
		@Pc(55) int local55 = 0;
		@Pc(57) int local57 = arg3;
		@Pc(59) int local59 = 0;
		@Pc(72) int local72;
		@Pc(68) int local68;
		if (local5 != 0) {
			local68 = MathUtils.cos[local5];
			local72 = MathUtils.sin[local5];
			local59 = local72 * -arg3 >> 16;
			local57 = local68 * arg3 >> 16;
		}
		if (local29 != 0) {
			local72 = MathUtils.sin[local29];
			local68 = MathUtils.cos[local29];
			local55 = local72 * local57 >> 16;
			local57 = local57 * local68 >> 16;
		}
		cameraPitch = arg6;
		cameraYaw = arg4;
		renderY = arg5 - local57;
		renderX = arg0 - local55;
		renderZ = arg2 - local59;
	}
}
