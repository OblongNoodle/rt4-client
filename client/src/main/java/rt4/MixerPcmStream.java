package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!ei")
public final class MixerPcmStream extends PcmStream {

	@OriginalMember(owner = "client!ei", name = "t", descriptor = "Lclient!ih;")
	private final LinkedList aClass69_43 = new LinkedList();

	@OriginalMember(owner = "client!ei", name = "u", descriptor = "Lclient!ih;")
	private final LinkedList aClass69_44 = new LinkedList();

	@OriginalMember(owner = "client!ei", name = "v", descriptor = "I")
	private int anInt1780 = 0;

	@OriginalMember(owner = "client!ei", name = "w", descriptor = "I")
	private int anInt1781 = -1;

	@OriginalMember(owner = "client!ok", name = "a", descriptor = "(Lclient!ab;Lclient!ab;I)V")
	public static void insertBefore(@OriginalArg(0) Node arg0, @OriginalArg(1) Node arg1) {
		if (arg0.previousNode != null) {
			arg0.unlink();
		}
		arg0.nextNode = arg1;
		arg0.previousNode = arg1.previousNode;
		arg0.previousNode.nextNode = arg0;
		arg0.nextNode.previousNode = arg0;
	}

	@OriginalMember(owner = "client!ei", name = "a", descriptor = "(Lclient!cc;)V")
	private void removeListener(@OriginalArg(0) MixerListener arg0) {
		arg0.unlink();
		arg0.onRemoved();
		@Pc(9) Node local9 = this.aClass69_44.sentinel.nextNode;
		if (local9 == this.aClass69_44.sentinel) {
			this.anInt1781 = -1;
		} else {
			this.anInt1781 = ((MixerListener) local9).anInt905;
		}
	}

	@OriginalMember(owner = "client!ei", name = "a", descriptor = "(Lclient!qb;)V")
	public final synchronized void addSubStream(@OriginalArg(0) PcmStream arg0) {
		this.aClass69_43.addHead(arg0);
	}

	@OriginalMember(owner = "client!ei", name = "b", descriptor = "([III)V")
	@Override
	public final synchronized void read(@OriginalArg(0) int[] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		do {
			if (this.anInt1781 < 0) {
				this.readSubStreams(arg0, arg1, arg2);
				return;
			}
			if (this.anInt1780 + arg2 < this.anInt1781) {
				this.anInt1780 += arg2;
				this.readSubStreams(arg0, arg1, arg2);
				return;
			}
			@Pc(33) int local33 = this.anInt1781 - this.anInt1780;
			this.readSubStreams(arg0, arg1, local33);
			arg1 += local33;
			arg2 -= local33;
			this.anInt1780 += local33;
			this.advanceListeners();
			@Pc(60) MixerListener local60 = (MixerListener) this.aClass69_44.head();
			synchronized (local60) {
				@Pc(68) int local68 = local60.process(this);
				if (local68 < 0) {
					local60.anInt905 = 0;
					this.removeListener(local60);
				} else {
					local60.anInt905 = local68;
					this.insertListenerSorted(local60.nextNode, local60);
				}
			}
		} while (arg2 != 0);
	}

	@OriginalMember(owner = "client!ei", name = "e", descriptor = "()V")
	private void advanceListeners() {
		if (this.anInt1780 <= 0) {
			return;
		}
		for (@Pc(8) MixerListener local8 = (MixerListener) this.aClass69_44.head(); local8 != null; local8 = (MixerListener) this.aClass69_44.next()) {
			local8.anInt905 -= this.anInt1780;
		}
		this.anInt1781 -= this.anInt1780;
		this.anInt1780 = 0;
	}

	@OriginalMember(owner = "client!ei", name = "b", descriptor = "()Lclient!qb;")
	@Override
	public final PcmStream firstSubStream() {
		return (PcmStream) this.aClass69_43.head();
	}

	@OriginalMember(owner = "client!ei", name = "d", descriptor = "(I)V")
	private void skipSubStreams(@OriginalArg(0) int arg0) {
		for (@Pc(5) PcmStream local5 = (PcmStream) this.aClass69_43.head(); local5 != null; local5 = (PcmStream) this.aClass69_43.next()) {
			local5.skip(arg0);
		}
	}

	@OriginalMember(owner = "client!ei", name = "c", descriptor = "([III)V")
	private void readSubStreams(@OriginalArg(0) int[] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		for (@Pc(5) PcmStream local5 = (PcmStream) this.aClass69_43.head(); local5 != null; local5 = (PcmStream) this.aClass69_43.next()) {
			local5.readIfActive(arg0, arg1, arg2);
		}
	}

	@OriginalMember(owner = "client!ei", name = "b", descriptor = "(Lclient!qb;)V")
	public final synchronized void removeSubStream(@OriginalArg(0) PcmStream arg0) {
		arg0.unlink();
	}

	@OriginalMember(owner = "client!ei", name = "a", descriptor = "()I")
	@Override
	public final int getActiveChannelCount() {
		return 0;
	}

	@OriginalMember(owner = "client!ei", name = "c", descriptor = "(I)V")
	@Override
	public final synchronized void skip(@OriginalArg(0) int arg0) {
		do {
			if (this.anInt1781 < 0) {
				this.skipSubStreams(arg0);
				return;
			}
			if (this.anInt1780 + arg0 < this.anInt1781) {
				this.anInt1780 += arg0;
				this.skipSubStreams(arg0);
				return;
			}
			@Pc(29) int local29 = this.anInt1781 - this.anInt1780;
			this.skipSubStreams(local29);
			arg0 -= local29;
			this.anInt1780 += local29;
			this.advanceListeners();
			@Pc(50) MixerListener local50 = (MixerListener) this.aClass69_44.head();
			synchronized (local50) {
				@Pc(58) int local58 = local50.process(this);
				if (local58 < 0) {
					local50.anInt905 = 0;
					this.removeListener(local50);
				} else {
					local50.anInt905 = local58;
					this.insertListenerSorted(local50.nextNode, local50);
				}
			}
		} while (arg0 != 0);
	}

	@OriginalMember(owner = "client!ei", name = "d", descriptor = "()Lclient!qb;")
	@Override
	public final PcmStream nextSubStream() {
		return (PcmStream) this.aClass69_43.next();
	}

	@OriginalMember(owner = "client!ei", name = "a", descriptor = "(Lclient!ab;Lclient!cc;)V")
	private void insertListenerSorted(@OriginalArg(0) Node arg0, @OriginalArg(1) MixerListener arg1) {
		while (arg0 != this.aClass69_44.sentinel && ((MixerListener) arg0).anInt905 <= arg1.anInt905) {
			arg0 = arg0.nextNode;
		}
		insertBefore(arg1, arg0);
		this.anInt1781 = ((MixerListener) this.aClass69_44.sentinel.nextNode).anInt905;
	}
}
