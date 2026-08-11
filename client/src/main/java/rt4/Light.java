package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!gi")
public final class Light {

	@OriginalMember(owner = "client!f", name = "P", descriptor = "[I")
	public static int[] NOISE;
	@OriginalMember(owner = "client!gi", name = "a", descriptor = "Z")
	public final boolean extendsDown;

	@OriginalMember(owner = "client!gi", name = "d", descriptor = "I")
	private int alphaMax;

	@OriginalMember(owner = "client!gi", name = "e", descriptor = "I")
	private int flickerType;

	@OriginalMember(owner = "client!gi", name = "i", descriptor = "I")
	public int z;

	@OriginalMember(owner = "client!gi", name = "j", descriptor = "I")
	public final int radius;

	@OriginalMember(owner = "client!gi", name = "p", descriptor = "I")
	private int alphaMin;

	@OriginalMember(owner = "client!gi", name = "q", descriptor = "I")
	public int x;

	@OriginalMember(owner = "client!gi", name = "r", descriptor = "I")
	public int level;

	@OriginalMember(owner = "client!gi", name = "t", descriptor = "I")
	public final int animationPreset;

	@OriginalMember(owner = "client!gi", name = "u", descriptor = "[S")
	public final short[] spanProfile;

	@OriginalMember(owner = "client!gi", name = "w", descriptor = "F")
	public float alpha;

	@OriginalMember(owner = "client!gi", name = "x", descriptor = "I")
	public int y;

	@OriginalMember(owner = "client!gi", name = "y", descriptor = "I")
	private int flickerSpeed;

	@OriginalMember(owner = "client!gi", name = "z", descriptor = "F")
	public float quadraticAttenuation;

	@OriginalMember(owner = "client!gi", name = "A", descriptor = "Z")
	public final boolean extendsUp;

	@OriginalMember(owner = "client!gi", name = "B", descriptor = "Lclient!fj;")
	public LightMesh mesh;

	@OriginalMember(owner = "client!gi", name = "C", descriptor = "I")
	public final int color;

	@OriginalMember(owner = "client!gi", name = "E", descriptor = "I")
	private final int animationPhase;

	@OriginalMember(owner = "client!gi", name = "g", descriptor = "Z")
	public boolean onBridge = false;

	public boolean matchesStaticLightOverride = false;

	@OriginalMember(owner = "client!gi", name = "I", descriptor = "[F")
	public final float[] diffuse = new float[4];

	@OriginalMember(owner = "client!gi", name = "<init>", descriptor = "(Lclient!wa;)V")
	public Light(@OriginalArg(0) Buffer arg0) {
		if (NOISE == null) {
			init();
		}
		this.level = arg0.g1();
		this.extendsDown = (this.level & 0x10) != 0;
		this.extendsUp = (this.level & 0x8) != 0;
		this.level &= 0x7;
		this.x = arg0.g2();
		this.y = arg0.g2();
		this.z = arg0.g2();
		this.radius = arg0.g1();
		this.computeAttenuation();
		this.spanProfile = new short[this.radius * 2 + 1];
		@Pc(87) int local87;
		for (local87 = 0; local87 < this.spanProfile.length; local87++) {
			this.spanProfile[local87] = (short) arg0.g2();
		}
		this.color = Rasteriser.palette[arg0.g2()];
		local87 = arg0.g1();
		this.animationPhase = (local87 & 0xE0) << 3;
		this.animationPreset = local87 & 0x1F;
		if (this.animationPreset != 31) {
			this.initAnimationPreset();
		}
	}

	@OriginalMember(owner = "client!gk", name = "b", descriptor = "(B)V")
	public static void init() {
		NOISE = craeteNoise(0.4F);
	}

	@OriginalMember(owner = "client!qk", name = "a", descriptor = "(ZIIIIFII)[I")
	public static int[] craeteNoise(@OriginalArg(5) float arg0) {
		@Pc(11) int[] local11 = new int[2048];
		@Pc(15) TextureOp34 local15 = new TextureOp34();
		local15.anInt646 = 8;
		local15.anInt642 = 4;
		local15.anInt650 = 35;
		local15.anInt641 = 8;
		local15.anInt648 = (int) (arg0 * 4096.0F);
		local15.aBoolean44 = true;
		local15.postDecode();
		Texture.setSize(1, 2048);
		local15.getNoiseRow(0, local11);
		return local11;
	}

	@OriginalMember(owner = "client!gi", name = "a", descriptor = "(BIIII)V")
	public final void setFlickerParams(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3) {
		this.flickerType = arg0;
		this.alphaMin = arg2;
		this.alphaMax = arg3;
		this.flickerSpeed = arg1;
	}

	@OriginalMember(owner = "client!gi", name = "a", descriptor = "(I)V")
	private void computeAttenuation() {
		@Pc(10) int local10 = (this.radius << 7) + 64;
		this.quadraticAttenuation = 1.0F / (float) (local10 * local10);
	}

	@OriginalMember(owner = "client!gi", name = "a", descriptor = "(ZII)V")
	public final void updateAnimation(@OriginalArg(0) boolean disableFlicker, @OriginalArg(1) int arg1) {
		@Pc(26) int t = this.animationPhase + arg1 * this.flickerSpeed / 50 & 0x7FF;
		@Pc(29) int local29 = this.flickerType;
		@Pc(62) int alpha;
		if (local29 == 1) {
			alpha = (MathUtils.sin[t] >> 6) + 1024;
		} else if (local29 == 3) {
			alpha = NOISE[t] >> 1;
		} else if (local29 == 4) {
			alpha = t >> 10 << 11;
		} else if (local29 == 2) {
			alpha = t;
		} else if (local29 == 5) {
			alpha = (t < 1024 ? t : 2048 - t) << 1;
		} else {
			alpha = 2048;
		}
		if (disableFlicker || matchesStaticLightOverride) {
			alpha = 2048;
		}
		this.alpha = (float) (this.alphaMax + (alpha * this.alphaMin >> 11)) / 2048.0F;
		@Pc(123) float local123 = this.alpha / 255.0F;
		this.diffuse[0] = (float) (this.color >> 16 & 0xFF) * local123;
		this.diffuse[2] = local123 * (float) (this.color & 0xFF);
		this.diffuse[1] = (float) (this.color >> 8 & 0xFF) * local123;
	}

	@OriginalMember(owner = "client!gi", name = "c", descriptor = "(I)V")
	private void initAnimationPreset() {
		@Pc(4) int local4 = this.animationPreset;
		if (local4 == 2) {
			this.alphaMin = 2048;
			this.alphaMax = 0;
			this.flickerType = 1;
			this.flickerSpeed = 2048;
		} else if (local4 == 3) {
			this.alphaMax = 0;
			this.flickerSpeed = 4096;
			this.flickerType = 1;
			this.alphaMin = 2048;
		} else if (local4 == 4) {
			this.alphaMax = 0;
			this.alphaMin = 2048;
			this.flickerType = 4;
			this.flickerSpeed = 2048;
		} else if (local4 == 5) {
			this.flickerType = 4;
			this.alphaMin = 2048;
			this.flickerSpeed = 8192;
			this.alphaMax = 0;
		} else if (local4 == 12) {
			this.alphaMin = 2048;
			this.flickerType = 2;
			this.flickerSpeed = 2048;
			this.alphaMax = 0;
		} else if (local4 == 13) {
			this.flickerSpeed = 8192;
			this.alphaMin = 2048;
			this.flickerType = 2;
			this.alphaMax = 0;
		} else if (local4 == 10) {
			this.alphaMin = 512;
			this.flickerType = 3;
			this.alphaMax = 1536;
			this.flickerSpeed = 2048;
		} else if (local4 == 11) {
			this.flickerType = 3;
			this.flickerSpeed = 4096;
			this.alphaMin = 512;
			this.alphaMax = 1536;
		} else if (local4 == 6) {
			this.alphaMin = 768;
			this.alphaMax = 1280;
			this.flickerType = 3;
			this.flickerSpeed = 2048;
		} else if (local4 == 7) {
			this.alphaMin = 768;
			this.alphaMax = 1280;
			this.flickerSpeed = 4096;
			this.flickerType = 3;
		} else if (local4 == 8) {
			this.flickerSpeed = 2048;
			this.flickerType = 3;
			this.alphaMin = 1024;
			this.alphaMax = 1024;
		} else if (local4 == 9) {
			this.flickerSpeed = 4096;
			this.alphaMax = 1024;
			this.alphaMin = 1024;
			this.flickerType = 3;
		} else if (local4 == 14) {
			this.flickerSpeed = 2048;
			this.alphaMax = 1280;
			this.flickerType = 1;
			this.alphaMin = 768;
		} else if (local4 == 15) {
			this.alphaMin = 512;
			this.flickerSpeed = 4096;
			this.alphaMax = 1536;
			this.flickerType = 1;
		} else if (local4 == 16) {
			this.flickerSpeed = 8192;
			this.alphaMax = 1792;
			this.flickerType = 1;
			this.alphaMin = 256;
		} else {
			this.flickerSpeed = 2048;
			this.alphaMax = 0;
			this.alphaMin = 2048;
			this.flickerType = 0;
		}
	}
}
