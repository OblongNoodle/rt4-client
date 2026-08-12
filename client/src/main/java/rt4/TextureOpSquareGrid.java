package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!f")
public final class TextureOpSquareGrid extends TextureOp {

	@OriginalMember(owner = "client!f", name = "R", descriptor = "I")
	private int borderSize = 204;

	@OriginalMember(owner = "client!f", name = "Y", descriptor = "I")
	private int repeatY = 1;

	@OriginalMember(owner = "client!f", name = "Q", descriptor = "I")
	private int repeatX = 1;

	@OriginalMember(owner = "client!f", name = "<init>", descriptor = "()V")
	public TextureOpSquareGrid() {
		super(0, true);
	}

	@OriginalMember(owner = "client!f", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.repeatX = arg1.g1();
		} else if (arg0 == 1) {
			this.repeatY = arg1.g1();
		} else if (arg0 == 2) {
			this.borderSize = arg1.g2();
		}
	}

	@OriginalMember(owner = "client!f", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(19) int[] local19 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			for (@Pc(25) int local25 = 0; local25 < Texture.width; local25++) {
				@Pc(32) int local32 = Texture.widthFractions[local25];
				@Pc(36) int local36 = Texture.heightFractions[arg0];
				@Pc(43) int local43 = this.repeatX * local32 >> 12;
				@Pc(50) int local50 = local36 * this.repeatY >> 12;
				@Pc(60) int local60 = this.repeatX * (local32 % (4096 / this.repeatX));
				@Pc(70) int local70 = local36 % (4096 / this.repeatY) * this.repeatY;
				if (this.borderSize > local70) {
					for (local43 -= local50; local43 < 0; local43 += 4) {
					}
					while (local43 > 3) {
						local43 -= 4;
					}
					if (local43 != 1) {
						local19[local25] = 0;
						continue;
					}
					if (this.borderSize > local60) {
						local19[local25] = 0;
						continue;
					}
				}
				if (local60 < this.borderSize) {
					for (local43 -= local50; local43 < 0; local43 += 4) {
					}
					while (local43 > 3) {
						local43 -= 4;
					}
					if (local43 > 0) {
						local19[local25] = 0;
						continue;
					}
				}
				local19[local25] = 4096;
			}
		}
		return local19;
	}
}
