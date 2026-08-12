package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!te")
public final class MidiNoteStream extends PcmStream {

	@OriginalMember(owner = "client!te", name = "w", descriptor = "Lclient!ih;")
	public final LinkedList notes = new LinkedList();

	@OriginalMember(owner = "client!te", name = "K", descriptor = "Lclient!ei;")
	public final MixerPcmStream mixer = new MixerPcmStream();

	@OriginalMember(owner = "client!te", name = "E", descriptor = "Lclient!va;")
	private final MidiPcmStream parent;

	@OriginalMember(owner = "client!te", name = "<init>", descriptor = "(Lclient!va;)V")
	public MidiNoteStream(@OriginalArg(0) MidiPcmStream parent) {
		this.parent = parent;
	}

	@OriginalMember(owner = "client!te", name = "d", descriptor = "()Lclient!qb;")
	@Override
	public final PcmStream nextSubStream() {
		@Pc(9) MidiNote note;
		do {
			note = (MidiNote) this.notes.next();
			if (note == null) {
				return null;
			}
		} while (note.stream == null);
		return note.stream;
	}

	@OriginalMember(owner = "client!te", name = "b", descriptor = "()Lclient!qb;")
	@Override
	public final PcmStream firstSubStream() {
		@Pc(9) MidiNote note = (MidiNote) this.notes.head();
		if (note == null) {
			return null;
		} else if (note.stream == null) {
			return this.nextSubStream();
		} else {
			return note.stream;
		}
	}

	@OriginalMember(owner = "client!te", name = "a", descriptor = "(Lclient!mf;II)V")
	private void skip(@OriginalArg(0) MidiNote instrument, @OriginalArg(2) int len) {
		if ((this.parent.channelFlags[instrument.channel] & 0x4) != 0 && instrument.releaseTimer < 0) {
			@Pc(27) int local27 = this.parent.channelDetuneRatio[instrument.channel] / AudioChannel.sampleRate;
			@Pc(37) int local37 = (local27 + 1048575 - instrument.portamentoPos) / local27;
			instrument.portamentoPos = local27 * len + instrument.portamentoPos & 0xFFFFF;
			if (len >= local37) {
				if (this.parent.channelSampleOffset[instrument.channel] == 0) {
					instrument.stream = SoundPcmStream.create(instrument.sound, instrument.stream.getAbsoluteRate(), instrument.stream.getVolume(), instrument.stream.getPan());
				} else {
					instrument.stream = SoundPcmStream.create(instrument.sound, instrument.stream.getAbsoluteRate(), 0, instrument.stream.getPan());
					this.parent.applyLoopStartOffset(instrument, instrument.instrument.notePitchOffset[instrument.noteKey] < 0);
				}
				if (instrument.instrument.notePitchOffset[instrument.noteKey] < 0) {
					instrument.stream.setLoops(-1);
				}
				len = instrument.portamentoPos / local27;
			}
		}
		instrument.stream.skip(len);
	}

	@OriginalMember(owner = "client!te", name = "a", descriptor = "()I")
	@Override
	public final int getActiveChannelCount() {
		return 0;
	}

	@OriginalMember(owner = "client!te", name = "c", descriptor = "(I)V")
	@Override
	public final void skip(@OriginalArg(0) int len) {
		this.mixer.skip(len);
		for (@Pc(15) MidiNote note = (MidiNote) this.notes.head(); note != null; note = (MidiNote) this.notes.next()) {
			if (!this.parent.isNoteFinished(note)) {
				@Pc(27) int len2 = len;
				do {
					if (len2 <= note.retriggerDelay) {
						this.skip(note, len2);
						note.retriggerDelay -= len2;
						break;
					}
					this.skip(note, note.retriggerDelay);
					len2 -= note.retriggerDelay;
				} while (!this.parent.advanceNoteEnvelope(len2, 0, note, null));
			}
		}
	}

	@OriginalMember(owner = "client!te", name = "b", descriptor = "([III)V")
	@Override
	public final void read(@OriginalArg(0) int[] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		this.mixer.read(arg0, arg1, arg2);
		for (@Pc(17) MidiNote local17 = (MidiNote) this.notes.head(); local17 != null; local17 = (MidiNote) this.notes.next()) {
			if (!this.parent.isNoteFinished(local17)) {
				@Pc(29) int local29 = arg2;
				@Pc(31) int local31 = arg1;
				do {
					if (local29 <= local17.retriggerDelay) {
						this.readNoteWithRetrigger(arg0, local17, local31, local29, local31 + local29);
						local17.retriggerDelay -= local29;
						break;
					}
					this.readNoteWithRetrigger(arg0, local17, local31, local17.retriggerDelay, local29 + local31);
					local29 -= local17.retriggerDelay;
					local31 += local17.retriggerDelay;
				} while (!this.parent.advanceNoteEnvelope(local29, local31, local17, arg0));
			}
		}
	}

	@OriginalMember(owner = "client!te", name = "a", descriptor = "([ILclient!mf;IIIB)V")
	private void readNoteWithRetrigger(@OriginalArg(0) int[] arg0, @OriginalArg(1) MidiNote arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
		if ((this.parent.channelFlags[arg1.channel] & 0x4) != 0 && arg1.releaseTimer < 0) {
			@Pc(26) int local26 = this.parent.channelDetuneRatio[arg1.channel] / AudioChannel.sampleRate;
			while (true) {
				@Pc(36) int local36 = (local26 + 1048575 - arg1.portamentoPos) / local26;
				if (arg3 < local36) {
					arg1.portamentoPos += arg3 * local26;
					break;
				}
				arg3 -= local36;
				arg1.stream.read(arg0, arg2, local36);
				@Pc(55) int local55 = AudioChannel.sampleRate / 100;
				@Pc(58) SoundPcmStream local58 = arg1.stream;
				@Pc(62) int local62 = 262144 / local26;
				if (local62 < local55) {
					local55 = local62;
				}
				arg1.portamentoPos += local26 * local36 - 1048576;
				if (this.parent.channelSampleOffset[arg1.channel] == 0) {
					arg1.stream = SoundPcmStream.create(arg1.sound, local58.getAbsoluteRate(), local58.getVolume(), local58.getPan());
				} else {
					arg1.stream = SoundPcmStream.create(arg1.sound, local58.getAbsoluteRate(), 0, local58.getPan());
					this.parent.applyLoopStartOffset(arg1, arg1.instrument.notePitchOffset[arg1.noteKey] < 0);
					arg1.stream.fadeToVolume(local55, local58.getVolume());
				}
				if (arg1.instrument.notePitchOffset[arg1.noteKey] < 0) {
					arg1.stream.setLoops(-1);
				}
				arg2 += local36;
				local58.fadeOutAndRelease(local55);
				local58.read(arg0, arg2, arg4 - arg2);
				if (local58.isTransitioning()) {
					this.mixer.addSubStream(local58);
				}
			}
		}
		arg1.stream.read(arg0, arg2, arg3);
	}
}
