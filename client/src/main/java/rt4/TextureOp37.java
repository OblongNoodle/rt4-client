package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!mh")
public final class TextureOp37 extends TextureOp {

	@OriginalMember(owner = "client!mh", name = "P", descriptor = "I")
	private int offsetY1 = 0;

	@OriginalMember(owner = "client!mh", name = "T", descriptor = "I")
	private int waveFrequency = 12288;

	@OriginalMember(owner = "client!mh", name = "bb", descriptor = "I")
	private int offsetY2 = 2048;

	@OriginalMember(owner = "client!mh", name = "U", descriptor = "I")
	private int offsetX2 = 0;

	@OriginalMember(owner = "client!mh", name = "ib", descriptor = "I")
	private int offsetX1 = 2048;

	@OriginalMember(owner = "client!mh", name = "Q", descriptor = "I")
	private int waveAmplitude = 4096;

	@OriginalMember(owner = "client!mh", name = "kb", descriptor = "I")
	private int waveDivisor = 8192;

	@OriginalMember(owner = "client!mh", name = "<init>", descriptor = "()V")
	public TextureOp37() {
		super(0, true);
	}

	@OriginalMember(owner = "client!mh", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.offsetX1 = arg1.g2();
		} else if (arg0 == 1) {
			this.offsetY1 = arg1.g2();
		} else if (arg0 == 2) {
			this.offsetX2 = arg1.g2();
		} else if (arg0 == 3) {
			this.offsetY2 = arg1.g2();
		} else if (arg0 == 4) {
			this.waveFrequency = arg1.g2();
		} else if (arg0 == 5) {
			this.waveAmplitude = arg1.g2();
		} else if (arg0 == 6) {
			this.waveDivisor = arg1.g2();
		}
	}

	@OriginalMember(owner = "client!mh", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(19) int[] local19 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			@Pc(30) int local30 = Texture.heightFractions[arg0] - 2048;
			for (@Pc(32) int local32 = 0; local32 < Texture.width; local32++) {
				@Pc(40) int local40 = local30 + this.offsetY1;
				@Pc(46) int local46 = Texture.widthFractions[local32] - 2048;
				@Pc(51) int local51 = this.offsetX1 + local46;
				@Pc(62) int local62 = local40 < -2048 ? local40 + 4096 : local40;
				@Pc(71) int local71 = local62 > 2048 ? local62 - 4096 : local62;
				@Pc(76) int local76 = local46 + this.offsetX2;
				@Pc(85) int local85 = local51 < -2048 ? local51 + 4096 : local51;
				@Pc(96) int local96 = local85 <= 2048 ? local85 : local85 - 4096;
				@Pc(107) int local107 = local76 >= -2048 ? local76 : local76 + 4096;
				@Pc(118) int local118 = local107 > 2048 ? local107 - 4096 : local107;
				@Pc(124) int local124 = local30 + this.offsetY2;
				@Pc(133) int local133 = local124 < -2048 ? local124 + 4096 : local124;
				@Pc(144) int local144 = local133 > 2048 ? local133 - 4096 : local133;
				local19[local32] = this.isInsidePrimaryWave(local96, local71) || this.isInsideSecondaryWave(local118, local144) ? 4096 : 0;
			}
		}
		return local19;
	}

	@OriginalMember(owner = "client!mh", name = "a", descriptor = "(BII)Z")
	private boolean isInsideSecondaryWave(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		@Pc(12) int local12 = this.waveFrequency * (arg0 + arg1) >> 12;
		@Pc(27) int local27 = TextureOp.COSINE[local12 * 255 >> 12 & 0xFF];
		@Pc(34) int local34 = (local27 << 12) / this.waveFrequency;
		@Pc(41) int local41 = (local34 << 12) / this.waveDivisor;
		@Pc(48) int local48 = local41 * this.waveAmplitude >> 12;
		return local48 > arg1 - arg0 && -local48 < arg1 - arg0;
	}

	@OriginalMember(owner = "client!mh", name = "e", descriptor = "(I)V")
	@Override
	public final void postDecode() {
		TextureOp.createTrigonometryTables();
	}

	@OriginalMember(owner = "client!mh", name = "b", descriptor = "(IIB)Z")
	private boolean isInsidePrimaryWave(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1) {
		@Pc(8) int local8 = (arg1 - arg0) * this.waveFrequency >> 12;
		@Pc(31) int local31 = TextureOp.COSINE[local8 * 255 >> 12 & 0xFF];
		@Pc(38) int local38 = (local31 << 12) / this.waveFrequency;
		@Pc(45) int local45 = (local38 << 12) / this.waveDivisor;
		@Pc(52) int local52 = local45 * this.waveAmplitude >> 12;
		return arg1 + arg0 < local52 && -local52 < arg1 + arg0;
	}
}
