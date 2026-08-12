package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

import java.util.Random;

@OriginalClass("client!mc")
public final class TextureOp4 extends TextureOp {

	@OriginalMember(owner = "client!mc", name = "U", descriptor = "I")
	private int rowHeight;

	@OriginalMember(owner = "client!mc", name = "V", descriptor = "I")
	private int halfMortarSize;

	@OriginalMember(owner = "client!mc", name = "X", descriptor = "[[I")
	private int[][] columnPositions;

	@OriginalMember(owner = "client!mc", name = "cb", descriptor = "[I")
	private int[] rowPositions;

	@OriginalMember(owner = "client!mc", name = "jb", descriptor = "I")
	private int columnWidth;

	@OriginalMember(owner = "client!mc", name = "sb", descriptor = "[[I")
	private int[][] cellBrightness;

	@OriginalMember(owner = "client!mc", name = "P", descriptor = "I")
	private int rowJitter = 204;

	@OriginalMember(owner = "client!mc", name = "bb", descriptor = "I")
	private int colorVariation = 1024;

	@OriginalMember(owner = "client!mc", name = "eb", descriptor = "I")
	private int scrollOffsetY = 0;

	@OriginalMember(owner = "client!mc", name = "lb", descriptor = "I")
	private int columnJitter = 409;

	@OriginalMember(owner = "client!mc", name = "hb", descriptor = "I")
	private int rows = 8;

	@OriginalMember(owner = "client!mc", name = "W", descriptor = "I")
	private int mortarSize = 81;

	@OriginalMember(owner = "client!mc", name = "ub", descriptor = "I")
	private int columns = 4;

	@OriginalMember(owner = "client!mc", name = "ib", descriptor = "I")
	private int columnOffset = 1024;

	@OriginalMember(owner = "client!mc", name = "<init>", descriptor = "()V")
	public TextureOp4() {
		super(0, true);
	}

	@OriginalMember(owner = "client!mc", name = "e", descriptor = "(I)V")
	@Override
	public final void postDecode() {
		this.buildLookupTable();
	}

	@OriginalMember(owner = "client!mc", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.columns = arg1.g1();
		} else if (arg0 == 1) {
			this.rows = arg1.g1();
		} else if (arg0 == 2) {
			this.columnJitter = arg1.g2();
		} else if (arg0 == 3) {
			this.rowJitter = arg1.g2();
		} else if (arg0 == 4) {
			this.columnOffset = arg1.g2();
		} else if (arg0 == 5) {
			this.scrollOffsetY = arg1.g2();
		} else if (arg0 == 6) {
			this.mortarSize = arg1.g2();
		} else if (arg0 == 7) {
			this.colorVariation = arg1.g2();
		}
	}

	@OriginalMember(owner = "client!mc", name = "i", descriptor = "(I)V")
	private void buildLookupTable() {
		@Pc(8) Random local8 = new Random(this.rows);
		this.rowHeight = 4096 / this.rows;
		this.halfMortarSize = this.mortarSize / 2;
		this.columnPositions = new int[this.rows][this.columns + 1];
		@Pc(34) int local34 = this.rowHeight / 2;
		this.rowPositions = new int[this.rows + 1];
		this.cellBrightness = new int[this.rows][this.columns];
		this.columnWidth = 4096 / this.columns;
		this.rowPositions[0] = 0;
		@Pc(64) int local64 = this.columnWidth / 2;
		for (@Pc(66) int local66 = 0; local66 < this.rows; local66++) {
			@Pc(82) int local82;
			@Pc(94) int local94;
			if (local66 > 0) {
				local82 = this.rowHeight;
				local94 = (RandomUtils.nextInt(4096, local8) - 2048) * this.rowJitter >> 12;
				@Pc(102) int local102 = local82 + (local94 * local34 >> 12);
				this.rowPositions[local66] = this.rowPositions[local66 - 1] + local102;
			}
			this.columnPositions[local66][0] = 0;
			for (local82 = 0; local82 < this.columns; local82++) {
				if (local82 > 0) {
					local94 = this.columnWidth;
					@Pc(150) int local150 = (RandomUtils.nextInt(4096, local8) - 2048) * this.columnJitter >> 12;
					local94 += local64 * local150 >> 12;
					this.columnPositions[local66][local82] = this.columnPositions[local66][local82 - 1] + local94;
				}
				this.cellBrightness[local66][local82] = this.colorVariation <= 0 ? 4096 : 4096 - RandomUtils.nextInt(this.colorVariation, local8);
			}
			this.columnPositions[local66][this.columns] = 4096;
		}
		this.rowPositions[this.rows] = 4096;
	}

	@OriginalMember(owner = "client!mc", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(11) int[] local11 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			@Pc(18) int local18 = 0;
			@Pc(25) int local25;
			for (local25 = Texture.heightFractions[arg0] + this.scrollOffsetY; local25 < 0; local25 += 4096) {
			}
			while (local25 > 4096) {
				local25 -= 4096;
			}
			while (this.rows > local18 && local25 >= this.rowPositions[local18]) {
				local18++;
			}
			@Pc(60) int local60 = local18 - 1;
			@Pc(65) int local65 = this.rowPositions[local18];
			@Pc(74) boolean local74 = (local18 & 0x1) == 0;
			@Pc(81) int local81 = this.rowPositions[local18 - 1];
			if (local81 + this.halfMortarSize < local25 && local65 - this.halfMortarSize > local25) {
				for (@Pc(100) int local100 = 0; local100 < Texture.width; local100++) {
					@Pc(105) int local105 = 0;
					@Pc(114) int local114 = local74 ? this.columnOffset : -this.columnOffset;
					@Pc(126) int local126;
					for (local126 = Texture.widthFractions[local100] + (this.columnWidth * local114 >> 12); local126 < 0; local126 += 4096) {
					}
					while (local126 > 4096) {
						local126 -= 4096;
					}
					while (this.columns > local105 && local126 >= this.columnPositions[local60][local105]) {
						local105++;
					}
					@Pc(172) int local172 = this.columnPositions[local60][local105];
					@Pc(176) int local176 = local105 - 1;
					@Pc(183) int local183 = this.columnPositions[local60][local176];
					if (local183 + this.halfMortarSize < local126 && local172 - this.halfMortarSize > local126) {
						local11[local100] = this.cellBrightness[local60][local176];
					} else {
						local11[local100] = 0;
					}
				}
			} else {
				ArrayUtils.fill(local11, 0, Texture.width, 0);
			}
		}
		return local11;
	}
}
