package rt4;

import com.jogamp.opengl.GL2;
import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.nio.ByteBuffer;

@OriginalClass("client!vi")
public final class GlVertexBufferObject {

	@OriginalMember(owner = "client!vi", name = "a", descriptor = "I")
	private int id;

	@OriginalMember(owner = "client!vi", name = "b", descriptor = "I")
	private int contextId;

	@OriginalMember(owner = "client!vi", name = "c", descriptor = "I")
	private int size;

	@OriginalMember(owner = "client!vi", name = "d", descriptor = "Z")
	private final boolean stream;

	// Not part of the original client. Buffers created off the GL thread (by
	// AsyncSceneLoader's builder) can't touch GL, so they keep a copy of their
	// data here and upload it on the GL thread, either when the loader flushes
	// them before swapping the new scene in or on first bind.
	private ByteBuffer pendingData;

	private int pendingTarget;

	@OriginalMember(owner = "client!vi", name = "<init>", descriptor = "()V")
	public GlVertexBufferObject() {
		this(false);
	}

	@OriginalMember(owner = "client!vi", name = "<init>", descriptor = "(Z)V")
	public GlVertexBufferObject(@OriginalArg(0) boolean stream) {
		this.id = -1;
		this.size = 0;
		this.stream = stream;
		if (!GlRenderer.isGlThread()) {
			AsyncSceneLoader.addPendingBuffer(this);
			return;
		}
		this.generate();
	}

	private void generate() {
		@Pc(9) GL2 gl = GlRenderer.gl;
		@Pc(12) int[] temp = new int[1];
		gl.glGenBuffers(1, temp, 0);
		this.id = temp[0];
		this.contextId = GlCleaner.contextId;
	}

	private void defer(int target, ByteBuffer buffer) {
		// Callers pass shared scratch buffers that get reused right away, so
		// the data has to be copied now
		ByteBuffer copy = ByteBuffer.allocateDirect(buffer.limit()).order(buffer.order());
		ByteBuffer src = buffer.duplicate();
		src.position(0);
		copy.put(src);
		copy.flip();
		this.pendingData = copy;
		this.pendingTarget = target;
	}

	// Not part of the original client, see pendingData. Must be called on the
	// GL thread.
	public final void uploadPending() {
		if (this.id == -1) {
			this.generate();
		}
		if (this.pendingData != null) {
			ByteBuffer data = this.pendingData;
			this.pendingData = null;
			if (this.pendingTarget == GL2.GL_ELEMENT_ARRAY_BUFFER) {
				this.setElementArrayBuffer(data);
			} else {
				this.setArrayBuffer(data);
			}
		}
	}

	@OriginalMember(owner = "client!vi", name = "a", descriptor = "(Ljava/nio/ByteBuffer;)V")
	public final void updateArrayBuffer(@OriginalArg(0) ByteBuffer buffer) {
		if (this.id == -1 || this.pendingData != null || !GlRenderer.isGlThread()) {
			// Not uploaded yet: the new data simply replaces the pending data
			this.setArrayBuffer(buffer);
			return;
		}
		if (buffer.limit() <= this.size) {
			@Pc(6) GL2 gl = GlRenderer.gl;
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.id);
			gl.glBufferSubData(GL2.GL_ARRAY_BUFFER, 0, buffer.limit(), buffer);
		} else {
			this.setArrayBuffer(buffer);
		}
	}

	@OriginalMember(owner = "client!vi", name = "finalize", descriptor = "()V")
	@Override
	public final void finalize() throws Throwable {
		if (this.id != -1) {
			GlCleaner.deleteBuffer(this.id, this.size, this.contextId);
			this.id = -1;
			this.size = 0;
		}
		super.finalize();
	}

	@OriginalMember(owner = "client!vi", name = "a", descriptor = "()V")
	public final void bindArray() {
		if (this.id == -1 || this.pendingData != null) {
			this.uploadPending();
		}
		@Pc(1) GL2 gl = GlRenderer.gl;
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.id);
	}

	@OriginalMember(owner = "client!vi", name = "b", descriptor = "(Ljava/nio/ByteBuffer;)V")
	public final void setElementArrayBuffer(@OriginalArg(0) ByteBuffer buffer) {
		if (!GlRenderer.isGlThread()) {
			this.defer(GL2.GL_ELEMENT_ARRAY_BUFFER, buffer);
			return;
		}
		if (this.id == -1) {
			this.generate();
		}
		this.pendingData = null;
		@Pc(1) GL2 gl = GlRenderer.gl;
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, this.id);
		gl.glBufferData(GL2.GL_ELEMENT_ARRAY_BUFFER, buffer.limit(), buffer, this.stream ? GL2.GL_STREAM_DRAW : GL2.GL_STATIC_DRAW);
		GlCleaner.onCardGeometry += buffer.limit() - this.size;
		this.size = buffer.limit();
	}

	@OriginalMember(owner = "client!vi", name = "b", descriptor = "()V")
	public final void bindElementArray() {
		if (this.id == -1 || this.pendingData != null) {
			this.uploadPending();
		}
		@Pc(1) GL2 gl = GlRenderer.gl;
		gl.glBindBuffer(GL2.GL_ELEMENT_ARRAY_BUFFER, this.id);
	}

	@OriginalMember(owner = "client!vi", name = "c", descriptor = "(Ljava/nio/ByteBuffer;)V")
	public final void setArrayBuffer(@OriginalArg(0) ByteBuffer buffer) {
		if (!GlRenderer.isGlThread()) {
			this.defer(GL2.GL_ARRAY_BUFFER, buffer);
			return;
		}
		if (this.id == -1) {
			this.generate();
		}
		this.pendingData = null;
		@Pc(1) GL2 gl = GlRenderer.gl;
		gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, this.id);
		gl.glBufferData(GL2.GL_ARRAY_BUFFER, buffer.limit(), buffer, this.stream ? GL2.GL_STREAM_DRAW : GL2.GL_STATIC_DRAW);
		GlCleaner.onCardGeometry += buffer.limit() - this.size;
		this.size = buffer.limit();
	}
}
