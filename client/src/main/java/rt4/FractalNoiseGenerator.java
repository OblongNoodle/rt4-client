package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!vd")
public class FractalNoiseGenerator extends PerlinNoiseGenerator {

	@OriginalMember(owner = "client!vd", name = "x", descriptor = "I")
	private int accumulator;

	@OriginalMember(owner = "client!vd", name = "H", descriptor = "[B")
	private byte[] output;

	@OriginalMember(owner = "client!vd", name = "J", descriptor = "I")
	private int writePos;

	@OriginalMember(owner = "client!vd", name = "G", descriptor = "[I")
	private final int[] octaveWeights = new int[this.octaveCount];

	@OriginalMember(owner = "client!vd", name = "<init>", descriptor = "(IIIIIF)V")
	protected FractalNoiseGenerator(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) float arg5) {
		super(arg0, arg1, arg2, arg3, arg4);
		for (@Pc(15) int local15 = 0; local15 < this.octaveCount; local15++) {
			this.octaveWeights[local15] = (short) (Math.pow(arg5, local15) * 4096.0D);
		}
	}

	@OriginalMember(owner = "client!vd", name = "a", descriptor = "(III)V")
	@Override
	protected final void accumulate(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1) {
		this.accumulator += arg0 * this.octaveWeights[arg1] >> 12;
	}

	@OriginalMember(owner = "client!vd", name = "a", descriptor = "(I)V")
	@Override
	protected final void resetState() {
		this.writePos = 0;
		this.accumulator = 0;
	}

	@OriginalMember(owner = "client!vd", name = "a", descriptor = "(IB)V")
	protected void writeSample(@OriginalArg(0) int arg0, @OriginalArg(1) byte arg1) {
		this.output[this.writePos++] = (byte) ((arg1 >> 1 & 0x7F) + 127);
	}

	@OriginalMember(owner = "client!vd", name = "a", descriptor = "(B)V")
	@Override
	protected final void finishSample() {
		this.accumulator = Math.abs(this.accumulator);
		if (this.accumulator >= 4096) {
			this.accumulator = 4095;
		}
		this.writeSample(this.writePos++, (byte) (this.accumulator >> 4));
		this.accumulator = 0;
	}
}
