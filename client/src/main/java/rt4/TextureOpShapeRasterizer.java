package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!si")
public final class TextureOpShapeRasterizer extends TextureOp {

	@OriginalMember(owner = "client!sd", name = "S", descriptor = "I")
	public static int clipRight = 100;
	@OriginalMember(owner = "client!ic", name = "j", descriptor = "I")
	public static int clipBottom = 100;
	@OriginalMember(owner = "client!nj", name = "i", descriptor = "I")
	public static int clipLeft = 0;
	@OriginalMember(owner = "client!vl", name = "h", descriptor = "I")
	public static int clipTop = 0;
	@OriginalMember(owner = "client!si", name = "U", descriptor = "[Lclient!kf;")
	private TextureOpShape[] shapes;

	@OriginalMember(owner = "client!si", name = "<init>", descriptor = "()V")
	public TextureOpShapeRasterizer() {
		super(0, true);
	}

	@OriginalMember(owner = "client!id", name = "a", descriptor = "(IIBII)V")
	public static void setClipBounds(@OriginalArg(3) int arg0, @OriginalArg(4) int arg1) {
		clipRight = arg1;
		clipLeft = 0;
		clipTop = 0;
		clipBottom = arg0;
	}

	@OriginalMember(owner = "client!ed", name = "a", descriptor = "(ZIIII)V")
	public static void drawVerticalLineClamped(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3) {
		if (arg3 >= clipLeft && arg3 <= clipRight) {
			@Pc(22) int local22 = IntUtils.clamp(clipBottom, arg1, clipTop);
			@Pc(28) int local28 = IntUtils.clamp(clipBottom, arg0, clipTop);
			TextureOpShapeLine.fillVerticalLine(local22, arg3, local28, arg2);
		}
	}

	@OriginalMember(owner = "client!ta", name = "a", descriptor = "(IIZII)V")
	public static void drawHorizontalLineClamped(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3) {
		if (arg3 >= clipTop && arg3 <= clipBottom) {
			@Pc(15) int local15 = IntUtils.clamp(clipRight, arg0, clipLeft);
			@Pc(21) int local21 = IntUtils.clamp(clipRight, arg2, clipLeft);
			TextureOpShapeLine.fillRange(arg1, arg3, local21, local15);
		}
	}

	@OriginalMember(owner = "client!sk", name = "a", descriptor = "(IIIIII)V")
	public static void drawLine(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		@Pc(15) int local15 = arg4 - arg2;
		@Pc(19) int local19 = arg1 - arg3;
		if (local15 == 0) {
			if (local19 != 0) {
				drawVerticalLineClamped(arg1, arg3, arg0, arg2);
			}
		} else if (local19 == 0) {
			drawHorizontalLineClamped(arg2, arg0, arg4, arg3);
		} else {
			@Pc(50) int local50 = (local19 << 12) / local15;
			@Pc(59) int local59 = arg3 - (local50 * arg2 >> 12);
			@Pc(68) int local68;
			@Pc(76) int local76;
			if (clipLeft > arg2) {
				local68 = clipLeft;
				local76 = (clipLeft * local50 >> 12) + local59;
			} else if (arg2 > clipRight) {
				local76 = (clipRight * local50 >> 12) + local59;
				local68 = clipRight;
			} else {
				local68 = arg2;
				local76 = arg3;
			}
			@Pc(109) int local109;
			@Pc(118) int local118;
			if (arg4 < clipLeft) {
				local109 = clipLeft;
				local118 = local59 + (local50 * clipLeft >> 12);
			} else if (clipRight < arg4) {
				local109 = clipRight;
				local118 = local59 + (local50 * clipRight >> 12);
			} else {
				local118 = arg1;
				local109 = arg4;
			}
			if (clipTop > local118) {
				local109 = (clipTop - local59 << 12) / local50;
				local118 = clipTop;
			} else if (local118 > clipBottom) {
				local118 = clipBottom;
				local109 = (clipBottom - local59 << 12) / local50;
			}
			if (local76 < clipTop) {
				local76 = clipTop;
				local68 = (clipTop - local59 << 12) / local50;
			} else if (clipBottom < local76) {
				local76 = clipBottom;
				local68 = (clipBottom - local59 << 12) / local50;
			}
			TextureOpShapeLine.plotLine(arg0, local118, local76, local109, local68);
		}
	}

	@OriginalMember(owner = "client!sk", name = "a", descriptor = "(IIIIBIII)V")
	public static void drawEllipseBorderedClipped(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6) {
		@Pc(7) int local7 = 0;
		@Pc(12) int local12 = arg0 - arg3;
		@Pc(14) int local14 = 0;
		@Pc(16) int local16 = arg6;
		@Pc(20) int local20 = arg6 * arg6;
		@Pc(25) int local25 = arg6 - arg3;
		@Pc(29) int local29 = arg0 * arg0;
		@Pc(33) int local33 = local20 << 1;
		@Pc(37) int local37 = local25 * local25;
		@Pc(41) int local41 = local12 * local12;
		@Pc(45) int local45 = local29 << 1;
		@Pc(49) int local49 = local37 << 1;
		@Pc(53) int local53 = local41 << 1;
		@Pc(57) int local57 = local25 << 1;
		@Pc(61) int local61 = arg6 << 1;
		@Pc(70) int local70 = local33 + local29 * (1 - local61);
		@Pc(79) int local79 = local20 - (local61 - 1) * local45;
		@Pc(88) int local88 = local49 + local41 * (1 - local57);
		@Pc(96) int local96 = local37 - local53 * (local57 - 1);
		@Pc(100) int local100 = local20 << 2;
		@Pc(104) int local104 = local29 << 2;
		@Pc(108) int local108 = local37 << 2;
		@Pc(112) int local112 = local33 * 3;
		@Pc(118) int local118 = (local61 - 3) * local45;
		@Pc(130) int local130 = local41 << 2;
		@Pc(134) int local134 = local49 * 3;
		@Pc(140) int local140 = (local57 - 3) * local53;
		@Pc(146) int local146 = (arg6 - 1) * local104;
		@Pc(148) int local148 = local108;
		@Pc(150) int local150 = local100;
		@Pc(156) int local156 = (local25 - 1) * local130;
		@Pc(174) int local174;
		@Pc(183) int local183;
		@Pc(192) int local192;
		@Pc(201) int local201;
		if (arg4 >= clipTop && clipBottom >= arg4) {
			@Pc(166) int[] local166 = TextureOpShapeLine.canvas[arg4];
			local174 = IntUtils.clamp(clipRight, arg5 - arg0, clipLeft);
			local183 = IntUtils.clamp(clipRight, arg5 + arg0, clipLeft);
			local192 = IntUtils.clamp(clipRight, arg5 - local12, clipLeft);
			local201 = IntUtils.clamp(clipRight, arg5 + local12, clipLeft);
			ArrayUtils.fillRange(local166, local174, local192, arg2);
			ArrayUtils.fillRange(local166, local192, local201, arg1);
			ArrayUtils.fillRange(local166, local201, local183, arg2);
		}
		while (local16 > 0) {
			if (local70 < 0) {
				while (local70 < 0) {
					local70 += local112;
					local112 += local100;
					local7++;
					local79 += local150;
					local150 += local100;
				}
			}
			@Pc(255) boolean local255 = local16 <= local25;
			if (local79 < 0) {
				local79 += local150;
				local70 += local112;
				local7++;
				local150 += local100;
				local112 += local100;
			}
			if (local255) {
				if (local88 < 0) {
					while (local88 < 0) {
						local14++;
						local96 += local148;
						local148 += local108;
						local88 += local134;
						local134 += local108;
					}
				}
				if (local96 < 0) {
					local14++;
					local96 += local148;
					local88 += local134;
					local148 += local108;
					local134 += local108;
				}
				local88 += -local156;
				local156 -= local130;
				local96 += -local140;
				local140 -= local130;
			}
			local79 += -local118;
			local70 += -local146;
			local146 -= local104;
			local118 -= local104;
			local16--;
			local183 = arg4 + local16;
			local174 = arg4 - local16;
			if (clipTop <= local183 && clipBottom >= local174) {
				local192 = IntUtils.clamp(clipRight, arg5 + local7, clipLeft);
				local201 = IntUtils.clamp(clipRight, arg5 - local7, clipLeft);
				if (local255) {
					@Pc(404) int local404 = IntUtils.clamp(clipRight, arg5 + local14, clipLeft);
					@Pc(412) int local412 = IntUtils.clamp(clipRight, arg5 - local14, clipLeft);
					@Pc(420) int[] local420;
					if (clipTop <= local174) {
						local420 = TextureOpShapeLine.canvas[local174];
						ArrayUtils.fillRange(local420, local201, local412, arg2);
						ArrayUtils.fillRange(local420, local412, local404, arg1);
						ArrayUtils.fillRange(local420, local404, local192, arg2);
					}
					if (local183 <= clipBottom) {
						local420 = TextureOpShapeLine.canvas[local183];
						ArrayUtils.fillRange(local420, local201, local412, arg2);
						ArrayUtils.fillRange(local420, local412, local404, arg1);
						ArrayUtils.fillRange(local420, local404, local192, arg2);
					}
				} else {
					if (clipTop <= local174) {
						ArrayUtils.fillRange(TextureOpShapeLine.canvas[local174], local201, local192, arg2);
					}
					if (clipBottom >= local183) {
						ArrayUtils.fillRange(TextureOpShapeLine.canvas[local183], local201, local192, arg2);
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!si", name = "a", descriptor = "(IB)[I")
	@Override
	public final int[] getMonochromeOutput(@OriginalArg(0) int arg0) {
		@Pc(13) int[] local13 = this.monochromeImageCache.get(arg0);
		if (this.monochromeImageCache.invalid) {
			this.rasterizeShapes(this.monochromeImageCache.getAll());
		}
		return local13;
	}

	@OriginalMember(owner = "client!si", name = "a", descriptor = "(I[[I)V")
	private void rasterizeShapes(@OriginalArg(1) int[][] arg0) {
		@Pc(7) int local7 = Texture.height;
		@Pc(9) int local9 = Texture.width;
		TextureOpShapeLine.setCanvas(arg0);
		setClipBounds(Texture.heightMask, Texture.widthMask);
		if (this.shapes == null) {
			return;
		}
		for (@Pc(23) int local23 = 0; local23 < this.shapes.length; local23++) {
			@Pc(33) TextureOpShape local33 = this.shapes[local23];
			@Pc(36) int local36 = local33.fillColor;
			@Pc(39) int local39 = local33.outlineColor;
			if (local36 >= 0) {
				if (local39 < 0) {
					local33.renderFilledShape(local9, local7);
				} else {
					local33.renderBorderedShape(local7, local9);
				}
			} else if (local39 >= 0) {
				local33.renderOutlinedShape(local7, local9);
			}
		}
	}

	@OriginalMember(owner = "client!si", name = "a", descriptor = "(ILclient!wa;Z)V")
	@Override
	public final void decode(@OriginalArg(0) int arg0, @OriginalArg(1) Buffer arg1) {
		if (arg0 == 0) {
			this.shapes = new TextureOpShape[arg1.g1()];
			for (@Pc(11) int local11 = 0; local11 < this.shapes.length; local11++) {
				@Pc(24) int local24 = arg1.g1();
				if (local24 == 0) {
					this.shapes[local11] = TextureOpShapeRect.create(arg1);
				} else if (local24 == 1) {
					this.shapes[local11] = TextureOpShapeBezier.create(arg1);
				} else if (local24 == 2) {
					this.shapes[local11] = TextureOpShapeLine.create(arg1);
				} else if (local24 == 3) {
					this.shapes[local11] = TextureOpShapeEllipse.create(arg1);
				}
			}
		} else if (arg0 == 1) {
			this.monochrome = arg1.g1() == 1;
		}
	}

	@OriginalMember(owner = "client!si", name = "b", descriptor = "(II)[[I")
	@Override
	public final int[][] getColorOutput(@OriginalArg(1) int arg0) {
		@Pc(14) int[][] local14 = this.colorImageCache.get(arg0);
		if (this.colorImageCache.invalid) {
			@Pc(20) int local20 = Texture.width;
			@Pc(22) int local22 = Texture.height;
			@Pc(26) int[][] local26 = new int[local22][local20];
			@Pc(31) int[][][] local31 = this.colorImageCache.get();
			this.rasterizeShapes(local26);
			for (@Pc(37) int local37 = 0; local37 < Texture.height; local37++) {
				@Pc(44) int[] local44 = local26[local37];
				@Pc(48) int[][] local48 = local31[local37];
				@Pc(52) int[] local52 = local48[0];
				@Pc(56) int[] local56 = local48[1];
				@Pc(60) int[] local60 = local48[2];
				for (@Pc(62) int local62 = 0; local62 < Texture.width; local62++) {
					@Pc(73) int local73 = local44[local62];
					local60[local62] = (local73 & 0xFF) << 4;
					local56[local62] = local73 >> 4 & 0xFF0;
					local52[local62] = local73 >> 12 & 0xFF0;
				}
			}
		}
		return local14;
	}
}
