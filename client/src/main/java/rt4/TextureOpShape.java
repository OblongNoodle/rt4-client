package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;

@OriginalClass("client!kf")
public abstract class TextureOpShape {

	@OriginalMember(owner = "client!kf", name = "e", descriptor = "I")
	public final int outlineColor;

	@OriginalMember(owner = "client!kf", name = "g", descriptor = "I")
	protected final int lineWidth;

	@OriginalMember(owner = "client!kf", name = "b", descriptor = "I")
	public final int fillColor;

	@OriginalMember(owner = "client!kf", name = "<init>", descriptor = "(III)V")
	protected TextureOpShape(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		this.outlineColor = arg1;
		this.lineWidth = arg2;
		this.fillColor = arg0;
	}

	@OriginalMember(owner = "client!kf", name = "a", descriptor = "(III)V")
	public abstract void renderBorderedShape(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1);

	@OriginalMember(owner = "client!kf", name = "a", descriptor = "(IZI)V")
	public abstract void renderOutlinedShape(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1);

	@OriginalMember(owner = "client!kf", name = "c", descriptor = "(III)V")
	public abstract void renderFilledShape(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1);
}
