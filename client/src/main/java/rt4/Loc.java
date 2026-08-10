package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!dc")
public final class Loc extends Entity {

	@OriginalMember(owner = "client!kf", name = "h", descriptor = "[I")
	public static final int[] LAYERS = new int[]{0, 0, 0, 0, 1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 3};

	@OriginalMember(owner = "client!ci", name = "q", descriptor = "Lclient!ek;")
	public static SoftwareIndexedSprite sprite1 = null;

	@OriginalMember(owner = "client!dc", name = "U", descriptor = "Lclient!ga;")
	private ParticleSystem particles;

	@OriginalMember(owner = "client!dc", name = "x", descriptor = "I")
	private int shadowX = 0;

	@OriginalMember(owner = "client!dc", name = "t", descriptor = "Z")
	private boolean hasMultiLoc = false;

	@OriginalMember(owner = "client!dc", name = "s", descriptor = "I")
	private int shadowElevation = 0;

	@OriginalMember(owner = "client!dc", name = "B", descriptor = "Z")
	private boolean shadowDirty = true;

	@OriginalMember(owner = "client!dc", name = "Q", descriptor = "Lclient!ek;")
	private SoftwareIndexedSprite sprite2 = null;

	@OriginalMember(owner = "client!dc", name = "T", descriptor = "I")
	private final int MIN_Y = -32768;

	@OriginalMember(owner = "client!dc", name = "eb", descriptor = "I")
	private int shadowZ = 0;

	@OriginalMember(owner = "client!dc", name = "gb", descriptor = "I")
	private int lastLocTypeId = -1;

	@OriginalMember(owner = "client!dc", name = "ib", descriptor = "I")
	private int lastSeqFrame = -1;

	@OriginalMember(owner = "client!dc", name = "H", descriptor = "I")
	private final int level;

	@OriginalMember(owner = "client!dc", name = "w", descriptor = "I")
	private final int orientation;

	@OriginalMember(owner = "client!dc", name = "N", descriptor = "I")
	private final int tileX;

	@OriginalMember(owner = "client!dc", name = "L", descriptor = "I")
	private final int shape;

	@OriginalMember(owner = "client!dc", name = "C", descriptor = "I")
	private final int locId;

	@OriginalMember(owner = "client!dc", name = "D", descriptor = "I")
	private final int tileZ;

	@OriginalMember(owner = "client!dc", name = "u", descriptor = "Lclient!tk;")
	private SeqType seq;

	@OriginalMember(owner = "client!dc", name = "y", descriptor = "I")
	private int seqFrame;

	@OriginalMember(owner = "client!dc", name = "I", descriptor = "I")
	private int seqNextFrame;

	@OriginalMember(owner = "client!dc", name = "bb", descriptor = "I")
	private int seqFrameProgress;

	@OriginalMember(owner = "client!dc", name = "fb", descriptor = "I")
	private int seqStartLoop;

	@OriginalMember(owner = "client!dc", name = "<init>", descriptor = "(IIIIIIIZLclient!th;)V")
	public Loc(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int seqId, @OriginalArg(7) boolean arg7, @OriginalArg(8) Entity arg8) {
		this.level = arg3;
		this.orientation = arg2;
		this.tileX = arg4;
		this.shape = arg1;
		this.locId = arg0;
		this.tileZ = arg5;
		@Pc(67) LocType local67;
		if (GlRenderer.enabled && arg8 != null) {
			if (arg8 instanceof Loc) {
				((Loc) arg8).resetShadow();
			} else {
				local67 = LocTypeList.get(this.locId);
				if (local67.multiLocs != null) {
					local67 = local67.getMultiLoc();
				}
				if (local67 != null) {
					buildStaticShadow(local67, 0, this.orientation, 0, this.shape, this.tileX, this.tileZ, this.level);
				}
			}
		}
		if (seqId != -1) {
			this.seq = SeqTypeList.get(seqId);
			this.seqFrame = 0;
			if (this.seq.frames.length <= 1) {
				this.seqNextFrame = 0;
			} else {
				this.seqNextFrame = 1;
			}
			this.seqFrameProgress = 1;
			this.seqStartLoop = client.loop - 1;
			if (this.seq.exactmove == 0 && arg8 != null && arg8 instanceof Loc) {
				@Pc(142) Loc local142 = (Loc) arg8;
				if (this.seq == local142.seq) {
					this.seqFrame = local142.seqFrame;
					this.seqStartLoop = local142.seqStartLoop;
					this.seqFrameProgress = local142.seqFrameProgress;
					this.seqNextFrame = local142.seqNextFrame;
					return;
				}
			}
			if (arg7 && this.seq.replayoff != -1) {
				this.seqFrame = (int) (Math.random() * (double) this.seq.frames.length);
				this.seqNextFrame = this.seqFrame + 1;
				if (this.seqNextFrame >= this.seq.frames.length) {
					this.seqNextFrame -= this.seq.replayoff;
					if (this.seqNextFrame < 0 || this.seqNextFrame >= this.seq.frames.length) {
						this.seqNextFrame = -1;
					}
				}
				this.seqFrameProgress = (int) (Math.random() * (double) this.seq.frameDelay[this.seqFrame]) + 1;
				this.seqStartLoop = client.loop - this.seqFrameProgress;
			}
		}
		if (GlRenderer.enabled && arg8 != null) {
			this.getOrBuildEntity(true);
		}
		if (arg8 == null) {
			local67 = LocTypeList.get(this.locId);
			if (local67.multiLocs != null) {
				this.hasMultiLoc = true;
			}
		}
	}

	@OriginalMember(owner = "client!al", name = "a", descriptor = "(Lclient!pb;BIIIIIII)V")
	public static void buildStaticShadow(@OriginalArg(0) LocType arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6, @OriginalArg(8) int arg7) {
		@Pc(5) int local5 = arg2 & 0x3;
		@Pc(28) int local28;
		@Pc(31) int local31;
		if (local5 == 1 || local5 == 3) {
			local28 = arg0.length;
			local31 = arg0.width;
		} else {
			local31 = arg0.length;
			local28 = arg0.width;
		}
		@Pc(53) int local53;
		@Pc(51) int local51;
		if (arg6 + local31 > 104) {
			local51 = arg6 + 1;
			local53 = arg6;
		} else {
			local53 = arg6 + (local31 >> 1);
			local51 = arg6 + (local31 + 1 >> 1);
		}
		@Pc(80) int local80 = (arg5 << 7) + (local28 << 6);
		@Pc(88) int local88 = (arg6 << 7) + (local31 << 6);
		@Pc(96) int local96;
		@Pc(100) int local100;
		if (arg5 + local28 > 104) {
			local96 = arg5;
			local100 = arg5 + 1;
		} else {
			local96 = arg5 + (local28 >> 1);
			local100 = (local28 + 1 >> 1) + arg5;
		}
		@Pc(120) int[][] local120 = SceneGraph.tileHeights[arg7];
		@Pc(122) int local122 = 0;
		@Pc(148) int local148 = local120[local96][local51] + local120[local96][local53] + local120[local100][local53] + local120[local100][local51] >> 2;
		@Pc(158) int[][] local158;
		if (arg7 != 0) {
			local158 = SceneGraph.tileHeights[0];
			local122 = local148 - (local158[local96][local51] + local158[local100][local53] + local158[local96][local53] + local158[local100][local51] >> 2);
		}
		local158 = null;
		if (arg7 < 3) {
			local158 = SceneGraph.tileHeights[arg7 + 1];
		}
		@Pc(215) LocEntity local215 = arg0.getStaticEntity(arg2, local80, local120, arg4, local148, local158, false, null, true, local88);
		ShadowManager.method4207(local215.sprite, local80 - arg3, local122, local88 - arg1);
	}

	@OriginalMember(owner = "client!dc", name = "a", descriptor = "(IIIII)V")
	@Override
	public final void updateModel(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
		if (GlRenderer.enabled) {
			this.getOrBuildEntity(true);
		} else {
			this.advanceAnimation(arg4, arg3);
		}
	}

	@OriginalMember(owner = "client!dc", name = "a", descriptor = "(IIIIIIIIJILclient!ga;)V")
	@Override
	public final void render(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6, @OriginalArg(7) int arg7, @OriginalArg(8) long arg8, @OriginalArg(9) int arg9, @OriginalArg(10) ParticleSystem arg10) {
		@Pc(3) Entity local3 = this.getEntity();
		if (local3 != null) {
			local3.render(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, this.particles);
		}
	}

	@OriginalMember(owner = "client!dc", name = "c", descriptor = "(I)V")
	public final void resetShadow() {
		if (this.sprite2 != null) {
			ShadowManager.method4207(this.sprite2, this.shadowX, this.shadowElevation, this.shadowZ);
		}
		this.lastLocTypeId = -1;
		this.lastSeqFrame = -1;
		this.sprite2 = null;
	}

	@OriginalMember(owner = "client!dc", name = "finalize", descriptor = "()V")
	@Override
	public final void finalize() {
	}

	@OriginalMember(owner = "client!dc", name = "b", descriptor = "(III)V")
	private void advanceAnimation(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1) {
		if (this.seq == null) {
			return;
		}
		@Pc(10) int local10 = client.loop - this.seqStartLoop;
		if (local10 > 100 && this.seq.replayoff > 0) {
			@Pc(29) int local29 = this.seq.frames.length - this.seq.replayoff;
			while (this.seqFrame < local29 && this.seq.frameDelay[this.seqFrame] < local10) {
				local10 -= this.seq.frameDelay[this.seqFrame];
				this.seqFrame++;
			}
			if (this.seqFrame >= local29) {
				@Pc(77) int local77 = 0;
				for (@Pc(79) int local79 = local29; local79 < this.seq.frames.length; local79++) {
					local77 += this.seq.frameDelay[local79];
				}
				local10 %= local77;
			}
			this.seqNextFrame = this.seqFrame + 1;
			if (this.seqNextFrame >= this.seq.frames.length) {
				this.seqNextFrame -= this.seq.replayoff;
				if (this.seqNextFrame < 0 || this.seq.frames.length <= this.seqNextFrame) {
					this.seqNextFrame = -1;
				}
			}
		}
		while (local10 > this.seq.frameDelay[this.seqFrame]) {
			SoundPlayer.playSeqSound(arg0, this.seq, arg1, false, this.seqFrame);
			local10 -= this.seq.frameDelay[this.seqFrame];
			this.seqFrame++;
			if (this.seq.frames.length <= this.seqFrame) {
				this.seqFrame -= this.seq.replayoff;
				if (this.seqFrame < 0 || this.seq.frames.length <= this.seqFrame) {
					this.seq = null;
					break;
				}
			}
			this.seqNextFrame = this.seqFrame + 1;
			if (this.seq.frames.length <= this.seqNextFrame) {
				this.seqNextFrame -= this.seq.replayoff;
				if (this.seqNextFrame < 0 || this.seqNextFrame >= this.seq.frames.length) {
					this.seqNextFrame = -1;
				}
			}
		}
		this.seqFrameProgress = local10;
		this.seqStartLoop = client.loop - local10;
	}

	@OriginalMember(owner = "client!dc", name = "a", descriptor = "(ZI)Lclient!th;")
	private Entity getOrBuildEntity(@OriginalArg(0) boolean arg0) {
		@Pc(12) boolean local12 = SceneGraph.surfaceTileHeights != SceneGraph.tileHeights;
		@Pc(19) LocType local19 = LocTypeList.get(this.locId);
		@Pc(22) int local22 = local19.anim;
		if (local19.multiLocs != null) {
			local19 = local19.getMultiLoc();
		}
		if (local19 == null) {
			if (GlRenderer.enabled && !local12) {
				this.resetShadow();
			}
			return null;
		}
		@Pc(69) int local69;
		if (client.game != 0 && this.hasMultiLoc && (this.seq == null || this.seq != null && this.seq.id != local19.anim)) {
			local69 = local19.anim;
			if (local19.anim == -1) {
				local69 = local22;
			}
			if (local69 == -1) {
				this.seq = null;
			} else {
				this.seq = SeqTypeList.get(local69);
			}
			if (this.seq != null) {
				if (local19.allowrandomizedanimation && this.seq.replayoff != -1) {
					this.seqFrame = (int) (Math.random() * (double) this.seq.frames.length);
					this.seqStartLoop -= (int) (Math.random() * (double) this.seq.frameDelay[this.seqFrame]);
				} else {
					this.seqFrame = 0;
					this.seqStartLoop = client.loop - 1;
				}
			}
		}
		local69 = this.orientation & 0x3;
		@Pc(160) int local160;
		@Pc(157) int local157;
		if (local69 == 1 || local69 == 3) {
			local157 = local19.width;
			local160 = local19.length;
		} else {
			local160 = local19.width;
			local157 = local19.length;
		}
		@Pc(178) int local178 = this.tileX + (local160 + 1 >> 1);
		@Pc(185) int local185 = (local160 >> 1) + this.tileX;
		@Pc(192) int local192 = (local157 >> 1) + this.tileZ;
		@Pc(201) int local201 = (local157 + 1 >> 1) + this.tileZ;
		this.advanceAnimation(local192 * 128, local185 * 128);
		@Pc(256) boolean local256 = !local12 && local19.castshadow && (local19.id != this.lastLocTypeId || (this.seqFrame != this.lastSeqFrame || this.seq != null && (this.seq.aBoolean280 || SeqType.applyTweening) && this.seqFrame != this.seqNextFrame) && Preferences.sceneryShadowsType >= 2);
		if (arg0 && !local256) {
			return null;
		}
		@Pc(267) int[][] local267 = SceneGraph.tileHeights[this.level];
		@Pc(293) int local293 = local267[local178][local201] + local267[local185][local201] + local267[local185][local192] + local267[local178][local192] >> 2;
		@Pc(302) int local302 = (local160 << 6) + (this.tileX << 7);
		@Pc(311) int local311 = (local157 << 6) + (this.tileZ << 7);
		@Pc(314) int[][] local314 = null;
		if (local12) {
			local314 = SceneGraph.surfaceTileHeights[0];
		} else if (this.level < 3) {
			local314 = SceneGraph.tileHeights[this.level + 1];
		}
		if (GlRenderer.enabled && local256) {
			ShadowManager.method4207(this.sprite2, this.shadowX, this.shadowElevation, this.shadowZ);
		}
		@Pc(356) boolean local356 = this.sprite2 == null;
		@Pc(389) LocEntity local389;
		if (this.seq == null) {
			local389 = local19.getStaticEntity(this.orientation, local302, local267, this.shape, local293, local314, false, local356 ? sprite1 : this.sprite2, local256, local311);
		} else {
			local389 = local19.getAnimatedEntity(local311, local302, local356 ? sprite1 : this.sprite2, local293, this.seq, this.orientation, local267, local256, this.seqFrame, local314, this.seqNextFrame, this.shape, this.seqFrameProgress);
		}
		if (local389 == null) {
			return null;
		}
		if (GlRenderer.enabled && local256) {
			if (local356) {
				sprite1 = local389.sprite;
			}
			@Pc(429) int local429 = 0;
			if (this.level != 0) {
				@Pc(439) int[][] local439 = SceneGraph.tileHeights[0];
				local429 = local293 - (local439[local178][local192] + local439[local185][local192] + local439[local185][local201] + local439[local178][local201] >> 2);
			}
			@Pc(471) SoftwareIndexedSprite local471 = local389.sprite;
			if (this.shadowDirty && ShadowManager.method4209(local471, local302, local429, local311)) {
				this.shadowDirty = false;
			}
			if (!this.shadowDirty) {
				ShadowManager.method4211(local471, local302, local429, local311);
				this.sprite2 = local471;
				this.shadowZ = local311;
				if (local356) {
					sprite1 = null;
				}
				this.shadowElevation = local429;
				this.shadowX = local302;
			}
			this.lastLocTypeId = local19.id;
			this.lastSeqFrame = this.seqFrame;
		}
		return local389.model;
	}

	@OriginalMember(owner = "client!dc", name = "d", descriptor = "(I)Lclient!th;")
	public final Entity getEntity() {
		return this.getOrBuildEntity(false);
	}

	@OriginalMember(owner = "client!dc", name = "b", descriptor = "()I")
	@Override
	public final int getMinY() {
		return this.MIN_Y;
	}
}
