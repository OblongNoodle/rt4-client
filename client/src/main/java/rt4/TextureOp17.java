package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!hk")
public final class TextureOp17 extends TextureOp {

	@OriginalMember(owner = "client!hk", name = "Q", descriptor = "I")
	private int green;

	@OriginalMember(owner = "client!hk", name = "Z", descriptor = "I")
	private int hue;

	@OriginalMember(owner = "client!hk", name = "cb", descriptor = "I")
	private int blue;

	@OriginalMember(owner = "client!hk", name = "gb", descriptor = "I")
	private int red;

	@OriginalMember(owner = "client!hk", name = "ib", descriptor = "I")
	private int lightness;

	@OriginalMember(owner = "client!hk", name = "jb", descriptor = "I")
	private int saturation;

	@OriginalMember(owner = "client!hk", name = "T", descriptor = "I")
	private int hueShift = 0;

	@OriginalMember(owner = "client!hk", name = "U", descriptor = "I")
	private int lightnessShift = 0;

	@OriginalMember(owner = "client!hk", name = "X", descriptor = "I")
	private int saturationShift = 0;

	@OriginalMember(owner = "client!hk", name = "<init>", descriptor = "()V")
	public TextureOp17() {
		super(1, false);
	}

	@OriginalMember(owner = "client!hk", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.hueShift = arg1.g2b();
		} else if (arg0 == 1) {
			this.saturationShift = (arg1.g1b() << 12) / 100;
		} else if (arg0 == 2) {
			this.lightnessShift = (arg1.g1b() << 12) / 100;
		}
	}

	@OriginalMember(owner = "client!hk", name = "a", descriptor = "(IIII)V")
	private void hslToRgb(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2) {
		@Pc(35) int local35 = arg0 <= 2048 ? arg0 * (arg1 + 4096) >> 12 : arg0 + arg1 - (arg0 * arg1 >> 12);
		if (local35 <= 0) {
			this.red = this.green = this.blue = arg0;
			return;
		}
		@Pc(44) int local44 = arg2 * 6;
		@Pc(51) int local51 = arg0 + arg0 - local35;
		@Pc(55) int local55 = local44 >> 12;
		@Pc(64) int local64 = (local35 - local51 << 12) / local35;
		@Pc(70) int local70 = local44 - (local55 << 12);
		@Pc(78) int local78 = local35 * local64 >> 12;
		@Pc(84) int local84 = local78 * local70 >> 12;
		@Pc(88) int local88 = local84 + local51;
		@Pc(93) int local93 = local35 - local84;
		if (local55 == 0) {
			this.blue = local51;
			this.red = local35;
			this.green = local88;
		} else if (local55 == 1) {
			this.blue = local51;
			this.green = local35;
			this.red = local93;
		} else if (local55 == 2) {
			this.red = local51;
			this.green = local35;
			this.blue = local88;
		} else if (local55 == 3) {
			this.green = local93;
			this.blue = local35;
			this.red = local51;
		} else if (local55 == 4) {
			this.blue = local35;
			this.red = local88;
			this.green = local51;
		} else if (local55 == 5) {
			this.green = local51;
			this.red = local35;
			this.blue = local93;
		}
	}

	@OriginalMember(owner = "client!hk", name = "a", descriptor = "(BIII)V")
	private void rgbToHsl(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2) {
		@Pc(12) int local12 = arg0 > arg1 ? arg0 : arg1;
		@Pc(31) int local31 = arg2 <= local12 ? local12 : arg2;
		@Pc(38) int local38 = arg1 > arg0 ? arg0 : arg1;
		@Pc(49) int local49 = arg2 >= local38 ? local38 : arg2;
		@Pc(54) int local54 = local31 - local49;
		if (local54 > 0) {
			@Pc(65) int local65 = (local31 - arg1 << 12) / local54;
			@Pc(74) int local74 = (local31 - arg0 << 12) / local54;
			@Pc(83) int local83 = (local31 - arg2 << 12) / local54;
			if (arg0 == local31) {
				this.hue = local49 == arg1 ? local83 + 20480 : -local65 + 4096;
			} else if (local31 == arg1) {
				this.hue = local49 == arg2 ? local74 + 4096 : -local83 + 12288;
			} else {
				this.hue = local49 == arg0 ? local65 + 12288 : -local74 + 20480;
			}
			this.hue /= 6;
		} else {
			this.hue = 0;
		}
		this.lightness = (local49 + local31) / 2;
		if (this.lightness > 0 && this.lightness < 4096) {
			this.saturation = (local54 << 12) / (this.lightness > 2048 ? 8192 - this.lightness * 2 : this.lightness * 2);
		} else {
			this.saturation = 0;
		}
	}

	@OriginalMember(owner = "client!hk", name = "b", descriptor = "(II)[[I")
	@Override
	public final int[][] getColorOutput(@OriginalArg(1) int arg0) {
		@Pc(17) int[][] local17 = this.colorImageCache.get(arg0);
		if (this.colorImageCache.invalid) {
			@Pc(28) int[][] local28 = this.getChildColorOutput(arg0, 0);
			@Pc(32) int[] local32 = local28[0];
			@Pc(36) int[] local36 = local28[1];
			@Pc(40) int[] local40 = local28[2];
			@Pc(44) int[] local44 = local17[1];
			@Pc(48) int[] local48 = local17[2];
			@Pc(52) int[] local52 = local17[0];
			for (@Pc(54) int local54 = 0; local54 < Texture.width; local54++) {
				this.rgbToHsl(local32[local54], local36[local54], local40[local54]);
				this.lightness += this.lightnessShift;
				if (this.lightness < 0) {
					this.lightness = 0;
				}
				this.saturation += this.saturationShift;
				if (this.lightness > 4096) {
					this.lightness = 4096;
				}
				if (this.saturation < 0) {
					this.saturation = 0;
				}
				if (this.saturation > 4096) {
					this.saturation = 4096;
				}
				for (this.hue += this.hueShift; this.hue < 0; this.hue += 4096) {
				}
				while (this.hue > 4096) {
					this.hue -= 4096;
				}
				this.hslToRgb(this.lightness, this.saturation, this.hue);
				local52[local54] = this.red;
				local44[local54] = this.green;
				local48[local54] = this.blue;
			}
		}
		return local17;
	}
}
