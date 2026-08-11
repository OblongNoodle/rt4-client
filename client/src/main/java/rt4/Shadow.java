package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@OriginalClass("client!wm")
public final class Shadow {

	@OriginalMember(owner = "client!wm", name = "g", descriptor = "[B")
	public static final byte[] pixels = new byte[16384];
	@OriginalMember(owner = "client!wm", name = "a", descriptor = "Lclient!vi;")
	private GlVertexBufferObject indexVbo;

	@OriginalMember(owner = "client!wm", name = "c", descriptor = "Ljava/nio/ByteBuffer;")
	private ByteBuffer indexBuffer;

	@OriginalMember(owner = "client!wm", name = "e", descriptor = "Ljava/nio/ByteBuffer;")
	private ByteBuffer vertexBuffer;

	@OriginalMember(owner = "client!wm", name = "f", descriptor = "Lclient!vi;")
	private GlVertexBufferObject vertexVbo;

	@OriginalMember(owner = "client!wm", name = "b", descriptor = "Z")
	public boolean outputToSprite = true;

	@OriginalMember(owner = "client!wm", name = "h", descriptor = "I")
	private int pixelHash = -1;

	@OriginalMember(owner = "client!wm", name = "d", descriptor = "I")
	private final int textureId;

	@OriginalMember(owner = "client!wm", name = "<init>", descriptor = "()V")
	public Shadow() {
		@Pc(9) GL2 gl = GlRenderer.gl;
		@Pc(12) int[] local12 = new int[1];
		gl.glGenTextures(1, local12, 0);
		this.textureId = local12[0];
		GlCleaner.onCardTexture += 16384;
		GlRenderer.setTextureId(this.textureId);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER, GL2.GL_LINEAR);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_S, GL2.GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_WRAP_T, GL2.GL_CLAMP_TO_EDGE);
	}

	@OriginalMember(owner = "client!wm", name = "a", descriptor = "([[III)V")
	public final void buildShadowMesh(@OriginalArg(0) int[][] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		@Pc(4) Buffer local4 = new Buffer(1620);
		@Pc(11) int local11;
		for (@Pc(6) int local6 = 0; local6 <= 8; local6++) {
			for (local11 = 0; local11 <= 8; local11++) {
				if (GlRenderer.bigEndian) {
					local4.pFloat((float) local11 / 8.0F);
					local4.pFloat((float) local6 / 8.0F);
					local4.pFloat((float) (local11 * 128));
					local4.pFloat((float) arg0[local11 + arg1][local6 + arg2]);
					local4.pFloat((float) (local6 * 128));
				} else {
					local4.gFloat((float) local11 / 8.0F);
					local4.gFloat((float) local6 / 8.0F);
					local4.gFloat((float) (local11 * 128));
					local4.gFloat((float) arg0[local11 + arg1][local6 + arg2]);
					local4.gFloat((float) (local6 * 128));
				}
			}
		}
		if (GlRenderer.arbVboSupported) {
			@Pc(112) ByteBuffer local112 = ByteBuffer.wrap(local4.data, 0, local4.offset);
			this.vertexVbo = new GlVertexBufferObject();
			this.vertexVbo.setArrayBuffer(local112);
		} else {
			this.vertexBuffer = ByteBuffer.allocateDirect(local4.offset).order(ByteOrder.nativeOrder());
			this.vertexBuffer.put(local4.data, 0, local4.offset);
			this.vertexBuffer.flip();
		}
		@Pc(147) Buffer local147 = new Buffer(1536);
		for (local11 = 0; local11 < 8; local11++) {
			for (@Pc(154) int local154 = 0; local154 < 8; local154++) {
				if (GlRenderer.bigEndian) {
					local147.p4(local154 + (local11 + 1) * 9);
					local147.p4(local154 + local11 * 9);
					local147.p4(local154 + local11 * 9 + 1);
					local147.p4(local154 + (local11 + 1) * 9);
					local147.p4(local154 + local11 * 9 + 1);
					local147.p4(local154 + (local11 + 1) * 9 + 1);
				} else {
					local147.ip4(local154 + (local11 + 1) * 9);
					local147.ip4(local154 + local11 * 9);
					local147.ip4(local154 + local11 * 9 + 1);
					local147.ip4(local154 + (local11 + 1) * 9);
					local147.ip4(local154 + local11 * 9 + 1);
					local147.ip4(local154 + (local11 + 1) * 9 + 1);
				}
			}
		}
		if (GlRenderer.arbVboSupported) {
			@Pc(293) ByteBuffer local293 = ByteBuffer.wrap(local147.data, 0, local147.offset);
			this.indexVbo = new GlVertexBufferObject();
			this.indexVbo.setElementArrayBuffer(local293);
		} else {
			this.indexBuffer = ByteBuffer.allocateDirect(local147.offset).order(ByteOrder.nativeOrder());
			this.indexBuffer.put(local147.data, 0, local147.offset);
			this.indexBuffer.flip();
		}
	}

	@OriginalMember(owner = "client!wm", name = "a", descriptor = "(Lclient!ek;II)Z")
	public final boolean updateShadowTexture(@OriginalArg(0) SoftwareIndexedSprite arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2) {
		@Pc(2) byte[] local2 = arg0.pixels;
		@Pc(5) int local5 = arg0.width;
		@Pc(19) int local19 = arg1 * 128 + (arg2 * 128 + 1) * local5 + 1;
		@Pc(21) int local21 = 0;
		@Pc(23) int local23;
		@Pc(33) int local33;
		for (local23 = -128; local23 < 0; local23++) {
			local21 = (local21 << 8) - local21;
			for (local33 = -128; local33 < 0; local33++) {
				if (local2[local19++] != 0) {
					local21++;
				}
			}
			local19 += local5 - 128;
		}
		if (local21 == this.pixelHash) {
			return false;
		}
		this.pixelHash = local21;
		local19 = arg1 * 128 + (arg2 * 128 + 1) * local5 + 1;
		local23 = 0;
		for (local33 = -128; local33 < 0; local33++) {
			for (@Pc(82) int local82 = -128; local82 < 0; local82++) {
				if (local2[local19] == 0) {
					@Pc(96) int local96 = 0;
					if (local2[local19 - 1] != 0) {
						local96++;
					}
					if (local2[local19 + 1] != 0) {
						local96++;
					}
					if (local2[local19 - local5] != 0) {
						local96++;
					}
					if (local2[local19 + local5] != 0) {
						local96++;
					}
					pixels[local23++] = (byte) (local96 * 17);
				} else {
					pixels[local23++] = 68;
				}
				local19++;
			}
			local19 += local5 - 128;
		}
		@Pc(145) GL2 gl = GlRenderer.gl;
		@Pc(148) ByteBuffer local148 = ByteBuffer.wrap(pixels);
		local148.limit(16384);
		GlRenderer.setTextureId(this.textureId);
		gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_ALPHA, 128, 128, 0, GL2.GL_ALPHA, GL2.GL_UNSIGNED_BYTE, local148);
		return true;
	}

	@OriginalMember(owner = "client!wm", name = "b", descriptor = "()V")
	public final void renderShadow() {
		@Pc(1) GL2 gl = GlRenderer.gl;
		GlRenderer.setTextureId(this.textureId);
		if (this.vertexVbo == null) {
			if (GlRenderer.arbVboSupported) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			}
			gl.glInterleavedArrays(GL2.GL_T2F_V3F, 20, this.vertexBuffer);
			GlRenderer.normalArrayEnabled = false;
		} else {
			this.vertexVbo.bindArray();
			gl.glInterleavedArrays(GL2.GL_T2F_V3F, 20, 0L);
			GlRenderer.normalArrayEnabled = false;
		}
		if (this.indexVbo == null) {
			if (GlRenderer.arbVboSupported) {
				gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
			}
			gl.glDrawElements(GL2.GL_TRIANGLES, 384, GL2.GL_UNSIGNED_INT, this.indexBuffer);
		} else {
			this.indexVbo.bindElementArray();
			gl.glDrawElements(GL2.GL_TRIANGLES, 384, GL2.GL_UNSIGNED_INT, 0L);
		}
	}
}
