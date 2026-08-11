package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!pe")
public final class Occluder {

	@OriginalMember(owner = "client!pe", name = "b", descriptor = "I")
	public int minZ;

	@OriginalMember(owner = "client!pe", name = "c", descriptor = "I")
	public int maxX;

	@OriginalMember(owner = "client!pe", name = "e", descriptor = "I")
	public int maxTileX;

	@OriginalMember(owner = "client!pe", name = "f", descriptor = "I")
	public int maxZ;

	@OriginalMember(owner = "client!pe", name = "g", descriptor = "I")
	public int farXSlope;

	@OriginalMember(owner = "client!pe", name = "h", descriptor = "I")
	public int maxY;

	@OriginalMember(owner = "client!pe", name = "i", descriptor = "I")
	public int farYSlope;

	@OriginalMember(owner = "client!pe", name = "k", descriptor = "I")
	public int minTileX;

	@OriginalMember(owner = "client!pe", name = "l", descriptor = "I")
	public int type;

	@OriginalMember(owner = "client!pe", name = "m", descriptor = "I")
	public int nearYSlope;

	@OriginalMember(owner = "client!pe", name = "p", descriptor = "I")
	public int nearXSlope;

	@OriginalMember(owner = "client!pe", name = "s", descriptor = "I")
	public int minY;

	@OriginalMember(owner = "client!pe", name = "t", descriptor = "I")
	public int nearZSlope;

	@OriginalMember(owner = "client!pe", name = "u", descriptor = "I")
	public int minX;

	@OriginalMember(owner = "client!pe", name = "v", descriptor = "I")
	public int minTileY;

	@OriginalMember(owner = "client!pe", name = "w", descriptor = "I")
	public int direction;

	@OriginalMember(owner = "client!pe", name = "x", descriptor = "I")
	public int farZSlope;

	@OriginalMember(owner = "client!pe", name = "y", descriptor = "I")
	public int maxTileY;

	@OriginalMember(owner = "client!wj", name = "a", descriptor = "(IIIIIII)V")
	public static void add(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6) {
		@Pc(3) Occluder local3 = new Occluder();
		local3.minTileX = arg1 / 128;
		local3.maxTileX = arg2 / 128;
		local3.minTileY = arg3 / 128;
		local3.maxTileY = arg4 / 128;
		local3.type = arg0;
		local3.minX = arg1;
		local3.maxX = arg2;
		local3.minY = arg3;
		local3.maxY = arg4;
		local3.minZ = arg5;
		local3.maxZ = arg6;
		SceneGraph.occluders[SceneGraph.occluderCount++] = local3;
	}
}
