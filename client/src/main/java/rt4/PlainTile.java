package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;

@OriginalClass("client!rh")
public final class PlainTile {

	@OriginalMember(owner = "client!rh", name = "m", descriptor = "Z")
	public boolean flat = true;

	@OriginalMember(owner = "client!rh", name = "f", descriptor = "I")
	public final int colorSE;

	@OriginalMember(owner = "client!rh", name = "c", descriptor = "I")
	public final int colorNE;

	@OriginalMember(owner = "client!rh", name = "n", descriptor = "I")
	public final int colorSW;

	@OriginalMember(owner = "client!rh", name = "l", descriptor = "I")
	public final int flatColor;

	@OriginalMember(owner = "client!rh", name = "b", descriptor = "I")
	public final int colorNW;

	@OriginalMember(owner = "client!rh", name = "i", descriptor = "I")
	public final int textureId;

	@OriginalMember(owner = "client!rh", name = "<init>", descriptor = "(IIIIIIZ)V")
	public PlainTile(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) boolean arg6) {
		this.colorSE = arg1;
		this.colorNE = arg2;
		this.colorSW = arg0;
		this.flatColor = arg5;
		this.flat = arg6;
		this.colorNW = arg3;
		this.textureId = arg4;
	}
}
