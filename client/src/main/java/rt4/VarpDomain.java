package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;
import plugin.PluginRepository;

public class VarpDomain {
	@OriginalMember(owner = "client!gj", name = "q", descriptor = "[I")
	public static final int[] updatedVarps = new int[32];
	@OriginalMember(owner = "client!ic", name = "e", descriptor = "[I")
	public static final int[] varp = new int[3500];
	@OriginalMember(owner = "client!ah", name = "j", descriptor = "[I")
	public static final int[] activeVarps = new int[3500];
	@OriginalMember(owner = "client!uj", name = "s", descriptor = "Lclient!na;")
	public static final JagString NULL_STRING = JagString.parse("null");
	@OriginalMember(owner = "client!ea", name = "s", descriptor = "[I")
	public static final int[] BIT_MASKS = new int[32];
	@OriginalMember(owner = "client!fi", name = "n", descriptor = "I")
	public static int updatedVarpsWriterIndex = 0;
	@OriginalMember(owner = "client!qc", name = "K", descriptor = "Lclient!sc;")
	public static HashTable pendingVarpTimers = new HashTable(16);
	@OriginalMember(owner = "client!ge", name = "m", descriptor = "I")
	public static int chatEffectsDisabled = 0;
	@OriginalMember(owner = "client!jb", name = "n", descriptor = "I")
	public static int mouseButtons = 0;
	@OriginalMember(owner = "client!oe", name = "b", descriptor = "I")
	public static int inserting = 0;

	static {
		@Pc(10) int local10 = 2;
		for (@Pc(12) int local12 = 0; local12 < 32; local12++) {
			BIT_MASKS[local12] = local10 - 1;
			local10 += local10;
		}
	}

	@OriginalMember(owner = "client!nh", name = "a", descriptor = "(BII)V")
	public static void set(@OriginalArg(1) int value, @OriginalArg(2) int id) {
		PluginRepository.OnVarpUpdate(id, value);

		if (id > varp.length) return;

		varp[id] = value;
		@Pc(20) LongNode local20 = (LongNode) pendingVarpTimers.get(id);
		if (local20 == null) {
			local20 = new LongNode(4611686018427387905L);
			pendingVarpTimers.put(local20, id);
		} else if (local20.value != 4611686018427387905L) {
			local20.value = MonotonicClock.currentTimeMillis() + 500L | 0x4000000000000000L;
		}
	}

	@OriginalMember(owner = "client!aj", name = "i", descriptor = "(I)V")
	public static void reset() {
		for (@Pc(3) int local3 = 0; local3 < VarpTypeList.count; local3++) {
			@Pc(19) VarpType local19 = VarpTypeList.get(local3);
			if (local19 != null && local19.clientCode == 0) {
				varp[local3] = 0;
				activeVarps[local3] = 0;
			}
		}
		pendingVarpTimers = new HashTable(16);
	}

	@OriginalMember(owner = "client!li", name = "a", descriptor = "(III)V")
	public static void setVarp(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1) {
		activeVarps[arg0] = arg1;
		@Pc(21) LongNode local21 = (LongNode) pendingVarpTimers.get(arg0);
		if (local21 == null) {
			local21 = new LongNode(MonotonicClock.currentTimeMillis() + 500L);
			pendingVarpTimers.put(local21, arg0);
		} else {
			local21.value = MonotonicClock.currentTimeMillis() + 500L;
		}
	}

	@OriginalMember(owner = "client!me", name = "a", descriptor = "(II)I")
	public static int getVarbit(@OriginalArg(1) int arg0) {
		@Pc(13) VarbitType local13 = VarbitTypeList.get(arg0);
		@Pc(16) int local16 = local13.baseVar;
		@Pc(19) int local19 = local13.endBit;
		@Pc(22) int local22 = local13.startBit;
		@Pc(29) int local29 = BIT_MASKS[local19 - local22];
		return activeVarps[local16] >> local22 & local29;
	}

	@OriginalMember(owner = "client!qg", name = "a", descriptor = "(IZI)V")
	public static void setVarbitClient(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1) {
		@Pc(7) VarbitType local7 = VarbitTypeList.get(arg0);
		@Pc(10) int local10 = local7.endBit;
		@Pc(16) int local16 = local7.startBit;
		@Pc(19) int local19 = local7.baseVar;
		@Pc(25) int local25 = BIT_MASKS[local10 - local16];
		if (arg1 < 0 || arg1 > local25) {
			arg1 = 0;
		}
		local25 <<= local16;
		setVarp(local19, local25 & arg1 << local16 | activeVarps[local19] & ~local25);
	}

	@OriginalMember(owner = "client!wd", name = "a", descriptor = "(BII)V")
	public static void setVarbit(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		@Pc(14) VarbitType local14 = VarbitTypeList.get(arg1);
		@Pc(17) int local17 = local14.baseVar;
		@Pc(20) int local20 = local14.endBit;
		@Pc(23) int local23 = local14.startBit;
		@Pc(29) int local29 = BIT_MASKS[local20 - local23];
		if (arg0 < 0 || local29 < arg0) {
			arg0 = 0;
		}
		local29 <<= local23;
		set(arg0 << local23 & local29 | ~local29 & varp[local17], local17);
	}

	@OriginalMember(owner = "client!gl", name = "a", descriptor = "(II)V")
	public static void refreshMagicVarp(@OriginalArg(1) int arg0) {
		InterfaceList.redrawActiveInterfaces();
		AreaSoundManager.updateMulti();
		@Pc(17) int local17 = VarpTypeList.get(arg0).clientCode;
		if (local17 == 0) {
			return;
		}
		@Pc(25) int local25 = activeVarps[arg0];
		if (local17 == 6) {
			chatEffectsDisabled = local25;
		}
		if (local17 == 5) {
			mouseButtons = local25;
		}
		if (local17 == 9) {
			inserting = local25;
		}
	}

	@OriginalMember(owner = "client!cn", name = "a", descriptor = "(ZI)I")
	public static int poll(@OriginalArg(0) boolean arg0) {
		@Pc(4) long local4 = MonotonicClock.currentTimeMillis();
		for (@Pc(28) LongNode local28 = arg0 ? (LongNode) pendingVarpTimers.head() : (LongNode) pendingVarpTimers.next(); local28 != null; local28 = (LongNode) pendingVarpTimers.next()) {
			if ((local28.value & 0x3FFFFFFFFFFFFFFFL) < local4) {
				if ((local28.value & 0x4000000000000000L) != 0L) {
					@Pc(58) int local58 = (int) local28.key;
					activeVarps[local58] = varp[local58];
					local28.unlink();
					return local58;
				}
				local28.unlink();
			}
		}
		return -1;
	}
}
