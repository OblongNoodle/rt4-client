package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!va")
public final class MidiPcmStream extends PcmStream {

	@OriginalMember(owner = "client!va", name = "Kb", descriptor = "Z")
	private boolean loop;

	@OriginalMember(owner = "client!va", name = "Lb", descriptor = "J")
	private long currentTimeMicros;

	@OriginalMember(owner = "client!va", name = "Mb", descriptor = "I")
	private int currentTick;

	@OriginalMember(owner = "client!va", name = "Nb", descriptor = "I")
	private int currentTrack;

	@OriginalMember(owner = "client!va", name = "Ob", descriptor = "J")
	private long elapsedTimeMicros;

	@OriginalMember(owner = "client!va", name = "Qb", descriptor = "Lclient!rf;")
	private Song nextSong;

	@OriginalMember(owner = "client!va", name = "Rb", descriptor = "I")
	private int crossfadeTick;

	@OriginalMember(owner = "client!va", name = "Sb", descriptor = "Z")
	private boolean crossfadeReset;

	@OriginalMember(owner = "client!va", name = "z", descriptor = "[I")
	private final int[] channelPan = new int[16];

	@OriginalMember(owner = "client!va", name = "D", descriptor = "[I")
	private final int[] defaultProgram = new int[16];

	@OriginalMember(owner = "client!va", name = "F", descriptor = "[I")
	private final int[] channelModulation = new int[16];

	@OriginalMember(owner = "client!va", name = "B", descriptor = "[I")
	private final int[] channelPitchBend = new int[16];

	@OriginalMember(owner = "client!va", name = "ab", descriptor = "I")
	private final int TEMPO_SCALE = 1000000;

	@OriginalMember(owner = "client!va", name = "cb", descriptor = "[[Lclient!mf;")
	private final MidiNote[][] exclusiveGroupNotes = new MidiNote[16][128];

	@OriginalMember(owner = "client!va", name = "kb", descriptor = "[I")
	private final int[] channelProgram = new int[16];

	@OriginalMember(owner = "client!va", name = "C", descriptor = "[I")
	private final int[] channelRpnNrpn = new int[16];

	@OriginalMember(owner = "client!va", name = "U", descriptor = "[I")
	public final int[] channelDetuneRatio = new int[16];

	@OriginalMember(owner = "client!va", name = "x", descriptor = "[I")
	private final int[] channelVolume = new int[16];

	@OriginalMember(owner = "client!va", name = "ib", descriptor = "[I")
	private final int[] channelExpression = new int[16];

	@OriginalMember(owner = "client!va", name = "tb", descriptor = "[I")
	public final int[] channelFlags = new int[16];

	@OriginalMember(owner = "client!va", name = "mb", descriptor = "[I")
	private final int[] channelVolumeScalar = new int[16];

	@OriginalMember(owner = "client!va", name = "Cb", descriptor = "[I")
	private final int[] channelDetuneValue = new int[16];

	@OriginalMember(owner = "client!va", name = "N", descriptor = "[I")
	private final int[] channelBankSelect = new int[16];

	@OriginalMember(owner = "client!va", name = "Bb", descriptor = "[I")
	public final int[] channelSampleOffset = new int[16];

	@OriginalMember(owner = "client!va", name = "bb", descriptor = "[[Lclient!mf;")
	private final MidiNote[][] activeNotes = new MidiNote[16][128];

	@OriginalMember(owner = "client!va", name = "Eb", descriptor = "I")
	private int volume = 256;

	@OriginalMember(owner = "client!va", name = "H", descriptor = "[I")
	private final int[] channelPitchBendRange = new int[16];

	@OriginalMember(owner = "client!va", name = "Z", descriptor = "[I")
	private final int[] channelPortamentoTime = new int[16];

	@OriginalMember(owner = "client!va", name = "K", descriptor = "Lclient!ki;")
	private final MidiDecoder midiDecoder = new MidiDecoder();

	@OriginalMember(owner = "client!va", name = "Pb", descriptor = "Lclient!te;")
	private final MidiNoteStream noteStream = new MidiNoteStream(this);

	@OriginalMember(owner = "client!va", name = "P", descriptor = "Lclient!sc;")
	private final HashTable instrumentCache = new HashTable(128);

	@OriginalMember(owner = "client!va", name = "<init>", descriptor = "()V")
	public MidiPcmStream() {
		this.initChannelVolumeScalars();
		this.stopAllNotes(true);
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(Lclient!rf;ILclient!ve;Lclient!le;I)Z")
	public final synchronized boolean isSongReady(@OriginalArg(0) Song arg0, @OriginalArg(2) Js5 arg1, @OriginalArg(3) SoundBank arg2) {
		arg0.createPrograms();
		@Pc(5) boolean local5 = true;
		@Pc(20) int[] local20 = new int[]{GlobalConfig.AUDIO_SAMPLE_RATE};
		for (@Pc(34) ByteArrayNode local34 = (ByteArrayNode) arg0.programs.head(); local34 != null; local34 = (ByteArrayNode) arg0.programs.next()) {
			@Pc(40) int local40 = (int) local34.key;
			@Pc(48) MidiInstrument local48 = (MidiInstrument) this.instrumentCache.get(local40);
			if (local48 == null) {
				local48 = MidiInstrument.loadInstrument(arg1, local40);
				if (local48 == null) {
					local5 = false;
					continue;
				}
				this.instrumentCache.put(local48, local40);
			}
			if (!local48.resolveSounds(local20, arg2, local34.value)) {
				local5 = false;
			}
		}
		if (local5) {
			arg0.releasePrograms();
		}
		return local5;
	}

	@OriginalMember(owner = "client!va", name = "d", descriptor = "(B)V")
	public final synchronized void releaseInstruments() {
		for (@Pc(15) MidiInstrument local15 = (MidiInstrument) this.instrumentCache.head(); local15 != null; local15 = (MidiInstrument) this.instrumentCache.next()) {
			local15.freeSoundTable();
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(III)V")
	private void resetPercussionChannel() {
		this.defaultProgram[9] = 128;
		this.channelBankSelect[9] = 128;
		this.programChange(128, 9);
	}

	@OriginalMember(owner = "client!va", name = "d", descriptor = "(I)Z")
	public final synchronized boolean isValid() {
		return this.midiDecoder.isValid();
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(ZLclient!rf;ZB)V")
	private synchronized void loadSong(@OriginalArg(0) boolean arg0, @OriginalArg(1) Song arg1, @OriginalArg(2) boolean arg2) {
		this.stopPlayback(arg2);
		this.midiDecoder.init(arg1.midiBytes);
		this.loop = arg0;
		this.elapsedTimeMicros = 0L;
		@Pc(24) int local24 = this.midiDecoder.getTrackCount();
		for (@Pc(26) int local26 = 0; local26 < local24; local26++) {
			this.midiDecoder.loadTrackPosition(local26);
			this.midiDecoder.addDeltaTime(local26);
			this.midiDecoder.saveTrackPosition(local26);
		}
		this.currentTrack = this.midiDecoder.getNextTrack();
		this.currentTick = this.midiDecoder.times[this.currentTrack];
		this.currentTimeMicros = this.midiDecoder.getTimeMillis(this.currentTick);
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(III)V")
	private void setPitchBend(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1) {
		this.channelPitchBend[arg0] = arg1;
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "()I")
	@Override
	public final synchronized int getActiveChannelCount() {
		return 0;
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(IZI)V")
	private void setChannelPressure(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1) {
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(BII)V")
	public final synchronized void init() {
		this.resetPercussionChannel();
	}

	@OriginalMember(owner = "client!va", name = "c", descriptor = "(II)V")
	private void resetAllControllers(@OriginalArg(1) int arg0) {
		if (arg0 < 0) {
			for (@Pc(10) int local10 = 0; local10 < 16; local10++) {
				this.resetAllControllers(local10);
			}
			return;
		}
		this.channelVolume[arg0] = 12800;
		this.channelPan[arg0] = 8192;
		this.channelExpression[arg0] = 16383;
		this.channelPitchBend[arg0] = 8192;
		this.channelModulation[arg0] = 0;
		this.channelPortamentoTime[arg0] = 8192;
		this.releaseOrphanedPortamentoNotes(arg0);
		this.releaseCustomHoldPedal(arg0);
		this.channelFlags[arg0] = 0;
		this.channelRpnNrpn[arg0] = 32767;
		this.channelPitchBendRange[arg0] = 256;
		this.channelSampleOffset[arg0] = 0;
		this.setDetune(arg0, 8192);
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(BI)V")
	private void allSoundOff(@OriginalArg(1) int arg0) {
		for (@Pc(20) MidiNote local20 = (MidiNote) this.noteStream.notes.head(); local20 != null; local20 = (MidiNote) this.noteStream.notes.next()) {
			if (arg0 < 0 || local20.channel == arg0) {
				if (local20.stream != null) {
					local20.stream.fadeOutAndRelease(AudioChannel.sampleRate / 100);
					if (local20.stream.isTransitioning()) {
						this.noteStream.mixer.addSubStream(local20.stream);
					}
					local20.release();
				}
				if (local20.releaseTimer < 0) {
					this.activeNotes[local20.channel][local20.noteKey] = null;
				}
				local20.unlink();
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(BII)V")
	private void setDetune(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		this.channelDetuneValue[arg0] = arg1;
		this.channelDetuneRatio[arg0] = (int) (Math.pow(2.0D, (double) arg1 * 5.4931640625E-4D) * 2097152.0D + 0.5D);
	}

	@OriginalMember(owner = "client!va", name = "c", descriptor = "(III)V")
	private synchronized void initChannelVolumeScalars() {
		for (@Pc(5) int local5 = 0; local5 < 16; local5++) {
			this.channelVolumeScalar[local5] = 256;
		}
	}

	@OriginalMember(owner = "client!va", name = "d", descriptor = "(III)V")
	private void programChange(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		if (this.channelProgram[arg1] != arg0) {
			this.channelProgram[arg1] = arg0;
			for (@Pc(21) int local21 = 0; local21 < 128; local21++) {
				this.exclusiveGroupNotes[arg1][local21] = null;
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "e", descriptor = "(I)V")
	public final synchronized void clearInstruments() {
		for (@Pc(7) MidiInstrument local7 = (MidiInstrument) this.instrumentCache.head(); local7 != null; local7 = (MidiInstrument) this.instrumentCache.next()) {
			local7.unlink();
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(IIII)V")
	private void noteOn(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2) {
		this.noteOff(arg2, 64, arg1);
		if ((this.channelFlags[arg1] & 0x2) != 0) {
			for (@Pc(28) MidiNote local28 = (MidiNote) this.noteStream.notes.tail(); local28 != null; local28 = (MidiNote) this.noteStream.notes.prev()) {
				if (arg1 == local28.channel && local28.releaseTimer < 0) {
					this.activeNotes[arg1][local28.noteKey] = null;
					this.activeNotes[arg1][arg2] = local28;
					@Pc(72) int local72 = local28.basePitch + (local28.pitchModifier * local28.pitchDelta >> 12);
					local28.pitchModifier = 4096;
					local28.basePitch += arg2 - local28.noteKey << 8;
					local28.pitchDelta = local72 - local28.basePitch;
					local28.noteKey = arg2;
					return;
				}
			}
		}
		@Pc(118) MidiInstrument local118 = (MidiInstrument) this.instrumentCache.get(this.channelProgram[arg1]);
		if (local118 == null) {
			return;
		}
		@Pc(126) PcmSound local126 = local118.sounds[arg2];
		if (local126 == null) {
			return;
		}
		@Pc(133) MidiNote local133 = new MidiNote();
		local133.sound = local126;
		local133.instrument = local118;
		local133.channel = arg1;
		local133.trackState = local118.trackStates[arg2];
		local133.exclusiveGroup = local118.noteExclusiveGroup[arg2];
		local133.noteKey = arg2;
		local133.volumeGain = local118.noteVolume[arg2] * arg0 * arg0 * local118.volumeMultiplier + 1024 >> 11;
		local133.pan = local118.notePan[arg2] & 0xFF;
		local133.basePitch = (arg2 << 8) - (local118.notePitchOffset[arg2] & 0x7FFF);
		local133.releaseTimer = -1;
		local133.attackEnvelopePos = 0;
		local133.releaseEnvelopeIndex = 0;
		local133.sustainTimer = 0;
		local133.attackEnvelopeIndex = 0;
		if (this.channelSampleOffset[arg1] == 0) {
			local133.stream = SoundPcmStream.create(local126, this.calculatePitch(local133), this.calculateVolume(local133), this.calculatePan(local133));
		} else {
			local133.stream = SoundPcmStream.create(local126, this.calculatePitch(local133), 0, this.calculatePan(local133));
			this.applyLoopStartOffset(local133, local118.notePitchOffset[arg2] < 0);
		}
		if (local118.notePitchOffset[arg2] < 0) {
			local133.stream.setLoops(-1);
		}
		if (local133.exclusiveGroup >= 0) {
			@Pc(289) MidiNote local289 = this.exclusiveGroupNotes[arg1][local133.exclusiveGroup];
			if (local289 != null && local289.releaseTimer < 0) {
				this.activeNotes[arg1][local289.noteKey] = null;
				local289.releaseTimer = 0;
			}
			this.exclusiveGroupNotes[arg1][local133.exclusiveGroup] = local133;
		}
		this.noteStream.notes.addTail(local133);
		this.activeNotes[arg1][arg2] = local133;
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(BI)V")
	private void dispatchMidiEvent(@OriginalArg(1) int arg0) {
		@Pc(9) int local9 = arg0 & 0xF0;
		@Pc(20) int local20;
		@Pc(32) int local32;
		@Pc(26) int local26;
		if (local9 == 128) {
			local20 = arg0 & 0xF;
			local26 = arg0 >> 16 & 0x7F;
			local32 = arg0 >> 8 & 0x7F;
			this.noteOff(local32, local26, local20);
		} else if (local9 == 144) {
			local32 = arg0 >> 8 & 0x7F;
			local20 = arg0 & 0xF;
			local26 = arg0 >> 16 & 0x7F;
			if (local26 > 0) {
				this.noteOn(local26, local20, local32);
			} else {
				this.noteOff(local32, 64, local20);
			}
		} else if (local9 == 160) {
			local20 = arg0 & 0xF;
			local32 = arg0 >> 8 & 0x7F;
			local26 = arg0 >> 16 & 0x7F;
			this.setPolyphonicAftertouch(local26, local32, local20);
		} else if (local9 == 176) {
			local32 = arg0 >> 8 & 0x7F;
			local20 = arg0 & 0xF;
			local26 = arg0 >> 16 & 0x7F;
			if (local32 == 0) {
				this.channelBankSelect[local20] = (local26 << 14) + (this.channelBankSelect[local20] & 0xFFE03FFF);
			}
			if (local32 == 32) {
				this.channelBankSelect[local20] = (this.channelBankSelect[local20] & 0xFFFFC07F) + (local26 << 7);
			}
			if (local32 == 1) {
				this.channelModulation[local20] = (local26 << 7) + (this.channelModulation[local20] & 0xFFFFC07F);
			}
			if (local32 == 33) {
				this.channelModulation[local20] = (this.channelModulation[local20] & 0xFFFFFF80) + local26;
			}
			if (local32 == 5) {
				this.channelPortamentoTime[local20] = (this.channelPortamentoTime[local20] & 0xFFFFC07F) + (local26 << 7);
			}
			if (local32 == 37) {
				this.channelPortamentoTime[local20] = local26 + (this.channelPortamentoTime[local20] & 0xFFFFFF80);
			}
			if (local32 == 7) {
				this.channelVolume[local20] = (local26 << 7) + (this.channelVolume[local20] & 0xFFFFC07F);
			}
			if (local32 == 39) {
				this.channelVolume[local20] = (this.channelVolume[local20] & 0xFFFFFF80) + local26;
			}
			if (local32 == 10) {
				this.channelPan[local20] = (this.channelPan[local20] & 0xFFFFC07F) + (local26 << 7);
			}
			if (local32 == 42) {
				this.channelPan[local20] = local26 + (this.channelPan[local20] & 0xFFFFFF80);
			}
			if (local32 == 11) {
				this.channelExpression[local20] = (this.channelExpression[local20] & 0xFFFFC07F) + (local26 << 7);
			}
			if (local32 == 43) {
				this.channelExpression[local20] = local26 + (this.channelExpression[local20] & 0xFFFFFF80);
			}
			if (local32 == 64) {
				if (local26 >= 64) {
					this.channelFlags[local20] |= 0x1;
				} else {
					this.channelFlags[local20] &= 0xFFFFFFFE;
				}
			}
			if (local32 == 65) {
				if (local26 < 64) {
					this.releaseOrphanedPortamentoNotes(local20);
					this.channelFlags[local20] &= 0xFFFFFFFD;
				} else {
					this.channelFlags[local20] |= 0x2;
				}
			}
			if (local32 == 99) {
				this.channelRpnNrpn[local20] = (local26 << 7) + (this.channelRpnNrpn[local20] & 0x7F);
			}
			if (local32 == 98) {
				this.channelRpnNrpn[local20] = (this.channelRpnNrpn[local20] & 0x3F80) + local26;
			}
			if (local32 == 101) {
				this.channelRpnNrpn[local20] = (local26 << 7) + (this.channelRpnNrpn[local20] & 0x7F) + 16384;
			}
			if (local32 == 100) {
				this.channelRpnNrpn[local20] = local26 + (this.channelRpnNrpn[local20] & 0x3F80) + 16384;
			}
			if (local32 == 120) {
				this.allSoundOff(local20);
			}
			if (local32 == 121) {
				this.resetAllControllers(local20);
			}
			if (local32 == 123) {
				this.allNotesOff(local20);
			}
			@Pc(522) int local522;
			if (local32 == 6) {
				local522 = this.channelRpnNrpn[local20];
				if (local522 == 16384) {
					this.channelPitchBendRange[local20] = (this.channelPitchBendRange[local20] & 0xFFFFC07F) + (local26 << 7);
				}
			}
			if (local32 == 38) {
				local522 = this.channelRpnNrpn[local20];
				if (local522 == 16384) {
					this.channelPitchBendRange[local20] = (this.channelPitchBendRange[local20] & 0xFFFFFF80) + local26;
				}
			}
			if (local32 == 16) {
				this.channelSampleOffset[local20] = (this.channelSampleOffset[local20] & 0xFFFFC07F) + (local26 << 7);
			}
			if (local32 == 48) {
				this.channelSampleOffset[local20] = (this.channelSampleOffset[local20] & 0xFFFFFF80) + local26;
			}
			if (local32 == 81) {
				if (local26 >= 64) {
					this.channelFlags[local20] |= 0x4;
				} else {
					this.releaseCustomHoldPedal(local20);
					this.channelFlags[local20] &= 0xFFFFFFFB;
				}
			}
			if (local32 == 17) {
				this.setDetune(local20, (local26 << 7) + (this.channelDetuneValue[local20] & 0xFFFFC07F));
			}
			if (local32 == 49) {
				this.setDetune(local20, (this.channelDetuneValue[local20] & 0xFFFFFF80) + local26);
			}
		} else if (local9 == 192) {
			local32 = arg0 >> 8 & 0x7F;
			local20 = arg0 & 0xF;
			this.programChange(this.channelBankSelect[local20] + local32, local20);
		} else if (local9 == 208) {
			local20 = arg0 & 0xF;
			local32 = arg0 >> 8 & 0x7F;
			this.setChannelPressure(local20, local32);
		} else if (local9 == 224) {
			local20 = arg0 & 0xF;
			local32 = (arg0 >> 9 & 0x3F80) + ((arg0 & 0x7FBE) >> 8);
			this.setPitchBend(local20, local32);
		} else {
			local9 = arg0 & 0xFF;
			if (local9 == 255) {
				this.stopAllNotes(true);
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "d", descriptor = "(II)V")
	private void allNotesOff(@OriginalArg(1) int arg0) {
		for (@Pc(12) MidiNote local12 = (MidiNote) this.noteStream.notes.head(); local12 != null; local12 = (MidiNote) this.noteStream.notes.next()) {
			if ((arg0 < 0 || arg0 == local12.channel) && local12.releaseTimer < 0) {
				this.activeNotes[local12.channel][local12.noteKey] = null;
				local12.releaseTimer = 0;
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(ZLclient!rf;I)V")
	public final synchronized void playSong(@OriginalArg(0) boolean arg0, @OriginalArg(1) Song arg1) {
		this.loadSong(arg0, arg1, true);
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "([III)V")
	@Override
	public final synchronized void read(@OriginalArg(0) int[] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		if (this.midiDecoder.isValid()) {
			@Pc(18) int local18 = this.midiDecoder.division * this.TEMPO_SCALE / AudioChannel.sampleRate;
			do {
				@Pc(28) long local28 = this.elapsedTimeMicros + (long) arg2 * (long) local18;
				if (this.currentTimeMicros - local28 >= 0L) {
					this.elapsedTimeMicros = local28;
					break;
				}
				@Pc(59) int local59 = (int) ((this.currentTimeMicros + (long) local18 - this.elapsedTimeMicros - 1L) / (long) local18);
				this.elapsedTimeMicros += (long) local18 * (long) local59;
				this.noteStream.read(arg0, arg1, local59);
				arg2 -= local59;
				arg1 += local59;
				this.advanceSequencer();
			} while (this.midiDecoder.isValid());
		}
		this.noteStream.read(arg0, arg1, arg2);
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(IILclient!mf;B[I)Z")
	public final boolean advanceNoteEnvelope(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) MidiNote arg2, @OriginalArg(4) int[] arg3) {
		arg2.retriggerDelay = AudioChannel.sampleRate / 100;
		if (arg2.releaseTimer >= 0 && (arg2.stream == null || arg2.stream.isOutOfBounds())) {
			arg2.release();
			arg2.unlink();
			if (arg2.exclusiveGroup > 0 && arg2 == this.exclusiveGroupNotes[arg2.channel][arg2.exclusiveGroup]) {
				this.exclusiveGroupNotes[arg2.channel][arg2.exclusiveGroup] = null;
			}
			return true;
		}
		@Pc(54) int local54 = arg2.pitchModifier;
		if (local54 > 0) {
			local54 -= (int) (Math.pow(2.0D, (double) this.channelPortamentoTime[arg2.channel] * 4.921259842519685E-4D) * 16.0D + 0.5D);
			if (local54 < 0) {
				local54 = 0;
			}
			arg2.pitchModifier = local54;
		}
		arg2.stream.setRate(this.calculatePitch(arg2));
		@Pc(103) MidiTrackState local103 = arg2.trackState;
		arg2.vibratoPhase += local103.vibratoSpeed;
		arg2.tickCount++;
		@Pc(134) double local134 = (double) ((arg2.noteKey - 60 << 8) + (arg2.pitchModifier * arg2.pitchDelta >> 12)) * 5.086263020833333E-6D;
		@Pc(136) boolean local136 = false;
		if (local103.decayRate > 0) {
			if (local103.sustainRate > 0) {
				arg2.sustainTimer += (int) (Math.pow(2.0D, local134 * (double) local103.sustainRate) * 128.0D + 0.5D);
			} else {
				arg2.sustainTimer += 128;
			}
			if (arg2.sustainTimer * local103.decayRate >= 819200) {
				local136 = true;
			}
		}
		if (local103.attackEnvelope != null) {
			if (local103.attackRate <= 0) {
				arg2.attackEnvelopePos += 128;
			} else {
				arg2.attackEnvelopePos += (int) (Math.pow(2.0D, (double) local103.attackRate * local134) * 128.0D + 0.5D);
			}
			while (arg2.attackEnvelopeIndex < local103.attackEnvelope.length - 2 && arg2.attackEnvelopePos > (local103.attackEnvelope[arg2.attackEnvelopeIndex + 2] & 0xFF) << 8) {
				arg2.attackEnvelopeIndex += 2;
			}
			if (local103.attackEnvelope.length - 2 == arg2.attackEnvelopeIndex && local103.attackEnvelope[arg2.attackEnvelopeIndex + 1] == 0) {
				local136 = true;
			}
		}
		if (arg2.releaseTimer >= 0 && local103.releaseEnvelope != null && (this.channelFlags[arg2.channel] & 0x1) == 0 && (arg2.exclusiveGroup < 0 || this.exclusiveGroupNotes[arg2.channel][arg2.exclusiveGroup] != arg2)) {
			if (local103.releaseRate > 0) {
				arg2.releaseTimer += (int) (Math.pow(2.0D, (double) local103.releaseRate * local134) * 128.0D + 0.5D);
			} else {
				arg2.releaseTimer += 128;
			}
			while (local103.releaseEnvelope.length - 2 > arg2.releaseEnvelopeIndex && (local103.releaseEnvelope[arg2.releaseEnvelopeIndex + 2] & 0xFF) << 8 < arg2.releaseTimer) {
				arg2.releaseEnvelopeIndex += 2;
			}
			if (arg2.releaseEnvelopeIndex == local103.releaseEnvelope.length - 2) {
				local136 = true;
			}
		}
		if (!local136) {
			arg2.stream.fadeToVolumeAndPan(arg2.retriggerDelay, this.calculateVolume(arg2), this.calculatePan(arg2));
			return false;
		}
		arg2.stream.fadeOutAndRelease(arg2.retriggerDelay);
		if (arg3 == null) {
			arg2.stream.skip(arg0);
		} else {
			arg2.stream.read(arg3, arg1, arg0);
		}
		if (arg2.stream.isTransitioning()) {
			this.noteStream.mixer.addSubStream(arg2.stream);
		}
		arg2.release();
		if (arg2.releaseTimer >= 0) {
			arg2.unlink();
			if (arg2.exclusiveGroup > 0 && this.exclusiveGroupNotes[arg2.channel][arg2.exclusiveGroup] == arg2) {
				this.exclusiveGroupNotes[arg2.channel][arg2.exclusiveGroup] = null;
			}
		}
		return true;
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "()Lclient!qb;")
	@Override
	public final synchronized PcmStream firstSubStream() {
		return this.noteStream;
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(BIII)V")
	private void noteOff(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2) {
		@Pc(12) MidiNote local12 = this.activeNotes[arg2][arg0];
		if (local12 == null) {
			return;
		}
		this.activeNotes[arg2][arg0] = null;
		if ((this.channelFlags[arg2] & 0x2) == 0) {
			local12.releaseTimer = 0;
			return;
		}
		for (@Pc(44) MidiNote local44 = (MidiNote) this.noteStream.notes.head(); local44 != null; local44 = (MidiNote) this.noteStream.notes.next()) {
			if (local44.channel == local12.channel && local44.releaseTimer < 0 && local44 != local12) {
				local12.releaseTimer = 0;
				break;
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "f", descriptor = "(I)V")
	private void advanceSequencer() {
		@Pc(8) int local8 = this.currentTrack;
		@Pc(11) int local11 = this.currentTick;
		@Pc(20) long local20 = this.currentTimeMicros;
		if (this.nextSong != null && local11 == this.crossfadeTick) {
			this.loadSong(this.loop, this.nextSong, this.crossfadeReset);
			this.advanceSequencer();
			return;
		}
		while (this.currentTick == local11) {
			while (local11 == this.midiDecoder.times[local8]) {
				this.midiDecoder.loadTrackPosition(local8);
				@Pc(64) int local64 = this.midiDecoder.getNextEvent(local8);
				if (local64 == 1) {
					this.midiDecoder.loadEndOfTrackPosition();
					this.midiDecoder.saveTrackPosition(local8);
					if (this.midiDecoder.hasNextTrack()) {
						if (this.nextSong != null) {
							this.playSong(this.loop, this.nextSong);
							this.advanceSequencer();
							return;
						}
						if (!this.loop || local11 == 0) {
							this.stopAllNotes(true);
							this.midiDecoder.release();
							return;
						}
						this.midiDecoder.setStartMillis(local20);
					}
					break;
				}
				if ((local64 & 0x80) != 0) {
					this.dispatchMidiEvent(local64);
				}
				this.midiDecoder.addDeltaTime(local8);
				this.midiDecoder.saveTrackPosition(local8);
			}
			local8 = this.midiDecoder.getNextTrack();
			local11 = this.midiDecoder.times[local8];
			local20 = this.midiDecoder.getTimeMillis(local11);
		}
		this.currentTrack = local8;
		this.currentTimeMicros = local20;
		this.currentTick = local11;
		if (this.nextSong != null && local11 > this.crossfadeTick) {
			this.currentTrack = -1;
			this.currentTick = this.crossfadeTick;
			this.currentTimeMicros = this.midiDecoder.getTimeMillis(this.currentTick);
		}
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(IIII)V")
	private void setPolyphonicAftertouch(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2) {
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(ILclient!mf;)I")
	private int calculatePan(@OriginalArg(1) MidiNote arg0) {
		@Pc(5) int local5 = this.channelPan[arg0.channel];
		return local5 < 8192 ? arg0.pan * local5 + 32 >> 6 : 16384 - ((128 - arg0.pan) * (-local5 + 16384) + 32 >> 6);
	}

	@OriginalMember(owner = "client!va", name = "c", descriptor = "(I)V")
	@Override
	public final synchronized void skip(@OriginalArg(0) int arg0) {
		if (this.midiDecoder.isValid()) {
			@Pc(15) int local15 = this.midiDecoder.division * this.TEMPO_SCALE / AudioChannel.sampleRate;
			do {
				@Pc(25) long local25 = this.elapsedTimeMicros + (long) arg0 * (long) local15;
				if (this.currentTimeMicros - local25 >= 0L) {
					this.elapsedTimeMicros = local25;
					break;
				}
				@Pc(57) int local57 = (int) (((long) local15 + this.currentTimeMicros - this.elapsedTimeMicros - 1L) / (long) local15);
				arg0 -= local57;
				this.elapsedTimeMicros += (long) local57 * (long) local15;
				this.noteStream.skip(local57);
				this.advanceSequencer();
			} while (this.midiDecoder.isValid());
		}
		this.noteStream.skip(arg0);
	}

	@OriginalMember(owner = "client!va", name = "e", descriptor = "(II)V")
	private void releaseCustomHoldPedal(@OriginalArg(0) int arg0) {
		if ((this.channelFlags[arg0] & 0x4) == 0) {
			return;
		}
		for (@Pc(24) MidiNote local24 = (MidiNote) this.noteStream.notes.head(); local24 != null; local24 = (MidiNote) this.noteStream.notes.next()) {
			if (local24.channel == arg0) {
				local24.portamentoPos = 0;
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(BLclient!mf;)I")
	private int calculatePitch(@OriginalArg(1) MidiNote arg0) {
		@Pc(6) MidiTrackState local6 = arg0.trackState;
		@Pc(17) int local17 = (arg0.pitchDelta * arg0.pitchModifier >> 12) + arg0.basePitch;
		local17 += this.channelPitchBendRange[arg0.channel] * (this.channelPitchBend[arg0.channel] - 8192) >> 12;
		@Pc(62) int local62;
		if (local6.vibratoSpeed > 0 && (local6.vibratoDepth > 0 || this.channelModulation[arg0.channel] > 0)) {
			local62 = local6.vibratoDepth << 2;
			@Pc(67) int local67 = local6.vibratoDelay << 1;
			if (local67 > arg0.tickCount) {
				local62 = arg0.tickCount * local62 / local67;
			}
			local62 += this.channelModulation[arg0.channel] >> 7;
			@Pc(102) double local102 = Math.sin((double) (arg0.vibratoPhase & 0x1FF) * 0.01227184630308513D);
			local17 += (int) ((double) local62 * local102);
		}
		local62 = (int) ((double) (arg0.sound.rate * 256) * Math.pow(2.0D, (double) local17 * 3.255208333333333E-4D) / (double) AudioChannel.sampleRate + 0.5D);
		return local62 >= 1 ? local62 : 1;
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(Z)I")
	public final int getVolume() {
		return this.volume;
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(ZB)V")
	private void stopAllNotes(@OriginalArg(0) boolean arg0) {
		if (arg0) {
			this.allSoundOff(-1);
		} else {
			this.allNotesOff(-1);
		}
		this.resetAllControllers(-1);
		@Pc(29) int local29;
		for (local29 = 0; local29 < 16; local29++) {
			this.channelProgram[local29] = this.defaultProgram[local29];
		}
		for (local29 = 0; local29 < 16; local29++) {
			this.channelBankSelect[local29] = this.defaultProgram[local29] & 0xFFFFFF80;
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(Lclient!mf;ZB)V")
	public final void applyLoopStartOffset(@OriginalArg(0) MidiNote arg0, @OriginalArg(1) boolean arg1) {
		@Pc(8) int local8 = arg0.sound.samples.length;
		@Pc(27) int local27;
		if (arg1 && arg0.sound.pingPongLoop) {
			@Pc(37) int local37 = local8 + local8 - arg0.sound.start;
			local8 <<= 0x8;
			local27 = (int) ((long) local37 * (long) this.channelSampleOffset[arg0.channel] >> 6);
			if (local27 >= local8) {
				arg0.stream.reverseDirection();
				local27 = local8 + local8 - local27 - 1;
			}
		} else {
			local27 = (int) ((long) local8 * (long) this.channelSampleOffset[arg0.channel] >> 6);
		}
		arg0.stream.setSamplePosition(local27);
	}

	@OriginalMember(owner = "client!va", name = "f", descriptor = "(II)V")
	private void releaseOrphanedPortamentoNotes(@OriginalArg(0) int arg0) {
		if ((this.channelFlags[arg0] & 0x2) == 0) {
			return;
		}
		for (@Pc(20) MidiNote local20 = (MidiNote) this.noteStream.notes.head(); local20 != null; local20 = (MidiNote) this.noteStream.notes.next()) {
			if (arg0 == local20.channel && this.activeNotes[arg0][local20.noteKey] == null && local20.releaseTimer < 0) {
				local20.releaseTimer = 0;
			}
		}
	}

	@OriginalMember(owner = "client!va", name = "a", descriptor = "(Lclient!mf;I)Z")
	public final boolean isNoteFinished(@OriginalArg(0) MidiNote arg0) {
		if (arg0.stream != null) {
			return false;
		}
		if (arg0.releaseTimer >= 0) {
			arg0.unlink();
			if (arg0.exclusiveGroup > 0 && this.exclusiveGroupNotes[arg0.channel][arg0.exclusiveGroup] == arg0) {
				this.exclusiveGroupNotes[arg0.channel][arg0.exclusiveGroup] = null;
			}
		}
		return true;
	}

	@OriginalMember(owner = "client!va", name = "f", descriptor = "(B)V")
	public final synchronized void reset() {
		this.stopPlayback(true);
	}

	@OriginalMember(owner = "client!va", name = "g", descriptor = "(II)V")
	public final synchronized void setVolume(@OriginalArg(1) int arg0) {
		this.volume = arg0;
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(ZB)V")
	private synchronized void stopPlayback(@OriginalArg(0) boolean arg0) {
		this.midiDecoder.release();
		this.nextSong = null;
		this.stopAllNotes(arg0);
	}

	@OriginalMember(owner = "client!va", name = "b", descriptor = "(BLclient!mf;)I")
	private int calculateVolume(@OriginalArg(1) MidiNote arg0) {
		if (this.channelVolumeScalar[arg0.channel] == 0) {
			return 0;
		}
		@Pc(21) MidiTrackState local21 = arg0.trackState;
		@Pc(37) int local37 = this.channelVolume[arg0.channel] * this.channelExpression[arg0.channel] + 4096 >> 13;
		@Pc(45) int local45 = local37 * local37 + 16384 >> 15;
		@Pc(54) int local54 = arg0.volumeGain * local45 + 16384 >> 15;
		@Pc(63) int local63 = local54 * this.volume + 128 >> 8;
		local37 = this.channelVolumeScalar[arg0.channel] * local63 + 128 >> 8;
		if (local21.decayRate > 0) {
			local37 = (int) (Math.pow(0.5D, (double) arg0.sustainTimer * 1.953125E-5D * (double) local21.decayRate) * (double) local37 + 0.5D);
		}
		@Pc(105) int local105;
		@Pc(113) int local113;
		@Pc(143) int local143;
		@Pc(133) int local133;
		if (local21.attackEnvelope != null) {
			local105 = arg0.attackEnvelopePos;
			local113 = local21.attackEnvelope[arg0.attackEnvelopeIndex + 1];
			if (local21.attackEnvelope.length - 2 > arg0.attackEnvelopeIndex) {
				local133 = (local21.attackEnvelope[arg0.attackEnvelopeIndex + 2] & 0xFF) << 8;
				local143 = (local21.attackEnvelope[arg0.attackEnvelopeIndex] & 0xFF) << 8;
				local113 += (local21.attackEnvelope[arg0.attackEnvelopeIndex + 3] - local113) * (local105 - local143) / (local133 - local143);
			}
			local37 = local113 * local37 + 32 >> 6;
		}
		if (arg0.releaseTimer > 0 && local21.releaseEnvelope != null) {
			local105 = arg0.releaseTimer;
			local113 = local21.releaseEnvelope[arg0.releaseEnvelopeIndex + 1];
			if (local21.releaseEnvelope.length - 2 > arg0.releaseEnvelopeIndex) {
				local143 = (local21.releaseEnvelope[arg0.releaseEnvelopeIndex] & 0xFF) << 8;
				local133 = (local21.releaseEnvelope[arg0.releaseEnvelopeIndex + 2] & 0xFF) << 8;
				local113 += (local105 - local143) * (-local113 + local21.releaseEnvelope[arg0.releaseEnvelopeIndex + 3]) / (local133 - local143);
			}
			local37 = local37 * local113 + 32 >> 6;
		}
		return local37;
	}

	@OriginalMember(owner = "client!va", name = "d", descriptor = "()Lclient!qb;")
	@Override
	public final synchronized PcmStream nextSubStream() {
		return null;
	}
}
