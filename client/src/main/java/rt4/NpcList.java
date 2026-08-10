package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

public class NpcList {
	@OriginalMember(owner = "client!nm", name = "S", descriptor = "[Lclient!km;")
	public static final Npc[] npcs = new Npc[32768];
	@OriginalMember(owner = "client!cj", name = "i", descriptor = "[I")
	public static final int[] ids = new int[32768];
	@OriginalMember(owner = "client!wd", name = "g", descriptor = "I")
	public static int size = 0;

	@OriginalMember(owner = "client!ig", name = "a", descriptor = "(I)V")
	public static void processAllNpcs() {
		for (@Pc(7) int local7 = 0; local7 < size; local7++) {
			@Pc(18) int local18 = ids[local7];
			@Pc(22) Npc local22 = npcs[local18];
			if (local22 != null) {
				processEntity(local22.type.size, local22);
			}
		}
	}

	@OriginalMember(owner = "client!vg", name = "a", descriptor = "(IILclient!fe;)V")
	public static void processEntity(@OriginalArg(0) int arg0, @OriginalArg(2) PathingEntity arg1) {
		if (client.loop < arg1.forceMoveCyclesToStart) {
			lerpToForceMoveStart(arg1);
		} else if (arg1.forceMoveCyclesToDest >= client.loop) {
			lerpToForceMoveDest(arg1);
		} else {
			processMovement(arg1);
		}
		if (arg1.xFine < 128 || arg1.yFine < 128 || arg1.xFine >= 13184 || arg1.yFine >= 13184) {
			arg1.seqId = -1;
			arg1.spotAnimId = -1;
			arg1.forceMoveCyclesToStart = 0;
			arg1.forceMoveCyclesToDest = 0;
			arg1.xFine = arg1.movementQueueX[0] * 128 + arg1.getSize() * 64;
			arg1.yFine = arg1.movementQueueY[0] * 128 + arg1.getSize() * 64;
			arg1.resetMovementQueue();
		}
		if (arg1 == PlayerList.self && (arg1.xFine < 1536 || arg1.yFine < 1536 || arg1.xFine >= 11776 || arg1.yFine >= 11776)) {
			arg1.spotAnimId = -1;
			arg1.forceMoveCyclesToStart = 0;
			arg1.forceMoveCyclesToDest = 0;
			arg1.seqId = -1;
			arg1.xFine = arg1.movementQueueX[0] * 128 + arg1.getSize() * 64;
			arg1.yFine = arg1.movementQueueY[0] * 128 + arg1.getSize() * 64;
			arg1.resetMovementQueue();
		}
		processFacing(arg1);
		processAnimations(arg1);
	}

	@OriginalMember(owner = "client!bh", name = "a", descriptor = "(Lclient!fe;Z)V")
	public static void lerpToForceMoveStart(@OriginalArg(0) PathingEntity arg0) {
		@Pc(8) int toStartCyclesLeft = arg0.forceMoveCyclesToStart - client.loop;
		@Pc(20) int startXFine = arg0.forceMoveStartX * 128 + arg0.getSize() * 64;
		@Pc(36) int startYFine = arg0.forceMoveStartY * 128 + arg0.getSize() * 64;
		if (arg0.forceMoveDirection == 0) {
			arg0.targetAngle = 1024;
		}
		arg0.xFine += (startXFine - arg0.xFine) / toStartCyclesLeft;
		arg0.yFine += (startYFine - arg0.yFine) / toStartCyclesLeft;
		if (arg0.forceMoveDirection == 1) {
			arg0.targetAngle = 1536;
		}
		arg0.movementCatchupTicks = 0;
		if (arg0.forceMoveDirection == 2) {
			arg0.targetAngle = 0;
		}
		if (arg0.forceMoveDirection == 3) {
			arg0.targetAngle = 512;
		}
	}

	@OriginalMember(owner = "client!wl", name = "a", descriptor = "(Lclient!fe;B)V")
	public static void lerpToForceMoveDest(@OriginalArg(0) PathingEntity arg0) {
		if (client.loop == arg0.forceMoveCyclesToDest || arg0.seqId == -1 || arg0.seqDelay != 0 || arg0.seqDelayClock + 1 > SeqTypeList.get(arg0.seqId).frameDelay[arg0.seqFrame]) {
			@Pc(35) int cyclesDiff = arg0.forceMoveCyclesToDest - arg0.forceMoveCyclesToStart;
			@Pc(41) int lerpCyclesRemaining = client.loop - arg0.forceMoveCyclesToStart;
			@Pc(52) int startXFine = arg0.forceMoveStartX * 128 + arg0.getSize() * 64;
			@Pc(64) int startYFine = arg0.forceMoveStartY * 128 + arg0.getSize() * 64;
			@Pc(75) int destXFine = arg0.forceMoveDestX * 128 + arg0.getSize() * 64;
			@Pc(86) int destYFine = arg0.forceMoveDestY * 128 + arg0.getSize() * 64;
			arg0.xFine = (lerpCyclesRemaining * destXFine + startXFine * (cyclesDiff - lerpCyclesRemaining)) / cyclesDiff;
			arg0.yFine = (destYFine * lerpCyclesRemaining + startYFine * (cyclesDiff - lerpCyclesRemaining)) / cyclesDiff;
		}
		arg0.movementCatchupTicks = 0;
		if (arg0.forceMoveDirection == 0) {
			arg0.targetAngle = 1024;
		}
		if (arg0.forceMoveDirection == 1) {
			arg0.targetAngle = 1536;
		}
		if (arg0.forceMoveDirection == 2) {
			arg0.targetAngle = 0;
		}
		if (arg0.forceMoveDirection == 3) {
			arg0.targetAngle = 512;
		}
		arg0.currentAngle = arg0.targetAngle;
	}

	@OriginalMember(owner = "client!ia", name = "a", descriptor = "(BLclient!fe;)V")
	public static void processMovement(@OriginalArg(1) PathingEntity arg0) {
		@Pc(9) BasType local9 = arg0.getBasType();
		arg0.movementSeqId = local9.idleAnimationId;
		if (arg0.movementQueueSize == 0) {
			arg0.movementCatchupTicks = 0;
			return;
		}
		if (arg0.seqId != -1 && arg0.seqDelay == 0) {
			@Pc(40) SeqType local40 = SeqTypeList.get(arg0.seqId);
			if (arg0.seqMovementSteps > 0 && local40.looptype == 0) {
				arg0.movementCatchupTicks++;
				return;
			}
			if (arg0.seqMovementSteps <= 0 && local40.movetype == 0) {
				arg0.movementCatchupTicks++;
				return;
			}
		}
		@Pc(79) int local79 = arg0.xFine;
		@Pc(82) int local82 = arg0.yFine;
		@Pc(99) int local99 = arg0.movementQueueX[arg0.movementQueueSize - 1] * 128 + arg0.getSize() * 64;
		@Pc(116) int local116 = arg0.movementQueueY[arg0.movementQueueSize - 1] * 128 + arg0.getSize() * 64;
		if (local99 - local79 > 256 || local99 - local79 < -256 || local116 - local82 > 256 || local116 - local82 < -256) {
			arg0.xFine = local99;
			arg0.yFine = local116;
			return;
		}
		if (local99 <= local79) {
			if (local79 <= local99) {
				if (local116 > local82) {
					arg0.targetAngle = 1024;
				} else if (local82 > local116) {
					arg0.targetAngle = 0;
				}
			} else if (local116 > local82) {
				arg0.targetAngle = 768;
			} else if (local116 < local82) {
				arg0.targetAngle = 256;
			} else {
				arg0.targetAngle = 512;
			}
		} else if (local116 > local82) {
			arg0.targetAngle = 1280;
		} else if (local82 > local116) {
			arg0.targetAngle = 1792;
		} else {
			arg0.targetAngle = 1536;
		}
		@Pc(224) int local224 = arg0.targetAngle - arg0.currentAngle & 0x7FF;
		@Pc(227) int local227 = local9.walkFullTurnAnimationId;
		if (local224 > 1024) {
			local224 -= 2048;
		}
		@Pc(233) boolean local233 = true;
		@Pc(235) byte local235 = 1;
		if (local224 >= -256 && local224 <= 256) {
			local227 = local9.walkAnimation;
		} else if (local224 >= 256 && local224 < 768) {
			local227 = local9.walkCWTurnAnimationId;
		} else if (local224 >= -768 && local224 <= -256) {
			local227 = local9.walkCCWTurnAnimationId;
		}
		@Pc(273) int local273 = 4;
		if (local227 == -1) {
			local227 = local9.walkAnimation;
		}
		arg0.movementSeqId = local227;
		if (arg0 instanceof Npc) {
			local233 = ((Npc) arg0).type.rotationflag;
		}
		if (local233) {
			if (arg0.currentAngle != arg0.targetAngle && arg0.faceEntity == -1 && arg0.turnSpeed != 0) {
				local273 = 2;
			}
			if (arg0.movementQueueSize > 2) {
				local273 = 6;
			}
			if (arg0.movementQueueSize > 3) {
				local273 = 8;
			}
			if (arg0.movementCatchupTicks > 0 && arg0.movementQueueSize > 1) {
				local273 = 8;
				arg0.movementCatchupTicks--;
			}
		} else {
			if (arg0.movementQueueSize > 1) {
				local273 = 6;
			}
			if (arg0.movementQueueSize > 2) {
				local273 = 8;
			}
			if (arg0.movementCatchupTicks > 0 && arg0.movementQueueSize > 1) {
				arg0.movementCatchupTicks--;
				local273 = 8;
			}
		}
		if (arg0.movementQueueSpeed[arg0.movementQueueSize - 1] == 2) {
			local273 <<= 0x1;
			local235 = 2;
		} else if (arg0.movementQueueSpeed[arg0.movementQueueSize - 1] == 0) {
			local235 = 0;
			local273 >>= 0x1;
		}
		if (local273 < 8 || local9.runAnimationId == -1) {
			if (local9.slowWalkAnimationId != -1 && local235 == 0) {
				if (local9.walkFullTurnAnimationId == arg0.movementSeqId && local9.slowWalkFullTurnAnimationId != -1) {
					arg0.movementSeqId = local9.slowWalkFullTurnAnimationId;
				} else if (local9.walkCCWTurnAnimationId == arg0.movementSeqId && local9.slowWalkCCWTurnAnimationId != -1) {
					arg0.movementSeqId = local9.slowWalkCCWTurnAnimationId;
				} else if (local9.walkCWTurnAnimationId == arg0.movementSeqId && local9.slowWalkCWTurnAnimationId != -1) {
					arg0.movementSeqId = local9.slowWalkCWTurnAnimationId;
				} else {
					arg0.movementSeqId = local9.slowWalkAnimationId;
				}
			}
		} else if (local9.walkFullTurnAnimationId == arg0.movementSeqId && local9.runFullTurnAnimationId != -1) {
			arg0.movementSeqId = local9.runFullTurnAnimationId;
		} else if (arg0.movementSeqId == local9.walkCCWTurnAnimationId && local9.runCCWTurnAnimationId != -1) {
			arg0.movementSeqId = local9.runCCWTurnAnimationId;
		} else if (arg0.movementSeqId == local9.walkCWTurnAnimationId && local9.runCWTurnAnimationId != -1) {
			arg0.movementSeqId = local9.runCWTurnAnimationId;
		} else {
			arg0.movementSeqId = local9.runAnimationId;
		}
		if (local9.movementAcceleration != -1) {
			local273 <<= 0x7;
			if (arg0.movementQueueSize == 1) {
				@Pc(594) int local594 = (local99 >= arg0.xFine ? local99 - arg0.xFine : -local99 + arg0.xFine) << 7;
				@Pc(600) int local600 = arg0.movementSpeed * arg0.movementSpeed;
				@Pc(622) int local622 = (local116 < arg0.yFine ? arg0.yFine - local116 : -arg0.yFine + local116) << 7;
				@Pc(629) int local629 = local594 > local622 ? local594 : local622;
				@Pc(636) int local636 = local9.movementAcceleration * 2 * local629;
				if (local636 < local600) {
					arg0.movementSpeed /= 2;
				} else if (local629 < local600 / 2) {
					arg0.movementSpeed -= local9.movementAcceleration;
					if (arg0.movementSpeed < 0) {
						arg0.movementSpeed = 0;
					}
				} else if (arg0.movementSpeed < local273) {
					arg0.movementSpeed += local9.movementAcceleration;
					if (arg0.movementSpeed > local273) {
						arg0.movementSpeed = local273;
					}
				}
			} else if (local273 > arg0.movementSpeed) {
				arg0.movementSpeed += local9.movementAcceleration;
				if (local273 < arg0.movementSpeed) {
					arg0.movementSpeed = local273;
				}
			} else if (arg0.movementSpeed > 0) {
				arg0.movementSpeed -= local9.movementAcceleration;
				if (arg0.movementSpeed < 0) {
					arg0.movementSpeed = 0;
				}
			}
			local273 = arg0.movementSpeed >> 7;
			if (local273 < 1) {
				local273 = 1;
			}
		}
		if (local79 < local99) {
			arg0.xFine += local273;
			if (local99 < arg0.xFine) {
				arg0.xFine = local99;
			}
		} else if (local79 > local99) {
			arg0.xFine -= local273;
			if (local99 > arg0.xFine) {
				arg0.xFine = local99;
			}
		}
		if (local82 < local116) {
			arg0.yFine += local273;
			if (arg0.yFine > local116) {
				arg0.yFine = local116;
			}
		} else if (local116 < local82) {
			arg0.yFine -= local273;
			if (local116 > arg0.yFine) {
				arg0.yFine = local116;
			}
		}
		if (arg0.xFine == local99 && local116 == arg0.yFine) {
			arg0.movementQueueSize--;
			if (arg0.seqMovementSteps > 0) {
				arg0.seqMovementSteps--;
			}
		}
	}

	@OriginalMember(owner = "client!cm", name = "a", descriptor = "(ILclient!fe;)V")
	public static void processFacing(@OriginalArg(1) PathingEntity arg0) {
		if (arg0.turnSpeed == 0) {
			return;
		}
		@Pc(13) BasType local13 = arg0.getBasType();
		@Pc(43) int local43;
		@Pc(36) int local36;
		if (arg0.faceEntity != -1 && arg0.faceEntity < 32768) {
			@Pc(26) Npc local26 = npcs[arg0.faceEntity];
			if (local26 != null) {
				local36 = arg0.yFine - local26.yFine;
				local43 = arg0.xFine - local26.xFine;
				if (local43 != 0 || local36 != 0) {
					arg0.targetAngle = (int) (Math.atan2(local43, local36) * 325.949D) & 0x7FF;
				}
			}
		}
		@Pc(94) int local94;
		@Pc(70) int local70;
		if (arg0.faceEntity >= 32768) {
			local70 = arg0.faceEntity - 32768;
			if (local70 == PlayerList.selfId) {
				local70 = 2047;
			}
			@Pc(83) Player local83 = PlayerList.players[local70];
			if (local83 != null) {
				local94 = arg0.yFine - local83.yFine;
				local36 = arg0.xFine - local83.xFine;
				if (local36 != 0 || local94 != 0) {
					arg0.targetAngle = (int) (Math.atan2(local36, local94) * 325.949D) & 0x7FF;
				}
			}
		}
		if ((arg0.faceX != 0 || arg0.faceY != 0) && (arg0.movementQueueSize == 0 || arg0.movementCatchupTicks > 0)) {
			local70 = arg0.xFine - (arg0.faceX - Camera.originX - Camera.originX) * 64;
			local43 = arg0.yFine - (arg0.faceY - Camera.originY - Camera.originY) * 64;
			if (local70 != 0 || local43 != 0) {
				arg0.targetAngle = (int) (Math.atan2(local70, local43) * 325.949D) & 0x7FF;
			}
			arg0.faceY = 0;
			arg0.faceX = 0;
		}
		local70 = arg0.targetAngle - arg0.currentAngle & 0x7FF;
		if (local70 == 0) {
			arg0.standingTurnCounter = 0;
			arg0.yawVelocity = 0;
		} else if (local13.yawAcceleration == 0) {
			arg0.standingTurnCounter++;
			@Pc(226) boolean local226;
			if (local70 > 1024) {
				arg0.currentAngle -= arg0.turnSpeed;
				local226 = true;
				if (local70 < arg0.turnSpeed || local70 > 2048 - arg0.turnSpeed) {
					arg0.currentAngle = arg0.targetAngle;
					local226 = false;
				}
				if (local13.idleAnimationId == arg0.movementSeqId && (arg0.standingTurnCounter > 25 || local226)) {
					if (local13.standingCCWTurn == -1) {
						arg0.movementSeqId = local13.walkAnimation;
					} else {
						arg0.movementSeqId = local13.standingCCWTurn;
					}
				}
			} else {
				local226 = true;
				arg0.currentAngle += arg0.turnSpeed;
				if (arg0.turnSpeed > local70 || local70 > 2048 - arg0.turnSpeed) {
					local226 = false;
					arg0.currentAngle = arg0.targetAngle;
				}
				if (local13.idleAnimationId == arg0.movementSeqId && (arg0.standingTurnCounter > 25 || local226)) {
					if (local13.standingCWTurn == -1) {
						arg0.movementSeqId = local13.walkAnimation;
					} else {
						arg0.movementSeqId = local13.standingCWTurn;
					}
				}
			}
			arg0.currentAngle &= 0x7FF;
		} else {
			if (local13.idleAnimationId == arg0.movementSeqId && arg0.standingTurnCounter > 25) {
				if (local13.standingCWTurn == -1) {
					arg0.movementSeqId = local13.walkAnimation;
				} else {
					arg0.movementSeqId = local13.standingCWTurn;
				}
			}
			local43 = arg0.targetAngle << 5;
			if (local43 != arg0.preciseTargetYaw) {
				arg0.yawDistanceTraveled = 0;
				arg0.preciseTargetYaw = local43;
				local36 = local43 - arg0.preciseYaw & 0xFFFF;
				local94 = arg0.yawVelocity * arg0.yawVelocity / (local13.yawAcceleration * 2);
				@Pc(471) int local471;
				if (arg0.yawVelocity > 0 && local36 >= local94 && local36 - local94 < 32768) {
					arg0.yawDecelThreshold = local36 / 2;
					arg0.yawAccelerating = true;
					local471 = local13.yawMaxSpeed * local13.yawMaxSpeed / (local13.yawAcceleration * 2);
					if (local471 > 32767) {
						local471 = 32767;
					}
					if (local471 < arg0.yawDecelThreshold) {
						arg0.yawDecelThreshold = local36 - local471;
					}
				} else if (arg0.yawVelocity < 0 && local94 <= 65536 - local36 && 65536 - local36 - local94 < 32768) {
					arg0.yawDecelThreshold = (65536 - local36) / 2;
					arg0.yawAccelerating = true;
					local471 = local13.yawMaxSpeed * local13.yawMaxSpeed / (local13.yawAcceleration * 2);
					if (local471 > 32767) {
						local471 = 32767;
					}
					if (local471 < arg0.yawDecelThreshold) {
						arg0.yawDecelThreshold = 65536 - local36 - local471;
					}
				} else {
					arg0.yawAccelerating = false;
				}
			}
			if (arg0.yawVelocity == 0) {
				local36 = arg0.preciseTargetYaw - arg0.preciseYaw & 0xFFFF;
				if (local36 < local13.yawAcceleration) {
					arg0.preciseYaw = arg0.preciseTargetYaw;
				} else {
					arg0.yawDistanceTraveled = 0;
					local94 = local13.yawMaxSpeed * local13.yawMaxSpeed / (local13.yawAcceleration * 2);
					arg0.yawAccelerating = true;
					if (local94 > 32767) {
						local94 = 32767;
					}
					if (local36 >= 32768) {
						arg0.yawVelocity = -local13.yawAcceleration;
						arg0.yawDecelThreshold = (65536 - local36) / 2;
						if (local94 < arg0.yawDecelThreshold) {
							arg0.yawDecelThreshold = 65536 - local36 - local94;
						}
					} else {
						arg0.yawVelocity = local13.yawAcceleration;
						arg0.yawDecelThreshold = local36 / 2;
						if (local94 < arg0.yawDecelThreshold) {
							arg0.yawDecelThreshold = local36 - local94;
						}
					}
				}
			} else if (arg0.yawVelocity <= 0) {
				if (arg0.yawDistanceTraveled >= arg0.yawDecelThreshold) {
					arg0.yawAccelerating = false;
				}
				if (!arg0.yawAccelerating) {
					arg0.yawVelocity += local13.yawAcceleration;
					if (arg0.yawVelocity > 0) {
						arg0.yawVelocity = 0;
					}
				} else if (-local13.yawMaxSpeed < arg0.yawVelocity) {
					arg0.yawVelocity -= local13.yawAcceleration;
				}
			} else {
				if (arg0.yawDecelThreshold <= arg0.yawDistanceTraveled) {
					arg0.yawAccelerating = false;
				}
				if (!arg0.yawAccelerating) {
					arg0.yawVelocity -= local13.yawAcceleration;
					if (arg0.yawVelocity < 0) {
						arg0.yawVelocity = 0;
					}
				} else if (arg0.yawVelocity < local13.yawMaxSpeed) {
					arg0.yawVelocity += local13.yawAcceleration;
				}
			}
			arg0.preciseYaw += arg0.yawVelocity;
			arg0.preciseYaw &= 0xFFFF;
			if (arg0.yawVelocity <= 0) {
				arg0.yawDistanceTraveled -= arg0.yawVelocity;
			} else {
				arg0.yawDistanceTraveled += arg0.yawVelocity;
			}
			arg0.currentAngle = arg0.preciseYaw >> 5;
		}
	}

	@OriginalMember(owner = "client!ck", name = "a", descriptor = "(Lclient!fe;I)V")
	public static void processAnimations(@OriginalArg(0) PathingEntity entity) {
		entity.seqStretches = false;
		@Pc(18) SeqType local18;
		if (entity.movementSeqId != -1) {
			local18 = SeqTypeList.get(entity.movementSeqId);
			if (local18 == null || local18.frames == null) {
				entity.movementSeqId = -1;
			} else {
				entity.movementSeqDelayClock++;
				if (local18.frames.length > entity.movementSeqFrame && entity.movementSeqDelayClock > local18.frameDelay[entity.movementSeqFrame]) {
					entity.movementSeqDelayClock = 1;
					entity.movementSeqFrame++;
					entity.movementSeqNextFrame++;
					SoundPlayer.playSeqSound(entity.yFine, local18, entity.xFine, entity == PlayerList.self, entity.movementSeqFrame);
				}
				if (entity.movementSeqFrame >= local18.frames.length) {
					entity.movementSeqFrame = 0;
					entity.movementSeqDelayClock = 0;
					SoundPlayer.playSeqSound(entity.yFine, local18, entity.xFine, PlayerList.self == entity, entity.movementSeqFrame);
				}
				entity.movementSeqNextFrame = entity.movementSeqFrame + 1;
				if (entity.movementSeqNextFrame >= local18.frames.length) {
					entity.movementSeqNextFrame = 0;
				}
			}
		}
		@Pc(156) int local156;
		if (entity.spotAnimId != -1 && client.loop >= entity.spotAnimStart) {
			local156 = SpotAnimTypeList.get(entity.spotAnimId).seqId;
			if (local156 == -1) {
				entity.spotAnimId = -1;
			} else {
				@Pc(165) SeqType local165 = SeqTypeList.get(local156);
				if (local165 == null || local165.frames == null) {
					entity.spotAnimId = -1;
				} else {
					if (entity.spotAnimFrame < 0) {
						entity.spotAnimFrame = 0;
						SoundPlayer.playSeqSound(entity.yFine, local165, entity.xFine, PlayerList.self == entity, 0);
					}
					entity.spotAnimDelayClock++;
					if (entity.spotAnimFrame < local165.frames.length && local165.frameDelay[entity.spotAnimFrame] < entity.spotAnimDelayClock) {
						entity.spotAnimFrame++;
						entity.spotAnimDelayClock = 1;
						SoundPlayer.playSeqSound(entity.yFine, local165, entity.xFine, PlayerList.self == entity, entity.spotAnimFrame);
					}
					if (entity.spotAnimFrame >= local165.frames.length) {
						entity.spotAnimId = -1;
					}
					entity.spotAnimNextFrame = entity.spotAnimFrame + 1;
					if (local165.frames.length <= entity.spotAnimNextFrame) {
						entity.spotAnimNextFrame = -1;
					}
				}
			}
		}
		if (entity.seqId != -1 && entity.seqDelay <= 1) {
			local18 = SeqTypeList.get(entity.seqId);
			if (local18.looptype == 1 && entity.seqMovementSteps > 0 && client.loop >= entity.forceMoveCyclesToStart && client.loop > entity.forceMoveCyclesToDest) {
				entity.seqDelay = 1;
				return;
			}
		}
		if (entity.seqId != -1 && entity.seqDelay == 0) {
			local18 = SeqTypeList.get(entity.seqId);
			if (local18 == null || local18.frames == null) {
				entity.seqId = -1;
			} else {
				entity.seqDelayClock++;
				if (entity.seqFrame < local18.frames.length && entity.seqDelayClock > local18.frameDelay[entity.seqFrame]) {
					entity.seqDelayClock = 1;
					entity.seqFrame++;
					SoundPlayer.playSeqSound(entity.yFine, local18, entity.xFine, entity == PlayerList.self, entity.seqFrame);
				}
				if (local18.frames.length <= entity.seqFrame) {
					entity.seqFrame -= local18.replayoff;
					entity.seqReplayCount++;
					if (entity.seqReplayCount >= local18.replaycount) {
						entity.seqId = -1;
					} else if (entity.seqFrame >= 0 && local18.frames.length > entity.seqFrame) {
						SoundPlayer.playSeqSound(entity.yFine, local18, entity.xFine, PlayerList.self == entity, entity.seqFrame);
					} else {
						entity.seqId = -1;
					}
				}
				entity.seqNextFrame = entity.seqFrame + 1;
				if (entity.seqNextFrame >= local18.frames.length) {
					entity.seqNextFrame -= local18.replayoff;
					if (local18.replaycount <= entity.seqReplayCount + 1) {
						entity.seqNextFrame = -1;
					} else if (entity.seqNextFrame < 0 || entity.seqNextFrame >= local18.frames.length) {
						entity.seqNextFrame = -1;
					}
				}
				entity.seqStretches = local18.stretches;
			}
		}
		if (entity.seqDelay > 0) {
			entity.seqDelay--;
		}

		for (local156 = 0; local156 < entity.slotAnimations.length; local156++) {
			@Pc(545) SlotAnimation c147 = entity.slotAnimations[local156];
			if (c147 != null) {
				if (c147.startDelay > 0) {
					c147.startDelay--;
				} else {
					@Pc(570) SeqType local570 = SeqTypeList.get(c147.seqId);
					if (local570 == null || local570.frames == null) {
						entity.slotAnimations[local156] = null;
					} else {
						c147.delayClock++;
						if (c147.currentFrame < local570.frames.length && c147.delayClock > local570.frameDelay[c147.currentFrame]) {
							c147.currentFrame++;
							c147.delayClock = 1;
							SoundPlayer.playSeqSound(entity.yFine, local570, entity.xFine, entity == PlayerList.self, c147.currentFrame);
						}
						if (local570.frames.length <= c147.currentFrame) {
							c147.replayCount++;
							c147.currentFrame -= local570.replayoff;
							if (local570.replaycount <= c147.replayCount) {
								entity.slotAnimations[local156] = null;
							} else if (c147.currentFrame >= 0 && c147.currentFrame < local570.frames.length) {
								SoundPlayer.playSeqSound(entity.yFine, local570, entity.xFine, PlayerList.self == entity, c147.currentFrame);
							} else {
								entity.slotAnimations[local156] = null;
							}
						}
						c147.nextFrame = c147.currentFrame + 1;
						if (local570.frames.length <= c147.nextFrame) {
							c147.nextFrame -= local570.replayoff;
							if (c147.replayCount + 1 >= local570.replaycount) {
								c147.nextFrame = -1;
							} else if (c147.nextFrame < 0 || local570.frames.length <= c147.nextFrame) {
								c147.nextFrame = -1;
							}
						}
					}
				}
			}
		}
	}
}
