package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalClass;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;

@OriginalClass("client!ta")
public final class TextureOpShapeLine extends TextureOpShape {

	@OriginalMember(owner = "client!fk", name = "j", descriptor = "[[I")
	public static int[][] canvas;

	@OriginalMember(owner = "client!ta", name = "D", descriptor = "I")
	private final int top;

	@OriginalMember(owner = "client!ta", name = "s", descriptor = "I")
	private final int right;

	@OriginalMember(owner = "client!ta", name = "C", descriptor = "I")
	private final int left;

	@OriginalMember(owner = "client!ta", name = "A", descriptor = "I")
	private final int bottom;

	@OriginalMember(owner = "client!ta", name = "<init>", descriptor = "(IIIIIII)V")
	public TextureOpShapeLine(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6) {
		super(arg4, arg5, arg6);
		this.top = arg1;
		this.right = arg2;
		this.left = arg0;
		this.bottom = arg3;
	}

	@OriginalMember(owner = "client!bl", name = "a", descriptor = "(IIIIIIII)V")
	public static void drawFramedRect(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6) {
		if (TextureOpShapeRasterizer.clipLeft <= arg6 && TextureOpShapeRasterizer.clipRight >= arg5 && arg4 >= TextureOpShapeRasterizer.clipTop && arg1 <= TextureOpShapeRasterizer.clipBottom) {
			drawFramedRectUnclamped(arg3, arg4, arg1, arg2, arg0, arg5, arg6);
		} else {
			drawFramedRectClipped(arg5, arg2, arg1, arg0, arg3, arg4, arg6);
		}
	}

	@OriginalMember(owner = "client!mc", name = "a", descriptor = "(IIIIIIII)V")
	public static void drawFramedRectUnclamped(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6) {
		@Pc(9) int local9 = arg4 + arg1;
		@Pc(13) int local13 = arg4 + arg6;
		@Pc(15) int local15;
		for (local15 = arg1; local15 < local9; local15++) {
			ArrayUtils.fillRange(canvas[local15], arg6, arg5, arg0);
		}
		@Pc(34) int local34 = arg2 - arg4;
		@Pc(39) int local39 = arg5 - arg4;
		for (local15 = arg2; local15 > local34; local15--) {
			ArrayUtils.fillRange(canvas[local15], arg6, arg5, arg0);
		}
		for (local15 = local9; local15 <= local34; local15++) {
			@Pc(72) int[] local72 = canvas[local15];
			ArrayUtils.fillRange(local72, arg6, local13, arg0);
			ArrayUtils.fillRange(local72, local13, local39, arg3);
			ArrayUtils.fillRange(local72, local39, arg5, arg0);
		}
	}

	@OriginalMember(owner = "client!tl", name = "a", descriptor = "(IIIIIIII)V")
	public static void drawFramedRectClipped(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6) {
		@Pc(11) int local11 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg5, TextureOpShapeRasterizer.clipTop);
		@Pc(17) int local17 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg2, TextureOpShapeRasterizer.clipTop);
		@Pc(23) int local23 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg6, TextureOpShapeRasterizer.clipLeft);
		@Pc(29) int local29 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg0, TextureOpShapeRasterizer.clipLeft);
		@Pc(37) int local37 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg3 + arg5, TextureOpShapeRasterizer.clipTop);
		@Pc(46) int local46 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg2 - arg3, TextureOpShapeRasterizer.clipTop);
		@Pc(48) int local48;
		for (local48 = local11; local48 < local37; local48++) {
			ArrayUtils.fillRange(canvas[local48], local23, local29, arg4);
		}
		for (local48 = local17; local48 > local46; local48--) {
			ArrayUtils.fillRange(canvas[local48], local23, local29, arg4);
		}
		@Pc(94) int local94 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg3 + arg6, TextureOpShapeRasterizer.clipLeft);
		@Pc(103) int local103 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg0 - arg3, TextureOpShapeRasterizer.clipLeft);
		for (local48 = local37; local48 <= local46; local48++) {
			@Pc(122) int[] local122 = canvas[local48];
			ArrayUtils.fillRange(local122, local23, local94, arg4);
			ArrayUtils.fillRange(local122, local94, local103, arg1);
			ArrayUtils.fillRange(local122, local103, local29, arg4);
		}
	}

	@OriginalMember(owner = "client!sj", name = "a", descriptor = "(IIBIII)V")
	public static void fillSolidRect(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		if (arg0 >= TextureOpShapeRasterizer.clipLeft && arg3 <= TextureOpShapeRasterizer.clipRight && TextureOpShapeRasterizer.clipTop <= arg4 && TextureOpShapeRasterizer.clipBottom >= arg2) {
			fillSolidRectUnclamped(arg2, arg3, arg4, arg0, arg1);
		} else {
			fillSolidRectClipped(arg1, arg3, arg4, arg0, arg2);
		}
	}

	@OriginalMember(owner = "client!ob", name = "a", descriptor = "(IIIIII)V")
	public static void fillSolidRectUnclamped(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		for (@Pc(8) int local8 = arg2; local8 <= arg0; local8++) {
			ArrayUtils.fillRange(canvas[local8], arg3, arg1, arg4);
		}
	}

	@OriginalMember(owner = "client!n", name = "a", descriptor = "(IIIIII)V")
	public static void fillSolidRectClipped(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		@Pc(11) int local11 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg2, TextureOpShapeRasterizer.clipTop);
		@Pc(17) int local17 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg4, TextureOpShapeRasterizer.clipTop);
		@Pc(23) int local23 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg3, TextureOpShapeRasterizer.clipLeft);
		@Pc(35) int local35 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg1, TextureOpShapeRasterizer.clipLeft);
		for (@Pc(37) int local37 = local11; local37 <= local17; local37++) {
			ArrayUtils.fillRange(canvas[local37], local23, local35, arg0);
		}
	}

	@OriginalMember(owner = "client!mf", name = "a", descriptor = "(BLclient!wa;)Lclient!ta;")
	public static TextureOpShapeLine create(@OriginalArg(1) Buffer arg0) {
		return new TextureOpShapeLine(arg0.g2b(), arg0.g2b(), arg0.g2b(), arg0.g2b(), arg0.g3(), arg0.g3(), arg0.g1());
	}

	@OriginalMember(owner = "client!dm", name = "a", descriptor = "(IBIII)V")
	public static void fillCircle(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3) {
		@Pc(15) int local15 = 0;
		ArrayUtils.fillRange(canvas[arg3], arg0 - arg1, arg0 - -arg1, arg2);
		@Pc(32) int local32 = -arg1;
		@Pc(34) int local34 = arg1;
		@Pc(36) int local36 = -1;
		while (local15 < local34) {
			local15++;
			local36 += 2;
			local32 += local36;
			if (local32 >= 0) {
				local34--;
				local32 -= local34 << 1;
				@Pc(65) int[] local65 = canvas[arg3 + local34];
				@Pc(71) int[] local71 = canvas[arg3 - local34];
				@Pc(76) int local76 = arg0 + local15;
				@Pc(81) int local81 = arg0 - local15;
				ArrayUtils.fillRange(local65, local81, local76, arg2);
				ArrayUtils.fillRange(local71, local81, local76, arg2);
			}
			@Pc(97) int local97 = local34 + arg0;
			@Pc(102) int local102 = arg0 - local34;
			@Pc(109) int[] local109 = canvas[arg3 + local15];
			@Pc(116) int[] local116 = canvas[arg3 - local15];
			ArrayUtils.fillRange(local109, local102, local97, arg2);
			ArrayUtils.fillRange(local116, local102, local97, arg2);
		}
	}

	@OriginalMember(owner = "client!sa", name = "a", descriptor = "(IIIBI)V")
	public static void fillRange(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(4) int arg3) {
		if (arg3 <= arg2) {
			ArrayUtils.fillRange(canvas[arg1], arg3, arg2, arg0);
		} else {
			ArrayUtils.fillRange(canvas[arg1], arg2, arg3, arg0);
		}
	}

	@OriginalMember(owner = "client!hm", name = "a", descriptor = "(IIIII)V")
	public static void fillVerticalLine(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3) {
		@Pc(8) int local8;
		if (arg0 <= arg2) {
			for (local8 = arg0; local8 < arg2; local8++) {
				canvas[local8][arg1] = arg3;
			}
		} else {
			for (local8 = arg2; local8 < arg0; local8++) {
				canvas[local8][arg1] = arg3;
			}
		}
	}

	@OriginalMember(owner = "client!th", name = "a", descriptor = "(BIIIII)V")
	public static void plotLine(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		@Pc(9) int local9 = arg1 - arg2;
		@Pc(14) int local14 = arg3 - arg4;
		if (local14 == 0) {
			if (local9 != 0) {
				fillVerticalLine(arg2, arg4, arg1, arg0);
			}
		} else if (local9 == 0) {
			fillRange(arg0, arg2, arg3, arg4);
		} else {
			if (local9 < 0) {
				local9 = -local9;
			}
			if (local14 < 0) {
				local14 = -local14;
			}
			@Pc(70) boolean local70 = local14 < local9;
			@Pc(74) int local74;
			@Pc(78) int local78;
			if (local70) {
				local74 = arg4;
				arg4 = arg2;
				local78 = arg3;
				arg2 = local74;
				arg3 = arg1;
				arg1 = local78;
			}
			if (arg3 < arg4) {
				local74 = arg4;
				arg4 = arg3;
				arg3 = local74;
				local78 = arg2;
				arg2 = arg1;
				arg1 = local78;
			}
			local74 = arg2;
			local78 = arg3 - arg4;
			@Pc(111) int local111 = arg1 - arg2;
			@Pc(116) int local116 = -(local78 >> 1);
			@Pc(123) int local123 = arg1 <= arg2 ? -1 : 1;
			if (local111 < 0) {
				local111 = -local111;
			}
			@Pc(136) int local136;
			if (local70) {
				for (local136 = arg4; local136 <= arg3; local136++) {
					canvas[local136][local74] = arg0;
					local116 += local111;
					if (local116 > 0) {
						local74 += local123;
						local116 -= local78;
					}
				}
			} else {
				for (local136 = arg4; local136 <= arg3; local136++) {
					local116 += local111;
					canvas[local74][local136] = arg0;
					if (local116 > 0) {
						local74 += local123;
						local116 -= local78;
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!ub", name = "a", descriptor = "(IIIIIII)V")
	public static void drawRectOutline(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5) {
		if (TextureOpShapeRasterizer.clipLeft <= arg1 && TextureOpShapeRasterizer.clipRight >= arg3 && TextureOpShapeRasterizer.clipTop <= arg2 && arg4 <= TextureOpShapeRasterizer.clipBottom) {
			if (arg5 == 1) {
				drawRectOutline1pxFast(arg0, arg3, arg2, arg4, arg1);
			} else {
				drawThickRectOutlineFast(arg3, arg2, arg0, arg4, arg5, arg1);
			}
		} else if (arg5 == 1) {
			drawRectOutline1pxClipped(arg0, arg1, arg4, arg3, arg2);
		} else {
			drawThickRectOutlineClipped(arg4, arg5, arg3, arg1, arg0, arg2);
		}
	}

	@OriginalMember(owner = "client!kh", name = "a", descriptor = "(IIIBII)V")
	public static void drawRectOutline1pxFast(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		@Pc(8) int local8 = arg2 + 1;
		ArrayUtils.fillRange(canvas[arg2], arg4, arg1, arg0);
		@Pc(17) int local17 = arg3 - 1;
		ArrayUtils.fillRange(canvas[arg3], arg4, arg1, arg0);
		for (@Pc(29) int local29 = local8; local29 <= local17; local29++) {
			@Pc(40) int[] local40 = canvas[local29];
			local40[arg4] = local40[arg1] = arg0;
		}
	}

	@OriginalMember(owner = "client!ok", name = "a", descriptor = "(IIIIIII)V")
	public static void drawThickRectOutlineFast(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5) {
		@Pc(5) int local5 = arg4 + arg1;
		@Pc(14) int local14 = arg3 - arg4;
		@Pc(29) int local29 = arg4 + arg5;
		@Pc(31) int local31;
		for (local31 = arg1; local31 < local5; local31++) {
			ArrayUtils.fillRange(canvas[local31], arg5, arg0, arg2);
		}
		for (local31 = arg3; local31 > local14; local31--) {
			ArrayUtils.fillRange(canvas[local31], arg5, arg0, arg2);
		}
		@Pc(70) int local70 = arg0 - arg4;
		for (local31 = local5; local31 <= local14; local31++) {
			@Pc(83) int[] local83 = canvas[local31];
			ArrayUtils.fillRange(local83, arg5, local29, arg2);
			ArrayUtils.fillRange(local83, local70, arg0, arg2);
		}
	}

	@OriginalMember(owner = "client!nk", name = "a", descriptor = "(IIIIIB)V")
	public static void drawRectOutline1pxClipped(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4) {
		if (arg4 > TextureOpShapeRasterizer.clipBottom || arg2 < TextureOpShapeRasterizer.clipTop) {
			return;
		}
		@Pc(24) boolean local24;
		if (TextureOpShapeRasterizer.clipLeft > arg1) {
			local24 = false;
			arg1 = TextureOpShapeRasterizer.clipLeft;
		} else if (TextureOpShapeRasterizer.clipRight >= arg1) {
			local24 = true;
		} else {
			local24 = false;
			arg1 = TextureOpShapeRasterizer.clipRight;
		}
		@Pc(43) boolean local43;
		if (arg3 < TextureOpShapeRasterizer.clipLeft) {
			arg3 = TextureOpShapeRasterizer.clipLeft;
			local43 = false;
		} else if (arg3 > TextureOpShapeRasterizer.clipRight) {
			arg3 = TextureOpShapeRasterizer.clipRight;
			local43 = false;
		} else {
			local43 = true;
		}
		if (TextureOpShapeRasterizer.clipTop > arg4) {
			arg4 = TextureOpShapeRasterizer.clipTop;
		} else {
			ArrayUtils.fillRange(canvas[arg4++], arg1, arg3, arg0);
		}
		if (arg2 <= TextureOpShapeRasterizer.clipBottom) {
			ArrayUtils.fillRange(canvas[arg2--], arg1, arg3, arg0);
		} else {
			arg2 = TextureOpShapeRasterizer.clipBottom;
		}
		@Pc(98) int local98;
		if (local24 && local43) {
			for (local98 = arg4; local98 <= arg2; local98++) {
				@Pc(105) int[] local105 = canvas[local98];
				local105[arg1] = local105[arg3] = arg0;
			}
		} else if (local24) {
			for (local98 = arg4; local98 <= arg2; local98++) {
				canvas[local98][arg1] = arg0;
			}
		} else if (local43) {
			for (local98 = arg4; local98 <= arg2; local98++) {
				canvas[local98][arg3] = arg0;
			}
		}
	}

	@OriginalMember(owner = "client!an", name = "a", descriptor = "(IIIIIII)V")
	public static void drawThickRectOutlineClipped(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5) {
		@Pc(11) int local11 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg5, TextureOpShapeRasterizer.clipTop);
		@Pc(17) int local17 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg0, TextureOpShapeRasterizer.clipTop);
		@Pc(23) int local23 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg3, TextureOpShapeRasterizer.clipLeft);
		@Pc(29) int local29 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg2, TextureOpShapeRasterizer.clipLeft);
		@Pc(42) int local42 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg5 + arg1, TextureOpShapeRasterizer.clipTop);
		@Pc(51) int local51 = IntUtils.clamp(TextureOpShapeRasterizer.clipBottom, arg0 - arg1, TextureOpShapeRasterizer.clipTop);
		@Pc(53) int local53;
		for (local53 = local11; local53 < local42; local53++) {
			ArrayUtils.fillRange(canvas[local53], local23, local29, arg4);
		}
		for (local53 = local17; local53 > local51; local53--) {
			ArrayUtils.fillRange(canvas[local53], local23, local29, arg4);
		}
		@Pc(95) int local95 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg1 + arg3, TextureOpShapeRasterizer.clipLeft);
		@Pc(104) int local104 = IntUtils.clamp(TextureOpShapeRasterizer.clipRight, arg2 - arg1, TextureOpShapeRasterizer.clipLeft);
		for (local53 = local42; local53 <= local51; local53++) {
			@Pc(117) int[] local117 = canvas[local53];
			ArrayUtils.fillRange(local117, local23, local95, arg4);
			ArrayUtils.fillRange(local117, local104, local29, arg4);
		}
	}

	@OriginalMember(owner = "client!gg", name = "a", descriptor = "([[IZ)V")
	public static void setCanvas(@OriginalArg(0) int[][] arg0) {
		canvas = arg0;
	}

	@OriginalMember(owner = "client!ta", name = "a", descriptor = "(IZI)V")
	@Override
	public final void renderOutlinedShape(@OriginalArg(0) int arg0, @OriginalArg(2) int arg1) {
		@Pc(10) int local10 = arg1 * this.right >> 12;
		@Pc(17) int local17 = this.bottom * arg0 >> 12;
		@Pc(24) int local24 = this.left * arg1 >> 12;
		@Pc(31) int local31 = this.top * arg0 >> 12;
		drawRectOutline(this.outlineColor, local24, local31, local10, local17, this.lineWidth);
	}

	@OriginalMember(owner = "client!ta", name = "c", descriptor = "(III)V")
	@Override
	public final void renderFilledShape(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1) {
		@Pc(10) int local10 = this.left * arg0 >> 12;
		@Pc(17) int local17 = arg0 * this.right >> 12;
		@Pc(24) int local24 = arg1 * this.top >> 12;
		@Pc(31) int local31 = arg1 * this.bottom >> 12;
		fillSolidRect(local10, this.fillColor, local31, local17, local24);
	}

	@OriginalMember(owner = "client!ta", name = "a", descriptor = "(III)V")
	@Override
	public final void renderBorderedShape(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1) {
		@Pc(14) int local14 = arg1 * this.left >> 12;
		@Pc(21) int local21 = this.top * arg0 >> 12;
		@Pc(28) int local28 = arg1 * this.right >> 12;
		@Pc(35) int local35 = this.bottom * arg0 >> 12;
		drawFramedRect(this.lineWidth, local35, this.fillColor, this.outlineColor, local21, local28, local14);
	}
}
