package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;
import plugin.PluginRepository;

import java.nio.charset.StandardCharsets;

public class Cs1ScriptRunner {
	@OriginalMember(owner = "client!bm", name = "p", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_1 = JagString.parse("(U1");
	@OriginalMember(owner = "client!wh", name = "u", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_2 = JagString.parse("(U2");
	@OriginalMember(owner = "client!mj", name = "g", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_3 = JagString.parse("(U3");
	@OriginalMember(owner = "client!hh", name = "a", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_4 = JagString.parse("(U4");
	@OriginalMember(owner = "client!tb", name = "P", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_5 = JagString.parse("(U5");
	@OriginalMember(owner = "client!jh", name = "g", descriptor = "Lclient!na;")
	public static final JagString CS1_PLACEHOLDER_DNS = JagString.parse("(Udns");
	@OriginalMember(owner = "client!lh", name = "z", descriptor = "Lclient!na;")
	public static final JagString CACHE_STAT_SEPARATOR = JagString.parse("(U (X");
	@OriginalMember(owner = "client!bd", name = "b", descriptor = "Lclient!na;")
	public static final JagString CACHE_STAT_SUFFIX = JagString.parse("(U(Y");
	@OriginalMember(owner = "client!oj", name = "p", descriptor = "I")
	public static final int SCROLLBAR_TRACK_COLOR = 0x23201b;
	@OriginalMember(owner = "client!ec", name = "l", descriptor = "I")
	public static final int SCROLLBAR_THUMB_COLOR = 0x4d4233;
	@OriginalMember(owner = "client!rl", name = "Z", descriptor = "I")
	public static final int SCROLLBAR_THUMB_HIGHLIGHT_COLOR = 0x766654;
	@OriginalMember(owner = "client!bj", name = "V", descriptor = "I")
	public static final int SCROLLBAR_THUMB_SHADOW_COLOR = 0x332d25;
	@OriginalMember(owner = "client!pg", name = "V", descriptor = "I")
	public static final int TOOLTIP_DISPLAY_DELAY = 50;
	@OriginalMember(owner = "client!th", name = "m", descriptor = "[Lclient!be;")
	public static Component[] deferredDragComponents;
	@OriginalMember(owner = "client!k", name = "j", descriptor = "I")
	public static int deferredDragRenderY;
	@OriginalMember(owner = "client!gf", name = "K", descriptor = "I")
	public static int deferredDragRenderX;
	@OriginalMember(owner = "client!ac", name = "p", descriptor = "Lclient!be;")
	public static Component dragParentComponent = null;
	@OriginalMember(owner = "client!ib", name = "e", descriptor = "Lclient!be;")
	public static Component draggedComponent = null;
	@OriginalMember(owner = "client!km", name = "pc", descriptor = "Z")
	public static boolean isDragging = false;
	@OriginalMember(owner = "client!gg", name = "db", descriptor = "I")
	public static int dragBoundsMinX = -1;
	@OriginalMember(owner = "client!nb", name = "d", descriptor = "I")
	public static int dragStartMouseY = 0;
	@OriginalMember(owner = "client!kd", name = "Bb", descriptor = "I")
	public static int tooltipRenderX = -1;
	@OriginalMember(owner = "client!d", name = "R", descriptor = "Lclient!be;")
	public static Component pleaseWaitComponent = null;
	@OriginalMember(owner = "client!hi", name = "a", descriptor = "I")
	public static int gameSceneTooltipX = -1;
	@OriginalMember(owner = "client!ld", name = "c", descriptor = "I")
	public static int tooltipRenderY = -1;
	@OriginalMember(owner = "client!lf", name = "k", descriptor = "I")
	public static int cachedActiveSpriteId = -1;
	@OriginalMember(owner = "client!mh", name = "X", descriptor = "I")
	public static int cachedDefaultSpriteId = -1;
	@OriginalMember(owner = "client!ig", name = "b", descriptor = "I")
	public static int worldMapViewportX;
	@OriginalMember(owner = "client!ig", name = "f", descriptor = "I")
	public static int worldMapViewportY;
	@OriginalMember(owner = "client!hc", name = "P", descriptor = "I")
	public static int mapHighlightPulseCounter;
	@OriginalMember(owner = "client!u", name = "a", descriptor = "I")
	public static int dragStartMouseX = 0;
	@OriginalMember(owner = "client!re", name = "y", descriptor = "I")
	public static int dragElapsedTicks;
	@OriginalMember(owner = "client!em", name = "z", descriptor = "Z")
	public static boolean isMenuOpen = false;

	@OriginalMember(owner = "client!we", name = "a", descriptor = "(BILclient!be;)I")
	public static int run(@OriginalArg(1) int arg0, @OriginalArg(2) Component arg1) {
		if (arg1.cs1Scripts == null || arg0 >= arg1.cs1Scripts.length) {
			return -2;
		}
		try {
			@Pc(33) int[] script = arg1.cs1Scripts[arg0];
			@Pc(35) byte accumulatorMode = 0;
			@Pc(37) int accumulator = 0;
			@Pc(39) int pc = 0;
			while (true) {
				@Pc(41) int value = 0;
				@Pc(46) int opcode = script[pc++];
				@Pc(48) byte nextAccumulatorMode = 0;
				if (opcode == 0) {
					return accumulator;
				}
				if (opcode == 15) {
					nextAccumulatorMode = 1;
				}
				if (opcode == 16) {
					nextAccumulatorMode = 2;
				}
				if (opcode == 1) {
					value = PlayerSkillXpTable.boostedLevels[script[pc++]];
				}
				if (opcode == 17) {
					nextAccumulatorMode = 3;
				}
				if (opcode == 2) {
					value = PlayerSkillXpTable.baseLevels[script[pc++]];
				}
				if (opcode == 3) {
					value = PlayerSkillXpTable.experience[script[pc++]];
				}
				@Pc(124) int pc2;
				@Pc(135) Component otherComponent;
				@Pc(140) int pc3;
				@Pc(152) int local152;
				if (opcode == 4) {
					pc2 = script[pc++] << 16;
					@Pc(131) int componentId = pc2 + script[pc++];
					otherComponent = InterfaceList.getComponent(componentId);
					pc3 = script[pc++];
					if (pc3 != -1 && (!ObjTypeList.get(pc3).members || LoginManager.mapMembers)) {
						for (local152 = 0; local152 < otherComponent.objTypes.length; local152++) {
							if (pc3 + 1 == otherComponent.objTypes[local152]) {
								value += otherComponent.objCounts[local152];
							}
						}
					}
				}
				if (opcode == 5) {
					value = VarpDomain.activeVarps[script[pc++]];
				}
				if (opcode == 6) {
					value = PlayerSkillXpTable.xpLevelLookup[PlayerSkillXpTable.baseLevels[script[pc++]] - 1];
				}
				if (opcode == 7) {
					value = VarpDomain.activeVarps[script[pc++]] * 100 / 46875;
				}
				if (opcode == 8) {
					value = PlayerList.self.combatLevel;
				}
				if (opcode == 9) {
					for (pc2 = 0; pc2 < 25; pc2++) {
						if (PlayerSkillXpTable.ENABLED_SKILLS[pc2]) {
							value += PlayerSkillXpTable.baseLevels[pc2];
						}
					}
				}
				if (opcode == 10) {
					pc2 = script[pc++] << 16;
					pc2 += script[pc++];
					otherComponent = InterfaceList.getComponent(pc2);
					pc3 = script[pc++];
					if (pc3 != -1 && (!ObjTypeList.get(pc3).members || LoginManager.mapMembers)) {
						for (local152 = 0; local152 < otherComponent.objTypes.length; local152++) {
							if (otherComponent.objTypes[local152] == pc3 + 1) {
								value = 999999999;
								break;
							}
						}
					}
				}
				if (opcode == 11) {
					value = Player.runEnergy;
				}
				if (opcode == 12) {
					value = Player.weight;
				}
				if (opcode == 13) {
					pc2 = VarpDomain.activeVarps[script[pc++]];
					@Pc(353) int local353 = script[pc++];
					value = (0x1 << local353 & pc2) == 0 ? 0 : 1;
				}
				if (opcode == 14) {
					pc2 = script[pc++];
					value = VarpDomain.getVarbit(pc2);
				}
				if (opcode == 18) {
					value = (PlayerList.self.xFine >> 7) + Camera.originX;
				}
				if (opcode == 19) {
					value = (PlayerList.self.yFine >> 7) + Camera.originY;
				}
				if (opcode == 20) {
					value = script[pc++];
				}
				if (nextAccumulatorMode == 0) {
					if (accumulatorMode == 0) {
						accumulator += value;
					}
					if (accumulatorMode == 1) {
						accumulator -= value;
					}
					if (accumulatorMode == 2 && value != 0) {
						accumulator /= value;
					}
					if (accumulatorMode == 3) {
						accumulator *= value;
					}
					accumulatorMode = 0;
				} else {
					accumulatorMode = nextAccumulatorMode;
				}
			}
		} catch (@Pc(464) Exception ignored) {
			return -1;
		}
	}

	@OriginalMember(owner = "client!k", name = "a", descriptor = "(Lclient!be;Lclient!na;I)Lclient!na;")
	public static JagString interpolate(@OriginalArg(0) Component arg0, @OriginalArg(1) JagString text) {
		if (text.indexOf(JagString.PERCENT_SIGN) == -1) {
			return text;
		}

		while (true) {
			@Pc(14) int local14 = text.indexOf(CS1_PLACEHOLDER_1);
			if (local14 == -1) {
				while (true) {
					local14 = text.indexOf(CS1_PLACEHOLDER_2);
					if (local14 == -1) {
						while (true) {
							local14 = text.indexOf(CS1_PLACEHOLDER_3);
							if (local14 == -1) {
								while (true) {
									local14 = text.indexOf(CS1_PLACEHOLDER_4);
									if (local14 == -1) {
										while (true) {
											local14 = text.indexOf(CS1_PLACEHOLDER_5);
											if (local14 == -1) {
												while (true) {
													local14 = text.indexOf(CS1_PLACEHOLDER_DNS);
													if (local14 == -1) {
														return text;
													}
													@Pc(246) JagString local246 = JagString.EMPTY;
													if (Player.lastLogAddress != null) {
														local246 = JagString.formatIp(Player.lastLogAddress.intArg2);
														if (Player.lastLogAddress.result != null) {
															@Pc(265) byte[] local265 = ((String) Player.lastLogAddress.result).getBytes(StandardCharsets.ISO_8859_1);
															local246 = JagString.decodeString(local265, local265.length, 0);
														}
													}
													text = JagString.concatenate(new JagString[]{text.substring(local14, 0), local246, text.substring(local14 + 4)});
												}
											}
											text = JagString.concatenate(new JagString[]{text.substring(local14, 0), StringUtils.toString(run(4, arg0)), text.substring(local14 + 2)});
										}
									}
									text = JagString.concatenate(new JagString[]{text.substring(local14, 0), StringUtils.toString(run(3, arg0)), text.substring(local14 + 2)});
								}
							}
							text = JagString.concatenate(new JagString[]{text.substring(local14, 0), StringUtils.toString(run(2, arg0)), text.substring(local14 + 2)});
						}
					}
					text = JagString.concatenate(new JagString[]{text.substring(local14, 0), StringUtils.toString(run(1, arg0)), text.substring(local14 + 2)});
				}
			}
			text = JagString.concatenate(new JagString[]{text.substring(local14, 0), StringUtils.toString(run(0, arg0)), text.substring(local14 + 2)});
		}
	}

	@OriginalMember(owner = "client!md", name = "a", descriptor = "(Lclient!be;I)Z")
	public static boolean isTrue(@OriginalArg(0) Component component) {
		if (component.cs1ComparisonOpcodes == null) {
			return false;
		}
		for (@Pc(14) int i = 0; i < component.cs1ComparisonOpcodes.length; i++) {
			@Pc(34) int value = run(i, component);
			@Pc(39) int operand = component.cs1ComparisonOperands[i];
			if (component.cs1ComparisonOpcodes[i] == 2) {
				if (operand <= value) {
					return false;
				}
			} else if (component.cs1ComparisonOpcodes[i] == 3) {
				if (value <= operand) {
					return false;
				}
			} else if (component.cs1ComparisonOpcodes[i] == 4) {
				if (value == operand) {
					return false;
				}
			} else if (operand != value) {
				return false;
			}
		}
		return true;
	}

	@OriginalMember(owner = "client!gn", name = "a", descriptor = "(III[Lclient!be;IIIIBI)V")
	public static void renderComponent(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) Component[] components, @OriginalArg(4) int arg4, @OriginalArg(5) int layer, @OriginalArg(6) int arg6, @OriginalArg(7) int arg7, @OriginalArg(9) int parentRectangle) {
		if (GlRenderer.enabled) {
			GlRaster.setClip(arg0, arg6, arg4, arg7);
		} else {
			SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
			Rasteriser.prepare();
		}

		for (@Pc(18) int i = 0; i < components.length; i++) {
			@Pc(30) Component component = components[i];
			if (component != null && (component.overlayer == layer || layer == 0xabcdabcd && component == draggedComponent)) {
				@Pc(57) int rectangle;
				if (parentRectangle == -1) {
					InterfaceList.rectangleX[InterfaceList.rectangles] = arg2 + component.x;
					InterfaceList.rectangleY[InterfaceList.rectangles] = component.y + arg1;
					InterfaceList.rectangleWidth[InterfaceList.rectangles] = component.width;
					InterfaceList.rectangleHeight[InterfaceList.rectangles] = component.height;
					rectangle = InterfaceList.rectangles++;
				} else {
					rectangle = parentRectangle;
				}
				component.rectangleLoop = client.loop;
				component.rectangle = rectangle;
				if (!component.if3 || !InterfaceList.isHidden(component)) {
					if (component.clientCode > 0) {
						applyClientCode(component);
					}
					@Pc(114) int local114 = arg1 + component.y;
					@Pc(117) int alpha = component.alpha;
					@Pc(123) int local123 = component.x + arg2;
					if (Cheat.qaOpTest && (InterfaceList.getServerActiveProperties(component).events != 0 || component.type == 0) && alpha > 127) {
						alpha = 127;
					}
					@Pc(166) int local166;
					@Pc(164) int local164;
					if (component == draggedComponent) {
						if (layer != 0xabcdabcd && !component.dragRenderBehavior) {
							deferredDragRenderX = arg2;
							deferredDragRenderY = arg1;
							deferredDragComponents = components;
							continue;
						}
						if (isDragging && InterfaceList.aBoolean174) {
							local164 = Mouse.lastMouseY;
							local166 = Mouse.lastMouseX;
							local164 -= dragStartMouseY;
							if (local164 < InterfaceList.anInt5103) {
								local164 = InterfaceList.anInt5103;
							}
							if (local164 + component.height > dragParentComponent.height + InterfaceList.anInt5103) {
								local164 = dragParentComponent.height + InterfaceList.anInt5103 - component.height;
							}
							local114 = local164;
							local166 -= dragStartMouseX;
							if (dragBoundsMinX > local166) {
								local166 = dragBoundsMinX;
							}
							if (dragParentComponent.width + dragBoundsMinX < component.width + local166) {
								local166 = dragParentComponent.width + dragBoundsMinX - component.width;
							}
							local123 = local166;
						}
						if (!component.dragRenderBehavior) {
							alpha = 128;
						}
					}
					@Pc(302) int local302;
					@Pc(291) int local291;
					@Pc(270) int local270;
					@Pc(276) int local276;
					if (component.type == 2) {
						local291 = arg7;
						local302 = arg4;
						local164 = arg6;
						local166 = arg0;
					} else {
						local164 = local114 > arg6 ? local114 : arg6;
						local166 = arg0 < local123 ? local123 : arg0;
						local270 = component.width + local123;
						local276 = local114 + component.height;
						if (component.type == 9) {
							local276++;
							local270++;
						}
						local291 = arg7 <= local276 ? arg7 : local276;
						local302 = local270 >= arg4 ? arg4 : local270;
					}
					if (!component.if3 || local302 > local166 && local164 < local291) {
						@Pc(468) int local468;
						@Pc(503) int memory;
						@Pc(514) int color;
						@Pc(518) int cardMemory;
						@Pc(556) int local556;
						@Pc(563) int local563;
						@Pc(571) int local571;
						@Pc(545) int objId;
						if (component.clientCode != 0) {
							if (component.clientCode == 1337 || component.clientCode == 1403 && GlRenderer.enabled) {
								InterfaceList.aClass13_26 = component;
								InterfaceList.viewportX = local114;
								gameSceneTooltipX = local123;
								ScriptRunner.renderGameScene(component.height, component.clientCode == 1403, local123, component.width, local114);
								if (GlRenderer.enabled) {
									GlRaster.setClip(arg0, arg6, arg4, arg7);
								} else {
									SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
								}
								continue;
							}
							if (component.clientCode == 1338) {
								if (!component.buildClickMask()) {
									continue;
								}
								MiniMap.render(rectangle, local114, local123, component);
								if (GlRenderer.enabled) {
									GlRaster.setClip(arg0, arg6, arg4, arg7);
								} else {
									SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
								}
								if (MiniMap.state != 0 && MiniMap.state != 3 || isMenuOpen || local166 > ScriptRunner.interfaceMouseX || ScriptRunner.interfaceMouseY < local164 || ScriptRunner.interfaceMouseX >= local302 || local291 <= ScriptRunner.interfaceMouseY) {
									continue;
								}
								local270 = ScriptRunner.interfaceMouseX - local123;
								local276 = ScriptRunner.interfaceMouseY - local114;
								local468 = component.clickMaskStart[local276];
								if (local270 < local468 || local270 > local468 + component.clickMaskWidth[local276]) {
									continue;
								}
								local276 -= component.height / 2;
								memory = (int) Camera.yawTarget + MiniMap.compassAngleOffset & 0x7FF;
								local270 -= component.width / 2;
								color = MathUtils.sin[memory];
								cardMemory = MathUtils.cos[memory];
								color = (MiniMap.zoomOffset + 256) * color >> 8;
								cardMemory = (MiniMap.zoomOffset + 256) * cardMemory >> 8;
								objId = cardMemory * local276 - color * local270 >> 11;
								local556 = local276 * color + local270 * cardMemory >> 11;
								local563 = PlayerList.self.xFine + local556 >> 7;
								local571 = PlayerList.self.yFine - objId >> 7;
								if (MiniMenu.isTargeting && (MiniMenu.targetMask & 0x40) != 0) {
									@Pc(583) Component local583 = InterfaceList.getComponent(MiniMenu.targetInterfaceId, MiniMenu.targetChildId);
									if (local583 == null) {
										MiniMenu.cancelTargeting();
									} else {
										MiniMenu.add(MiniMenu.targetCursorId, 1L, MiniMenu.ARROW_SUFFIX, local563, (short) 11, MiniMenu.targetVerb, local571);
									}
									continue;
								}
								if (client.game == 1) {
									MiniMenu.add(-1, 1L, JagString.EMPTY, local563, (short) 36, LocalizedText.FACEHERE, local571);
								}
								MiniMenu.add(-1, 1L, JagString.EMPTY, local563, (short) 60, MiniMenu.walkText, local571);
								continue;
							}
							if (component.clientCode == 1339) {
								if (component.buildClickMask()) {
									renderCompass(local123, local114, component, rectangle);
									if (GlRenderer.enabled) {
										GlRaster.setClip(arg0, arg6, arg4, arg7);
									} else {
										SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
									}
								}
								continue;
							}
							if (component.clientCode == 1400) {
								WorldMap.render(local123, local114, component.height, component.width);
								InterfaceList.rectangleDirty[rectangle] = true;
								InterfaceList.rectangleRedraw[rectangle] = true;
								if (GlRenderer.enabled) {
									GlRaster.setClip(arg0, arg6, arg4, arg7);
								} else {
									SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
								}
								continue;
							}
							if (component.clientCode == 1401) {
								renderWorldMapOverview(local123, component.height, component.width, local114);
								InterfaceList.rectangleDirty[rectangle] = true;
								InterfaceList.rectangleRedraw[rectangle] = true;
								if (GlRenderer.enabled) {
									GlRaster.setClip(arg0, arg6, arg4, arg7);
								} else {
									SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
								}
								continue;
							}
							if (component.clientCode == 1402) {
								if (!GlRenderer.enabled) {
									Flames.render(local123, local114);
									InterfaceList.rectangleDirty[rectangle] = true;
									InterfaceList.rectangleRedraw[rectangle] = true;
								}
								continue;
							}
							if (component.clientCode == 1405) {
								PluginRepository.Draw();
								InterfaceList.rectangleDirty[rectangle] = true;
								InterfaceList.rectangleRedraw[rectangle] = true;

								//Above are inauthentic changes to call plugin draws and redraw this interface. Below I have left intact all the authentic code.

								if (!Cheat.displayFps) {
									continue;
								}
								local270 = component.width + local123;
								local276 = local114 + 15;
								Fonts.p12Full.renderRight(JagString.concatenate(new JagString[]{Cheat.DEBUG_FPS2, JagString.parseInt((int) GameShell.framesPerSecond)}), local270, local276, 16776960, 0);
								local276 += 15;
								@Pc(795) Runtime runtime = Runtime.getRuntime();
								memory = (int) ((runtime.totalMemory() - runtime.freeMemory()) / 1024L / 1024L);
								color = 16776960;
								if (memory > 128) {
									color = 16711680;
								}
								Fonts.p12Full.renderRight(JagString.concatenate(new JagString[]{Cheat.DEBUG_MEM, JagString.parseInt(memory), Cheat.DEBUG_MEM_UNIT}), local270, local276, color, 0);
								local276 += 15;
								if (GlRenderer.enabled) {
									color = 16776960;
									cardMemory = (GlCleaner.onCardTexture + GlCleaner.onCardGeometry + GlCleaner.onCard2d) / 1024 / 1024;
									if (cardMemory > 64) {
										color = 16711680;
									}
									Fonts.p12Full.renderRight(JagString.concatenate(new JagString[]{Cheat.DEBUG_CARD, JagString.parseInt(cardMemory), Cheat.DEBUG_MEM_UNIT}), local270, local276, color, 0);
									local276 += 15;
								}
								cardMemory = 0;
								objId = 0;
								local556 = 0;
								for (local563 = 0; local563 < 28; local563++) {
									cardMemory += client.js5Providers[local563].getIndexSize();
									local556 += client.js5Providers[local563].getVerifiedGroups();
									objId += client.js5Providers[local563].getTotalVerifiedGroups();
								}
								local571 = local556 * 10000 / cardMemory;
								local563 = objId * 100 / cardMemory;
								@Pc(968) JagString local968 = JagString.concatenate(new JagString[]{Cheat.DEBUG_CAHE, StringUtils.formatNumber(0, true, 2, local571), CACHE_STAT_SEPARATOR, JagString.parseInt(local563), CACHE_STAT_SUFFIX});
								Fonts.p11Full.renderRight(local968, local270, local276, 16776960, 0);
								local276 += 12;
								InterfaceList.rectangleDirty[rectangle] = true;
								InterfaceList.rectangleRedraw[rectangle] = true;
								continue;
							}
							if (component.clientCode == 1406) {
								tooltipRenderY = local114;
								LoginManager.tooltipComponent = component;
								tooltipRenderX = local123;
								continue;
							}
						}
						if (!isMenuOpen) {
							if (component.type == 0 && component.noClickThrough && ScriptRunner.interfaceMouseX >= local166 && ScriptRunner.interfaceMouseY >= local164 && ScriptRunner.interfaceMouseX < local302 && local291 > ScriptRunner.interfaceMouseY && !Cheat.qaOpTest) {
								MiniMenu.size = 1;
								MiniMenu.cursors[0] = MiniMenu.defaultCursorId;
								MiniMenu.ops[0] = LocalizedText.CANCEL;
								MiniMenu.opBases[0] = JagString.EMPTY;
								MiniMenu.actions[0] = 1005;
							}
							if (local166 <= ScriptRunner.interfaceMouseX && local164 <= ScriptRunner.interfaceMouseY && local302 > ScriptRunner.interfaceMouseX && local291 > ScriptRunner.interfaceMouseY) {
								MiniMenu.addComponentEntries(ScriptRunner.interfaceMouseY - local114, -local123 + ScriptRunner.interfaceMouseX, component);
							}
						}
						if (component.type == 0) {
							if (!component.if3 && InterfaceList.isHidden(component) && InterfaceList.aClass13_22 != component) {
								continue;
							}
							if (!component.if3) {
								if (component.scrollMaxV - component.height < component.scrollY) {
									component.scrollY = component.scrollMaxV - component.height;
								}
								if (component.scrollY < 0) {
									component.scrollY = 0;
								}
							}
							renderComponent(local166, local114 - component.scrollY, -component.scrollX + local123, components, local302, component.id, local164, local291, rectangle);
							if (component.createdComponents != null) {
								renderComponent(local166, local114 - component.scrollY, -component.scrollX + local123, component.createdComponents, local302, component.id, local164, local291, rectangle);
							}
							@Pc(1186) ComponentPointer local1186 = (ComponentPointer) InterfaceList.openInterfaces.get(component.id);
							if (local1186 != null) {
								if (local1186.anInt5879 == 0 && !isMenuOpen && ScriptRunner.interfaceMouseX >= local166 && local164 <= ScriptRunner.interfaceMouseY && local302 > ScriptRunner.interfaceMouseX && ScriptRunner.interfaceMouseY < local291 && !Cheat.qaOpTest) {
									MiniMenu.ops[0] = LocalizedText.CANCEL;
									MiniMenu.size = 1;
									MiniMenu.cursors[0] = MiniMenu.defaultCursorId;
									MiniMenu.actions[0] = 1005;
									MiniMenu.opBases[0] = JagString.EMPTY;
								}
								renderInterface(local1186.interfaceId, local166, local302, local123, rectangle, local291, local164, local114);
							}
							if (GlRenderer.enabled) {
								GlRaster.setClip(arg0, arg6, arg4, arg7);
							} else {
								SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
								Rasteriser.prepare();
							}
						}
						if (InterfaceList.rectangleDirtySnapshot[rectangle] || Cheat.rectDebug > 1) {
							if (component.type == 0 && !component.if3 && component.scrollMaxV > component.height) {
								renderScrollbar(component.scrollY, component.scrollMaxV, component.width + local123, local114, component.height);
							}

							if (component.type != 1) {
								if (component.type == 2) {
									local270 = 0;
									for (local276 = 0; local276 < component.baseHeight; local276++) {
										for (local468 = 0; local468 < component.baseWidth; local468++) {
											int y = local114 + local276 * (component.invMarginY + 32);
											int x = (component.invMarginX + 32) * local468 + local123;
											if (local270 < 20) {
												y += component.invOffsetY[local270];
												x += component.invOffsetX[local270];
											}
											if (component.objTypes[local270] > 0) {
												objId = component.objTypes[local270] - 1;
												if (arg0 < x + 32 && x < arg4 && arg6 < y + 32 && y < arg7 || component == InterfaceList.clickedInventoryComponent && InterfaceList.mouseOverInventoryObjectIndex == local270) {
													@Pc(1476) Sprite sprite;

													if (MiniMenu.itemTargetMode == 1 && MiniMenu.selectedObjSlot == local270 && component.id == MiniMap.selectedComponentId) {
														sprite = Inv.getObjectSprite(2, objId, component.objDrawText, component.objCounts[local270], 0);
													} else {
														sprite = Inv.getObjectSprite(1, objId, component.objDrawText, component.objCounts[local270], 3153952);
													}

													if (Rasteriser.textureHasTransparency) {
														InterfaceList.rectangleDirty[rectangle] = true;
													}

													if (sprite == null) {
														InterfaceList.redraw(component);
													} else if (InterfaceList.clickedInventoryComponent == component && local270 == InterfaceList.mouseOverInventoryObjectIndex) {
														int dragX = Mouse.lastMouseX - InterfaceList.clickedInventoryComponentX;
														int dragY = Mouse.lastMouseY - InterfaceList.clickedInventoryComponentY;

														if (dragY < 5 && dragY > -5) {
															dragY = 0;
														}

														if (dragX < 5 && dragX > -5) {
															dragX = 0;
														}

														if (InterfaceList.clickedInventoryComponentCycle < 5) {
															dragX = 0;
															dragY = 0;
														}

														// draw dragged icon (at half opacity)
														sprite.renderAlpha(x + dragX, y + dragY, 128);

														if (layer != -1) {
															@Pc(1571) Component local1571 = components[layer & 0xFFFF];
															@Pc(1577) int top;
															@Pc(1575) int bottom;

															if (GlRenderer.enabled) {
																bottom = GlRaster.clipBottom;
																top = GlRaster.clipTop;
															} else {
																top = SoftwareRaster.clipTop;
																bottom = SoftwareRaster.clipBottom;
															}

															@Pc(1611) int local1611;
															if (top > dragY + y && local1571.scrollY > 0) {
																local1611 = Protocol.sceneDelta * (top - dragY - y) / 3;
																if (local1611 > Protocol.sceneDelta * 10) {
																	local1611 = Protocol.sceneDelta * 10;
																}

																if (local1611 > local1571.scrollY) {
																	local1611 = local1571.scrollY;
																}

																local1571.scrollY -= local1611;
																InterfaceList.clickedInventoryComponentY += local1611;
																InterfaceList.redraw(local1571);
															}

															if (bottom < dragY + y + 32 && local1571.scrollY < local1571.scrollMaxV - local1571.height) {
																local1611 = (y + dragY + 32 - bottom) * Protocol.sceneDelta / 3;
																if (local1611 > Protocol.sceneDelta * 10) {
																	local1611 = Protocol.sceneDelta * 10;
																}

																if (local1571.scrollMaxV - local1571.scrollY - local1571.height < local1611) {
																	local1611 = local1571.scrollMaxV - local1571.height - local1571.scrollY;
																}

																local1571.scrollY += local1611;
																InterfaceList.clickedInventoryComponentY -= local1611;
																InterfaceList.redraw(local1571);
															}
														}
													} else if (component == MiniMenu.pressedInventoryComponent && local270 == MiniMenu.pressedSlotIndex) {
														sprite.renderAlpha(x, y, 128);
													} else {
														sprite.render(x, y);
														// downscale:
														// sprite.renderResized(x, y, 36, 32);
													}
												}
											} else if (component.invSprite != null && local270 < 20) {
												@Pc(1381) Sprite local1381 = component.getInvSprite(local270);
												if (local1381 != null) {
													local1381.render(x, y);
												} else if (Component.loadFailed) {
													InterfaceList.redraw(component);
												}
											}

											local270++;
										}
									}
									PluginRepository.ComponentDraw(i, component, local123, local114);
								} else if (component.type == 3) {
									if (isTrue(component)) {
										local270 = component.activeColor;
										if (InterfaceList.aClass13_22 == component && component.activeOverColor != 0) {
											local270 = component.activeOverColor;
										}
									} else {
										local270 = component.color;
										if (component == InterfaceList.aClass13_22 && component.overColor != 0) {
											local270 = component.overColor;
										}
									}
									if (alpha == 0) {
										if (component.filled) {
											if (GlRenderer.enabled) {
												GlRaster.fillRect(local123, local114, component.width, component.height, local270);
											} else {
												SoftwareRaster.fillRect(local123, local114, component.width, component.height, local270);
											}
										} else if (GlRenderer.enabled) {
											GlRaster.drawRect(local123, local114, component.width, component.height, local270);
										} else {
											SoftwareRaster.drawRect(local123, local114, component.width, component.height, local270);
										}
									} else if (component.filled) {
										if (GlRenderer.enabled) {
											GlRaster.fillRectAlpha(local123, local114, component.width, component.height, local270, 256 - (alpha & 0xFF));
										} else {
											SoftwareRaster.fillRectAlpha(local123, local114, component.width, component.height, local270, 256 - (alpha & 0xFF));
										}
									} else if (GlRenderer.enabled) {
										GlRaster.drawRectAlpha(local123, local114, component.width, component.height, local270, 256 - (alpha & 0xFF));
									} else {
										SoftwareRaster.drawRectAlpha(local123, local114, component.width, component.height, local270, 256 - (alpha & 0xFF));
									}
									PluginRepository.ComponentDraw(i, component, local123, local114);
								} else {
									@Pc(1921) Font local1921;
									if (component.type == 4) {
										local1921 = component.getFont(Sprites.nameIcons);
										if (local1921 != null) {
											@Pc(1934) JagString local1934 = component.text;
											if (isTrue(component)) {
												local276 = component.activeColor;
												if (InterfaceList.aClass13_22 == component && component.activeOverColor != 0) {
													local276 = component.activeOverColor;
												}
												if (component.activeText.length() > 0) {
													local1934 = component.activeText;
												}
											} else {
												local276 = component.color;
												if (InterfaceList.aClass13_22 == component && component.overColor != 0) {
													local276 = component.overColor;
												}
											}
											if (component.if3 && component.objId != -1) {
												@Pc(1989) ObjType local1989 = ObjTypeList.get(component.objId);
												local1934 = local1989.name;
												if (local1934 == null) {
													local1934 = MiniMenu.NULL;
												}
												if ((local1989.stackable == 1 || component.objCount != 1) && component.objCount != -1) {
													local1934 = JagString.concatenate(new JagString[]{MiniMenu.COLOR_ITEM_ORANGE, local1934, JagString.CLOSE_COLOR_TIMES, formatItemCount(component.objCount)});
												}
											}
											if (pleaseWaitComponent == component) {
												local276 = component.color;
												local1934 = LocalizedText.PLEASEWAIT;
											}
											if (!component.if3) {
												local1934 = interpolate(component, local1934);
											}
											local1921.drawInterfaceText(local1934, local123, local114, component.width, component.height, local276, component.shadowed ? 0 : -1, component.halign, component.valign, component.vpadding);
											PluginRepository.ComponentDraw(i, component, local123, local114);
										} else if (Component.loadFailed) {
											InterfaceList.redraw(component);
										}
									} else if (component.type == 5) {
										@Pc(2094) Sprite sprite = null;
										if (component.if3) {
											if (component.objId == -1) {
												sprite = component.getSprite(false);
											} else {
												sprite = Inv.getObjectSprite(component.outlineThickness, component.objId, component.objDrawText, component.objCount, component.shadowColor);
											}

											if (sprite != null) {
												local276 = sprite.innerWidth;
												local468 = sprite.innerHeight;

												if (component.spriteTiling) {
													memory = (local276 + component.width - 1) / local276;
													color = (component.height + local468 - 1) / local468;

													if (GlRenderer.enabled) {
														GlRaster.setClipRegion(local123, local114, component.width + local123, component.height + local114);

														@Pc(2274) boolean local2274 = IntUtils.isPowerOfTwo(sprite.width);
														@Pc(2279) boolean local2279 = IntUtils.isPowerOfTwo(sprite.height);
														@Pc(2282) GlSprite local2282 = (GlSprite) sprite;
														if (local2274 && local2279) {
															if (alpha == 0) {
																local2282.renderTiled(local123, local114, memory, color);
															} else {
																local2282.renderTiledAlpha(local123, local114, 256 - (alpha & 0xFF), memory, color);
															}
															PluginRepository.ComponentDraw(i, component, local123, local114);
														} else if (local2274) {
															for (local563 = 0; local563 < color; local563++) {
																if (alpha == 0) {
																	local2282.renderTiled(local123, local563 * local468 + local114, memory, 1);
																} else {
																	local2282.renderTiledAlpha(local123, local114 + local563 * local468, -(alpha & 0xFF) + 256, memory, 1);
																}
															}
															PluginRepository.ComponentDraw(i, component, local123, local114);
														} else if (local2279) {
															for (local563 = 0; local563 < memory; local563++) {
																if (alpha == 0) {
																	local2282.renderTiled(local276 * local563 + local123, local114, 1, color);
																} else {
																	local2282.renderTiledAlpha(local276 * local563 + local123, local114, 256 - (alpha & 0xFF), 1, color);
																}
															}
															PluginRepository.ComponentDraw(i, component, local123, local114);
														} else {
															for (local563 = 0; local563 < memory; local563++) {
																for (local571 = 0; local571 < color; local571++) {
																	if (alpha == 0) {
																		sprite.render(local123 + local276 * local563, local468 * local571 + local114);
																	} else {
																		sprite.renderAlpha(local563 * local276 + local123, local468 * local571 + local114, 256 - (alpha & 0xFF));
																	}
																}
															}
															PluginRepository.ComponentDraw(i, component, local123 + local276, local468 + local114);
														}

														GlRaster.setClip(arg0, arg6, arg4, arg7);
													} else {
														SoftwareRaster.shrinkClip(local123, local114, local123 + component.width, local114 - -component.height);
														for (cardMemory = 0; cardMemory < memory; cardMemory++) {
															for (local556 = 0; local556 < color; local556++) {
																if (component.angle2d != 0) {
																	sprite.renderAngled(local114 + local468 * local556 + local468 / 2, component.angle2d, 4096, cardMemory * local276 + local123 + local276 / 2);
																} else if (alpha == 0) {
																	sprite.render(cardMemory * local276 + local123, local468 * local556 + local114);
																} else {
																	sprite.renderAlpha(cardMemory * local276 + local123, local114 + local556 * local468, 256 - (alpha & 0xFF));
																}
															}
														}
														PluginRepository.ComponentDraw(i, component, local123 + local276, local114 + local468);

														SoftwareRaster.setClip(arg0, arg6, arg4, arg7);
													}
												} else {
													memory = component.width * 4096 / local276;
													if (component.angle2d != 0) {
														sprite.renderAngled(local114 + component.height / 2, component.angle2d, memory, local123 + component.width / 2);
													} else if (alpha != 0) {
														sprite.renderAlpha(local123, local114, component.width, component.height, 256 - (alpha & 0xFF));
													} else if (local276 == component.width && local468 == component.height) {
														sprite.render(local123, local114);
													} else {
														// render icons in a container i.e bank icons
														sprite.renderResized(local123, local114, component.width, component.height);
													}
													PluginRepository.ComponentDraw(i, component, local123, local114);
												}
											} else if (Component.loadFailed) {
												InterfaceList.redraw(component);
											}
										} else {
											sprite = component.getSprite(isTrue(component));
											if (sprite != null) {
												sprite.render(local123, local114);
											} else if (Component.loadFailed) {
												InterfaceList.redraw(component);
											}
										}
									} else {
										@Pc(2611) ObjType local2611;
										if (component.type == 6) {
											@Pc(2587) boolean local2587 = isTrue(component);
											@Pc(2589) Model local2589 = null;
											if (local2587) {
												local276 = component.activeModelSeqId;
											} else {
												local276 = component.modelSeqId;
											}
											memory = 0;
											if (component.objId != -1) {
												local2611 = ObjTypeList.get(component.objId);
												if (local2611 != null) {
													local2611 = local2611.getCountVariant(component.objCount);
													@Pc(2630) SeqType local2630 = local276 == -1 ? null : SeqTypeList.get(local276);
													local2589 = local2611.getModel(component.seqNextFrame, component.seqCycle, local2630, 1, component.seqFrame);
													if (local2589 == null) {
														InterfaceList.redraw(component);
													} else {
														memory = -local2589.getMinY() / 2;
													}
												}
											} else if (component.modelType == 5) {
												if (component.modelId == -1) {
													local2589 = PlayerAppearance.DEFAULT.getBodyModel(null, -1, null, null, 0, -1, 0, -1, -1);
												} else {
													color = component.modelId & 0x7FF;
													if (color == PlayerList.selfId) {
														color = 2047;
													}
													@Pc(2751) Player local2751 = PlayerList.players[color];
													@Pc(2760) SeqType local2760 = local276 == -1 ? null : SeqTypeList.get(local276);
													if (local2751 != null && (int) local2751.username.encode37() << 11 == (component.modelId & 0xFFFFF800)) {
														local2589 = local2751.appearance.getBodyModel(null, -1, null, local2760, 0, -1, 0, component.seqFrame, 0);
													}
												}
											} else if (local276 == -1) {
												local2589 = component.getModel(-1, null, -1, 0, local2587, PlayerList.self.appearance);
												if (local2589 == null && Component.loadFailed) {
													InterfaceList.redraw(component);
												}
											} else {
												@Pc(2689) SeqType local2689 = SeqTypeList.get(local276);
												local2589 = component.getModel(component.seqNextFrame, local2689, component.seqFrame, component.seqCycle, local2587, PlayerList.self.appearance);
												if (local2589 == null && Component.loadFailed) {
													InterfaceList.redraw(component);
												}
											}
											if (local2589 != null) {
												if (component.modelViewportWidth > 0) {
													color = (component.width << 8) / component.modelViewportWidth;
												} else {
													color = 256;
												}
												if (component.modelViewportHeight <= 0) {
													cardMemory = 256;
												} else {
													cardMemory = (component.height << 8) / component.modelViewportHeight;
												}
												local556 = local123 + component.width / 2 + (color * component.modelOriginX >> 8);
												objId = component.height / 2 + local114 + (cardMemory * component.modelOriginY >> 8);
												if (GlRenderer.enabled) {
													if (component.modelOrtho) {
														GlRenderer.setupModelPreview(local556, objId, component.modelZoom, component.modelViewAngle, color, cardMemory);
													} else {
														GlRenderer.setFullscreenCamera(local556, objId, color, cardMemory);
														GlRenderer.setDepthBias(component.modelNearClip, (float) component.modelViewAngle * 1.5F);
													}
													GlRenderer.restoreLighting();
													GlRenderer.setDepthTestEnabled(true);
													GlRenderer.setFogEnabled(false);
													FogManager.init(Preferences.brightness);
													if (ScriptRunner.glSceneNeedsRender) {
														GlRaster.resetClipRegion();
														GlRenderer.clearDepthBuffer();
														GlRaster.setClip(arg0, arg6, arg4, arg7);
														ScriptRunner.glSceneNeedsRender = false;
													}
													if (component.modelTransparent) {
														GlRenderer.disableDepthMask();
													}
													local563 = MathUtils.sin[component.modelXAngle] * component.modelZoom >> 16;
													local571 = component.modelZoom * MathUtils.cos[component.modelXAngle] >> 16;
													if (component.if3) {
														local2589.setCamera(component.modelYAngle, component.modelYOffset, component.modelXAngle, component.modelXOffset, component.modelZOffset + local563 + memory, component.modelZOffset + local571, -1L);
													} else {
														local2589.setCamera(component.modelYAngle, 0, component.modelXAngle, 0, local563, local571, -1L);
													}
													if (component.modelTransparent) {
														GlRenderer.enableDepthMask();
													}
												} else {
													Rasteriser.setBounds(local556, objId);
													local563 = MathUtils.sin[component.modelXAngle] * component.modelZoom >> 16;
													local571 = component.modelZoom * MathUtils.cos[component.modelXAngle] >> 16;
													if (!component.if3) {
														local2589.setCamera(component.modelYAngle, 0, component.modelXAngle, 0, local563, local571, -1L);
													} else if (component.modelOrtho) {
														((SoftwareModel) local2589).renderOnInterface(component.modelYAngle, component.modelYOffset, component.modelXAngle, component.modelXOffset, component.modelZOffset + memory + local563, local571 + component.modelZOffset, component.modelZoom);
													} else {
														local2589.setCamera(component.modelYAngle, component.modelYOffset, component.modelXAngle, component.modelXOffset, component.modelZOffset + local563 + memory, local571 + component.modelZOffset, -1L);
													}
													Rasteriser.prepareOffsets();
												}
											}
											PluginRepository.ComponentDraw(i, component, local123 + component.width / 2, local114 + component.height / 2);
										} else {
											if (component.type == 7) {
												local1921 = component.getFont(Sprites.nameIcons);
												if (local1921 == null) {
													if (Component.loadFailed) {
														InterfaceList.redraw(component);
													}
													continue;
												}
												local276 = 0;
												for (local468 = 0; local468 < component.baseHeight; local468++) {
													for (memory = 0; memory < component.baseWidth; memory++) {
														if (component.objTypes[local276] > 0) {
															local2611 = ObjTypeList.get(component.objTypes[local276] - 1);
															@Pc(3159) JagString local3159;
															if (local2611.stackable != 1 && component.objCounts[local276] == 1) {
																local3159 = JagString.concatenate(new JagString[]{MiniMenu.COLOR_ITEM_ORANGE, local2611.name, JagString.CLOSE_COLOR});
															} else {
																local3159 = JagString.concatenate(new JagString[]{MiniMenu.COLOR_ITEM_ORANGE, local2611.name, JagString.CLOSE_COLOR_TIMES, formatItemCount(component.objCounts[local276])});
															}
															local556 = local123 + memory * (component.invMarginX + 115);
															objId = (component.invMarginY + 12) * local468 + local114;
															if (component.halign == 0) {
																local1921.renderLeft(local3159, local556, objId, component.color, component.shadowed ? 0 : -1);
															} else if (component.halign == 1) {
																local1921.renderCenter(local3159, local556 + 57, objId, component.color, component.shadowed ? 0 : -1);
															} else {
																local1921.renderRight(local3159, local556 + 115 - 1, objId, component.color, component.shadowed ? 0 : -1);
															}
														}
														local276++;
													}
												}
												PluginRepository.ComponentDraw(i, component, local123 + component.invMarginX + 115, local114 + component.invMarginY + 12);
											}
											if (component.type == 8 && Protocol.aClass13_11 == component && Protocol.anInt5235 == TOOLTIP_DISPLAY_DELAY) {
												local276 = 0;
												local270 = 0;
												@Pc(3297) JagString local3297 = component.text;
												@Pc(3299) Font local3299 = Fonts.p12Full;
												local3297 = interpolate(component, local3297);
												@Pc(3325) JagString local3325;
												while (local3297.length() > 0) {
													cardMemory = local3297.indexOf(JagString.LINE_BREAK);
													if (cardMemory == -1) {
														local3325 = local3297;
														local3297 = JagString.EMPTY;
													} else {
														local3325 = local3297.substring(cardMemory, 0);
														local3297 = local3297.substring(cardMemory + 4);
													}
													local556 = local3299.getStringWidth(local3325);
													local276 += local3299.lineHeight + 1;
													if (local270 < local556) {
														local270 = local556;
													}
												}
												local556 = local114 + component.height + 5;
												local270 += 6;
												local276 += 7;
												if (local556 + local276 > arg7) {
													local556 = arg7 - local276;
												}
												cardMemory = local123 + component.width - local270 - 5;
												if (cardMemory < local123 + 5) {
													cardMemory = local123 + 5;
												}
												if (local270 + cardMemory > arg4) {
													cardMemory = arg4 - local270;
												}
												if (GlRenderer.enabled) {
													GlRaster.fillRect(cardMemory, local556, local270, local276, 16777120);
													GlRaster.drawRect(cardMemory, local556, local270, local276, 0);
												} else {
													SoftwareRaster.fillRect(cardMemory, local556, local270, local276, 16777120);
													SoftwareRaster.drawRect(cardMemory, local556, local270, local276, 0);
												}
												local3297 = component.text;
												objId = local556 + local3299.lineHeight + 2;
												local3297 = interpolate(component, local3297);
												while (local3297.length() > 0) {
													local563 = local3297.indexOf(JagString.LINE_BREAK);
													if (local563 == -1) {
														local3325 = local3297;
														local3297 = JagString.EMPTY;
													} else {
														local3325 = local3297.substring(local563, 0);
														local3297 = local3297.substring(local563 + 4);
													}
													local3299.renderLeft(local3325, cardMemory + 3, objId, 0, -1);
													objId += local3299.lineHeight + 1;
												}
												PluginRepository.ComponentDraw(i, component, cardMemory + 3, objId);
											}
											if (component.type == 9) {
												if (component.lineFlipped) {
													local468 = local123 + component.width;
													local276 = local114 + component.height;
													memory = local114;
												} else {
													local276 = local114;
													memory = local114 + component.height;
													local468 = local123 + component.width;
												}
												if (component.lineWidth == 1) {
													if (GlRenderer.enabled) {
														GlRaster.drawLine(local123, local276, local468, memory, component.color);
													} else {
														SoftwareRaster.drawLine(local123, local276, local468, memory, component.color);
													}
												} else if (GlRenderer.enabled) {
													GlRaster.drawThickLine(local123, local276, local468, memory, component.color, component.lineWidth);
												} else {
													SoftwareRaster.drawThickLine(local123, local276, local468, memory, component.color, component.lineWidth);
												}
												PluginRepository.ComponentDraw(i, component, local468, local276);
											}
										}
									}
								}
							}
						}
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!ag", name = "a", descriptor = "(IIIIIIIII)V")
	public static void renderInterface(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5, @OriginalArg(7) int arg6, @OriginalArg(8) int arg7) {
		if (InterfaceList.load(arg0)) {
			renderComponent(arg1, arg7, arg3, InterfaceList.components[arg0], arg2, -1, arg6, arg5, arg4);
		} else if (arg4 == -1) {
			for (@Pc(27) int local27 = 0; local27 < 100; local27++) {
				InterfaceList.rectangleDirty[local27] = true;
			}
		} else {
			InterfaceList.rectangleDirty[arg4] = true;
		}
	}

	@OriginalMember(owner = "client!al", name = "a", descriptor = "(Z)V")
	public static void renderTopLevelInterface() {
		deferredDragComponents = null;
		renderInterface(InterfaceList.topLevelInterface, 0, GameShell.canvasWidth, 0, -1, GameShell.canvasHeight, 0, 0);
		if (deferredDragComponents != null) {
			renderComponent(0, deferredDragRenderY, deferredDragRenderX, deferredDragComponents, GameShell.canvasWidth, -1412584499, 0, GameShell.canvasHeight, dragParentComponent.rectangle);
			deferredDragComponents = null;
		}
	}

	@OriginalMember(owner = "client!mj", name = "a", descriptor = "(IILclient!be;IB)V")
	public static void renderCompass(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) Component arg2, @OriginalArg(3) int arg3) {
		if (GlRenderer.enabled) {
			GlRaster.setClip(arg0, arg1, arg2.width + arg0, arg2.height + arg1);
		}
		if (MiniMap.state >= 3) {
			if (GlRenderer.enabled) {
				@Pc(44) Sprite local44 = arg2.getSprite(false);
				if (local44 != null) {
					local44.render(arg0, arg1);
				}
			} else {
				SoftwareRaster.clearMaskedRegion(arg0, arg1, arg2.clickMaskStart, arg2.clickMaskWidth);
			}
		} else if (GlRenderer.enabled) {
			((GlSprite) Sprites.compass).renderRotatedTransparent(arg0, arg1, arg2.width, arg2.height, Sprites.compass.width / 2, Sprites.compass.height / 2, (int) Camera.yawTarget, 256, (GlSprite) arg2.getSprite(false));
		} else {
			((SoftwareSprite) Sprites.compass).renderRotated(arg0, arg1, arg2.width, arg2.height, Sprites.compass.width / 2, Sprites.compass.height / 2, (int) Camera.yawTarget, arg2.clickMaskStart, arg2.clickMaskWidth);
		}
		InterfaceList.rectangleRedraw[arg3] = true;
	}

	@OriginalMember(owner = "client!fn", name = "a", descriptor = "(BIIIII)V")
	public static void renderScrollbar(@OriginalArg(1) int arg0, @OriginalArg(2) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4) {
		Sprites.scrollbars[0].renderTransparent(arg2, arg3);
		Sprites.scrollbars[1].renderTransparent(arg2, arg4 + arg3 - 16);
		@Pc(35) int local35 = arg4 * (arg4 - 32) / arg1;
		if (local35 < 8) {
			local35 = 8;
		}
		@Pc(54) int local54 = arg0 * (arg4 - local35 - 32) / (arg1 - arg4);
		if (!GlRenderer.enabled) {
			SoftwareRaster.fillRect(arg2, arg3 + 16, 16, arg4 - 32, SCROLLBAR_TRACK_COLOR);
			SoftwareRaster.fillRect(arg2, local54 + arg3 + 16, 16, local35, SCROLLBAR_THUMB_COLOR);
			SoftwareRaster.drawVerticalLine(arg2, local54 + arg3 + 16, local35, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
			SoftwareRaster.drawVerticalLine(arg2 + 1, local54 + 16 + arg3, local35, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
			SoftwareRaster.drawHorizontalLine(arg2, arg3 + local54 + 16, 16, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
			SoftwareRaster.drawHorizontalLine(arg2, arg3 + local54 + 17, 16, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
			SoftwareRaster.drawVerticalLine(arg2 + 15, local54 + 16 + arg3, local35, SCROLLBAR_THUMB_SHADOW_COLOR);
			SoftwareRaster.drawVerticalLine(arg2 + 14, arg3 - -17 - -local54, local35 - 1, SCROLLBAR_THUMB_SHADOW_COLOR);
			SoftwareRaster.drawHorizontalLine(arg2, local35 + arg3 + local54 + 15, 16, SCROLLBAR_THUMB_SHADOW_COLOR);
			SoftwareRaster.drawHorizontalLine(arg2 + 1, local35 + arg3 - (-local54 + -14), 15, SCROLLBAR_THUMB_SHADOW_COLOR);
			return;
		}
		GlRaster.fillRect(arg2, arg3 + 16, 16, arg4 - 32, SCROLLBAR_TRACK_COLOR);
		GlRaster.fillRect(arg2, arg3 + local54 + 16, 16, local35, SCROLLBAR_THUMB_COLOR);
		GlRaster.drawVerticalLine(arg2, local54 + arg3 + 16, local35, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
		GlRaster.drawVerticalLine(arg2 + 1, local54 + 16 + arg3, local35, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
		GlRaster.drawHorizontalLine(arg2, local54 + arg3 + 16, 16, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
		GlRaster.drawHorizontalLine(arg2, local54 + arg3 + 17, 16, SCROLLBAR_THUMB_HIGHLIGHT_COLOR);
		GlRaster.drawVerticalLine(arg2 + 15, arg3 + (16 - -local54), local35, SCROLLBAR_THUMB_SHADOW_COLOR);
		GlRaster.drawVerticalLine(arg2 + 14, arg3 - -local54 + 17, local35 - 1, SCROLLBAR_THUMB_SHADOW_COLOR);
		GlRaster.drawHorizontalLine(arg2, local35 + arg3 + local54 + 15, 16, SCROLLBAR_THUMB_SHADOW_COLOR);
		GlRaster.drawHorizontalLine(arg2 + 1, arg3 + 14 - -local54 + local35, 15, SCROLLBAR_THUMB_SHADOW_COLOR);
	}

	@OriginalMember(owner = "client!aa", name = "a", descriptor = "(BLclient!be;)V")
	public static void applyClientCode(@OriginalArg(1) Component arg0) {
		@Pc(16) int local16 = arg0.clientCode;
		if (local16 == 324) {
			if (cachedDefaultSpriteId == -1) {
				cachedDefaultSpriteId = arg0.spriteId;
				cachedActiveSpriteId = arg0.activeSpriteId;
			}
			if (PlayerAppearance.DEFAULT.gender) {
				arg0.spriteId = cachedDefaultSpriteId;
			} else {
				arg0.spriteId = cachedActiveSpriteId;
			}
		} else if (local16 == 325) {
			if (cachedDefaultSpriteId == -1) {
				cachedActiveSpriteId = arg0.activeSpriteId;
				cachedDefaultSpriteId = arg0.spriteId;
			}
			if (PlayerAppearance.DEFAULT.gender) {
				arg0.spriteId = cachedActiveSpriteId;
			} else {
				arg0.spriteId = cachedDefaultSpriteId;
			}
		} else if (local16 == 327) {
			arg0.modelXAngle = 150;
			arg0.modelYAngle = (int) (Math.sin((double) client.loop / 40.0D) * 256.0D) & 0x7FF;
			arg0.modelType = 5;
			arg0.modelId = -1;
		} else if (local16 == 328) {
			if (PlayerList.self.username == null) {
				arg0.modelId = 0;
			} else {
				arg0.modelXAngle = 150;
				arg0.modelYAngle = (int) (Math.sin((double) client.loop / 40.0D) * 256.0D) & 0x7FF;
				arg0.modelType = 5;
				arg0.modelId = ((int) PlayerList.self.username.encode37() << 11) + 2047;
				arg0.seqNextFrame = PlayerList.self.movementSeqNextFrame;
				arg0.seqCycle = 0;
				arg0.modelSeqId = PlayerList.self.movementSeqId;
				arg0.seqFrame = PlayerList.self.movementSeqFrame;
			}
		}
	}

	@OriginalMember(owner = "client!fi", name = "a", descriptor = "(BI)Lclient!na;")
	public static JagString formatItemCount(@OriginalArg(1) int arg0) {
		@Pc(9) JagString local9 = JagString.parseInt(arg0);
		for (@Pc(21) int local21 = local9.length() - 3; local21 > 0; local21 -= 3) {
			local9 = JagString.concatenate(new JagString[]{local9.substring(local21, 0), JagString.COMMA, local9.substring(local21)});
		}
		if (local9.length() > 9) {
			return JagString.concatenate(new JagString[]{JagString.GREEN_COLOR_PREFIX, local9.substring(local9.length() - 8, 0), LocalizedText.MILLION_SHORT, MiniMenu.OPEN_PARENTHESIS, local9, JagString.CLOSE_PAREN_CLOSE_COLOR});
		} else if (local9.length() > 6) {
			return JagString.concatenate(new JagString[]{JagString.WHITE_COLOR_PREFIX, local9.substring(local9.length() - 4, 0), LocalizedText.THOUSAND_SHORT, MiniMenu.OPEN_PARENTHESIS, local9, JagString.CLOSE_PAREN_CLOSE_COLOR});
		} else {
			return JagString.concatenate(new JagString[]{JagString.YELLOW_COLOR_PREFIX, local9, JagString.CLOSE_COLOR});
		}
	}

	@OriginalMember(owner = "client!a", name = "a", descriptor = "(IIIII)V")
	public static void renderWorldMapOverview(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(4) int arg3) {
		if (GlRenderer.enabled) {
			GlRaster.setClip(arg0, arg3, arg2 + arg0, arg1 + arg3);
			GlRaster.fillRect(arg0, arg3, arg2, arg1, 0);
		} else {
			SoftwareRaster.setClip(arg0, arg3, arg2 + arg0, arg3 + arg1);
			SoftwareRaster.fillRect(arg0, arg3, arg2, arg1, 0);
		}
		if (WorldMap.loadPercentage < 100) {
			return;
		}
		if (WorldMap.aClass3_Sub2_Sub1_2 == null || arg2 != WorldMap.aClass3_Sub2_Sub1_2.width || WorldMap.aClass3_Sub2_Sub1_2.height != arg1) {
			@Pc(63) SoftwareSprite local63 = new SoftwareSprite(arg2, arg1);
			SoftwareRaster.setSize(local63.pixels, arg2, arg1);
			WorldMap.renderMapViewport(arg2, 0, WorldMap.width, 0, 0, WorldMap.length, arg1, 0);
			if (GlRenderer.enabled) {
				WorldMap.aClass3_Sub2_Sub1_2 = new GlSprite(local63);
			} else {
				WorldMap.aClass3_Sub2_Sub1_2 = local63;
			}
			if (GlRenderer.enabled) {
				SoftwareRaster.pixels = null;
			} else {
				SoftwareRaster.frameBuffer.makeTarget();
			}
		}
		WorldMap.aClass3_Sub2_Sub1_2.drawPixels(arg0, arg3);
		@Pc(147) int local147 = arg1 * worldMapViewportY / WorldMap.length + arg3;
		@Pc(153) int local153 = WorldMap.anInt1176 * arg1 / WorldMap.length;
		@Pc(161) int local161 = arg0 + arg2 * worldMapViewportX / WorldMap.width;
		@Pc(167) int local167 = arg2 * WorldMap.anInt2387 / WorldMap.width;
		@Pc(169) int local169 = 16711680;
		if (client.game == 1) {
			local169 = 16777215;
		}
		if (GlRenderer.enabled) {
			GlRaster.fillRectAlpha(local161, local147, local167, local153, local169, 128);
			GlRaster.drawRect(local161, local147, local167, local153, local169);
		} else {
			SoftwareRaster.fillRectAlpha(local161, local147, local167, local153, local169, 128);
			SoftwareRaster.drawRect(local161, local147, local167, local153, local169);
		}
		if (WorldMap.anInt1864 <= 0) {
			return;
		}
		@Pc(225) int local225;
		if (mapHighlightPulseCounter > 10) {
			local225 = (20 - mapHighlightPulseCounter) * 25;
		} else {
			local225 = mapHighlightPulseCounter * 25;
		}
		for (@Pc(238) MapElement local238 = (MapElement) WorldMap.mapElements.head(); local238 != null; local238 = (MapElement) WorldMap.mapElements.next()) {
			if (local238.id == WorldMap.anInt172) {
				@Pc(258) int local258 = arg3 + local238.anInt4314 * arg1 / WorldMap.length;
				@Pc(267) int local267 = arg2 * local238.anInt4307 / WorldMap.width + arg0;
				if (GlRenderer.enabled) {
					GlRaster.fillRectAlpha(local267 - 2, local258 + -2, 4, 4, 16776960, local225);
				} else {
					SoftwareRaster.fillRectAlpha(local267 - 2, local258 + -2, 4, 4, 16776960, local225);
				}
			}
		}
	}

	@OriginalMember(owner = "client!da", name = "a", descriptor = "(IIILclient!be;)V")
	public static void startComponentDrag(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) Component arg2) {
		if (draggedComponent != null || isMenuOpen || (arg2 == null || getDragParent(arg2) == null)) {
			return;
		}
		draggedComponent = arg2;
		dragParentComponent = getDragParent(arg2);
		dragStartMouseX = arg1;
		isDragging = false;
		dragElapsedTicks = 0;
		dragStartMouseY = arg0;
	}

	@OriginalMember(owner = "client!ha", name = "a", descriptor = "(ILclient!be;)Lclient!be;")
	public static Component getDragParent(@OriginalArg(1) Component arg0) {
		@Pc(12) Component local12 = InterfaceList.getDragRenderParent(arg0);
		if (local12 == null) {
			local12 = arg0.dragParent;
		}
		return local12;
	}

	@OriginalMember(owner = "client!ac", name = "b", descriptor = "(I)V")
	public static void updateComponentDrag() {
		InterfaceList.redraw(draggedComponent);
		dragElapsedTicks++;
		if (InterfaceList.aBoolean83 && InterfaceList.aBoolean174) {
			@Pc(30) int local30 = Mouse.lastMouseX;
			local30 -= dragStartMouseX;
			if (dragBoundsMinX > local30) {
				local30 = dragBoundsMinX;
			}
			@Pc(41) int local41 = Mouse.lastMouseY;
			if (dragBoundsMinX + dragParentComponent.width < local30 - -draggedComponent.width) {
				local30 = dragBoundsMinX + dragParentComponent.width - draggedComponent.width;
			}
			local41 -= dragStartMouseY;
			if (local41 < InterfaceList.anInt5103) {
				local41 = InterfaceList.anInt5103;
			}
			if (InterfaceList.anInt5103 + dragParentComponent.height < local41 - -draggedComponent.height) {
				local41 = InterfaceList.anInt5103 + dragParentComponent.height - draggedComponent.height;
			}
			@Pc(109) int local109 = local41 - InterfaceList.anInt660;
			@Pc(114) int local114 = local30 - InterfaceList.anInt3075;
			@Pc(122) int local122 = local30 + dragParentComponent.scrollX - dragBoundsMinX;
			@Pc(130) int local130 = dragParentComponent.scrollY + local41 - InterfaceList.anInt5103;
			@Pc(133) int local133 = draggedComponent.dragDeadzone;
			if (dragElapsedTicks > draggedComponent.dragDeadtime && (local133 < local114 || -local133 > local114 || local109 > local133 || local109 < -local133)) {
				isDragging = true;
			}
			@Pc(176) HookRequest local176;
			if (draggedComponent.onDragStart != null && isDragging) {
				local176 = new HookRequest();
				local176.source = draggedComponent;
				local176.arguments = draggedComponent.onDragStart;
				local176.mouseX = local122;
				local176.mouseY = local130;
				ScriptRunner.run(local176);
			}
			if (Mouse.pressedButton == 0) {
				if (isDragging) {
					if (draggedComponent.onDragRelease != null) {
						local176 = new HookRequest();
						local176.mouseY = local130;
						local176.target = InterfaceList.aClass13_12;
						local176.mouseX = local122;
						local176.arguments = draggedComponent.onDragRelease;
						local176.source = draggedComponent;
						ScriptRunner.run(local176);
					}
					if (InterfaceList.aClass13_12 != null && InterfaceList.getDragRenderParent(draggedComponent) != null) {
						Protocol.outboundBuffer.p1isaac(79);
						Protocol.outboundBuffer.mp4(draggedComponent.id);
						Protocol.outboundBuffer.ip2(InterfaceList.aClass13_12.createdComponentId);
						Protocol.outboundBuffer.p4(InterfaceList.aClass13_12.id);
						Protocol.outboundBuffer.ip2(draggedComponent.createdComponentId);
					}
				} else if ((VarpDomain.mouseButtons == 1 || MiniMenu.isComponentOptionAction(MiniMenu.size - 1)) && MiniMenu.size > 2) {
					ScriptRunner.layoutMiniMenu();
				} else if (MiniMenu.size > 0) {
					MiniMenu.processClick();
				}
				draggedComponent = null;
			}
		} else if (dragElapsedTicks > 1) {
			draggedComponent = null;
		}
	}
}
