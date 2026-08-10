package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!fe")
public abstract class PathingEntity extends Entity {

	@OriginalMember(owner = "client!ef", name = "a", descriptor = "[I")
	public static final int[] ANGLES = new int[]{768, 1024, 1280, 512, 1536, 256, 0, 1792};

	@OriginalMember(owner = "client!hn", name = "Y", descriptor = "I")
	public static int terrainRollAngle = 0;

	@OriginalMember(owner = "client!hm", name = "ab", descriptor = "I")
	public static int terrainPitchAngle = 0;

	@OriginalMember(owner = "client!f", name = "V", descriptor = "I")
	public static int terrainYOffset = 0;

	@OriginalMember(owner = "client!fe", name = "A", descriptor = "I")
	public int spotAnimStart;

	@OriginalMember(owner = "client!fe", name = "S", descriptor = "I")
	public int hitpointsBar;

	@OriginalMember(owner = "client!fe", name = "U", descriptor = "I")
	public int attachmentY0;

	@OriginalMember(owner = "client!fe", name = "Z", descriptor = "I")
	public int attachmentXFine;

	@OriginalMember(owner = "client!fe", name = "bb", descriptor = "I")
	public int forceMoveStartX;

	@OriginalMember(owner = "client!fe", name = "cb", descriptor = "I")
	public int currentAngle;

	@OriginalMember(owner = "client!fe", name = "fb", descriptor = "I")
	public int atachmentX0;

	@OriginalMember(owner = "client!fe", name = "hb", descriptor = "I")
	public int forceMoveCyclesToDest;

	@OriginalMember(owner = "client!fe", name = "nb", descriptor = "Ljava/lang/Object;")
	public Object attachment;

	@OriginalMember(owner = "client!fe", name = "rb", descriptor = "I")
	public int forceMoveDestY;

	@OriginalMember(owner = "client!fe", name = "tb", descriptor = "I")
	public int spotAnimY;

	@OriginalMember(owner = "client!fe", name = "ub", descriptor = "I")
	public int forceMoveCyclesToStart;

	@OriginalMember(owner = "client!fe", name = "vb", descriptor = "Lclient!ga;")
	protected ParticleSystem particleSystem;

	@OriginalMember(owner = "client!fe", name = "Ab", descriptor = "I")
	public int targetAngle;

	@OriginalMember(owner = "client!fe", name = "Jb", descriptor = "I")
	public int attachmentY;

	@OriginalMember(owner = "client!fe", name = "Ob", descriptor = "I")
	public int attachmentY1;

	@OriginalMember(owner = "client!fe", name = "Pb", descriptor = "I")
	public int attachmentX1;

	@OriginalMember(owner = "client!fe", name = "Qb", descriptor = "I")
	public int xFine;

	@OriginalMember(owner = "client!fe", name = "Ub", descriptor = "I")
	public int forceMoveDestX;

	@OriginalMember(owner = "client!fe", name = "ac", descriptor = "I")
	public int yFine;

	@OriginalMember(owner = "client!fe", name = "dc", descriptor = "I")
	public int tileHeight;

	@OriginalMember(owner = "client!fe", name = "fc", descriptor = "I")
	public int attachmentYFine;

	@OriginalMember(owner = "client!fe", name = "hc", descriptor = "I")
	public int forceMoveStartY;

	@OriginalMember(owner = "client!fe", name = "mc", descriptor = "I")
	public int forceMoveDirection;

	@OriginalMember(owner = "client!fe", name = "w", descriptor = "[I")
	public final int[] movementQueueY = new int[10];

	@OriginalMember(owner = "client!fe", name = "z", descriptor = "I")
	public int movementSpeed = 0;

	@OriginalMember(owner = "client!fe", name = "J", descriptor = "I")
	private int pitchTarget = 0;

	@OriginalMember(owner = "client!fe", name = "L", descriptor = "[I")
	public final int[] hitVisibleUntil = new int[4];

	@OriginalMember(owner = "client!fe", name = "K", descriptor = "[I")
	public final int[] movementQueueX = new int[10];

	@OriginalMember(owner = "client!fe", name = "B", descriptor = "I")
	public int seqDelayClock = 0;

	@OriginalMember(owner = "client!fe", name = "O", descriptor = "I")
	public int seqId = -1;

	@OriginalMember(owner = "client!fe", name = "ab", descriptor = "Z")
	private boolean pitchAccelerating = false;

	@OriginalMember(owner = "client!fe", name = "M", descriptor = "Z")
	public boolean yawAccelerating = false;

	@OriginalMember(owner = "client!fe", name = "db", descriptor = "I")
	public int faceX = 0;

	@OriginalMember(owner = "client!fe", name = "Q", descriptor = "I")
	public int seqReplayCount = 0;

	@OriginalMember(owner = "client!fe", name = "E", descriptor = "I")
	public int faceY = 0;

	@OriginalMember(owner = "client!fe", name = "yb", descriptor = "I")
	private int rollMidpoint = 0;

	@OriginalMember(owner = "client!fe", name = "G", descriptor = "I")
	public int basTypeId = -1;

	@OriginalMember(owner = "client!fe", name = "gb", descriptor = "I")
	public int standingTurnCounter = 0;

	@OriginalMember(owner = "client!fe", name = "pb", descriptor = "I")
	public int attachmentSetAt = 0;

	@OriginalMember(owner = "client!fe", name = "mb", descriptor = "[B")
	public final byte[] movementQueueSpeed = new byte[10];

	@OriginalMember(owner = "client!fe", name = "W", descriptor = "I")
	public int turnSpeed = 32;

	@OriginalMember(owner = "client!fe", name = "P", descriptor = "I")
	public int faceEntity = -1;

	@OriginalMember(owner = "client!fe", name = "Bb", descriptor = "Z")
	private boolean rollAccelerating = false;

	@OriginalMember(owner = "client!fe", name = "eb", descriptor = "I")
	private int rollAngle = 0;

	@OriginalMember(owner = "client!fe", name = "ib", descriptor = "I")
	public int yawDistanceTraveled = 0;

	@OriginalMember(owner = "client!fe", name = "X", descriptor = "I")
	public int preciseYaw = 0;

	@OriginalMember(owner = "client!fe", name = "Eb", descriptor = "[Lclient!ub;")
	public final SlotAnimation[] slotAnimations = new SlotAnimation[12];

	@OriginalMember(owner = "client!fe", name = "Lb", descriptor = "I")
	public int chatLoops = 100;

	@OriginalMember(owner = "client!fe", name = "V", descriptor = "I")
	public int attachmentResetAt = 0;

	@OriginalMember(owner = "client!fe", name = "Y", descriptor = "I")
	public int hitpointsBarVisibleUntil = -1000;

	@OriginalMember(owner = "client!fe", name = "Rb", descriptor = "I")
	protected int minY = -32768;

	@OriginalMember(owner = "client!fe", name = "Nb", descriptor = "I")
	public int movementQueueSize = 0;

	@OriginalMember(owner = "client!fe", name = "Zb", descriptor = "I")
	public int seqDelay = 0;

	@OriginalMember(owner = "client!fe", name = "cc", descriptor = "I")
	private int pitchVelocity = 0;

	@OriginalMember(owner = "client!fe", name = "C", descriptor = "I")
	public int spotAnimDelayClock = 0;

	@OriginalMember(owner = "client!fe", name = "Yb", descriptor = "I")
	private int rollTarget = 0;

	@OriginalMember(owner = "client!fe", name = "v", descriptor = "I")
	private int rollVelocity = 0;

	@OriginalMember(owner = "client!fe", name = "Mb", descriptor = "[I")
	public final int[] hitTypes = new int[4];

	@OriginalMember(owner = "client!fe", name = "zb", descriptor = "I")
	public int spotAnimFrame = 0;

	@OriginalMember(owner = "client!fe", name = "Db", descriptor = "I")
	public int preciseTargetYaw = 0;

	@OriginalMember(owner = "client!fe", name = "Vb", descriptor = "I")
	public int movementCatchupTicks = 0;

	@OriginalMember(owner = "client!fe", name = "s", descriptor = "I")
	public int chatEffect = 0;

	@OriginalMember(owner = "client!fe", name = "T", descriptor = "I")
	public int seqNextFrame = -1;

	@OriginalMember(owner = "client!fe", name = "Fb", descriptor = "Z")
	public boolean seqStretches = false;

	@OriginalMember(owner = "client!fe", name = "xb", descriptor = "I")
	public int yawDecelThreshold = 0;

	@OriginalMember(owner = "client!fe", name = "kb", descriptor = "I")
	public int movementSeqNextFrame = -1;

	@OriginalMember(owner = "client!fe", name = "Sb", descriptor = "I")
	public int yawVelocity = 0;

	@OriginalMember(owner = "client!fe", name = "H", descriptor = "I")
	public int movementSeqId = -1;

	@OriginalMember(owner = "client!fe", name = "gc", descriptor = "I")
	private int pitchAngle = 0;

	@OriginalMember(owner = "client!fe", name = "Tb", descriptor = "I")
	private int size = 1;

	@OriginalMember(owner = "client!fe", name = "Kb", descriptor = "I")
	public int movementSeqFrame = 0;

	@OriginalMember(owner = "client!fe", name = "ic", descriptor = "[I")
	public final int[] hitDamages = new int[4];

	@OriginalMember(owner = "client!fe", name = "Ib", descriptor = "I")
	public int seqMovementSteps = 0;

	@OriginalMember(owner = "client!fe", name = "y", descriptor = "I")
	private int pitchMidpoint = 0;

	@OriginalMember(owner = "client!fe", name = "wb", descriptor = "I")
	public int movementSeqDelayClock = 0;

	@OriginalMember(owner = "client!fe", name = "jc", descriptor = "I")
	public int chatColor = 0;

	@OriginalMember(owner = "client!fe", name = "kc", descriptor = "I")
	public int lastSeenLoop = 0;

	@OriginalMember(owner = "client!fe", name = "ec", descriptor = "I")
	public int seqFrame = 0;

	@OriginalMember(owner = "client!fe", name = "Wb", descriptor = "Lclient!na;")
	public JagString chatMessage = null;

	@OriginalMember(owner = "client!fe", name = "oc", descriptor = "I")
	public int spotAnimId = -1;

	@OriginalMember(owner = "client!fe", name = "Xb", descriptor = "I")
	public int spotAnimNextFrame = -1;

	@OriginalMember(owner = "client!fe", name = "b", descriptor = "(Z)Lclient!ck;")
	public final BasType getBasType() {
		@Pc(7) int basId = this.getBasId();
		return basId == -1 ? BasType.DEFAULT : BasTypeList.get(basId);
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(B)Z")
	public boolean isVisible() {
		return false;
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(IIIIZ)V")
	public final void teleport(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) boolean arg3) {
		if (this.seqId != -1 && SeqTypeList.get(this.seqId).movetype == 1) {
			this.seqId = -1;
		}
		if (!arg3) {
			@Pc(32) int local32 = arg1 - this.movementQueueX[0];
			@Pc(40) int local40 = arg2 - this.movementQueueY[0];
			if (local32 >= -8 && local32 <= 8 && local40 >= -8 && local40 <= 8) {
				if (this.movementQueueSize < 9) {
					this.movementQueueSize++;
				}
				for (@Pc(72) int local72 = this.movementQueueSize; local72 > 0; local72--) {
					this.movementQueueX[local72] = this.movementQueueX[local72 - 1];
					this.movementQueueY[local72] = this.movementQueueY[local72 - 1];
					this.movementQueueSpeed[local72] = this.movementQueueSpeed[local72 - 1];
				}
				this.movementQueueSpeed[0] = 1;
				this.movementQueueX[0] = arg1;
				this.movementQueueY[0] = arg2;
				return;
			}
		}
		this.movementCatchupTicks = 0;
		this.movementQueueX[0] = arg1;
		this.movementQueueY[0] = arg2;
		this.movementQueueSize = 0;
		this.seqMovementSteps = 0;
		this.yFine = arg0 * 64 + this.movementQueueY[0] * 128;
		this.xFine = arg0 * 64 + this.movementQueueX[0] * 128;
		if (GlRenderer.enabled && PlayerList.self == this) {
			FogManager.setInstantFade();
		}
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(IBI)V")
	public final void move(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1) {
		@Pc(10) int local10 = this.movementQueueX[0];
		@Pc(15) int local15 = this.movementQueueY[0];
		if (arg1 == 0) {
			local10--;
			local15++;
		}
		if (this.seqId != -1 && SeqTypeList.get(this.seqId).movetype == 1) {
			this.seqId = -1;
		}
		if (this.movementQueueSize < 9) {
			this.movementQueueSize++;
		}
		for (@Pc(50) int local50 = this.movementQueueSize; local50 > 0; local50--) {
			this.movementQueueX[local50] = this.movementQueueX[local50 - 1];
			this.movementQueueY[local50] = this.movementQueueY[local50 - 1];
			this.movementQueueSpeed[local50] = this.movementQueueSpeed[local50 - 1];
		}
		if (arg1 == 1) {
			local15++;
		}
		this.movementQueueSpeed[0] = (byte) arg0;
		if (arg1 == 2) {
			local15++;
			local10++;
		}
		if (arg1 == 3) {
			local10--;
		}
		if (arg1 == 4) {
			local10++;
		}
		if (arg1 == 5) {
			local15--;
			local10--;
		}
		if (arg1 == 6) {
			local15--;
		}
		if (arg1 == 7) {
			local15--;
			local10++;
		}
		this.movementQueueX[0] = local10;
		this.movementQueueY[0] = local15;
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(BLclient!ak;I)V")
	protected final void alignToTerrain(@OriginalArg(1) Model arg0, @OriginalArg(2) int arg1) {
		terrainRollAngle = 0;
		terrainPitchAngle = 0;
		terrainYOffset = 0;
		@Pc(21) BasType type = this.getBasType();
		@Pc(24) int local24 = type.anInt1059;
		@Pc(27) int local27 = type.anInt1050;
		if (local24 == 0 || local27 == 0) {
			return;
		}
		@Pc(39) int local39 = MathUtils.sin[arg1];
		@Pc(43) int local43 = MathUtils.cos[arg1];
		@Pc(48) int local48 = -local24 / 2;
		@Pc(53) int local53 = -local27 / 2;
		@Pc(64) int local64 = local53 * local43 - local48 * local39 >> 16;
		@Pc(75) int local75 = local39 * local53 + local43 * local48 >> 16;
		@Pc(87) int local87 = SceneGraph.getTileHeight(Player.plane, local75 + this.xFine, this.yFine + local64);
		@Pc(91) int local91 = local24 / 2;
		@Pc(96) int local96 = -local27 / 2;
		@Pc(106) int local106 = local91 * local43 + local96 * local39 >> 16;
		@Pc(110) int local110 = local27 / 2;
		@Pc(121) int local121 = local96 * local43 - local91 * local39 >> 16;
		@Pc(134) int local134 = SceneGraph.getTileHeight(Player.plane, local106 + this.xFine, this.yFine - -local121);
		@Pc(139) int local139 = -local24 / 2;
		@Pc(150) int local150 = local110 * local43 - local39 * local139 >> 16;
		@Pc(154) int local154 = local27 / 2;
		@Pc(158) int local158 = local24 / 2;
		@Pc(169) int local169 = local39 * local110 + local43 * local139 >> 16;
		@Pc(179) int local179 = local154 * local43 - local39 * local158 >> 16;
		@Pc(189) int local189 = local39 * local154 + local43 * local158 >> 16;
		@Pc(201) int local201 = SceneGraph.getTileHeight(Player.plane, this.xFine + local169, local150 + this.yFine);
		@Pc(212) int local212 = local134 > local87 ? local87 : local134;
		@Pc(224) int local224 = SceneGraph.getTileHeight(Player.plane, local189 + this.xFine, local179 + this.yFine);
		@Pc(231) int local231 = local224 > local201 ? local201 : local224;
		@Pc(238) int local238 = local224 > local134 ? local134 : local224;
		@Pc(245) int local245 = local201 <= local87 ? local201 : local87;
		terrainPitchAngle = (int) (Math.atan2(local212 - local231, local27) * 325.95D) & 0x7FF;
		if (terrainPitchAngle != 0) {
			arg0.rotateX(terrainPitchAngle);
		}
		terrainRollAngle = (int) (Math.atan2(local245 - local238, local24) * 325.95D) & 0x7FF;
		if (terrainRollAngle != 0) {
			arg0.rotateZ(terrainRollAngle);
		}
		terrainYOffset = local224 + local87;
		if (local201 + local134 < terrainYOffset) {
			terrainYOffset = local201 + local134;
		}
		terrainYOffset = (terrainYOffset >> 1) - this.tileHeight;
		if (terrainYOffset != 0) {
			arg0.translate(0, terrainYOffset, 0);
		}
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(IIII)V")
	public final void addHit(@OriginalArg(0) int type, @OriginalArg(2) int loop, @OriginalArg(3) int damage) {
		for (@Pc(11) int i = 0; i < 4; i++) {
			if (loop >= this.hitVisibleUntil[i]) {
				this.hitDamages[i] = damage;
				this.hitTypes[i] = type;
				this.hitVisibleUntil[i] = loop + 70;
				return;
			}
		}
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(Lclient!ak;B)V")
	protected final void applyBodyLean(@OriginalArg(0) Model arg0) {
		@Pc(16) BasType local16 = this.getBasType();
		if (local16.rollTargetAngle == 0 && local16.pitchTargetAngle == 0) {
			return;
		}
		@Pc(26) int local26 = 0;
		@Pc(28) int local28 = 0;
		if (this.yawAccelerating && this.yawVelocity != 0) {
			local28 = local16.pitchTargetAngle;
			if (this.yawVelocity >= 0) {
				local26 = local16.rollTargetAngle;
			} else {
				local26 = -local16.rollTargetAngle;
			}
		}
		@Pc(101) int local101;
		@Pc(106) int local106;
		@Pc(134) int local134;
		@Pc(138) int local138;
		if (this.rollTarget != local26) {
			this.rollTarget = local26;
			if (this.rollVelocity > 0 && this.rollAngle < local26) {
				local101 = this.rollVelocity * this.rollVelocity / (local16.rollAcceleration * 2);
				local106 = local26 - this.rollAngle;
				if (local101 <= local106) {
					this.rollAccelerating = true;
					this.rollMidpoint = (local26 + this.rollAngle - local101) / 2;
					local134 = local16.rollMaxSpeed * local16.rollMaxSpeed / (local16.rollAcceleration * 2);
					local138 = local26 - local134;
					if (this.rollMidpoint < local138) {
						this.rollMidpoint = local138;
					}
				} else {
					this.rollAccelerating = false;
				}
			} else if (this.rollVelocity < 0 && this.rollAngle > local26) {
				local101 = this.rollVelocity * this.rollVelocity / (local16.rollAcceleration * 2);
				local106 = local26 - this.rollAngle;
				if (local106 >= local101) {
					this.rollAccelerating = true;
					this.rollMidpoint = (this.rollAngle + local101 + local26) / 2;
					local134 = local16.rollMaxSpeed * local16.rollMaxSpeed / (local16.rollAcceleration * 2);
					local138 = local134 + local26;
					if (this.rollMidpoint > local138) {
						this.rollMidpoint = local138;
					}
				} else {
					this.rollAccelerating = false;
				}
			} else {
				this.rollAccelerating = false;
			}
		}
		if (this.rollVelocity == 0) {
			local101 = this.rollTarget - this.rollAngle;
			if (-local16.rollAcceleration < local101 && local16.rollAcceleration > local101) {
				this.rollAngle = this.rollTarget;
			} else {
				this.rollAccelerating = true;
				local106 = local16.rollMaxSpeed * local16.rollMaxSpeed / (local16.rollAcceleration * 2);
				this.rollMidpoint = (this.rollTarget + this.rollAngle) / 2;
				if (local101 >= 0) {
					local134 = this.rollTarget - local106;
					this.rollVelocity = local16.rollAcceleration;
					if (local134 > this.rollMidpoint) {
						this.rollMidpoint = local134;
					}
				} else {
					this.rollVelocity = -local16.rollAcceleration;
					local134 = local106 + this.rollTarget;
					if (this.rollMidpoint > local134) {
						this.rollMidpoint = local134;
					}
				}
			}
		} else if (this.rollVelocity > 0) {
			if (this.rollAngle >= this.rollMidpoint) {
				this.rollAccelerating = false;
			}
			if (!this.rollAccelerating) {
				this.rollVelocity -= local16.rollAcceleration;
				if (this.rollVelocity < 0) {
					this.rollVelocity = 0;
				}
			} else if (local16.rollMaxSpeed > this.rollVelocity) {
				this.rollVelocity += local16.rollAcceleration;
			}
		} else {
			if (this.rollMidpoint >= this.rollAngle) {
				this.rollAccelerating = false;
			}
			if (!this.rollAccelerating) {
				this.rollVelocity += local16.rollAcceleration;
				if (this.rollVelocity > 0) {
					this.rollVelocity = 0;
				}
			} else if (this.rollVelocity > -local16.rollMaxSpeed) {
				this.rollVelocity -= local16.rollAcceleration;
			}
		}
		this.rollAngle += this.rollVelocity;
		if (this.rollAngle != 0) {
			local101 = this.rollAngle >> 5 & 0x7FF;
			local106 = arg0.getMinY() / 2;
			arg0.translate(0, -local106, 0);
			arg0.rotateZ(local101);
			arg0.translate(0, local106, 0);
		}
		if (local28 != this.pitchTarget) {
			this.pitchTarget = local28;
			if (this.pitchVelocity > 0 && this.pitchAngle < local28) {
				local101 = this.pitchVelocity * this.pitchVelocity / (local16.pitchAcceleration * 2);
				local106 = local28 - this.pitchAngle;
				if (local101 > local106) {
					this.pitchAccelerating = false;
				} else {
					this.pitchMidpoint = (this.pitchAngle + local28 - local101) / 2;
					this.pitchAccelerating = true;
					local134 = local16.pitchMaxSpeed * local16.pitchMaxSpeed / (local16.pitchAcceleration * 2);
					local138 = local28 - local134;
					if (this.pitchMidpoint < local138) {
						this.pitchMidpoint = local138;
					}
				}
			} else if (this.pitchVelocity < 0 && this.pitchAngle > local28) {
				local106 = local28 - this.pitchAngle;
				local101 = this.pitchVelocity * this.pitchVelocity / (local16.pitchAcceleration * 2);
				if (local106 >= local101) {
					this.pitchMidpoint = (local101 + this.pitchAngle + local28) / 2;
					this.pitchAccelerating = true;
					local134 = local16.pitchMaxSpeed * local16.pitchMaxSpeed / (local16.pitchAcceleration * 2);
					local138 = local134 + local28;
					if (local138 < this.pitchMidpoint) {
						this.pitchMidpoint = local138;
					}
				} else {
					this.pitchAccelerating = false;
				}
			} else {
				this.pitchAccelerating = false;
			}
		}
		if (this.pitchVelocity == 0) {
			local101 = this.pitchTarget - this.pitchAngle;
			if (local101 > -local16.pitchAcceleration && local16.pitchAcceleration > local101) {
				this.pitchAngle = this.pitchTarget;
			} else {
				this.pitchMidpoint = (this.pitchTarget + this.pitchAngle) / 2;
				this.pitchAccelerating = true;
				local106 = local16.pitchMaxSpeed * local16.pitchMaxSpeed / (local16.pitchAcceleration * 2);
				if (local101 < 0) {
					this.pitchVelocity = -local16.pitchAcceleration;
					local134 = local106 + this.pitchTarget;
					if (this.pitchMidpoint > local134) {
						this.pitchMidpoint = local134;
					}
				} else {
					this.pitchVelocity = local16.pitchAcceleration;
					local134 = this.pitchTarget - local106;
					if (this.pitchMidpoint < local134) {
						this.pitchMidpoint = local134;
					}
				}
			}
		} else if (this.pitchVelocity > 0) {
			if (this.pitchAngle >= this.pitchMidpoint) {
				this.pitchAccelerating = false;
			}
			if (!this.pitchAccelerating) {
				this.pitchVelocity -= local16.pitchAcceleration;
				if (this.pitchVelocity < 0) {
					this.pitchVelocity = 0;
				}
			} else if (this.pitchVelocity < local16.pitchMaxSpeed) {
				this.pitchVelocity += local16.pitchAcceleration;
			}
		} else {
			if (this.pitchMidpoint >= this.pitchAngle) {
				this.pitchAccelerating = false;
			}
			if (!this.pitchAccelerating) {
				this.pitchVelocity += local16.pitchAcceleration;
				if (this.pitchVelocity > 0) {
					this.pitchVelocity = 0;
				}
			} else if (-local16.pitchMaxSpeed < this.pitchVelocity) {
				this.pitchVelocity -= local16.pitchAcceleration;
			}
		}
		this.pitchAngle += this.pitchVelocity;
		if (this.pitchAngle != 0) {
			local101 = this.pitchAngle >> 5 & 0x7FF;
			local106 = arg0.getMinY() / 2;
			arg0.translate(0, -local106, 0);
			arg0.rotateX(local101);
			arg0.translate(0, local106, 0);
		}
	}

	@OriginalMember(owner = "client!fe", name = "b", descriptor = "(I)I")
	public abstract int getBasId();

	@OriginalMember(owner = "client!fe", name = "c", descriptor = "(I)V")
	public final void resetMovementQueue() {
		this.movementQueueSize = 0;
		this.seqMovementSteps = 0;
	}

	@OriginalMember(owner = "client!fe", name = "d", descriptor = "(I)I")
	public final int getModelHeight() {
		return this.minY == -32768 ? 200 : -this.minY;
	}

	@OriginalMember(owner = "client!fe", name = "a", descriptor = "(II)V")
	public final void setSize(@OriginalArg(0) int size) {
		this.size = size;
	}

	@OriginalMember(owner = "client!fe", name = "c", descriptor = "(B)I")
	public int getSize() {
		return this.size;
	}
}
