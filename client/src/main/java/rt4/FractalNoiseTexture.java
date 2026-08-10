package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!sb")
public final class FractalNoiseTexture extends FractalNoiseGenerator {

	@OriginalMember(owner = "client!sb", name = "I", descriptor = "[B")
	private byte[] textureData;

	@OriginalMember(owner = "client!sb", name = "<init>", descriptor = "()V")
	public FractalNoiseTexture() {
		super(12, 5, 16, 2, 2, 0.45F);
	}

	@OriginalMember(owner = "client!sb", name = "a", descriptor = "(IB)V")
	@Override
	protected final void writeSample(@OriginalArg(0) int arg0, @OriginalArg(1) byte arg1) {
		@Pc(3) int local3 = arg0 * 2;
		@Pc(12) byte local12 = (byte) ((arg1 >> 1 & 0x7F) + 127);
		@Pc(15) int local15 = local3;
		@Pc(16) int local16 = local3 + 1;
		this.textureData[local15] = local12;
		this.textureData[local16] = local12;
	}

	@OriginalMember(owner = "client!sb", name = "b", descriptor = "(III)[B")
	public final byte[] generateTexture() {
		this.textureData = new byte[524288];
		this.generate();
		return this.textureData;
	}
}
