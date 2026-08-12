package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

public class StringUtils {
	@OriginalMember(owner = "client!ag", name = "ab", descriptor = "Lclient!na;")
	public static final JagString ZERO_STRING = JagString.parse("0");
	@OriginalMember(owner = "client!ca", name = "cb", descriptor = "Lclient!na;")
	public static final JagString nonBreakingSpace = JagString.getNbsp();
	@OriginalMember(owner = "client!rm", name = "i", descriptor = "Lclient!na;")
	public static final JagString ASTERISK = JagString.parse("(Z");
	@OriginalMember(owner = "client!fm", name = "W", descriptor = "Lclient!na;")
	public static final JagString MINUS_SIGN = JagString.parse(")2");
	@OriginalMember(owner = "client!cg", name = "h", descriptor = "Lclient!na;")
	public static final JagString PERIOD = JagString.parse(")3");
	@OriginalMember(owner = "client!tl", name = "i", descriptor = "Lclient!na;")
	public static final JagString COMMA = JagString.parse(")1");

	@OriginalMember(owner = "client!vf", name = "a", descriptor = "(IB)Lclient!na;")
	public static JagString toString(@OriginalArg(0) int arg0) {
		return arg0 >= 999999999 ? ASTERISK : JagString.parseInt(arg0);
	}

	@OriginalMember(owner = "client!oj", name = "a", descriptor = "(IZIJI)Lclient!na;")
	public static JagString formatNumber(@OriginalArg(0) int arg0, @OriginalArg(1) boolean arg1, @OriginalArg(2) int arg2, @OriginalArg(3) long arg3) {
		@Pc(9) JagString local9 = JagString.allocate(0);
		if (arg3 < 0L) {
			arg3 = -arg3;
			local9.appendString(MINUS_SIGN);
		}
		@Pc(26) JagString local26 = COMMA;
		@Pc(28) JagString local28 = PERIOD;
		if (arg0 == 1) {
			local26 = PERIOD;
			local28 = COMMA;
		}
		if (arg0 == 2) {
			local28 = COMMA;
			local26 = nonBreakingSpace;
		}
		if (arg0 == 3) {
			local26 = PERIOD;
			local28 = COMMA;
		}
		@Pc(59) JagString local59 = JagString.allocate(0);
		@Pc(61) int local61;
		for (local61 = 0; local61 < arg2; local61++) {
			local59.appendString(JagString.parseInt((int) (arg3 % 10L)));
			arg3 /= 10L;
		}
		local61 = 0;
		@Pc(137) JagString local137;
		if (arg3 == 0L) {
			local137 = ZERO_STRING;
		} else {
			@Pc(95) JagString local95 = JagString.allocate(0);
			while (arg3 > 0L) {
				if (arg1 && local61 != 0 && local61 % 3 == 0) {
					local95.appendString(local26);
				}
				local95.appendString(JagString.parseInt((int) (arg3 % 10L)));
				local61++;
				arg3 /= 10L;
			}
			local137 = local95;
		}
		if (local59.length() > 0) {
			local59.appendString(local28);
		}
		return JagString.concatenate(new JagString[]{local9, local137.reverse(), local59.reverse()});
	}
}
