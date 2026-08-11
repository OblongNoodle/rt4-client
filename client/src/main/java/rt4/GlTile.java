package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@OriginalClass("client!hg")
public final class GlTile extends Node {

	@OriginalMember(owner = "client!hg", name = "J", descriptor = "Ljava/nio/ByteBuffer;")
	public static ByteBuffer blendByteBuffer;

	@OriginalMember(owner = "client!hg", name = "U", descriptor = "Lclient!wa;")
	public static Buffer opaqueIndexBuffer;

	@OriginalMember(owner = "client!hg", name = "K", descriptor = "Lclient!wa;")
	public static Buffer blendIndexBuffer;

	@OriginalMember(owner = "client!hg", name = "Q", descriptor = "Ljava/nio/ByteBuffer;")
	public static ByteBuffer opaqueByteBuffer;

	@OriginalMember(owner = "client!hg", name = "s", descriptor = "Ljava/nio/ByteBuffer;")
	private ByteBuffer vertexByteBuffer;

	@OriginalMember(owner = "client!hg", name = "t", descriptor = "[F")
	private float[] vertexUnderwaterCoord;

	@OriginalMember(owner = "client!hg", name = "v", descriptor = "[I")
	private int[] vertexColor;

	@OriginalMember(owner = "client!hg", name = "w", descriptor = "[I")
	private int[] faceTileY;

	@OriginalMember(owner = "client!hg", name = "x", descriptor = "[I")
	private int[] faceTileX;

	@OriginalMember(owner = "client!hg", name = "z", descriptor = "[I")
	private int[] vertexZ;

	@OriginalMember(owner = "client!hg", name = "A", descriptor = "Lclient!sc;")
	private HashTable vertexDedup;

	@OriginalMember(owner = "client!hg", name = "B", descriptor = "[F")
	private float[] vertexNormalZ;

	@OriginalMember(owner = "client!hg", name = "E", descriptor = "[[I")
	private int[][] faceVertexIndices;

	@OriginalMember(owner = "client!hg", name = "F", descriptor = "[I")
	private int[] vertexY;

	@OriginalMember(owner = "client!hg", name = "I", descriptor = "[[I")
	private int[][] faceBlendIndices;

	@OriginalMember(owner = "client!hg", name = "L", descriptor = "Lclient!vi;")
	private GlVertexBufferObject vertexVbo;

	@OriginalMember(owner = "client!hg", name = "N", descriptor = "[F")
	private float[] vertexNormalY;

	@OriginalMember(owner = "client!hg", name = "P", descriptor = "[I")
	private int[] facePlane;

	@OriginalMember(owner = "client!hg", name = "R", descriptor = "[F")
	private float[] vertexNormalX;

	@OriginalMember(owner = "client!hg", name = "S", descriptor = "[Z")
	private boolean[] faceBlend;

	@OriginalMember(owner = "client!hg", name = "T", descriptor = "[I")
	private int[] vertexX;

	@OriginalMember(owner = "client!hg", name = "p", descriptor = "I")
	public int vertexCapacity = 0;

	@OriginalMember(owner = "client!hg", name = "D", descriptor = "I")
	private int blendIndexCount = 0;

	@OriginalMember(owner = "client!hg", name = "G", descriptor = "I")
	private int opaqueIndexCount = 0;

	@OriginalMember(owner = "client!hg", name = "q", descriptor = "I")
	public int vertexCount = 0;

	@OriginalMember(owner = "client!hg", name = "r", descriptor = "I")
	public int faceCapacity = 0;

	@OriginalMember(owner = "client!hg", name = "O", descriptor = "I")
	private int faceCount = 0;

	@OriginalMember(owner = "client!hg", name = "y", descriptor = "I")
	public final int texture;

	@OriginalMember(owner = "client!hg", name = "V", descriptor = "F")
	private final float textureScale;

	@OriginalMember(owner = "client!hg", name = "M", descriptor = "Z")
	public final boolean blend;

	@OriginalMember(owner = "client!hg", name = "u", descriptor = "Z")
	private final boolean underwater;

	@OriginalMember(owner = "client!hg", name = "C", descriptor = "I")
	public final int underwaterColor;

	@OriginalMember(owner = "client!hg", name = "<init>", descriptor = "(IFZZI)V")
	public GlTile(@OriginalArg(0) int arg0, @OriginalArg(1) float arg1, @OriginalArg(2) boolean arg2, @OriginalArg(3) boolean arg3, @OriginalArg(4) int arg4) {
		this.texture = arg0;
		this.textureScale = arg1;
		this.blend = arg2;
		this.underwater = arg3;
		this.underwaterColor = arg4;
	}

	@OriginalMember(owner = "client!hg", name = "a", descriptor = "()V")
	public static void resetStaticBuffers() {
		opaqueIndexBuffer = null;
		blendIndexBuffer = null;
		opaqueByteBuffer = null;
		blendByteBuffer = null;
	}

	@OriginalMember(owner = "client!hg", name = "b", descriptor = "()V")
	public final void allocateBuffers() {
		this.vertexX = new int[this.vertexCapacity];
		this.vertexY = new int[this.vertexCapacity];
		this.vertexZ = new int[this.vertexCapacity];
		if (this.underwater) {
			this.vertexUnderwaterCoord = new float[this.vertexCapacity];
		}
		this.vertexColor = new int[this.vertexCapacity];
		this.vertexNormalX = new float[this.vertexCapacity];
		this.vertexNormalY = new float[this.vertexCapacity];
		this.vertexNormalZ = new float[this.vertexCapacity];
		this.faceTileX = new int[this.faceCapacity];
		this.faceTileY = new int[this.faceCapacity];
		this.facePlane = new int[this.faceCapacity];
		this.faceVertexIndices = new int[this.faceCapacity][];
		this.vertexDedup = new HashTable(IntUtils.clp2(this.vertexCapacity));
		if (this.blend) {
			this.faceBlendIndices = new int[this.faceCapacity][];
			this.faceBlend = new boolean[this.faceCapacity];
		}
	}

	@OriginalMember(owner = "client!hg", name = "a", descriptor = "(IIIFFFIF)I")
	public final int addVertex(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) float arg3, @OriginalArg(4) float arg4, @OriginalArg(5) float arg5, @OriginalArg(6) int arg6, @OriginalArg(7) float arg7) {
		@Pc(1) long local1 = 0L;
		if ((arg0 & 0x7F) == 0 || (arg2 & 0x7F) == 0) {
			local1 = (long) (arg0 + (arg2 << 16)) + ((long) arg6 << 32);
			@Pc(28) IntNode local28 = (IntNode) this.vertexDedup.get(local1);
			if (local28 != null) {
				if (arg1 < this.vertexY[local28.value]) {
					this.vertexY[local28.value] = arg1;
				}
				return local28.value;
			}
		}
		this.vertexX[this.vertexCount] = arg0;
		this.vertexY[this.vertexCount] = arg1;
		this.vertexZ[this.vertexCount] = arg2;
		if (this.underwater) {
			this.vertexUnderwaterCoord[this.vertexCount] = arg7;
		}
		this.vertexNormalX[this.vertexCount] = arg3;
		this.vertexNormalY[this.vertexCount] = arg4;
		this.vertexNormalZ[this.vertexCount] = arg5;
		this.vertexColor[this.vertexCount] = arg6;
		if (local1 != 0L) {
			this.vertexDedup.put(new IntNode(this.vertexCount), local1);
		}
		return this.vertexCount++;
	}

	@OriginalMember(owner = "client!hg", name = "d", descriptor = "()V")
	public final void uploadVertexData() {
		@Pc(12) Buffer local12 = new Buffer((this.underwater ? 40 : 36) * this.vertexCount);
		for (@Pc(14) int local14 = 0; local14 < this.vertexCount; local14++) {
			if (GlRenderer.bigEndian) {
				local12.pFloat((float) this.vertexX[local14]);
				local12.pFloat((float) this.vertexY[local14]);
				local12.pFloat((float) this.vertexZ[local14]);
				local12.p4(this.vertexColor[local14]);
				local12.pFloat(this.vertexNormalX[local14]);
				local12.pFloat(this.vertexNormalY[local14]);
				local12.pFloat(this.vertexNormalZ[local14]);
				local12.pFloat((float) this.vertexX[local14] / this.textureScale);
				local12.pFloat((float) this.vertexZ[local14] / this.textureScale);
				if (this.underwater) {
					local12.pFloat(this.vertexUnderwaterCoord[local14]);
				}
			} else {
				local12.gFloat((float) this.vertexX[local14]);
				local12.gFloat((float) this.vertexY[local14]);
				local12.gFloat((float) this.vertexZ[local14]);
				local12.p4(this.vertexColor[local14]);
				local12.gFloat(this.vertexNormalX[local14]);
				local12.gFloat(this.vertexNormalY[local14]);
				local12.gFloat(this.vertexNormalZ[local14]);
				local12.gFloat((float) this.vertexX[local14] / this.textureScale);
				local12.gFloat((float) this.vertexZ[local14] / this.textureScale);
				if (this.underwater) {
					local12.gFloat(this.vertexUnderwaterCoord[local14]);
				}
			}
		}
		if (GlRenderer.arbVboSupported) {
			@Pc(200) ByteBuffer local200 = ByteBuffer.wrap(local12.data, 0, local12.offset);
			this.vertexVbo = new GlVertexBufferObject();
			this.vertexVbo.setArrayBuffer(local200);
		} else {
			this.vertexByteBuffer = ByteBuffer.allocateDirect(local12.offset).order(ByteOrder.nativeOrder());
			this.vertexByteBuffer.put(local12.data, 0, local12.offset);
			this.vertexByteBuffer.flip();
		}
		this.vertexX = null;
		this.vertexY = null;
		this.vertexZ = null;
		this.vertexColor = null;
		this.vertexNormalX = null;
		this.vertexNormalY = null;
		this.vertexNormalZ = null;
		this.vertexDedup = null;
		this.vertexUnderwaterCoord = null;
	}

	@OriginalMember(owner = "client!hg", name = "a", descriptor = "([[[Lclient!bj;FZ)V")
	public final void renderTiles(@OriginalArg(0) Tile[][][] arg0, @OriginalArg(1) float arg1, @OriginalArg(2) boolean arg2) {
		if (opaqueIndexBuffer == null || opaqueIndexBuffer.data.length < this.opaqueIndexCount * 4) {
			opaqueIndexBuffer = new Buffer(this.opaqueIndexCount * 4);
		} else {
			opaqueIndexBuffer.offset = 0;
		}
		if (blendIndexBuffer == null || blendIndexBuffer.data.length < this.blendIndexCount * 4) {
			blendIndexBuffer = new Buffer(this.blendIndexCount * 4);
		} else {
			blendIndexBuffer.offset = 0;
		}
		@Pc(47) int local47;
		@Pc(68) Tile local68;
		@Pc(111) Buffer local111;
		@Pc(78) int[] local78;
		@Pc(86) int[] local86;
		@Pc(90) int local90;
		@Pc(116) int local116;
		if (GlRenderer.bigEndian) {
			for (local47 = 0; local47 < this.faceCount; local47++) {
				local68 = arg0[this.facePlane[local47]][this.faceTileX[local47]][this.faceTileY[local47]];
				if (local68 != null && local68.visible) {
					local78 = this.faceVertexIndices[local47];
					if (this.blend) {
						local86 = this.faceBlendIndices[local47];
						if (local86 != null) {
							for (local90 = 0; local90 < local86.length; local90++) {
								blendIndexBuffer.p4(local86[local90]);
							}
						}
						local111 = this.faceBlend[local47] ? blendIndexBuffer : opaqueIndexBuffer;
					} else {
						local111 = opaqueIndexBuffer;
					}
					for (local116 = 1; local116 < local78.length - 1; local116++) {
						local111.p4(local78[0]);
						local111.p4(local78[local116]);
						local111.p4(local78[local116 + 1]);
					}
				}
			}
		} else {
			for (local47 = 0; local47 < this.faceCount; local47++) {
				local68 = arg0[this.facePlane[local47]][this.faceTileX[local47]][this.faceTileY[local47]];
				if (local68 != null && local68.visible) {
					local78 = this.faceVertexIndices[local47];
					if (this.blend) {
						local86 = this.faceBlendIndices[local47];
						if (local86 != null) {
							for (local90 = 0; local90 < local86.length; local90++) {
								blendIndexBuffer.ip4(local86[local90]);
							}
						}
						local111 = this.faceBlend[local47] ? blendIndexBuffer : opaqueIndexBuffer;
					} else {
						local111 = opaqueIndexBuffer;
					}
					for (local116 = 1; local116 < local78.length - 1; local116++) {
						local111.ip4(local78[0]);
						local111.ip4(local78[local116]);
						local111.ip4(local78[local116 + 1]);
					}
				}
			}
		}
		if (opaqueIndexBuffer.offset == 0 && blendIndexBuffer.offset == 0) {
			return;
		}
		@Pc(257) GL2 gl = GlRenderer.gl;
		if (this.texture == -1 || arg2) {
			GlRenderer.setTextureId(-1);
			MaterialManager.setMaterial(0, 0);
		} else {
			Rasteriser.textureProvider.bindTexture(this.texture);
		}
		@Pc(282) int i = this.underwater ? 40 : 36;
		if (this.vertexVbo == null) {
			if (GlRenderer.arbVboSupported) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			}
			this.vertexByteBuffer.position(0);
			gl.glVertexPointer(3, GL2.GL_FLOAT, i, this.vertexByteBuffer);
			this.vertexByteBuffer.position(12);
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, i, this.vertexByteBuffer);
			if (Preferences.highDetailLighting) {
				this.vertexByteBuffer.position(16);
				gl.glNormalPointer(GL2.GL_FLOAT, i, this.vertexByteBuffer);
			}
			this.vertexByteBuffer.position(28);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, i, this.vertexByteBuffer);
			if (this.underwater) {
				gl.glClientActiveTexture(UnderwaterMaterialRenderer.getSecondaryTextureUnit());
				this.vertexByteBuffer.position(36);
				gl.glTexCoordPointer(1, GL2.GL_FLOAT, i, this.vertexByteBuffer);
				gl.glClientActiveTexture(GL2.GL_TEXTURE0);
			}
		} else {
			this.vertexVbo.bindArray();
			gl.glVertexPointer(3, GL2.GL_FLOAT, i, 0L);
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, i, 12L);
			if (Preferences.highDetailLighting) {
				gl.glNormalPointer(GL2.GL_FLOAT, i, 16L);
			}
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, i, 28L);
			if (this.underwater) {
				gl.glClientActiveTexture(UnderwaterMaterialRenderer.getSecondaryTextureUnit());
				gl.glTexCoordPointer(1, GL2.GL_FLOAT, i, 36L);
				gl.glClientActiveTexture(GL2.GL_TEXTURE0);
			}
		}
		if (GlRenderer.arbVboSupported) {
			gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
		if (opaqueIndexBuffer.offset != 0) {
			if (opaqueByteBuffer == null || opaqueByteBuffer.capacity() < opaqueIndexBuffer.offset) {
				opaqueByteBuffer = ByteBuffer.allocateDirect(opaqueIndexBuffer.offset).order(ByteOrder.nativeOrder());
			} else {
				opaqueByteBuffer.clear();
			}
			opaqueByteBuffer.put(opaqueIndexBuffer.data, 0, opaqueIndexBuffer.offset);
			opaqueByteBuffer.flip();
			GlRenderer.setDepthLayer(arg1);
			gl.glDrawElements(GL2.GL_TRIANGLES, opaqueIndexBuffer.offset / 4, GL2.GL_UNSIGNED_INT, opaqueByteBuffer);
		}
		if (blendIndexBuffer.offset == 0) {
			return;
		}
		if (blendByteBuffer == null || blendByteBuffer.capacity() < blendIndexBuffer.offset) {
			blendByteBuffer = ByteBuffer.allocateDirect(blendIndexBuffer.offset).order(ByteOrder.nativeOrder());
		} else {
			blendByteBuffer.clear();
		}
		blendByteBuffer.put(blendIndexBuffer.data, 0, blendIndexBuffer.offset);
		blendByteBuffer.flip();
		GlRenderer.setDepthLayer(arg1 - 100.0F);
		GlRenderer.disableDepthMask();
		gl.glDrawElements(GL2.GL_TRIANGLES, blendIndexBuffer.offset / 4, GL2.GL_UNSIGNED_INT, blendByteBuffer);
		GlRenderer.enableDepthMask();
	}

	@OriginalMember(owner = "client!hg", name = "a", descriptor = "(III[I[IZ)I")
	public final int addFace(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int[] arg3, @OriginalArg(4) int[] arg4, @OriginalArg(5) boolean arg5) {
		if (this.blend) {
			this.faceBlendIndices[this.faceCount] = arg4;
			this.faceBlend[this.faceCount] = arg5;
			if (arg4 != null) {
				this.blendIndexCount += arg4.length;
			}
			if (arg5) {
				this.blendIndexCount += (arg3.length - 2) * 3;
			} else {
				this.opaqueIndexCount += (arg3.length - 2) * 3;
			}
		} else {
			this.opaqueIndexCount += (arg3.length - 2) * 3;
		}
		this.facePlane[this.faceCount] = arg0;
		this.faceTileX[this.faceCount] = arg1;
		this.faceTileY[this.faceCount] = arg2;
		this.faceVertexIndices[this.faceCount] = arg3;
		return this.faceCount++;
	}
}
