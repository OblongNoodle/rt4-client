package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!se")
public final class MapElementList {

	@OriginalMember(owner = "client!se", name = "i", descriptor = "I")
	public final int count;

	@OriginalMember(owner = "client!se", name = "j", descriptor = "[Lclient!na;")
	public final JagString[] names;

	@OriginalMember(owner = "client!se", name = "d", descriptor = "[S")
	public final short[] coordY;

	@OriginalMember(owner = "client!se", name = "n", descriptor = "[I")
	public final int[] colors;

	@OriginalMember(owner = "client!se", name = "u", descriptor = "[B")
	public final byte[] flags;

	@OriginalMember(owner = "client!se", name = "q", descriptor = "[S")
	public final short[] coordX;

	@OriginalMember(owner = "client!se", name = "<init>", descriptor = "(I)V")
	public MapElementList(@OriginalArg(0) int arg0) {
		this.count = arg0;
		this.names = new JagString[this.count];
		this.coordY = new short[this.count];
		this.colors = new int[this.count];
		this.flags = new byte[this.count];
		this.coordX = new short[this.count];
	}

	@OriginalMember(owner = "client!la", name = "a", descriptor = "(ILclient!na;Lclient!ve;)Lclient!se;")
	public static MapElementList create(@OriginalArg(1) JagString arg0, @OriginalArg(2) Js5 arg1) {
		@Pc(10) int local10 = arg1.getGroupId(arg0);
		if (local10 == -1) {
			return new MapElementList(0);
		}
		@Pc(29) int[] local29 = arg1.getFileIds(local10);
		@Pc(35) MapElementList local35 = new MapElementList(local29.length);
		for (@Pc(37) int local37 = 0; local37 < local35.count; local37++) {
			@Pc(56) Buffer local56 = new Buffer(arg1.fetchFile(local10, local29[local37]));
			local35.names[local37] = local56.gjstr();
			local35.flags[local37] = local56.g1b();
			local35.coordX[local37] = (short) local56.g2();
			local35.coordY[local37] = (short) local56.g2();
			local35.colors[local37] = local56.g4();
		}
		return local35;
	}

	@OriginalMember(owner = "client!se", name = "a", descriptor = "(IB)Z")
	public final boolean isTextLabel(@OriginalArg(0) int arg0) {
		return (this.flags[arg0] & 0x8) != 0;
	}

	@OriginalMember(owner = "client!se", name = "a", descriptor = "(II)Z")
	public final boolean isMinimapLabelVisible(@OriginalArg(0) int arg0) {
		return (this.flags[arg0] & 0x4) != 0;
	}

	@OriginalMember(owner = "client!se", name = "b", descriptor = "(II)I")
	public final int getLabelSize(@OriginalArg(0) int arg0) {
		return this.flags[arg0] & 0x3;
	}

	@OriginalMember(owner = "client!se", name = "c", descriptor = "(II)Z")
	public final boolean isVisible(@OriginalArg(0) int arg0) {
		return (this.flags[arg0] & 0x10) == 0;
	}
}
