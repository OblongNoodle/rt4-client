package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!lj")
public final class MillisTimer extends Timer {

	@OriginalMember(owner = "client!lj", name = "o", descriptor = "I")
	private int sampleIndex;

	@OriginalMember(owner = "client!lj", name = "x", descriptor = "[J")
	private final long[] sampleTimes = new long[10];

	@OriginalMember(owner = "client!lj", name = "r", descriptor = "I")
	private int tickRate = 256;

	@OriginalMember(owner = "client!lj", name = "u", descriptor = "I")
	private int sleepTime = 1;

	@OriginalMember(owner = "client!lj", name = "v", descriptor = "I")
	private int tickAccumulator = 0;

	@OriginalMember(owner = "client!lj", name = "k", descriptor = "J")
	private long currentTime = MonotonicClock.currentTimeMillis();

	@OriginalMember(owner = "client!lj", name = "<init>", descriptor = "()V")
	public MillisTimer() {
		for (@Pc(22) int local22 = 0; local22 < 10; local22++) {
			this.sampleTimes[local22] = this.currentTime;
		}
	}

	@OriginalMember(owner = "client!lj", name = "b", descriptor = "(I)V")
	@Override
	public final void reset() {
		for (@Pc(7) int local7 = 0; local7 < 10; local7++) {
			this.sampleTimes[local7] = 0L;
		}
	}

	@OriginalMember(owner = "client!lj", name = "a", descriptor = "(III)I")
	@Override
	public final int sleep(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		@Pc(17) int local17 = this.sleepTime;
		@Pc(20) int local20 = this.tickRate;
		this.tickRate = 300;
		this.sleepTime = 1;
		this.currentTime = MonotonicClock.currentTimeMillis();
		if (this.sampleTimes[this.sampleIndex] == 0L) {
			this.tickRate = local20;
			this.sleepTime = local17;
		} else if (this.sampleTimes[this.sampleIndex] < this.currentTime) {
			this.tickRate = (int) ((long) (arg1 * 2560) / (this.currentTime - this.sampleTimes[this.sampleIndex]));
		}
		if (this.tickRate < 25) {
			this.tickRate = 25;
		}
		if (this.tickRate > 256) {
			this.tickRate = 256;
			this.sleepTime = (int) ((long) arg1 - (this.currentTime - this.sampleTimes[this.sampleIndex]) / 10L);
		}
		if (arg1 < this.sleepTime) {
			this.sleepTime = arg1;
		}
		this.sampleTimes[this.sampleIndex] = this.currentTime;
		this.sampleIndex = (this.sampleIndex + 1) % 10;
		@Pc(139) int local139;
		if (this.sleepTime > 1) {
			for (local139 = 0; local139 < 10; local139++) {
				if (this.sampleTimes[local139] != 0L) {
					this.sampleTimes[local139] += this.sleepTime;
				}
			}
		}
		if (arg0 > this.sleepTime) {
			this.sleepTime = arg0;
		}
		ThreadUtils.sleep(this.sleepTime);
		local139 = 0;
		while (this.tickAccumulator < 256) {
			this.tickAccumulator += this.tickRate;
			local139++;
		}
		this.tickAccumulator &= 0xFF;
		return local139;
	}

	public int count(int arg0, int arg1) {
		return 1;
	}
}
