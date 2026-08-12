package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!gm")
public final class TextureOpMandelbrot extends TextureOp {

	@OriginalMember(owner = "client!gm", name = "Z", descriptor = "I")
	private int zoom = 1365;

	@OriginalMember(owner = "client!gm", name = "ab", descriptor = "I")
	private int offsetY = 0;

	@OriginalMember(owner = "client!gm", name = "V", descriptor = "I")
	private int offsetX = 0;

	@OriginalMember(owner = "client!gm", name = "Y", descriptor = "I")
	private int maxIterations = 20;

	@OriginalMember(owner = "client!gm", name = "<init>", descriptor = "()V")
	public TextureOpMandelbrot() {
		super(0, true);
	}

	@OriginalMember(owner = "client!gm", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.zoom = arg1.g2();
		} else if (arg0 == 1) {
			this.maxIterations = arg1.g2();
		} else if (arg0 == 2) {
			this.offsetX = arg1.g2();
		} else if (arg0 == 3) {
			this.offsetY = arg1.g2();
		}
	}

	@OriginalMember(owner = "client!gm", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(19) int[] local19 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			for (@Pc(26) int local26 = 0; local26 < Texture.width; local26++) {
				@Pc(45) int local45 = this.offsetY + (Texture.heightFractions[arg0] << 12) / this.zoom;
				@Pc(57) int local57 = this.offsetX + (Texture.widthFractions[local26] << 12) / this.zoom;
				@Pc(61) int local61 = local57;
				@Pc(65) int local65 = local45;
				@Pc(67) int local67 = 0;
				@Pc(73) int local73 = local57 * local57 >> 12;
				@Pc(79) int local79 = local45 * local45 >> 12;
				while (local73 + local79 < 16384 && this.maxIterations > local67) {
					local65 = (local61 * local65 >> 12) * 2 + local45;
					local67++;
					local61 = local73 + local57 - local79;
					local79 = local65 * local65 >> 12;
					local73 = local61 * local61 >> 12;
				}
				local19[local26] = local67 >= this.maxIterations - 1 ? 0 : (local67 << 12) / this.maxIterations;
			}
		}
		return local19;
	}
}
