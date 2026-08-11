package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!l")
public final class HashTableIterator {

	@OriginalMember(owner = "client!l", name = "a", descriptor = "Lclient!ab;")
	private Node cursor;

	@OriginalMember(owner = "client!l", name = "c", descriptor = "I")
	private int bucketIndex = 0;

	@OriginalMember(owner = "client!l", name = "e", descriptor = "Lclient!sc;")
	private final HashTable table;

	@OriginalMember(owner = "client!l", name = "<init>", descriptor = "(Lclient!sc;)V")
	public HashTableIterator(@OriginalArg(0) HashTable arg0) {
		this.table = arg0;
	}

	@OriginalMember(owner = "client!l", name = "a", descriptor = "(I)Lclient!ab;")
	public final Node next() {
		@Pc(30) Node local30;
		if (this.bucketIndex > 0 && this.table.nodes[this.bucketIndex - 1] != this.cursor) {
			local30 = this.cursor;
			this.cursor = local30.nextNode;
			return local30;
		}
		do {
			if (this.table.bucketCount <= this.bucketIndex) {
				return null;
			}
			local30 = this.table.nodes[this.bucketIndex++].nextNode;
		} while (local30 == this.table.nodes[this.bucketIndex - 1]);
		this.cursor = local30.nextNode;
		return local30;
	}

	@OriginalMember(owner = "client!l", name = "a", descriptor = "(B)Lclient!ab;")
	public final Node first() {
		this.bucketIndex = 0;
		return this.next();
	}
}
