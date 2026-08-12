package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!bi")
public final class TextureOpPerlinNoise extends TextureOp {

	@OriginalMember(owner = "client!bi", name = "ib", descriptor = "[S")
	private short[] octaveFrequencies;

	@OriginalMember(owner = "client!bi", name = "lb", descriptor = "[S")
	private short[] octaveAmplitudes;

	@OriginalMember(owner = "client!bi", name = "X", descriptor = "I")
	public int frequencyY = 4;

	@OriginalMember(owner = "client!bi", name = "eb", descriptor = "I")
	public int frequencyX = 4;

	@OriginalMember(owner = "client!bi", name = "kb", descriptor = "Z")
	public boolean normalizeOutput = true;

	@OriginalMember(owner = "client!bi", name = "gb", descriptor = "I")
	public int persistence = 1638;

	@OriginalMember(owner = "client!bi", name = "ab", descriptor = "[B")
	private byte[] permutationTable = new byte[512];

	@OriginalMember(owner = "client!bi", name = "Z", descriptor = "I")
	public int octaveCount = 4;

	@OriginalMember(owner = "client!bi", name = "mb", descriptor = "I")
	public int seed = 0;

	@OriginalMember(owner = "client!bi", name = "<init>", descriptor = "()V")
	public TextureOpPerlinNoise() {
		super(0, true);
	}

	@OriginalMember(owner = "client!bi", name = "a", descriptor = "(ZI[I)V")
	public final void getNoiseRow(@OriginalArg(1) int arg0, @OriginalArg(2) int[] arg1) {
		@Pc(12) int local12 = this.frequencyY * Texture.heightFractions[arg0];
		@Pc(115) int local115;
		@Pc(129) int local129;
		@Pc(40) int local40;
		@Pc(27) short local27;
		@Pc(105) int local105;
		@Pc(60) int local60;
		@Pc(54) int local54;
		@Pc(47) int local47;
		@Pc(85) int local85;
		@Pc(64) int local64;
		@Pc(68) int local68;
		@Pc(77) int local77;
		@Pc(103) int local103;
		if (this.octaveCount == 1) {
			local27 = this.octaveAmplitudes[0];
			local40 = this.octaveFrequencies[0] << 12;
			local60 = local12 * local40 >> 12;
			local54 = this.frequencyX * local40 >> 12;
			local47 = local40 * this.frequencyY >> 12;
			local64 = local60 >> 12;
			local77 = this.permutationTable[local64 & 0xFF] & 0xFF;
			local60 &= 0xFFF;
			local85 = MonochromeImageCache.smoothstepLookup[local60];
			local68 = local64 + 1;
			if (local47 <= local68) {
				local68 = 0;
			}
			local103 = this.permutationTable[local68 & 0xFF] & 0xFF;
			if (this.normalizeOutput) {
				for (local105 = 0; local105 < Texture.width; local105++) {
					local115 = this.frequencyX * Texture.widthFractions[local105];
					local129 = this.sampleNoise(local40 * local115 >> 12, local103, local77, local54, local60, local85);
					local129 = local27 * local129 >> 12;
					arg1[local105] = (local129 >> 1) + 2048;
				}
			} else {
				for (local105 = 0; local105 < Texture.width; local105++) {
					local115 = this.frequencyX * Texture.widthFractions[local105];
					local129 = this.sampleNoise(local40 * local115 >> 12, local103, local77, local54, local60, local85);
					arg1[local105] = local27 * local129 >> 12;
				}
			}
			return;
		}
		local27 = this.octaveAmplitudes[0];
		if (local27 > 8 || local27 < -8) {
			local40 = this.octaveFrequencies[0] << 12;
			local47 = local40 * this.frequencyY >> 12;
			local54 = this.frequencyX * local40 >> 12;
			local60 = local12 * local40 >> 12;
			local64 = local60 >> 12;
			local68 = local64 + 1;
			local77 = this.permutationTable[local64 & 0xFF] & 0xFF;
			@Pc(81) int local81 = local60 & 0xFFF;
			local85 = MonochromeImageCache.smoothstepLookup[local81];
			if (local68 >= local47) {
				local68 = 0;
			}
			local103 = this.permutationTable[local68 & 0xFF] & 0xFF;
			for (local105 = 0; local105 < Texture.width; local105++) {
				local115 = Texture.widthFractions[local105] * this.frequencyX;
				local129 = this.sampleNoise(local115 * local40 >> 12, local103, local77, local54, local81, local85);
				arg1[local105] = local129 * local27 >> 12;
			}
		}
		for (@Pc(142) int local142 = 1; local142 < this.octaveCount; local142++) {
			local27 = this.octaveAmplitudes[local142];
			if (local27 > 8 || local27 < -8) {
				local40 = this.octaveFrequencies[local142] << 12;
				local60 = local40 * local12 >> 12;
				local64 = local60 >> 12;
				local77 = this.permutationTable[local64 & 0xFF] & 0xFF;
				local54 = this.frequencyX * local40 >> 12;
				local68 = local64 + 1;
				local60 &= 0xFFF;
				local85 = MonochromeImageCache.smoothstepLookup[local60];
				local47 = this.frequencyY * local40 >> 12;
				if (local68 >= local47) {
					local68 = 0;
				}
				local103 = this.permutationTable[local68 & 0xFF] & 0xFF;
				if (this.normalizeOutput && local142 == this.octaveCount - 1) {
					for (local105 = 0; local105 < Texture.width; local105++) {
						local115 = Texture.widthFractions[local105] * this.frequencyX;
						local129 = this.sampleNoise(local40 * local115 >> 12, local103, local77, local54, local60, local85);
						local129 = (local27 * local129 >> 12) + arg1[local105];
						arg1[local105] = (local129 >> 1) + 2048;
					}
				} else {
					for (local105 = 0; local105 < Texture.width; local105++) {
						local115 = Texture.widthFractions[local105] * this.frequencyX;
						local129 = this.sampleNoise(local115 * local40 >> 12, local103, local77, local54, local60, local85);
						arg1[local105] += local129 * local27 >> 12;
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!bi", name = "e", descriptor = "(I)V")
	@Override
	public final void postDecode() {
		this.permutationTable = TextureOpVoronoi.getPermutationTable(this.seed);
		this.initOctaves();
		for (@Pc(15) int local15 = this.octaveCount - 1; local15 >= 1; local15--) {
			@Pc(23) short local23 = this.octaveAmplitudes[local15];
			if (local23 > 8 || local23 < -8) {
				break;
			}
			this.octaveCount--;
		}
	}

	@OriginalMember(owner = "client!bi", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.normalizeOutput = arg1.g1() == 1;
		} else if (arg0 == 1) {
			this.octaveCount = arg1.g1();
		} else if (arg0 == 2) {
			this.persistence = arg1.g2b();
			if (this.persistence < 0) {
				this.octaveAmplitudes = new short[this.octaveCount];
				for (@Pc(93) int local93 = 0; local93 < this.octaveCount; local93++) {
					this.octaveAmplitudes[local93] = (short) arg1.g2b();
				}
			}
		} else if (arg0 == 3) {
			this.frequencyX = this.frequencyY = arg1.g1();
		} else if (arg0 == 4) {
			this.seed = arg1.g1();
		} else if (arg0 == 5) {
			this.frequencyX = arg1.g1();
		} else if (arg0 == 6) {
			this.frequencyY = arg1.g1();
		}
	}

	@OriginalMember(owner = "client!bi", name = "b", descriptor = "(Z)V")
	private void initOctaves() {
		@Pc(21) int local21;
		if (this.persistence > 0) {
			this.octaveAmplitudes = new short[this.octaveCount];
			this.octaveFrequencies = new short[this.octaveCount];
			for (local21 = 0; local21 < this.octaveCount; local21++) {
				this.octaveAmplitudes[local21] = (short) (Math.pow((float) this.persistence / 4096.0F, local21) * 4096.0D);
				this.octaveFrequencies[local21] = (short) Math.pow(2.0D, local21);
			}
		} else if (this.octaveAmplitudes != null && this.octaveAmplitudes.length == this.octaveCount) {
			this.octaveFrequencies = new short[this.octaveCount];
			for (local21 = 0; local21 < this.octaveCount; local21++) {
				this.octaveFrequencies[local21] = (short) Math.pow(2.0D, local21);
			}
		}
	}

	@OriginalMember(owner = "client!bi", name = "a", descriptor = "(IIIIIII)I")
	private int sampleNoise(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5) {
		@Pc(15) int local15 = arg4 - 4096;
		@Pc(19) int local19 = arg0 >> 12;
		@Pc(23) int local23 = local19 + 1;
		@Pc(27) int local27 = local19 & 0xFF;
		if (local23 >= arg3) {
			local23 = 0;
		}
		@Pc(40) int local40 = arg0 & 0xFFF;
		@Pc(50) int local50 = this.permutationTable[local27 + arg2] & 0x3;
		@Pc(54) int local54 = MonochromeImageCache.smoothstepLookup[local40];
		@Pc(70) int local70;
		if (local50 > 1) {
			local70 = local50 == 2 ? local40 - arg4 : -local40 + -arg4;
		} else {
			local70 = local50 == 0 ? arg4 + local40 : -local40 + arg4;
		}
		local23 &= 0xFF;
		@Pc(92) int local92 = local40 - 4096;
		local50 = this.permutationTable[arg2 + local23] & 0x3;
		@Pc(118) int local118;
		if (local50 <= 1) {
			local118 = local50 == 0 ? arg4 + local92 : -local92 + arg4;
		} else {
			local118 = local50 == 2 ? local92 - arg4 : -local92 + -arg4;
		}
		local50 = this.permutationTable[local27 + arg1] & 0x3;
		@Pc(155) int local155 = local70 + ((local118 - local70) * local54 >> 12);
		if (local50 > 1) {
			local70 = local50 == 2 ? local40 - local15 : -local40 - local15;
		} else {
			local70 = local50 == 0 ? local40 + local15 : local15 + -local40;
		}
		local50 = this.permutationTable[arg1 + local23] & 0x3;
		if (local50 > 1) {
			local118 = local50 == 2 ? local92 - local15 : -local15 + -local92;
		} else {
			local118 = local50 == 0 ? local92 + local15 : local15 + -local92;
		}
		@Pc(237) int local237 = local70 + ((local118 - local70) * local54 >> 12);
		return local155 + (arg5 * (local237 - local155) >> 12);
	}

	@OriginalMember(owner = "client!bi", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(17) int[] local17 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			this.getNoiseRow(arg0, local17);
		}
		return local17;
	}
}
