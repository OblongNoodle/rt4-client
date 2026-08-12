package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;
import java.awt.Component;

@OriginalClass("client!qa")
public final class JavaAudioChannel extends AudioChannel {

	@OriginalMember(owner = "client!qa", name = "L", descriptor = "I")
	private int bufferSize;

	@OriginalMember(owner = "client!qa", name = "M", descriptor = "Ljavax/sound/sampled/SourceDataLine;")
	private SourceDataLine dataLine;

	@OriginalMember(owner = "client!qa", name = "O", descriptor = "Ljavax/sound/sampled/AudioFormat;")
	private AudioFormat audioFormat;

	@OriginalMember(owner = "client!qa", name = "P", descriptor = "[B")
	private byte[] outputBuffer;

	@OriginalMember(owner = "client!qa", name = "N", descriptor = "Z")
	private boolean isSoundMax = false;

	@OriginalMember(owner = "client!qa", name = "d", descriptor = "()V")
	@Override
	protected final void flush() {
		if (this.dataLine != null) {
			this.dataLine.close();
			this.dataLine = null;
		}
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "(Ljava/awt/Component;)V")
	@Override
	public final void init(@OriginalArg(0) Component arg0) {
		@Pc(1) Info[] local1 = AudioSystem.getMixerInfo();
		if (local1 != null) {
			for (@Pc(9) int local9 = 0; local9 < local1.length; local9++) {
				@Pc(21) Info local21 = local1[local9];
				if (local21 != null) {
					@Pc(28) String local28 = local21.getName();
					if (local28 != null && local28.toLowerCase().indexOf("soundmax") >= 0) {
						this.isSoundMax = true;
					}
				}
			}
		}
		this.audioFormat = new AudioFormat((float) AudioChannel.sampleRate, 16, AudioChannel.stereo ? 2 : 1, true, false);
		this.outputBuffer = new byte[0x100 << (AudioChannel.stereo ? 2 : 1)];
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "(I)V")
	@Override
	public final void open(@OriginalArg(0) int arg0) throws LineUnavailableException {
		try {
			@Pc(20) javax.sound.sampled.DataLine.Info local20 = new javax.sound.sampled.DataLine.Info(SourceDataLine.class, this.audioFormat, arg0 << (AudioChannel.stereo ? 2 : 1));
			this.dataLine = (SourceDataLine) AudioSystem.getLine(local20);
			this.dataLine.open();
			this.dataLine.start();
			this.bufferSize = arg0;
		} catch (@Pc(36) LineUnavailableException local36) {
			if (IntUtils.bitCountFast(arg0) == 1) {
				this.dataLine = null;
				throw local36;
			} else {
				this.open(IntUtils.clp2(arg0));
			}
		}
	}

	@OriginalMember(owner = "client!qa", name = "b", descriptor = "()V")
	@Override
	protected final void close() throws LineUnavailableException {
		this.dataLine.flush();
		if (!this.isSoundMax) {
			return;
		}
		this.dataLine.close();
		this.dataLine = null;
		@Pc(34) javax.sound.sampled.DataLine.Info local34 = new javax.sound.sampled.DataLine.Info(SourceDataLine.class, this.audioFormat, this.bufferSize << (AudioChannel.stereo ? 2 : 1));
		this.dataLine = (SourceDataLine) AudioSystem.getLine(local34);
		this.dataLine.open();
		this.dataLine.start();
	}

	@OriginalMember(owner = "client!qa", name = "c", descriptor = "()I")
	@Override
	protected final int getBufferSize() {
		return this.bufferSize - (this.dataLine.available() >> (AudioChannel.stereo ? 2 : 1));
	}

	@OriginalMember(owner = "client!qa", name = "a", descriptor = "()V")
	@Override
	protected final void write() {
		@Pc(1) short local1 = 256;
		if (AudioChannel.stereo) {
			local1 = 512;
		}
		for (@Pc(9) int local9 = 0; local9 < local1; local9++) {
			@Pc(17) int local17 = this.samples[local9];
			if ((local17 + 8388608 & 0xFF000000) != 0) {
				local17 = local17 >> 31 ^ 0x7FFFFF;
			}
			this.outputBuffer[local9 * 2] = (byte) (local17 >> 8);
			this.outputBuffer[local9 * 2 + 1] = (byte) (local17 >> 16);
		}
		this.dataLine.write(this.outputBuffer, 0, local1 << 1);
	}
}
