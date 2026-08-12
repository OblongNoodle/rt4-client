package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!ic")
public final class LightType {

	@OriginalMember(owner = "client!ic", name = "g", descriptor = "I")
	public int alphaMin = 2048;

	@OriginalMember(owner = "client!ic", name = "c", descriptor = "I")
	public int flickerType = 0;

	@OriginalMember(owner = "client!ic", name = "o", descriptor = "I")
	public int alphaMax = 0;

	@OriginalMember(owner = "client!ic", name = "p", descriptor = "I")
	public int flickerSpeed = 2048;

	@OriginalMember(owner = "client!ic", name = "a", descriptor = "(ILclient!wa;I)V")
	public final void decode(@OriginalArg(1) Buffer arg0, @OriginalArg(2) int arg1) {
		while (true) {
			@Pc(5) int local5 = arg0.g1();
			if (local5 == 0) {
				return;
			}
			this.decode(local5, arg0, arg1);
		}
	}

	@OriginalMember(owner = "client!ic", name = "a", descriptor = "(ILclient!wa;IZ)V")
	private void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1, @OriginalArg(2) int arg2) {
		if (arg0 == 1) {
			this.flickerType = arg1.g1();
		} else if (arg0 == 2) {
			this.flickerSpeed = arg1.g2();
		} else if (arg0 == 3) {
			this.alphaMin = arg1.g2();
		} else if (arg0 == 4) {
			this.alphaMax = arg1.g2b();
		}
	}
}
