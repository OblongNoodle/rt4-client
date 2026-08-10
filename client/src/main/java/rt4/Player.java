package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!e")
public final class Player extends PathingEntity {

	@OriginalMember(owner = "client!mj", name = "d", descriptor = "[Lclient!na;")
	public static final JagString[] options = new JagString[8];
	@OriginalMember(owner = "client!pg", name = "ab", descriptor = "[I")
	public static final int[] cursors = new int[8];
	@OriginalMember(owner = "client!a", name = "f", descriptor = "[Z")
	public static final boolean[] secondaryOptions = new boolean[8];
	@OriginalMember(owner = "client!pa", name = "P", descriptor = "Lclient!na;")
	public static JagString password = JagString.EMPTY;
	@OriginalMember(owner = "client!pa", name = "S", descriptor = "Lclient!na;")
	public static JagString usernameInput = JagString.EMPTY;
	@OriginalMember(owner = "client!ba", name = "w", descriptor = "I")
	public static int inTutorialIsland = 0;
	@OriginalMember(owner = "client!ee", name = "b", descriptor = "I")
	public static int plane;
	@OriginalMember(owner = "client!bb", name = "E", descriptor = "I")
	public static int runEnergy = 0;
	@OriginalMember(owner = "client!ug", name = "o", descriptor = "I")
	public static int weight = 0;
	@OriginalMember(owner = "client!jl", name = "H", descriptor = "I")
	public static int worldId = -1;
	@OriginalMember(owner = "client!em", name = "B", descriptor = "I")
	public static int rebootTimer = 0;
	@OriginalMember(owner = "client!hm", name = "gb", descriptor = "J")
	public static long name37;
	@OriginalMember(owner = "client!sm", name = "k", descriptor = "Lsignlink!im;")
	public static PrivilegedRequest lastLogAddress;
	@OriginalMember(owner = "client!ib", name = "l", descriptor = "I")
	public static int glPaddingCount = 0;
	@OriginalMember(owner = "client!ea", name = "r", descriptor = "[[B")
	public static byte[][] glPaddingBuffers;
	@OriginalMember(owner = "client!e", name = "Bc", descriptor = "Lclient!hh;")
	public PlayerAppearance appearance;

	@OriginalMember(owner = "client!e", name = "Mc", descriptor = "Lclient!na;")
	public JagString username;

	@OriginalMember(owner = "client!e", name = "tc", descriptor = "I")
	public int prayerHeadIcon = -1;

	@OriginalMember(owner = "client!e", name = "wc", descriptor = "I")
	private int titleIndex = 0;

	@OriginalMember(owner = "client!e", name = "uc", descriptor = "I")
	public int team = 0;

	@OriginalMember(owner = "client!e", name = "yc", descriptor = "I")
	public int combatLevel = 0;

	@OriginalMember(owner = "client!e", name = "Fc", descriptor = "I")
	public int combatLevelWithSummoning = 0;

	@OriginalMember(owner = "client!e", name = "Cc", descriptor = "I")
	public int walkSound = -1;

	@OriginalMember(owner = "client!e", name = "qc", descriptor = "I")
	public int idleSound = -1;

	@OriginalMember(owner = "client!e", name = "Pc", descriptor = "I")
	public int soundRadius = 0;

	@OriginalMember(owner = "client!e", name = "Hc", descriptor = "I")
	public int crawlSound = -1;

	@OriginalMember(owner = "client!e", name = "Sc", descriptor = "I")
	public int combatRange = -1;

	@OriginalMember(owner = "client!e", name = "Oc", descriptor = "Z")
	public boolean lowDetail = false;

	@OriginalMember(owner = "client!e", name = "Xc", descriptor = "I")
	public int skill = 0;

	@OriginalMember(owner = "client!e", name = "Vc", descriptor = "I")
	public int skullHeadIcon = -1;

	@OriginalMember(owner = "client!e", name = "Wc", descriptor = "I")
	public int runSound = -1;

	@OriginalMember(owner = "client!pa", name = "a", descriptor = "(IIILclient!e;)V")
	public static void animate(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) Player arg2) {
		if (arg1 == arg2.seqId && arg1 != -1) {
			@Pc(89) SeqType local89 = SeqTypeList.get(arg1);
			@Pc(92) int local92 = local89.exactmove;
			if (local92 == 1) {
				arg2.seqDelay = arg0;
				arg2.seqDelayClock = 0;
				arg2.seqNextFrame = 1;
				arg2.seqFrame = 0;
				arg2.seqReplayCount = 0;
				SoundPlayer.playSeqSound(arg2.yFine, local89, arg2.xFine, PlayerList.self == arg2, arg2.seqFrame);
			}
			if (local92 == 2) {
				arg2.seqReplayCount = 0;
			}
		} else if (arg1 == -1 || arg2.seqId == -1 || SeqTypeList.get(arg1).priority >= SeqTypeList.get(arg2.seqId).priority) {
			arg2.seqNextFrame = 1;
			arg2.seqFrame = 0;
			arg2.seqDelay = arg0;
			arg2.seqMovementSteps = arg2.movementQueueSize;
			arg2.seqReplayCount = 0;
			arg2.seqDelayClock = 0;
			arg2.seqId = arg1;
			if (arg2.seqId != -1) {
				SoundPlayer.playSeqSound(arg2.yFine, SeqTypeList.get(arg2.seqId), arg2.xFine, arg2 == PlayerList.self, arg2.seqFrame);
			}
		}
	}

	@OriginalMember(owner = "client!ci", name = "a", descriptor = "([I[ILclient!e;B[I)V")
	public static void applySlotAnimations(@OriginalArg(0) int[] arg0, @OriginalArg(1) int[] arg1, @OriginalArg(2) Player arg2, @OriginalArg(4) int[] arg3) {
		for (@Pc(7) int local7 = 0; local7 < arg1.length; local7++) {
			@Pc(20) int local20 = arg1[local7];
			@Pc(24) int local24 = arg3[local7];
			@Pc(28) int local28 = arg0[local7];
			@Pc(30) int local30 = 0;
			while (local24 != 0 && local30 < arg2.slotAnimations.length) {
				if ((local24 & 0x1) != 0) {
					if (local20 == -1) {
						arg2.slotAnimations[local30] = null;
					} else {
						@Pc(68) SeqType local68 = SeqTypeList.get(local20);
						@Pc(71) int local71 = local68.exactmove;
						@Pc(76) SlotAnimation local76 = arg2.slotAnimations[local30];
						if (local76 != null) {
							if (local20 == local76.seqId) {
								if (local71 == 0) {
									local76 = arg2.slotAnimations[local30] = null;
								} else if (local71 == 1) {
									local76.replayCount = 0;
									local76.nextFrame = 1;
									local76.currentFrame = 0;
									local76.startDelay = local28;
									local76.delayClock = 0;
									SoundPlayer.playSeqSound(arg2.yFine, local68, arg2.xFine, arg2 == PlayerList.self, 0);
								} else if (local71 == 2) {
									local76.replayCount = 0;
								}
							} else if (local68.priority >= SeqTypeList.get(local76.seqId).priority) {
								local76 = arg2.slotAnimations[local30] = null;
							}
						}
						if (local76 == null) {
							local76 = arg2.slotAnimations[local30] = new SlotAnimation();
							local76.seqId = local20;
							local76.nextFrame = 1;
							local76.delayClock = 0;
							local76.startDelay = local28;
							local76.currentFrame = 0;
							local76.replayCount = 0;
							SoundPlayer.playSeqSound(arg2.yFine, local68, arg2.xFine, arg2 == PlayerList.self, 0);
						}
					}
				}
				local30++;
				local24 >>>= 0x1;
			}
		}
	}

	@OriginalMember(owner = "client!la", name = "a", descriptor = "(ILclient!e;)I")
	public static int getSound(@OriginalArg(1) Player arg0) {
		@Pc(14) int local14 = arg0.walkSound;
		@Pc(18) BasType local18 = arg0.getBasType();
		if (local18.idleAnimationId == arg0.movementSeqId) {
			local14 = arg0.idleSound;
		} else if (local18.runAnimationId == arg0.movementSeqId || arg0.movementSeqId == local18.runFullTurnAnimationId || arg0.movementSeqId == local18.runCWTurnAnimationId || local18.runCCWTurnAnimationId == arg0.movementSeqId) {
			local14 = arg0.runSound;
		} else if (arg0.movementSeqId == local18.slowWalkAnimationId || arg0.movementSeqId == local18.slowWalkFullTurnAnimationId || arg0.movementSeqId == local18.slowWalkCWTurnAnimationId || arg0.movementSeqId == local18.slowWalkCCWTurnAnimationId) {
			local14 = arg0.crawlSound;
		}
		return local14;
	}

	@OriginalMember(owner = "client!um", name = "a", descriptor = "(Z)V")
	public static void setTutorialIsland() {
		inTutorialIsland = 0;
		@Pc(17) int local17 = Camera.originX + (PlayerList.self.xFine >> 7);
		@Pc(25) int local25 = (PlayerList.self.yFine >> 7) + Camera.originZ;
		if (local17 >= 3053 && local17 <= 3156 && local25 >= 3056 && local25 <= 3136) {
			inTutorialIsland = 1;
		}
		if (local17 >= 3072 && local17 <= 3118 && local25 >= 9492 && local25 <= 9535) {
			inTutorialIsland = 1;
		}
		if (inTutorialIsland == 1 && local17 >= 3139 && local17 <= 3199 && local25 >= 3008 && local25 <= 3062) {
			inTutorialIsland = 0;
		}
	}

	@OriginalMember(owner = "client!rg", name = "a", descriptor = "(Lclient!e;I)V")
	public static void updateAreaSound(@OriginalArg(0) Player arg0) {
		@Pc(12) AreaSound local12 = (AreaSound) AreaSoundManager.playerSounds.get(arg0.username.encode37());
		if (local12 == null) {
			AreaSoundManager.add(arg0.movementQueueY[0], null, 0, null, arg0.movementQueueX[0], plane, arg0);
		} else {
			local12.update();
		}
	}

	@OriginalMember(owner = "client!bf", name = "c", descriptor = "(I)V")
	public static void invalidateGlModels() {
		if (!GlRenderer.enabled || LoginManager.aBoolean252) {
			return;
		}
		@Pc(14) Tile[][][] local14 = SceneGraph.tiles;
		for (@Pc(22) int local22 = 0; local22 < local14.length; local22++) {
			@Pc(30) Tile[][] local30 = local14[local22];
			for (@Pc(32) int local32 = 0; local32 < local30.length; local32++) {
				for (@Pc(42) int local42 = 0; local42 < local30[local32].length; local42++) {
					@Pc(54) Tile local54 = local30[local32][local42];
					if (local54 != null) {
						@Pc(71) GlModel local71;
						if (local54.groundDecor != null && local54.groundDecor.entity instanceof GlModel) {
							local71 = (GlModel) local54.groundDecor.entity;
							if ((local54.groundDecor.key & Long.MIN_VALUE) == 0L) {
								local71.method4111(false, true, true, false, true, true);
							} else {
								local71.method4111(true, true, true, true, true, true);
							}
						}
						if (local54.wallDecor != null) {
							if (local54.wallDecor.primary instanceof GlModel) {
								local71 = (GlModel) local54.wallDecor.primary;
								if ((local54.wallDecor.key & Long.MIN_VALUE) == 0L) {
									local71.method4111(false, true, true, false, true, true);
								} else {
									local71.method4111(true, true, true, true, true, true);
								}
							}
							if (local54.wallDecor.secondary instanceof GlModel) {
								local71 = (GlModel) local54.wallDecor.secondary;
								if ((Long.MIN_VALUE & local54.wallDecor.key) == 0L) {
									local71.method4111(false, true, true, false, true, true);
								} else {
									local71.method4111(true, true, true, true, true, true);
								}
							}
						}
						if (local54.wall != null) {
							if (local54.wall.primary instanceof GlModel) {
								local71 = (GlModel) local54.wall.primary;
								if ((local54.wall.key & Long.MIN_VALUE) == 0L) {
									local71.method4111(false, true, true, false, true, true);
								} else {
									local71.method4111(true, true, true, true, true, true);
								}
							}
							if (local54.wall.secondary instanceof GlModel) {
								local71 = (GlModel) local54.wall.secondary;
								if ((Long.MIN_VALUE & local54.wall.key) == 0L) {
									local71.method4111(false, true, true, false, true, true);
								} else {
									local71.method4111(true, true, true, true, true, true);
								}
							}
						}
						for (@Pc(270) int local270 = 0; local270 < local54.sceneryLen; local270++) {
							if (local54.scenery[local270].entity instanceof GlModel) {
								@Pc(293) GlModel local293 = (GlModel) local54.scenery[local270].entity;
								if ((Long.MIN_VALUE & local54.scenery[local270].key) == 0L) {
									local293.method4111(false, true, true, false, true, true);
								} else {
									local293.method4111(true, true, true, true, true, true);
								}
							}
						}
					}
				}
			}
		}
		LoginManager.aBoolean252 = true;
	}

	@OriginalMember(owner = "client!ja", name = "a", descriptor = "(IIIIB)V")
	public static void renderCross(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3) {
		if (Cross.type == 1) {
			Sprites.crosses[Cross.milliseconds / 100].render(Cross.x - 8, Cross.y + -8);
		}
		if (Cross.type == 2) {
			Sprites.crosses[Cross.milliseconds / 100 + 4].render(Cross.x - 8, Cross.y + -8);
		}
		setTutorialIsland();
	}

	@OriginalMember(owner = "client!e", name = "c", descriptor = "(B)I")
	@Override
	public final int getSize() {
		return this.appearance == null || this.appearance.npcId == -1 ? super.getSize() : NpcTypeList.get(this.appearance.npcId).size;
	}

	@OriginalMember(owner = "client!e", name = "b", descriptor = "(I)I")
	@Override
	public final int getBasId() {
		return this.basTypeId;
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(ILclient!wa;)V")
	public final void decodeAppearance(@OriginalArg(1) Buffer arg0) {
		arg0.offset = 0;
		@Pc(20) int local20 = arg0.g1();
		@Pc(22) int local22 = -1;
		@Pc(26) int local26 = local20 & 0x1;
		@Pc(37) boolean showSkillLevel = (local20 & 0x4) != 0;
		@Pc(41) int local41 = super.getSize();
		@Pc(44) int[] local44 = new int[12];
		this.setSize((local20 >> 3 & 0x7) + 1);
		this.titleIndex = local20 >> 6 & 0x3;
		this.xFine += (this.getSize() - local41) * 64;
		this.yFine += (this.getSize() - local41) * 64;
		this.skullHeadIcon = arg0.g1b();
		this.prayerHeadIcon = arg0.g1b();
		this.team = 0;
		@Pc(111) int local111;
		@Pc(127) int local127;
		@Pc(134) int local134;
		@Pc(175) int local175;
		for (@Pc(102) int local102 = 0; local102 < 12; local102++) {
			local111 = arg0.g1();
			if (local111 == 0) {
				local44[local102] = 0;
			} else {
				local127 = arg0.g1();
				local134 = (local111 << 8) + local127;
				if (local102 == 0 && local134 == 65535) {
					local22 = arg0.g2();
					this.team = arg0.g1();
					break;
				}
				if (local134 >= 32768) {
					local134 = Equipment.objIds[local134 - 32768];
					local44[local102] = local134 | 0x40000000;
					local175 = ObjTypeList.get(local134).team;
					if (local175 != 0) {
						this.team = local175;
					}
				} else {
					local44[local102] = local134 - 256 | Integer.MIN_VALUE;
				}
			}
		}
		@Pc(197) int[] local197 = new int[5];
		for (local111 = 0; local111 < 5; local111++) {
			local127 = arg0.g1();
			if (local127 < 0 || local127 >= PlayerAppearance.destinationBodyColors[local111].length) {
				local127 = 0;
			}
			local197[local111] = local127;
		}
		this.basTypeId = arg0.g2();
		@Pc(236) long local236 = arg0.g8();
		this.username = Base37.decode37(local236).toTitleCase();
		this.combatLevel = arg0.g1();
		if (showSkillLevel) {
			this.skill = arg0.g2();
			this.combatLevelWithSummoning = this.combatLevel;
			this.combatRange = -1;
		} else {
			this.skill = 0;
			this.combatLevelWithSummoning = arg0.g1();
			this.combatRange = arg0.g1();
			if (this.combatRange == 255) {
				this.combatRange = -1;
			}
		}
		local134 = this.soundRadius;
		this.soundRadius = arg0.g1();
		if (this.soundRadius == 0) {
			AreaSoundManager.remove(this);
		} else {
			@Pc(309) int local309 = this.crawlSound;
			@Pc(312) int local312 = this.walkSound;
			@Pc(315) int local315 = this.runSound;
			local175 = this.idleSound;
			this.idleSound = arg0.g2();
			this.crawlSound = arg0.g2();
			this.walkSound = arg0.g2();
			this.runSound = arg0.g2();
			if (this.soundRadius != local134 || this.idleSound != local175 || this.crawlSound != local309 || local312 != this.walkSound || this.runSound != local315) {
				updateAreaSound(this);
			}
		}
		if (this.appearance == null) {
			this.appearance = new PlayerAppearance();
		}
		local175 = this.appearance.npcId;
		this.appearance.set(local197, local22, local26 == 1, local44, this.basTypeId);
		if (local175 != local22) {
			this.xFine = this.movementQueueX[0] * 128 + this.getSize() * 64;
			this.yFine = this.movementQueueY[0] * 128 + this.getSize() * 64;
		}
		if (this.particleSystem != null) {
			this.particleSystem.method1646();
		}
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(IIIIIIIIJILclient!ga;)V")
	@Override
	public final void render(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6, @OriginalArg(7) int arg7, @OriginalArg(8) long arg8, @OriginalArg(9) int arg9, @OriginalArg(10) ParticleSystem arg10) {
		if (this.appearance == null) {
			return;
		}
		@Pc(25) SeqType local25 = this.seqId != -1 && this.seqDelay == 0 ? SeqTypeList.get(this.seqId) : null;
		@Pc(54) SeqType local54 = this.movementSeqId == -1 || this.lowDetail || this.movementSeqId == this.getBasType().idleAnimationId && local25 != null ? null : SeqTypeList.get(this.movementSeqId);
		@Pc(76) Model local76 = this.appearance.getBodyModel(this.slotAnimations, this.seqNextFrame, local54, local25, this.movementSeqDelayClock, this.movementSeqNextFrame, this.seqDelayClock, this.seqFrame, this.movementSeqFrame);
		@Pc(79) int local79 = PlayerAppearance.getModelCacheSize();
		if (GlRenderer.enabled && GameShell.maxMemory < 96 && local79 > 50) {
			invalidateGlModels();
		}
		@Pc(102) int local102;
		if (client.modeWhat != 0 && local79 < 50) {
			local102 = 50 - local79;
			while (glPaddingCount < local102) {
				glPaddingBuffers[glPaddingCount] = new byte[102400];
				glPaddingCount++;
			}
			while (glPaddingCount > local102) {
				glPaddingCount--;
				glPaddingBuffers[glPaddingCount] = null;
			}
		}
		if (local76 == null) {
			return;
		}
		this.minY = local76.getMinY();
		@Pc(184) Model local184;
		if (Preferences.characterShadowsOn && (this.appearance.npcId == -1 || NpcTypeList.get(this.appearance.npcId).hasshadow)) {
			local184 = ShadowModelList.method1043(160, this.seqStretches, local54 == null ? local25 : local54, this.xFine, 0, this.yFine, 0, 1, local76, arg0, local54 == null ? this.seqFrame : this.movementSeqFrame, this.tileHeight, 240);
			if (GlRenderer.enabled) {
				@Pc(188) float local188 = GlRenderer.method4179();
				@Pc(190) float local190 = GlRenderer.method4166();
				GlRenderer.disableDepthMask();
				GlRenderer.method4152(local188, local190 - 150.0F);
				local184.render(0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, -1L, arg9, null);
				GlRenderer.enableDepthMask();
				GlRenderer.method4152(local188, local190);
			} else {
				local184.render(0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, -1L, arg9, null);
			}
		}
		if (PlayerList.self == this) {
			for (local102 = MiniMap.hintMapMarkers.length - 1; local102 >= 0; local102--) {
				@Pc(245) MapMarker local245 = MiniMap.hintMapMarkers[local102];
				if (local245 != null && local245.playerModelId != -1) {
					@Pc(291) int local291;
					@Pc(302) int local302;
					if (local245.type == 1 && local245.actorTargetId >= 0 && NpcList.npcs.length > local245.actorTargetId) {
						@Pc(278) Npc local278 = NpcList.npcs[local245.actorTargetId];
						if (local278 != null) {
							local291 = local278.xFine / 32 - PlayerList.self.xFine / 32;
							local302 = local278.yFine / 32 - PlayerList.self.yFine / 32;
							this.renderHintArrow(null, local302, local76, local291, arg5, arg9, arg0, arg7, arg4, arg3, arg1, local245.playerModelId, arg2, arg6);
						}
					}
					if (local245.type == 2) {
						@Pc(340) int local340 = (local245.targetX - Camera.originX) * 4 + 2 - PlayerList.self.xFine / 32;
						local291 = (local245.anInt4046 - Camera.originZ) * 4 + 2 - PlayerList.self.yFine / 32;
						this.renderHintArrow(null, local291, local76, local340, arg5, arg9, arg0, arg7, arg4, arg3, arg1, local245.playerModelId, arg2, arg6);
					}
					if (local245.type == 10 && local245.actorTargetId >= 0 && PlayerList.players.length > local245.actorTargetId) {
						@Pc(395) Player local395 = PlayerList.players[local245.actorTargetId];
						if (local395 != null) {
							local291 = local395.xFine / 32 - PlayerList.self.xFine / 32;
							local302 = local395.yFine / 32 - PlayerList.self.yFine / 32;
							this.renderHintArrow(null, local302, local76, local291, arg5, arg9, arg0, arg7, arg4, arg3, arg1, local245.playerModelId, arg2, arg6);
						}
					}
				}
			}
		}
		this.applyBodyLean(local76);
		this.alignToTerrain(local76, arg0);
		local184 = null;
		if (!this.lowDetail && this.spotAnimId != -1 && this.spotAnimFrame != -1) {
			@Pc(471) SpotAnimType local471 = SpotAnimTypeList.get(this.spotAnimId);
			local184 = local471.constructModel(this.spotAnimNextFrame, this.spotAnimFrame, this.spotAnimDelayClock);
			if (local184 != null) {
				local184.translate(0, -this.spotAnimY, 0);
				if (local471.aBoolean100) {
					if (PathingEntity.terrainPitchAngle != 0) {
						local184.rotateX(PathingEntity.terrainPitchAngle);
					}
					if (PathingEntity.terrainRollAngle != 0) {
						local184.rotateZ(PathingEntity.terrainRollAngle);
					}
					if (PathingEntity.terrainYOffset != 0) {
						local184.translate(0, PathingEntity.terrainYOffset, 0);
					}
				}
			}
		}
		@Pc(515) Model local515 = null;
		if (!this.lowDetail && this.attachment != null) {
			if (client.loop >= this.attachmentResetAt) {
				this.attachment = null;
			}
			if (this.attachmentSetAt <= client.loop && this.attachmentResetAt > client.loop) {
				if (this.attachment instanceof Loc) {
					local515 = (Model) ((Loc) this.attachment).getEntity();
				} else {
					local515 = (Model) this.attachment;
				}
				local515.translate(this.attachmentXFine - this.xFine, this.attachmentY + -this.tileHeight, this.attachmentYFine - this.yFine);
				if (this.targetAngle == 512) {
					local515.method4578();
				} else if (this.targetAngle == 1024) {
					local515.method4552();
				} else if (this.targetAngle == 1536) {
					local515.rotateCounterClockwise();
				}
			}
		}
		if (GlRenderer.enabled) {
			local76.pickable = true;
			local76.render(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, this.particleSystem);
			if (local184 != null) {
				local184.pickable = true;
				local184.render(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, this.particleSystem);
			}
		} else {
			if (local184 != null) {
				local76 = ((SoftwareModel) local76).method4588(local184);
			}
			if (local515 != null) {
				local76 = ((SoftwareModel) local76).method4588(local515);
			}
			local76.pickable = true;
			local76.render(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, this.particleSystem);
		}
		if (local515 == null) {
			return;
		}
		if (this.targetAngle == 512) {
			local515.rotateCounterClockwise();
		} else if (this.targetAngle == 1024) {
			local515.method4552();
		} else if (this.targetAngle == 1536) {
			local515.method4578();
		}
		local515.translate(this.xFine - this.attachmentXFine, -this.attachmentY + this.tileHeight, this.yFine - this.attachmentYFine);
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(Lclient!ga;ILclient!ak;IIIIIIIIIIII)V")
	private void renderHintArrow(@OriginalArg(0) ParticleSystem arg0, @OriginalArg(1) int arg1, @OriginalArg(2) Model arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(7) int arg6, @OriginalArg(8) int arg7, @OriginalArg(9) int arg8, @OriginalArg(10) int arg9, @OriginalArg(11) int arg10, @OriginalArg(12) int arg11, @OriginalArg(13) int arg12, @OriginalArg(14) int arg13) {
		@Pc(12) int local12 = arg3 * arg3 + arg1 * arg1;
		if (local12 < 16 || local12 > 360000) {
			return;
		}
		@Pc(34) int local34 = (int) (Math.atan2(arg3, arg1) * 325.949D) & 0x7FF;
		@Pc(46) Model local46 = HintArrowManager.getModel(local34, this.yFine, arg11, this.xFine, arg2, this.tileHeight);
		if (local46 == null) {
			return;
		}
		if (!GlRenderer.enabled) {
			local46.render(0, arg10, arg12, arg9, arg8, arg4, arg13, arg7, -1L, arg5, arg0);
			return;
		}
		@Pc(52) float local52 = GlRenderer.method4179();
		@Pc(54) float local54 = GlRenderer.method4166();
		GlRenderer.disableDepthMask();
		GlRenderer.method4152(local52, local54 - 150.0F);
		local46.render(0, arg10, arg12, arg9, arg8, arg4, arg13, arg7, -1L, arg5, arg0);
		GlRenderer.enableDepthMask();
		GlRenderer.method4152(local52, local54);
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(B)Z")
	@Override
	public final boolean isVisible() {
		return this.appearance != null;
	}

	@OriginalMember(owner = "client!e", name = "e", descriptor = "(I)Lclient!na;")
	public final JagString getName() {
		@Pc(2) JagString local2 = this.username;
		if (PlayerList.playerNames != null) {
			local2 = JagString.concatenate(new JagString[]{PlayerList.playerNames[this.titleIndex], local2});
		}
		if (PlayerList.playerNames2 != null) {
			local2 = JagString.concatenate(new JagString[]{local2, PlayerList.playerNames2[this.titleIndex]});
		}
		return local2;
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(IIIII)V")
	@Override
	public final void updateModel(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
	}

	@OriginalMember(owner = "client!e", name = "a", descriptor = "(BIZI)V")
	public final void teleport(@OriginalArg(1) int arg0, @OriginalArg(2) boolean arg1, @OriginalArg(3) int arg2) {
		super.teleport(this.getSize(), arg0, arg2, arg1);
	}

	@OriginalMember(owner = "client!e", name = "finalize", descriptor = "()V")
	@Override
	public final void finalize() {
	}

	@OriginalMember(owner = "client!e", name = "b", descriptor = "()I")
	@Override
	public final int getMinY() {
		return this.minY;
	}
}
