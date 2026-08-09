package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!bh")
public final class SpotAnim extends Entity {

	@OriginalMember(owner = "client!bh", name = "y", descriptor = "Lclient!ga;")
	private ParticleSystem particleSystem;

	@OriginalMember(owner = "client!bh", name = "B", descriptor = "I")
	private int seqFrame = 0;

	@OriginalMember(owner = "client!bh", name = "O", descriptor = "I")
	private final int seqNextFrame = -1;

	@OriginalMember(owner = "client!bh", name = "P", descriptor = "I")
	private int minY = -32768;

	@OriginalMember(owner = "client!bh", name = "T", descriptor = "Z")
	public boolean finished = false;

	@OriginalMember(owner = "client!bh", name = "U", descriptor = "I")
	private int frameClock = 0;

	@OriginalMember(owner = "client!bh", name = "I", descriptor = "I")
	public final int posZ;

	@OriginalMember(owner = "client!bh", name = "Q", descriptor = "I")
	public final int posX;

	@OriginalMember(owner = "client!bh", name = "S", descriptor = "I")
	public final int plane;

	@OriginalMember(owner = "client!bh", name = "w", descriptor = "I")
	public final int endLoop;

	@OriginalMember(owner = "client!bh", name = "F", descriptor = "I")
	private final int spotAnimTypeId;

	@OriginalMember(owner = "client!bh", name = "K", descriptor = "I")
	public final int y;

	@OriginalMember(owner = "client!bh", name = "J", descriptor = "Lclient!tk;")
	private SeqType seq;

	@OriginalMember(owner = "client!bh", name = "<init>", descriptor = "(IIIIIII)V")
	public SpotAnim(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6) {
		this.posZ = arg3;
		this.posX = arg2;
		this.plane = arg1;
		this.endLoop = arg6 + arg5;
		this.spotAnimTypeId = arg0;
		this.y = arg4;
		@Pc(42) int local42 = SpotAnimTypeList.get(this.spotAnimTypeId).seqId;
		if (local42 == -1) {
			this.finished = true;
		} else {
			this.finished = false;
			this.seq = SeqTypeList.get(local42);
		}
	}

	@OriginalMember(owner = "client!bh", name = "b", descriptor = "(Z)Lclient!ak;")
	private Model getModel() {
		@Pc(8) SpotAnimType local8 = SpotAnimTypeList.get(this.spotAnimTypeId);
		@Pc(26) Model local26;
		if (this.finished) {
			local26 = local8.constructModel(-1, -1, 0);
		} else {
			local26 = local8.constructModel(this.seqNextFrame, this.seqFrame, this.frameClock);
		}
		return local26;
	}

	@OriginalMember(owner = "client!bh", name = "b", descriptor = "()I")
	@Override
	public final int getMinY() {
		return this.minY;
	}

	@OriginalMember(owner = "client!bh", name = "a", descriptor = "(IIIIIIIIJILclient!ga;)V")
	@Override
	public final void render(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6, @OriginalArg(7) int arg7, @OriginalArg(8) long arg8, @OriginalArg(9) int arg9, @OriginalArg(10) ParticleSystem arg10) {
		@Pc(7) Model local7 = this.getModel();
		if (local7 != null) {
			local7.render(arg0, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, this.particleSystem);
			this.minY = local7.getMinY();
		}
	}

	@OriginalMember(owner = "client!bh", name = "a", descriptor = "(ZI)V")
	public final void advanceAnimation(@OriginalArg(1) int arg0) {
		if (this.finished) {
			return;
		}
		this.frameClock += arg0;
		while (this.frameClock > this.seq.frameDelay[this.seqFrame]) {
			this.frameClock -= this.seq.frameDelay[this.seqFrame];
			this.seqFrame++;
			if (this.seq.frames.length <= this.seqFrame) {
				this.finished = true;
				break;
			}
		}
	}

	@OriginalMember(owner = "client!bh", name = "a", descriptor = "(IIIII)V")
	@Override
	public final void method4545(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
	}
}
