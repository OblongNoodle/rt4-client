package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!d")
public final class TextureOpStripes extends TextureOp {

	@OriginalMember(owner = "client!d", name = "U", descriptor = "[I")
	private int[] stripeStarts;

	@OriginalMember(owner = "client!d", name = "W", descriptor = "[I")
	private int[] stripeEnds;

	@OriginalMember(owner = "client!d", name = "Q", descriptor = "I")
	private int direction = 0;

	@OriginalMember(owner = "client!d", name = "P", descriptor = "I")
	private int stripeCount = 10;

	@OriginalMember(owner = "client!d", name = "bb", descriptor = "I")
	private int stripeWidth = 2048;

	@OriginalMember(owner = "client!d", name = "<init>", descriptor = "()V")
	public TextureOpStripes() {
		super(0, true);
	}

	@OriginalMember(owner = "client!d", name = "e", descriptor = "(I)V")
	@Override
	public final void postDecode() {
		this.initStripes();
	}

	@OriginalMember(owner = "client!d", name = "h", descriptor = "(I)V")
	private void initStripes() {
		@Pc(7) int local7 = 0;
		this.stripeEnds = new int[this.stripeCount + 1];
		@Pc(23) int local23 = 4096 / this.stripeCount;
		this.stripeStarts = new int[this.stripeCount + 1];
		@Pc(37) int local37 = this.stripeWidth * local23 >> 12;
		for (@Pc(39) int local39 = 0; local39 < this.stripeCount; local39++) {
			this.stripeStarts[local39] = local7;
			this.stripeEnds[local39] = local7 + local37;
			local7 += local23;
		}
		this.stripeStarts[this.stripeCount] = 4096;
		this.stripeEnds[this.stripeCount] = this.stripeEnds[0] + 4096;
	}

	@OriginalMember(owner = "client!d", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.stripeCount = arg1.g1();
		} else if (arg0 == 1) {
			this.stripeWidth = arg1.g2();
		} else if (arg0 == 2) {
			this.direction = arg1.g1();
		}
	}

	@OriginalMember(owner = "client!d", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(19) int[] local19 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			@Pc(28) int local28 = Texture.heightFractions[arg0];
			@Pc(36) int local36;
			if (this.direction == 0) {
				@Pc(34) short local34 = 0;
				for (local36 = 0; local36 < this.stripeCount; local36++) {
					if (this.stripeStarts[local36] <= local28 && local28 < this.stripeStarts[local36 + 1]) {
						if (local28 < this.stripeEnds[local36]) {
							local34 = 4096;
						}
						break;
					}
				}
				ArrayUtils.fill(local19, 0, Texture.width, local34);
			} else {
				for (@Pc(85) int local85 = 0; local85 < Texture.width; local85++) {
					@Pc(96) int local96 = Texture.widthFractions[local85];
					local36 = 0;
					@Pc(101) int local101 = this.direction;
					if (local101 == 1) {
						local36 = local96;
					} else if (local101 == 2) {
						local36 = (local96 + local28 - 4096 >> 1) + 2048;
					} else if (local101 == 3) {
						local36 = (local96 - local28 >> 1) + 2048;
					}
					@Pc(143) short local143 = 0;
					for (local101 = 0; local101 < this.stripeCount; local101++) {
						if (this.stripeStarts[local101] <= local36 && local36 < this.stripeStarts[local101 + 1]) {
							if (local36 < this.stripeEnds[local101]) {
								local143 = 4096;
							}
							break;
						}
					}
					local19[local85] = local143;
				}
			}
		}
		return local19;
	}
}
