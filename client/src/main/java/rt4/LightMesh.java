package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.nio.ByteBuffer;

@OriginalClass("client!fj")
public final class LightMesh {

	@OriginalMember(owner = "client!fj", name = "a", descriptor = "Lclient!vi;")
	private GlVertexBufferObject vertexVbo;

	@OriginalMember(owner = "client!fj", name = "b", descriptor = "[I")
	private int[] indices;

	@OriginalMember(owner = "client!fj", name = "c", descriptor = "[B")
	private byte[] colorG;

	@OriginalMember(owner = "client!fj", name = "d", descriptor = "[I")
	private int[] vertexX;

	@OriginalMember(owner = "client!fj", name = "e", descriptor = "Lclient!vi;")
	private GlVertexBufferObject indexVbo;

	@OriginalMember(owner = "client!fj", name = "f", descriptor = "Ljava/nio/ByteBuffer;")
	private ByteBuffer vertexBuffer;

	@OriginalMember(owner = "client!fj", name = "g", descriptor = "[I")
	private int[] vertexZ;

	@OriginalMember(owner = "client!fj", name = "h", descriptor = "[I")
	private int[] vertexY;

	@OriginalMember(owner = "client!fj", name = "i", descriptor = "[B")
	private byte[] colorB;

	@OriginalMember(owner = "client!fj", name = "j", descriptor = "I")
	public int indexCapacity;

	@OriginalMember(owner = "client!fj", name = "k", descriptor = "Ljava/nio/ByteBuffer;")
	private ByteBuffer indexBuffer;

	@OriginalMember(owner = "client!fj", name = "l", descriptor = "I")
	public int vertexCount;

	@OriginalMember(owner = "client!fj", name = "m", descriptor = "[B")
	private byte[] colorR;

	@OriginalMember(owner = "client!fj", name = "n", descriptor = "I")
	public int indexCount;

	@OriginalMember(owner = "client!fj", name = "o", descriptor = "Lclient!sc;")
	private HashTable vertexCache;

	@OriginalMember(owner = "client!fj", name = "p", descriptor = "I")
	public int vertexCapacity;

	@OriginalMember(owner = "client!fj", name = "a", descriptor = "(Lclient!gi;IIIFFF)I")
	public final int addVertex(@OriginalArg(0) Light arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) float arg4, @OriginalArg(5) float arg5, @OriginalArg(6) float arg6) {
		@Pc(1) long local1 = 0L;
		if ((arg1 & 0x7F) == 0 || (arg3 & 0x7F) == 0) {
			local1 = arg1 + (arg3 << 16);
			@Pc(23) IntNode local23 = (IntNode) this.vertexCache.get(local1);
			if (local23 != null) {
				return local23.value;
			}
		}
		@Pc(31) int local31 = arg0.color;
		@Pc(37) float local37 = (float) (arg0.x - arg1);
		@Pc(43) float local43 = (float) (arg0.y - arg2);
		@Pc(49) float local49 = (float) (arg0.z - arg3);
		@Pc(64) float local64 = (float) Math.sqrt(local37 * local37 + local43 * local43 + local49 * local49);
		@Pc(68) float local68 = 1.0F / local64;
		@Pc(72) float local72 = local37 * local68;
		@Pc(76) float local76 = local43 * local68;
		@Pc(80) float local80 = local49 * local68;
		@Pc(90) float local90 = local64 / (float) ((arg0.radius << 7) + 64);
		@Pc(96) float local96 = 1.0F - local90 * local90;
		if (local96 < 0.0F) {
			local96 = 0.0F;
		}
		@Pc(114) float local114 = local72 * arg4 + local76 * arg5 + local80 * arg6;
		if (local114 < 0.0F) {
			local114 = 0.0F;
		}
		@Pc(126) float local126 = local114 * local96 * 2.0F;
		if (local126 > 1.0F) {
			local126 = 1.0F;
		}
		@Pc(142) int local142 = (int) (local126 * (float) (local31 >> 16 & 0xFF));
		if (local142 > 255) {
			local142 = 255;
		}
		@Pc(157) int local157 = (int) (local126 * (float) (local31 >> 8 & 0xFF));
		if (local157 > 255) {
			local157 = 255;
		}
		@Pc(170) int local170 = (int) (local126 * (float) (local31 & 0xFF));
		if (local170 > 255) {
			local170 = 255;
		}
		this.colorR[this.vertexCount] = (byte) local142;
		this.colorG[this.vertexCount] = (byte) local157;
		this.colorB[this.vertexCount] = (byte) local170;
		this.vertexX[this.vertexCount] = arg1;
		this.vertexY[this.vertexCount] = arg2;
		this.vertexZ[this.vertexCount] = arg3;
		this.vertexCache.put(new IntNode(this.vertexCount), local1);
		return this.vertexCount++;
	}

	@OriginalMember(owner = "client!fj", name = "a", descriptor = "()V")
	public final void upload() {
		@Pc(7) Buffer local7 = new Buffer(this.indexCount * 4);
		@Pc(15) Buffer local15 = new Buffer(this.vertexCount * 16);
		@Pc(19) int local19;
		if (GlRenderer.bigEndian) {
			for (local19 = 0; local19 < this.vertexCount; local19++) {
				local15.p1(this.colorR[local19]);
				local15.p1(this.colorG[local19]);
				local15.p1(this.colorB[local19]);
				local15.p1(255);
				local15.pFloat((float) this.vertexX[local19]);
				local15.pFloat((float) this.vertexY[local19]);
				local15.pFloat((float) this.vertexZ[local19]);
			}
			for (local19 = 0; local19 < this.indexCount; local19++) {
				local7.p4(this.indices[local19]);
			}
		} else {
			for (local19 = 0; local19 < this.vertexCount; local19++) {
				local15.p1(this.colorR[local19]);
				local15.p1(this.colorG[local19]);
				local15.p1(this.colorB[local19]);
				local15.p1(255);
				local15.gFloat((float) this.vertexX[local19]);
				local15.gFloat((float) this.vertexY[local19]);
				local15.gFloat((float) this.vertexZ[local19]);
			}
			for (local19 = 0; local19 < this.indexCount; local19++) {
				local7.ip4(this.indices[local19]);
			}
		}
		if (GlRenderer.arbVboSupported) {
			this.vertexVbo = new GlVertexBufferObject();
			@Pc(173) ByteBuffer local173 = ByteBuffer.wrap(local15.data);
			this.vertexVbo.setArrayBuffer(local173);
			this.indexVbo = new GlVertexBufferObject();
			@Pc(186) ByteBuffer local186 = ByteBuffer.wrap(local7.data);
			this.indexVbo.setElementArrayBuffer(local186);
		} else {
			this.vertexBuffer = ByteBuffer.allocateDirect(local15.offset);
			this.vertexBuffer.put(local15.data);
			this.vertexBuffer.flip();
			this.indexBuffer = ByteBuffer.allocateDirect(local7.offset);
			this.indexBuffer.put(local7.data);
			this.indexBuffer.flip();
		}
		this.vertexX = null;
		this.vertexY = null;
		this.vertexZ = null;
		this.colorR = null;
		this.colorG = null;
		this.colorB = null;
		this.indices = null;
		this.vertexCache = null;
	}

	@OriginalMember(owner = "client!fj", name = "b", descriptor = "()V")
	public final void allocate() {
		this.indices = new int[this.indexCapacity];
		this.vertexX = new int[this.vertexCapacity];
		this.vertexY = new int[this.vertexCapacity];
		this.vertexZ = new int[this.vertexCapacity];
		this.colorR = new byte[this.vertexCapacity];
		this.colorG = new byte[this.vertexCapacity];
		this.colorB = new byte[this.vertexCapacity];
		this.vertexCache = new HashTable(IntUtils.clp2(this.vertexCapacity));
	}

	@OriginalMember(owner = "client!fj", name = "c", descriptor = "()V")
	public final void draw() {
		@Pc(1) GL2 gl = GlRenderer.gl;
		if (GlRenderer.arbVboSupported) {
			this.vertexVbo.bindArray();
			gl.glInterleavedArrays(GL2.GL_C4UB_V3F, 16, 0L);
			GlRenderer.normalArrayEnabled = false;
			this.indexVbo.bindElementArray();
			gl.glDrawElements(GL2.GL_TRIANGLES, this.indexCount, GL2.GL_UNSIGNED_INT, 0L);
			return;
		}
		if (GlRenderer.arbVboSupported) {
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
		gl.glInterleavedArrays(GL2.GL_C4UB_V3F, 16, this.vertexBuffer);
		GlRenderer.normalArrayEnabled = false;
		gl.glDrawElements(GL2.GL_TRIANGLES, this.indexCount, GL2.GL_UNSIGNED_INT, this.indexBuffer);
	}

	@OriginalMember(owner = "client!fj", name = "a", descriptor = "([I)V")
	public final void addFan(@OriginalArg(0) int[] arg0) {
		for (@Pc(1) int local1 = 1; local1 < arg0.length - 1; local1++) {
			this.indices[this.indexCount++] = arg0[0];
			this.indices[this.indexCount++] = arg0[local1];
			this.indices[this.indexCount++] = arg0[local1 + 1];
		}
	}
}
