package rt4;

import org.openrs2.deob.annotation.OriginalArg;
import org.openrs2.deob.annotation.OriginalMember;
import org.openrs2.deob.annotation.Pc;
import plugin.PluginRepository;

import java.io.IOException;
import java.net.Socket;

public class LoginManager {
	@OriginalMember(owner = "client!bg", name = "g", descriptor = "Lclient!i;")
	public static final Packet buffer = new Packet(5000);
	@OriginalMember(owner = "client!nd", name = "r", descriptor = "Lclient!na;")
	public static final JagString ZAP = JagString.parse("zap");
	@OriginalMember(owner = "client!ef", name = "h", descriptor = "Lclient!na;")
	public static final JagString UNZAP = JagString.parse("unzap");
	@OriginalMember(owner = "client!wj", name = "f", descriptor = "Lclient!na;")
	public static final JagString UNDERWATER_LOC_PREFIX = JagString.parse("ul");
	@OriginalMember(owner = "client!i", name = "ic", descriptor = "Lclient!na;")
	public static final JagString MAP_PREFIX = JagString.parse("m");
	@OriginalMember(owner = "client!wb", name = "e", descriptor = "Lclient!na;")
	public static final JagString LOC_PREFIX = JagString.parse("l");
	@OriginalMember(owner = "client!oe", name = "o", descriptor = "Lclient!na;")
	public static final JagString NPC_PREFIX = JagString.parse("n");
	@OriginalMember(owner = "client!nb", name = "a", descriptor = "Lclient!na;")
	public static final JagString UNDERWATER_MAP_PREFIX = JagString.parse("um");
	@OriginalMember(owner = "client!gm", name = "W", descriptor = "Lclient!na;")
	public static final JagString UNDERSCORE = JagString.parse("_");
	@OriginalMember(owner = "client!gd", name = "h", descriptor = "[I")
	public static final int[] WALL_ROTATION_FLAGS = new int[]{16, 32, 64, 128};
	@OriginalMember(owner = "client!rj", name = "Z", descriptor = "[I")
	public static final int[] decodedNpcRegions = new int[64];
	@OriginalMember(owner = "client!e", name = "Dc", descriptor = "Lclient!na;")
	public static final JagString LABELS_SUFFIX = JagString.parse("_labels");
	@OriginalMember(owner = "client!bh", name = "C", descriptor = "Lclient!na;")
	public static final JagString COMPLETE_PERCENT = JagString.parse("<br>(X100(U(Y");
	@OriginalMember(owner = "client!rl", name = "X", descriptor = "I")
	public static int hopStep = 0;
	@OriginalMember(owner = "client!sd", name = "X", descriptor = "Z")
	public static boolean pingFailed = false;
	@OriginalMember(owner = "client!sk", name = "ib", descriptor = "I")
	public static int hopTime = 0;
	@OriginalMember(owner = "client!vk", name = "b", descriptor = "I")
	public static int reply = -2;
	@OriginalMember(owner = "client!hd", name = "a", descriptor = "I")
	public static int loops = 0;
	@OriginalMember(owner = "client!ol", name = "V", descriptor = "I")
	public static int step = 0;
	@OriginalMember(owner = "client!qi", name = "A", descriptor = "I")
	public static int disallowResult = -1;
	@OriginalMember(owner = "client!wh", name = "s", descriptor = "I")
	public static int errors = 0;
	@OriginalMember(owner = "client!bj", name = "Y", descriptor = "I")
	public static int hopLoops = 0;
	@OriginalMember(owner = "client!pl", name = "i", descriptor = "I")
	public static int hopErrors = 0;
	@OriginalMember(owner = "client!pi", name = "P", descriptor = "J")
	public static long serverKey = 0L;
	@OriginalMember(owner = "client!af", name = "c", descriptor = "I")
	public static int loginType = -1;
	@OriginalMember(owner = "client!pg", name = "S", descriptor = "I")
	public static int staffModLevel = 0;
	@OriginalMember(owner = "client!ud", name = "O", descriptor = "I")
	public static int blackmarks = 0;
	@OriginalMember(owner = "client!jk", name = "G", descriptor = "Z")
	public static boolean playerUnderage = false;
	@OriginalMember(owner = "client!ql", name = "c", descriptor = "Z")
	public static boolean parentalChatConsent = false;
	@OriginalMember(owner = "client!qg", name = "W", descriptor = "Z")
	public static boolean playerMember = false;
	@OriginalMember(owner = "client!aa", name = "l", descriptor = "Z")
	public static boolean mapMembers = false;
	@OriginalMember(owner = "client!gm", name = "bb", descriptor = "Z")
	public static boolean mapQuickChat = false;
	@OriginalMember(owner = "client!c", name = "hb", descriptor = "Z")
	public static boolean parentalAdvertConsent = false;
	@OriginalMember(owner = "client!we", name = "H", descriptor = "[[B")
	public static byte[][] mapFilesBuffer;
	@OriginalMember(owner = "client!mf", name = "O", descriptor = "[[B")
	public static byte[][] locationMapFilesBuffer;
	@OriginalMember(owner = "client!cl", name = "V", descriptor = "[I")
	public static int[] underWaterLocationsMapFileIds;
	@OriginalMember(owner = "client!pg", name = "jb", descriptor = "[[B")
	public static byte[][] npcSpawnsFilesBuffer;
	@OriginalMember(owner = "client!client", name = "lb", descriptor = "[I")
	public static int[] mapFileIds;
	@OriginalMember(owner = "client!pa", name = "L", descriptor = "[[B")
	public static byte[][] underWaterMapFilesBuffer;
	@OriginalMember(owner = "client!fl", name = "D", descriptor = "[[I")
	public static int[][] regionsXteaKeys;
	@OriginalMember(owner = "client!te", name = "H", descriptor = "[I")
	public static int[] regionBitPacked;
	@OriginalMember(owner = "client!nm", name = "P", descriptor = "[I")
	public static int[] npcSpawnsFileIds;
	@OriginalMember(owner = "client!hk", name = "bb", descriptor = "[I")
	public static int[] underWaterMapFileIds;
	@OriginalMember(owner = "client!nj", name = "j", descriptor = "[I")
	public static int[] locationsMapFileIds;
	@OriginalMember(owner = "client!bi", name = "Y", descriptor = "[[B")
	public static byte[][] underWaterLocationsMapFilesBuffer;
	@OriginalMember(owner = "client!mh", name = "hb", descriptor = "Lclient!bn;")
	public static Map map;
	@OriginalMember(owner = "client!tb", name = "X", descriptor = "Lclient!se;")
	public static MapElementList mapElementList;
	@OriginalMember(owner = "client!ja", name = "n", descriptor = "I")
	public static int mapFlagX = 0;
	@OriginalMember(owner = "client!gk", name = "h", descriptor = "I")
	public static int mapFlagY = 0;
	@OriginalMember(owner = "client!qf", name = "M", descriptor = "I")
	public static int ticksSinceLastPacket = 0;
	@OriginalMember(owner = "client!nm", name = "U", descriptor = "I")
	public static int mapFilesMissingCount = 0;
	@OriginalMember(owner = "client!t", name = "y", descriptor = "I")
	public static int loadingScreenState = 0;
	@OriginalMember(owner = "client!sk", name = "lb", descriptor = "Z")
	public static boolean glModelsInvalidated = false;
	@OriginalMember(owner = "client!mg", name = "Q", descriptor = "I")
	public static int decodedNpcRegionCount = 0;
	@OriginalMember(owner = "client!mf", name = "X", descriptor = "I")
	public static int loginScreenId;
	@OriginalMember(owner = "client!hd", name = "e", descriptor = "Lclient!qf;")
	public static Sprite menuSideFillSprite;
	@OriginalMember(owner = "client!d", name = "ib", descriptor = "Lclient!qf;")
	public static Sprite menuHeaderFillSprite;
	@OriginalMember(owner = "client!oi", name = "h", descriptor = "Lclient!qf;")
	public static Sprite menuHeaderEdgeSprite;
	@OriginalMember(owner = "client!qi", name = "z", descriptor = "Lclient!qf;")
	public static Sprite menuBottomEdgeSprite;
	@OriginalMember(owner = "client!nb", name = "i", descriptor = "Lclient!qf;")
	public static Sprite menuBottomFillSprite;
	@OriginalMember(owner = "client!qf", name = "X", descriptor = "Lclient!be;")
	public static Component tooltipComponent = null;
	@OriginalMember(owner = "client!vf", name = "c", descriptor = "I")
	public static int menuHighlightTextColor;
	@OriginalMember(owner = "client!pk", name = "Y", descriptor = "I")
	public static int menuTextColor;
	@OriginalMember(owner = "client!da", name = "ab", descriptor = "I")
	public static int menuFillColor;
	@OriginalMember(owner = "client!ii", name = "y", descriptor = "I")
	public static int menuFillTransparency;
	@OriginalMember(owner = "client!ug", name = "p", descriptor = "I")
	public static int menuHighlightColor;
	@OriginalMember(owner = "client!sm", name = "b", descriptor = "I")
	public static int menuHighlightTransparency;
	@OriginalMember(owner = "client!ee", name = "c", descriptor = "I")
	public static int menuHeaderFillSpriteId;
	@OriginalMember(owner = "client!gl", name = "d", descriptor = "I")
	public static int menuBottomFillSpriteId;
	@OriginalMember(owner = "client!nf", name = "e", descriptor = "I")
	public static int menuHeaderEdgeSpriteId;
	@OriginalMember(owner = "client!kk", name = "i", descriptor = "I")
	public static int menuSideFillSpriteId;
	@OriginalMember(owner = "client!uj", name = "E", descriptor = "I")
	public static int menuBottomEdgeSpriteId;
	@OriginalMember(owner = "client!wc", name = "g", descriptor = "I")
	public static int missingLocModelCount = 0;

	@OriginalMember(owner = "client!dm", name = "d", descriptor = "(I)V")
	public static void clear() {
		pingFailed = false;
		hopTime = 0;
		reply = -3;
		loops = 0;
		step = 1;
		errors = 0;
		disallowResult = -1;
	}

	@OriginalMember(owner = "client!ch", name = "b", descriptor = "(B)V")
	public static void loopAuto() {
		if (hopStep == 0) {
			return;
		}
		try {
			if (++hopLoops > 1500) {
				if (Protocol.socket != null) {
					Protocol.socket.close();
					Protocol.socket = null;
				}
				if (hopErrors >= 1) {
					reply = -5;
					hopStep = 0;
					return;
				}
				hopLoops = 0;
				hopErrors++;
				hopStep = 1;
				if (client.worldListPort == client.worldListDefaultPort) {
					client.worldListPort = client.worldListAlternatePort;
				} else {
					client.worldListPort = client.worldListDefaultPort;
				}
			}
			if (hopStep == 1) {
				Protocol.socketRequest = GameShell.signLink.openSocket(client.worldListHostname, client.worldListPort);
				hopStep = 2;
			}
			@Pc(126) int response;
			if (hopStep == 2) {
				if (Protocol.socketRequest.status == 2) {
					throw new IOException();
				}
				if (Protocol.socketRequest.status != 1) {
					return;
				}
				Protocol.socket = new BufferedSocket((Socket) Protocol.socketRequest.result, GameShell.signLink);
				Protocol.socketRequest = null;
				Protocol.socket.write(Protocol.outboundBuffer.data, Protocol.outboundBuffer.offset);
				if (client.musicChannel != null) {
					client.musicChannel.pauseConsumptionCheck();
				}
				if (client.soundChannel != null) {
					client.soundChannel.pauseConsumptionCheck();
				}
				response = Protocol.socket.read();
				if (client.musicChannel != null) {
					client.musicChannel.pauseConsumptionCheck();
				}
				if (client.soundChannel != null) {
					client.soundChannel.pauseConsumptionCheck();
				}
				if (response != 101) {
					reply = response;
					hopStep = 0;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
				hopStep = 3;
			}
			if (hopStep == 3) {
				if (Protocol.socket.available() < 2) {
					return;
				}
				response = Protocol.socket.read() << 8 | Protocol.socket.read();
				WorldList.hopWorld(response);
				if (Player.worldId == -1) {
					hopStep = 0;
					reply = 6;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
				hopStep = 0;
				Protocol.socket.close();
				Protocol.socket = null;
				clear();
			}
		} catch (@Pc(210) IOException local210) {
			if (Protocol.socket != null) {
				Protocol.socket.close();
				Protocol.socket = null;
			}
			if (hopErrors < 1) {
				if (client.worldListPort == client.worldListDefaultPort) {
					client.worldListPort = client.worldListAlternatePort;
				} else {
					client.worldListPort = client.worldListDefaultPort;
				}
				hopStep = 1;
				hopLoops = 0;
				hopErrors++;
			} else {
				reply = -4;
				hopStep = 0;
			}
		}
	}

	@OriginalMember(owner = "client!ri", name = "a", descriptor = "(B)V")
	public static void loop() {
		if (step == 0 || step == 5) {
			return;
		}
		try {
			if (++loops > 2000) {
				if (Protocol.socket != null) {
					Protocol.socket.close();
					Protocol.socket = null;
				}
				if (errors >= 1) {
					reply = -5;
					step = 0;
					return;
				}
				loops = 0;
				if (client.port == client.defaultPort) {
					client.port = client.alternatePort;
				} else {
					client.port = client.defaultPort;
				}
				step = 1;
				errors++;
			}
			if (step == 1) {
				if (GlobalJsonConfig.instance != null) {
					client.hostname = GlobalJsonConfig.instance.ip_management;
					client.port = GlobalJsonConfig.instance.server_port + client.worldListId;
				}
				Protocol.socketRequest = GameShell.signLink.openSocket(client.hostname, client.port);
				step = 2;
			}
			if (step == 2) {
				if (Protocol.socketRequest.status == 2) {
					throw new IOException();
				}
				if (Protocol.socketRequest.status != 1) {
					return;
				}
				Protocol.socket = new BufferedSocket((Socket) Protocol.socketRequest.result, GameShell.signLink);
				Protocol.socketRequest = null;
				@Pc(106) long encodedName = Player.name37 = Player.usernameInput.encode37();
				Protocol.outboundBuffer.offset = 0;
				Protocol.outboundBuffer.p1(14);
				@Pc(120) int nameHash = (int) (encodedName >> 16 & 0x1FL);
				Protocol.outboundBuffer.p1(nameHash);
				Protocol.socket.write(Protocol.outboundBuffer.data, 2);
				if (client.musicChannel != null) {
					client.musicChannel.pauseConsumptionCheck();
				}
				if (client.soundChannel != null) {
					client.soundChannel.pauseConsumptionCheck();
				}
				@Pc(150) int response = Protocol.socket.read();
				if (client.musicChannel != null) {
					client.musicChannel.pauseConsumptionCheck();
				}
				if (client.soundChannel != null) {
					client.soundChannel.pauseConsumptionCheck();
				}
				if (response != 0) {
					reply = response;
					step = 0;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
				step = 3;
			}
			if (step == 3) {
				if (Protocol.socket.available() < 8) {
					return;
				}
				Protocol.socket.read(0, 8, Protocol.inboundBuffer.data);
				Protocol.inboundBuffer.offset = 0;
				serverKey = Protocol.inboundBuffer.g8();
				@Pc(210) int[] key = new int[4];
				Protocol.outboundBuffer.offset = 0;
				key[2] = (int) (serverKey >> 32);
				key[3] = (int) serverKey;
				key[1] = (int) (Math.random() * 9.9999999E7D);
				key[0] = (int) (Math.random() * 9.9999999E7D);
				Protocol.outboundBuffer.p1(10);
				Protocol.outboundBuffer.p4(key[0]);
				Protocol.outboundBuffer.p4(key[1]);
				Protocol.outboundBuffer.p4(key[2]);
				Protocol.outboundBuffer.p4(key[3]);
				Protocol.outboundBuffer.p8(Player.usernameInput.encode37());
				Protocol.outboundBuffer.pjstr(Player.password);
				if (GlobalConfig.LOGIN_EXTRA_INFO) {
					Protocol.outboundBuffer.pjstr(JagString.parse(""));
					Protocol.outboundBuffer.pjstr(JagString.parse(""));
					Protocol.outboundBuffer.pjstr(JagString.parse(""));
				}
				Protocol.outboundBuffer.rsaenc(GlobalConfig.RSA_EXPONENT, GlobalConfig.RSA_MODULUS);
				buffer.offset = 0;
				if (client.gameState == 40) {
					buffer.p1(18);
				} else {
					buffer.p1(16);
				}
				int offset = 0;
				if (GlobalConfig.LOGIN_FAKE_IDX28) {
					// pretend that we're loading the archive so we don't throw the packet size off
					offset = 4;
				}
				buffer.p2(Protocol.outboundBuffer.offset + Buffer.gjstrlen(client.settings) + (159 + offset));
				buffer.p4(530);
				buffer.p1(loginType);
				buffer.p1(client.advertSuppressed ? 1 : 0);
				buffer.p1(1);
				buffer.p1(DisplayMode.getWindowMode());
				buffer.p2(GameShell.canvasWidth);
				buffer.p2(GameShell.canvasHeight);
				buffer.p1(Preferences.antiAliasingMode);
				client.writeUid(buffer);
				buffer.pjstr(client.settings);
				buffer.p4(client.affiliate);
				buffer.p4(Preferences.toInt());
				Preferences.sentToServer = true;
				buffer.p2(Protocol.verifyId);
				buffer.p4(client.js5Archive0.getChecksum());
				buffer.p4(client.js5Archive1.getChecksum());
				buffer.p4(client.js5Archive2.getChecksum());
				buffer.p4(client.js5Archive3.getChecksum());
				buffer.p4(client.js5Archive4.getChecksum());
				buffer.p4(client.js5Archive5.getChecksum());
				buffer.p4(client.js5Archive6.getChecksum());
				buffer.p4(client.js5Archive7.getChecksum());
				buffer.p4(client.js5Archive8.getChecksum());
				buffer.p4(client.js5Archive9.getChecksum());
				buffer.p4(client.js5Archive10.getChecksum());
				buffer.p4(client.js5Archive11.getChecksum());
				buffer.p4(client.js5Archive12.getChecksum());
				buffer.p4(client.js5Archive13.getChecksum());
				buffer.p4(client.js5Archive14.getChecksum());
				buffer.p4(client.js5Archive15.getChecksum());
				buffer.p4(client.js5Archive16.getChecksum());
				buffer.p4(client.js5Archive17.getChecksum());
				buffer.p4(client.js5Archive18.getChecksum());
				buffer.p4(client.js5Archive19.getChecksum());
				buffer.p4(client.js5Archive20.getChecksum());
				buffer.p4(client.js5Archive21.getChecksum());
				buffer.p4(client.js5Archive22.getChecksum());
				buffer.p4(client.js5Archive23.getChecksum());
				buffer.p4(client.js5Archive24.getChecksum());
				buffer.p4(client.js5Archive25.getChecksum());
				buffer.p4(client.js5Archive26.getChecksum());
				buffer.p4(client.js5Archive27.getChecksum());
				if (GlobalConfig.LOGIN_FAKE_IDX28) {
					buffer.p4(0);
				}
				buffer.pdata(Protocol.outboundBuffer.data, Protocol.outboundBuffer.offset);
				Protocol.socket.write(buffer.data, buffer.offset);
				Protocol.outboundBuffer.setKey(key);
				for (@Pc(583) int i = 0; i < 4; i++) {
					key[i] += 50;
				}
				Protocol.inboundBuffer.setKey(key);
				step = 4;
			}
			if (step == 4) {
				if (Protocol.socket.available() < 1) {
					return;
				}
				@Pc(623) int reply = Protocol.socket.read();
				if (reply == 21) {
					step = 7;
				} else if (reply == 29) {
					step = 10;
				} else if (reply == 1) {
					step = 5;
					LoginManager.reply = reply;
					return;
				} else if (reply == 2) {
					step = 8;
				} else if (reply == 15) {
					step = 0;
					LoginManager.reply = reply;
					return;
				} else if (reply == 23 && errors < 1) {
					step = 1;
					errors++;
					loops = 0;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				} else {
					LoginManager.reply = reply;
					step = 0;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
			}
			if (step == 6) {
				Protocol.outboundBuffer.offset = 0;
				Protocol.outboundBuffer.p1isaac(17);
				Protocol.socket.write(Protocol.outboundBuffer.data, Protocol.outboundBuffer.offset);
				step = 4;
				return;
			}
			if (step == 7) {
				if (Protocol.socket.available() >= 1) {
					hopTime = (Protocol.socket.read() + 3) * 60;
					step = 0;
					reply = 21;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
				return;
			}
			if (step == 10) {
				if (Protocol.socket.available() >= 1) {
					disallowResult = Protocol.socket.read();
					step = 0;
					reply = 29;
					Protocol.socket.close();
					Protocol.socket = null;
					return;
				}
				return;
			}
			if (step == 8) {
				if (Protocol.socket.available() < 14) {
					return;
				}
				Protocol.socket.read(0, 14, Protocol.inboundBuffer.data);
				Protocol.inboundBuffer.offset = 0;
				staffModLevel = Protocol.inboundBuffer.g1();
				blackmarks = Protocol.inboundBuffer.g1();
				playerUnderage = Protocol.inboundBuffer.g1() == 1;
				parentalChatConsent = Protocol.inboundBuffer.g1() == 1;
				parentalAdvertConsent = Protocol.inboundBuffer.g1() == 1;
				mapQuickChat = Protocol.inboundBuffer.g1() == 1;
				MouseRecorder.enabled = Protocol.inboundBuffer.g1() == 1;
				PlayerList.selfId = Protocol.inboundBuffer.g2();
				playerMember = Protocol.inboundBuffer.g1() == 1;
				mapMembers = Protocol.inboundBuffer.g1() == 1;
				LocTypeList.setAllowMembers(mapMembers);
				ObjTypeList.setAllowMembers(mapMembers);
				if (!client.advertSuppressed) {
					if (playerUnderage && !parentalAdvertConsent || playerMember) {
						try {
							ZAP.browserControlCall(GameShell.signLink.applet);
						} catch (@Pc(910) Throwable ignored) {
						}
					} else {
						try {
							UNZAP.browserControlCall(GameShell.signLink.applet);
						} catch (@Pc(920) Throwable ignored) {
						}
					}
				}
				Protocol.opcode = Protocol.inboundBuffer.g1isaac();
				Protocol.length = Protocol.inboundBuffer.g2();
				step = 9;
			}
			if (step == 9) {
				if (Protocol.socket.available() < Protocol.length) {
					return;
				}
				Protocol.inboundBuffer.offset = 0;
				Protocol.socket.read(0, Protocol.length, Protocol.inboundBuffer.data);
				reply = 2;
				step = 0;
				client.resetGameSessionState();
				SceneGraph.centralZoneX = -1;
				Protocol.readRebuildPacket(false);
				Protocol.opcode = -1;
			}
		} catch (@Pc(977) IOException ex) {
			if (Protocol.socket != null) {
				Protocol.socket.close();
				Protocol.socket = null;
			}
			if (errors >= 1) {
				step = 0;
				reply = -4;
			} else {
				step = 1;
				loops = 0;
				errors++;
				if (client.defaultPort == client.port) {
					client.port = client.alternatePort;
				} else {
					client.port = client.defaultPort;
				}
			}
		}
	}

	@OriginalMember(owner = "client!p", name = "a", descriptor = "(I)V")
	public static void continueDelayedLogin() {
		if (step == 5) {
			step = 6;
		}
	}

	@OriginalMember(owner = "client!se", name = "a", descriptor = "(Lclient!na;Lclient!na;IB)V")
	public static void startLogin(@OriginalArg(0) JagString username, @OriginalArg(1) JagString password, @OriginalArg(2) int authType) {
		Player.password = password;
		loginType = authType;
		Player.usernameInput = username;
		if (Player.usernameInput.strEquals(JagString.EMPTY) || Player.password.strEquals(JagString.EMPTY)) {
			reply = 3;
		} else if (Player.worldId == -1) {
			hopLoops = 0;
			hopErrors = 0;
			reply = -3;
			hopStep = 1;
			@Pc(43) Buffer hopBuffer = new Buffer(128);
			hopBuffer.p1(10);
			hopBuffer.p2((int) (Math.random() * 99999.0D));
			hopBuffer.p2(530);
			hopBuffer.p8(Player.usernameInput.encode37());
			hopBuffer.p4((int) (Math.random() * 9.9999999E7D));
			hopBuffer.pjstr(Player.password);
			hopBuffer.p4((int) (Math.random() * 9.9999999E7D));
			hopBuffer.rsaenc(GlobalConfig.RSA_EXPONENT, GlobalConfig.RSA_MODULUS);
			Protocol.outboundBuffer.offset = 0;
			Protocol.outboundBuffer.p1(210);
			Protocol.outboundBuffer.p1(hopBuffer.offset);
			Protocol.outboundBuffer.pdata(hopBuffer.data, hopBuffer.offset);
		} else {
			clear();
		}
	}

	@OriginalMember(owner = "client!ca", name = "h", descriptor = "(I)V")
	public static void setupLoadingScreenRegion() {
		@Pc(10) int zoneX = (Camera.renderX >> 10) + (Camera.originX >> 3);
		@Pc(23) int zoneY = (Camera.renderY >> 10) + (Camera.originY >> 3);
		locationMapFilesBuffer = new byte[18][];
		underWaterLocationsMapFileIds = new int[18];
		npcSpawnsFilesBuffer = new byte[18][];
		mapFileIds = new int[18];
		regionsXteaKeys = new int[18][4];
		underWaterMapFilesBuffer = new byte[18][];
		regionBitPacked = new int[18];
		mapFilesBuffer = new byte[18][];
		npcSpawnsFileIds = new int[18];
		underWaterMapFileIds = new int[18];
		locationsMapFileIds = new int[18];
		underWaterLocationsMapFilesBuffer = new byte[18][];
		@Pc(74) int regionIndex = 0;
		@Pc(80) int regionX;
		for (regionX = (zoneX - 6) / 8; regionX <= (zoneX + 6) / 8; regionX++) {
			for (@Pc(97) int regionY = (zoneY - 6) / 8; regionY <= (zoneY + 6) / 8; regionY++) {
				@Pc(115) int packedRegion = (regionX << 8) + regionY;
				regionBitPacked[regionIndex] = packedRegion;
				mapFileIds[regionIndex] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{MAP_PREFIX, JagString.parseInt(regionX), UNDERSCORE, JagString.parseInt(regionY)}));
				locationsMapFileIds[regionIndex] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LOC_PREFIX, JagString.parseInt(regionX), UNDERSCORE, JagString.parseInt(regionY)}));
				npcSpawnsFileIds[regionIndex] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{NPC_PREFIX, JagString.parseInt(regionX), UNDERSCORE, JagString.parseInt(regionY)}));
				underWaterMapFileIds[regionIndex] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{UNDERWATER_MAP_PREFIX, JagString.parseInt(regionX), UNDERSCORE, JagString.parseInt(regionY)}));
				underWaterLocationsMapFileIds[regionIndex] = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{UNDERWATER_LOC_PREFIX, JagString.parseInt(regionX), UNDERSCORE, JagString.parseInt(regionY)}));
				if (npcSpawnsFileIds[regionIndex] == -1) {
					mapFileIds[regionIndex] = -1;
					locationsMapFileIds[regionIndex] = -1;
					underWaterMapFileIds[regionIndex] = -1;
					underWaterLocationsMapFileIds[regionIndex] = -1;
				}
				regionIndex++;
			}
		}
		for (regionX = regionIndex; regionX < npcSpawnsFileIds.length; regionX++) {
			npcSpawnsFileIds[regionX] = -1;
			mapFileIds[regionX] = -1;
			locationsMapFileIds[regionX] = -1;
			underWaterMapFileIds[regionX] = -1;
			underWaterLocationsMapFileIds[regionX] = -1;
		}
		loadRegion(0, zoneY, zoneX, 8, true, 8);
	}

	@OriginalMember(owner = "client!wj", name = "b", descriptor = "(B)V")
	public static void processLogout() {
		if (Protocol.socket != null) {
			Protocol.socket.close();
			Protocol.socket = null;
		}
		client.unload();
		SceneGraph.clear();
		@Pc(19) int i;
		for (i = 0; i < 4; i++) {
			PathFinder.collisionMaps[i].clear();
		}
		WorldMap.clear(false);
		System.gc();
		MidiPlayer.playFadeOut();
		MidiPlayer.jingle = false;
		MusicPlayer.groupId = -1;
		AreaSoundManager.clear(true);
		SceneGraph.dynamicMapRegion = false;
		Camera.originY = 0;
		SceneGraph.centralZoneX = 0;
		SceneGraph.centralZoneY = 0;
		Camera.originX = 0;
		for (i = 0; i < MiniMap.hintMapMarkers.length; i++) {
			MiniMap.hintMapMarkers[i] = null;
		}
		PlayerList.size = 0;
		NpcList.size = 0;
		for (i = 0; i < 2048; i++) {
			PlayerList.players[i] = null;
			PlayerList.appearanceCache[i] = null;
		}
		for (i = 0; i < 32768; i++) {
			NpcList.npcs[i] = null;
		}
		for (int level = 0; level < 4; level++) {
			for (@Pc(115) int x = 0; x < 104; x++) {
				for (@Pc(122) int y = 0; y < 104; y++) {
					SceneGraph.objStacks[level][x][y] = null;
				}
			}
		}
		Camera.resetCameraEffects();
		Protocol.verifyId = 0;
		VarpDomain.reset();
		InterfaceList.resetToLoginScreen(true);
		PluginRepository.OnLogout();
	}

	@OriginalMember(owner = "client!k", name = "a", descriptor = "(IIIIZIZ)V")
	public static void loadRegion(@OriginalArg(0) int plane, @OriginalArg(1) int zoneY, @OriginalArg(2) int zoneX, @OriginalArg(3) int localTileY, @OriginalArg(4) boolean fromLogin, @OriginalArg(5) int localTileX) {
		if (SceneGraph.centralZoneX == zoneX && zoneY == SceneGraph.centralZoneY && (SceneGraph.centralPlane == plane || SceneGraph.allLevelsAreVisible())) {
			return;
		}
		SceneGraph.centralZoneX = zoneX;
		SceneGraph.centralZoneY = zoneY;
		SceneGraph.centralPlane = plane;
		if (SceneGraph.allLevelsAreVisible()) {
			SceneGraph.centralPlane = 0;
		}
		if (fromLogin) {
			client.setGameState(28);
		} else {
			client.setGameState(25);
		}
		Fonts.drawTextOnScreen(true, LocalizedText.LOADING);
		@Pc(53) int prevOriginY = Camera.originY;
		@Pc(55) int prevOriginX = Camera.originX;
		Camera.originY = zoneY * 8 - 48;
		Camera.originX = (zoneX - 6) * 8;
		map = MapList.getContainingSource(SceneGraph.centralZoneX * 8, SceneGraph.centralZoneY * 8);
		@Pc(81) int deltaY = Camera.originY - prevOriginY;
		@Pc(86) int deltaX = Camera.originX - prevOriginX;
		mapElementList = null;
		@Pc(96) int i;
		@Pc(103) Npc npc;
		@Pc(109) int j;
		if (fromLogin) {
			NpcList.size = 0;
			for (i = 0; i < 32768; i++) {
				npc = NpcList.npcs[i];
				if (npc != null) {
					npc.xFine -= deltaX * 128;
					npc.yFine -= deltaY * 128;
					if (npc.xFine >= 0 && npc.xFine <= 13184 && npc.yFine >= 0 && npc.yFine <= 13184) {
						for (j = 0; j < 10; j++) {
							npc.movementQueueX[j] -= deltaX;
							npc.movementQueueY[j] -= deltaY;
						}
						NpcList.ids[NpcList.size++] = i;
					} else {
						NpcList.npcs[i].setNpcType(null);
						NpcList.npcs[i] = null;
					}
				}
			}
		} else {
			for (i = 0; i < 32768; i++) {
				npc = NpcList.npcs[i];
				if (npc != null) {
					for (j = 0; j < 10; j++) {
						npc.movementQueueX[j] -= deltaX;
						npc.movementQueueY[j] -= deltaY;
					}
					npc.xFine -= deltaX * 128;
					npc.yFine -= deltaY * 128;
				}
			}
		}
		for (i = 0; i < 2048; i++) {
			@Pc(265) Player player = PlayerList.players[i];
			if (player != null) {
				for (j = 0; j < 10; j++) {
					player.movementQueueX[j] -= deltaX;
					player.movementQueueY[j] -= deltaY;
				}
				player.xFine -= deltaX * 128;
				player.yFine -= deltaY * 128;
			}
		}
		Player.plane = plane;
		PlayerList.self.teleport(localTileX, false, localTileY);
		@Pc(322) byte xEnd = 104;
		@Pc(324) byte xStart = 0;
		@Pc(326) byte yStart = 0;
		@Pc(328) byte yStep = 1;
		@Pc(330) byte yEnd = 104;
		@Pc(332) byte xStep = 1;
		if (deltaY < 0) {
			yStep = -1;
			yEnd = -1;
			yStart = 103;
		}
		if (deltaX < 0) {
			xStep = -1;
			xStart = 103;
			xEnd = -1;
		}
		for (@Pc(358) int x = xStart; x != xEnd; x += xStep) {
			for (@Pc(367) int y = yStart; y != yEnd; y += yStep) {
				@Pc(378) int srcX = deltaX + x;
				@Pc(382) int srcY = y + deltaY;
				for (@Pc(384) int level = 0; level < 4; level++) {
					if (srcX >= 0 && srcY >= 0 && srcX < 104 && srcY < 104) {
						SceneGraph.objStacks[level][x][y] = SceneGraph.objStacks[level][srcX][srcY];
					} else {
						SceneGraph.objStacks[level][x][y] = null;
					}
				}
			}
		}
		for (@Pc(451) ChangeLocRequest locRequest = (ChangeLocRequest) ChangeLocRequest.queue.head(); locRequest != null; locRequest = (ChangeLocRequest) ChangeLocRequest.queue.next()) {
			locRequest.y -= deltaY;
			locRequest.x -= deltaX;
			if (locRequest.x < 0 || locRequest.y < 0 || locRequest.x >= 104 || locRequest.y >= 104) {
				locRequest.unlink();
			}
		}
		if (fromLogin) {
			Camera.renderX -= deltaX * 128;
			Camera.renderY -= deltaY * 128;
			Camera.lockedTargetY -= deltaY;
			Camera.lockedLookAtX -= deltaX;
			Camera.lockedLookAtY -= deltaY;
			Camera.lockedTargetX -= deltaX;
		} else {
			Camera.cameraType = 1;
		}
		SoundPlayer.size = 0;
		if (mapFlagX != 0) {
			mapFlagY -= deltaY;
			mapFlagX -= deltaX;
		}
		if (GlRenderer.enabled && fromLogin && (Math.abs(deltaX) > 104 || Math.abs(deltaY) > 104)) {
			FogManager.setInstantFade();
		}
		LightingManager.minimapRenderedPlane = -1;
		SceneGraph.spotanims.clear();
		SceneGraph.projectiles.clear();
	}

	@OriginalMember(owner = "client!dh", name = "a", descriptor = "(Z)V")
	public static void reconnect() {
		Protocol.outboundBuffer.offset = 0;
		Protocol.opcode3 = -1;
		Cs1ScriptRunner.isMenuOpen = false;
		Protocol.length = 0;
		mapFlagX = 0;
		MiniMenu.size = 0;
		Protocol.opcode2 = -1;
		MiniMap.state = 0;
		Player.rebootTimer = 0;
		Protocol.opcode4 = -1;
		Protocol.inboundBuffer.offset = 0;
		ticksSinceLastPacket = 0;
		Protocol.opcode = -1;
		@Pc(35) int i;
		for (i = 0; i < PlayerList.players.length; i++) {
			if (PlayerList.players[i] != null) {
				PlayerList.players[i].faceEntity = -1;
			}
		}
		for (i = 0; i < NpcList.npcs.length; i++) {
			if (NpcList.npcs[i] != null) {
				NpcList.npcs[i].faceEntity = -1;
			}
		}
		Inv.clear();
		Camera.cameraType = 1;
		client.setGameState(30);
		for (i = 0; i < 100; i++) {
			InterfaceList.rectangleDirty[i] = true;
		}
		ClientProt.sendWindowDetails();
	}

	@OriginalMember(owner = "client!ca", name = "a", descriptor = "(ZI)V")
	public static void readStaticLocs(@OriginalArg(0) boolean underwater) {
		@Pc(13) int regionCount = mapFilesBuffer.length;
		@Pc(19) byte[][] locFiles;
		if (GlRenderer.enabled && underwater) {
			locFiles = underWaterLocationsMapFilesBuffer;
		} else {
			locFiles = locationMapFilesBuffer;
		}
		for (@Pc(25) int i = 0; i < regionCount; i++) {
			@Pc(32) byte[] locData = locFiles[i];
			if (locData != null) {
				@Pc(45) int baseX = (regionBitPacked[i] >> 8) * 64 - Camera.originX;
				@Pc(56) int baseY = (regionBitPacked[i] & 0xFF) * 64 - Camera.originY;
				client.audioLoop();
				SceneGraph.readLocs(baseX, underwater, locData, baseY, PathFinder.collisionMaps);
			}
		}
	}

	@OriginalMember(owner = "client!gd", name = "c", descriptor = "(I)V")
	public static void rebuildMap() {
		ClientProt.ping(false);
		mapFilesMissingCount = 0;
		@Pc(12) boolean fileExists = true;
		@Pc(14) int id;
		for (id = 0; id < mapFilesBuffer.length; id++) {
			if (mapFileIds[id] != -1 && mapFilesBuffer[id] == null) {
				mapFilesBuffer[id] = client.js5Archive5.fetchFile(mapFileIds[id], 0);
				if (mapFilesBuffer[id] == null) {
					mapFilesMissingCount++;
					fileExists = false;
				}
			}
			if (locationsMapFileIds[id] != -1 && locationMapFilesBuffer[id] == null) {
				locationMapFilesBuffer[id] = client.js5Archive5.fetchFile(locationsMapFileIds[id], regionsXteaKeys[id], 0);
				if (locationMapFilesBuffer[id] == null) {
					fileExists = false;
					mapFilesMissingCount++;
				}
			}

			if (GlRenderer.enabled) {
				if (underWaterMapFileIds[id] != -1 && underWaterMapFilesBuffer[id] == null) {
					underWaterMapFilesBuffer[id] = client.js5Archive5.fetchFile(underWaterMapFileIds[id], 0);
					if (underWaterMapFilesBuffer[id] == null) {
						fileExists = false;
						mapFilesMissingCount++;
					}
				}
				if (underWaterLocationsMapFileIds[id] != -1 && underWaterLocationsMapFilesBuffer[id] == null) {
					underWaterLocationsMapFilesBuffer[id] = client.js5Archive5.fetchFile(underWaterLocationsMapFileIds[id], 0);
					if (underWaterLocationsMapFilesBuffer[id] == null) {
						mapFilesMissingCount++;
						fileExists = false;
					}
				}
			}

			if (npcSpawnsFileIds != null && npcSpawnsFilesBuffer[id] == null && npcSpawnsFileIds[id] != -1) {
				npcSpawnsFilesBuffer[id] = client.js5Archive5.fetchFile(npcSpawnsFileIds[id], regionsXteaKeys[id], 0);
				if (npcSpawnsFilesBuffer[id] == null) {
					mapFilesMissingCount++;
					fileExists = false;
				}
			}
		}

		if (mapElementList == null) {
			if (map == null || !client.js5Archive23.isGroupNameValid(JagString.concatenate(new JagString[]{map.group, LABELS_SUFFIX}))) {
				mapElementList = new MapElementList(0);
			} else if (client.js5Archive23.isGroupReady(JagString.concatenate(new JagString[]{map.group, LABELS_SUFFIX}))) {
				mapElementList = MapElementList.create(JagString.concatenate(new JagString[]{map.group, LABELS_SUFFIX}), client.js5Archive23);
			} else {
				fileExists = false;
				mapFilesMissingCount++;
			}
		}

		if (!fileExists) {
			loadingScreenState = 1;
			return;
		}

		missingLocModelCount = 0;
		fileExists = true;
		@Pc(320) int chunkX;
		@Pc(309) int chunkY;
		for (id = 0; id < mapFilesBuffer.length; id++) {
			@Pc(294) byte[] local294 = locationMapFilesBuffer[id];
			if (local294 != null) {
				chunkY = (regionBitPacked[id] & 0xFF) * 64 - Camera.originY;
				chunkX = (regionBitPacked[id] >> 8) * 64 - Camera.originX;
				if (SceneGraph.dynamicMapRegion) {
					chunkY = 10;
					chunkX = 10;
				}
				fileExists &= areLocModelsReady(chunkX, chunkY, local294);
			}
			if (GlRenderer.enabled) {
				local294 = underWaterLocationsMapFilesBuffer[id];
				if (local294 != null) {
					chunkX = (regionBitPacked[id] >> 8) * 64 - Camera.originX;
					chunkY = (regionBitPacked[id] & 0xFF) * 64 - Camera.originY;
					if (SceneGraph.dynamicMapRegion) {
						chunkY = 10;
						chunkX = 10;
					}
					fileExists &= areLocModelsReady(chunkX, chunkY, local294);
				}
			}
		}
		if (!fileExists) {
			loadingScreenState = 2;
			return;
		}

		if (loadingScreenState != 0) {
			Fonts.drawTextOnScreen(true, JagString.concatenate(new JagString[]{LocalizedText.LOADING, COMPLETE_PERCENT}));
		}

		client.audioLoop();
		client.unload();
		@Pc(420) boolean hasUnderWaterMap = false;
		@Pc(427) int i;
		if (GlRenderer.enabled && Preferences.highWaterDetail) {
			for (i = 0; i < mapFilesBuffer.length; i++) {
				if (underWaterLocationsMapFilesBuffer[i] != null || underWaterMapFilesBuffer[i] != null) {
					hasUnderWaterMap = true;
					break;
				}
			}
		}
		SceneGraph.init(GlRenderer.enabled ? GlobalConfig.TILE_DISTANCE : 25, hasUnderWaterMap);
		for (i = 0; i < 4; i++) {
			PathFinder.collisionMaps[i].clear();
		}
		for (i = 0; i < 4; i++) {
			for (chunkX = 0; chunkX < 104; chunkX++) {
				for (chunkY = 0; chunkY < 104; chunkY++) {
					SceneGraph.renderFlags[i][chunkX][chunkY] = 0;
				}
			}
		}
		AreaSoundManager.clear(false);
		if (GlRenderer.enabled) {
			ShadowManager.shadowMapImage.clear();
			for (i = 0; i < 13; i++) {
				for (chunkX = 0; chunkX < 13; chunkX++) {
					ShadowManager.shadows[i][chunkX].outputToSprite = true;
				}
			}
		}
		if (GlRenderer.enabled) {
			LightingManager.clearLightGrid();
		}
		if (GlRenderer.enabled) {
			FogManager.setDefaultChunksAtmosphere();
		}
		client.audioLoop();
		System.gc();
		ClientProt.ping(true);
		SceneGraph.load(false);
		if (!SceneGraph.dynamicMapRegion) {
			readStaticTerrain(false);
			ClientProt.ping(true);
			if (GlRenderer.enabled) {
				i = PlayerList.self.movementQueueX[0] >> 3;
				chunkX = PlayerList.self.movementQueueY[0] >> 3;
				FogManager.setLightPosition(chunkX, i);
			}
			readStaticLocs(false);
			if (npcSpawnsFilesBuffer != null) {
				decodeNpcFiles();
			}
		}
		if (SceneGraph.dynamicMapRegion) {
			readDynamicTerrain(false);
			ClientProt.ping(true);
			if (GlRenderer.enabled) {
				i = PlayerList.self.movementQueueX[0] >> 3;
				chunkX = PlayerList.self.movementQueueY[0] >> 3;
				FogManager.setLightPosition(chunkX, i);
			}
			readDynamicLocs(false);
		}
		client.unload();
		ClientProt.ping(true);
		SceneGraph.buildScene(PathFinder.collisionMaps, false);
		if (GlRenderer.enabled) {
			LightingManager.buildLightGrid();
		}
		ClientProt.ping(true);
		i = SceneGraph.firstVisibleLevel;
		if (i > Player.plane) {
			i = Player.plane;
		}
		if (i < Player.plane - 1) {
		}
		if (SceneGraph.allLevelsAreVisible()) {
			SceneGraph.setBaseLevel(0);
		} else {
			SceneGraph.setBaseLevel(SceneGraph.firstVisibleLevel);
		}
		SceneGraph.unload();
		if (GlRenderer.enabled && hasUnderWaterMap) {
			SceneGraph.setUnderwater(true);
			SceneGraph.load(true);
			if (!SceneGraph.dynamicMapRegion) {
				readStaticTerrain(true);
				ClientProt.ping(true);
				readStaticLocs(true);
			}
			if (SceneGraph.dynamicMapRegion) {
				readDynamicTerrain(true);
				ClientProt.ping(true);
				readDynamicLocs(true);
			}
			client.unload();
			ClientProt.ping(true);
			SceneGraph.buildScene(PathFinder.collisionMaps, true);
			ClientProt.ping(true);
			SceneGraph.unload();
			SceneGraph.setUnderwater(false);
		}
		if (GlRenderer.enabled) {
			for (chunkX = 0; chunkX < 13; chunkX++) {
				for (chunkY = 0; chunkY < 13; chunkY++) {
					ShadowManager.shadows[chunkX][chunkY].buildShadowMesh(SceneGraph.tileHeights[0], chunkX * 8, chunkY * 8);
				}
			}
		}
		for (chunkX = 0; chunkX < 104; chunkX++) {
			for (chunkY = 0; chunkY < 104; chunkY++) {
				Protocol.spawnGroundObject(chunkY, chunkX);
			}
		}
		ScriptRunner.updateRoofRemovalMode();
		client.audioLoop();
		ChangeLocRequest.flush();
		client.unload();
		glModelsInvalidated = false;
		if (GameShell.frame != null && Protocol.socket != null && client.gameState == 25) {
			Protocol.outboundBuffer.p1isaac(20);
			Protocol.outboundBuffer.p4(1057001181);
		}
		if (!SceneGraph.dynamicMapRegion) {
			@Pc(815) int local815 = (SceneGraph.centralZoneY + 6) / 8;
			@Pc(821) int local821 = (SceneGraph.centralZoneY - 6) / 8;
			chunkX = (SceneGraph.centralZoneX - 6) / 8;
			chunkY = (SceneGraph.centralZoneX + 6) / 8;
			for (@Pc(837) int local837 = chunkX - 1; local837 <= chunkY + 1; local837++) {
				for (@Pc(850) int local850 = local821 - 1; local850 <= local815 + 1; local850++) {
					if (local837 < chunkX || local837 > chunkY || local850 < local821 || local850 > local815) {
						client.js5Archive5.prefetchGroup(JagString.concatenate(new JagString[]{MAP_PREFIX, JagString.parseInt(local837), UNDERSCORE, JagString.parseInt(local850)}));
						client.js5Archive5.prefetchGroup(JagString.concatenate(new JagString[]{LOC_PREFIX, JagString.parseInt(local837), UNDERSCORE, JagString.parseInt(local850)}));
					}
				}
			}
		}
		if (client.gameState == 28) {
			client.setGameState(10);
		} else {
			client.setGameState(30);
			if (Protocol.socket != null) {
				Protocol.outboundBuffer.p1isaac(110);
			}
		}
		WorldMap.restorePreviousMap();
		client.audioLoop();
		GameShell.resetTimer();
	}

	@OriginalMember(owner = "client!dm", name = "a", descriptor = "(BII[B)Z")
	public static boolean areLocModelsReady(@OriginalArg(1) int offsetX, @OriginalArg(2) int offsetY, @OriginalArg(3) byte[] data) {
		@Pc(15) boolean allReady = true;
		@Pc(17) int locId = -1;
		@Pc(22) Buffer buf = new Buffer(data);
		label70:
		while (true) {
			@Pc(26) int locIdDelta = buf.gVarSmart();
			if (locIdDelta == 0) {
				return allReady;
			}
			@Pc(33) int packedPos = 0;
			locId += locIdDelta;
			@Pc(39) boolean locFound = false;
			while (true) {
				@Pc(78) int locShape;
				@Pc(95) LocType locType;
				do {
					@Pc(72) int tileX;
					@Pc(68) int tileY;
					do {
						do {
							do {
								do {
									@Pc(45) int posDelta;
									while (locFound) {
										posDelta = buf.gsmarts();
										if (posDelta == 0) {
											continue label70;
										}
										buf.g1();
									}
									posDelta = buf.gsmarts();
									if (posDelta == 0) {
										continue label70;
									}
									packedPos += posDelta - 1;
									@Pc(58) int localY = packedPos & 0x3F;
									@Pc(64) int localX = packedPos >> 6 & 0x3F;
									tileY = offsetY + localY;
									tileX = offsetX + localX;
									locShape = buf.g1() >> 2;
								} while (tileX <= 0);
							} while (tileY <= 0);
						} while (tileX >= 103);
					} while (tileY >= 103);
					locType = LocTypeList.get(locId);
				} while (locShape == 22 && !Preferences.showGroundDecorations && locType.interactable == 0 && locType.blockwalk != 1 && !locType.forcedecor);
				locFound = true;
				if (!locType.isReady()) {
					allReady = false;
					missingLocModelCount++;
				}
			}
		}
	}

	@OriginalMember(owner = "client!t", name = "a", descriptor = "(ZB)V")
	public static void readDynamicLocs(@OriginalArg(0) boolean arg0) {
		@Pc(19) byte local19;
		@Pc(21) byte[][] local21;
		if (GlRenderer.enabled && arg0) {
			local21 = underWaterLocationsMapFilesBuffer;
			local19 = 1;
		} else {
			local19 = 4;
			local21 = locationMapFilesBuffer;
		}
		for (@Pc(29) int local29 = 0; local29 < local19; local29++) {
			client.audioLoop();
			for (@Pc(36) int local36 = 0; local36 < 13; local36++) {
				for (@Pc(43) int local43 = 0; local43 < 13; local43++) {
					@Pc(56) int local56 = Protocol.buildAreaChunks[local29][local36][local43];
					if (local56 != -1) {
						@Pc(67) int local67 = local56 >> 24 & 0x3;
						if (!arg0 || local67 == 0) {
							@Pc(77) int local77 = local56 >> 1 & 0x3;
							@Pc(83) int local83 = local56 >> 14 & 0x3FF;
							@Pc(89) int local89 = local56 >> 3 & 0x7FF;
							@Pc(99) int local99 = local89 / 8 + (local83 / 8 << 8);
							for (@Pc(101) int local101 = 0; local101 < regionBitPacked.length; local101++) {
								if (regionBitPacked[local101] == local99 && local21[local101] != null) {
									readDynamicRegionLocs(PathFinder.collisionMaps, local29, local21[local101], local67, local77, local36 * 8, local43 * 8, arg0, (local83 & 0x7) * 8, (local89 & 0x7) * 8);
									break;
								}
							}
						}
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!rj", name = "a", descriptor = "([Lclient!mj;I[BIIIIZIIB)V")
	public static void readDynamicRegionLocs(@OriginalArg(0) CollisionMap[] arg0, @OriginalArg(1) int arg1, @OriginalArg(2) byte[] arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(5) int arg5, @OriginalArg(6) int arg6, @OriginalArg(7) boolean arg7, @OriginalArg(8) int arg8, @OriginalArg(9) int arg9) {
		@Pc(7) int local7 = -1;
		@Pc(12) Buffer local12 = new Buffer(arg2);
		while (true) {
			@Pc(20) int local20 = local12.gVarSmart();
			if (local20 == 0) {
				return;
			}
			local7 += local20;
			@Pc(31) int local31 = 0;
			while (true) {
				@Pc(35) int local35 = local12.gsmarts();
				if (local35 == 0) {
					break;
				}
				local31 += local35 - 1;
				@Pc(50) int local50 = local31 & 0x3F;
				@Pc(56) int local56 = local31 >> 6 & 0x3F;
				@Pc(60) int local60 = local31 >> 12;
				@Pc(64) int local64 = local12.g1();
				@Pc(68) int local68 = local64 >> 2;
				@Pc(72) int local72 = local64 & 0x3;
				if (arg3 == local60 && local56 >= arg8 && local56 < arg8 + 8 && arg9 <= local50 && arg9 + 8 > local50) {
					@Pc(103) LocType local103 = LocTypeList.get(local7);
					@Pc(120) int local120 = rotateLocX(local50 & 0x7, arg4, local72, local103.length, local103.width, local56 & 0x7) + arg5;
					@Pc(137) int local137 = rotateLocY(local103.width, arg4, local103.length, local56 & 0x7, local72, local50 & 0x7) + arg6;
					if (local120 > 0 && local137 > 0 && local120 < 103 && local137 < 103) {
						@Pc(154) CollisionMap local154 = null;
						if (!arg7) {
							@Pc(159) int local159 = arg1;
							if ((SceneGraph.renderFlags[1][local120][local137] & 0x2) == 2) {
								local159 = arg1 - 1;
							}
							if (local159 >= 0) {
								local154 = arg0[local159];
							}
						}
						SceneGraph.addLoc(arg1, !arg7, arg1, arg7, local154, local7, local68, local120, local137, local72 + arg4 & 0x3);
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!eb", name = "a", descriptor = "(IIIIIII)I")
	public static int rotateLocX(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(2) int arg2, @OriginalArg(3) int arg3, @OriginalArg(4) int arg4, @OriginalArg(6) int arg5) {
		if ((arg2 & 0x1) == 1) {
			@Pc(10) int local10 = arg4;
			arg4 = arg3;
			arg3 = local10;
		}
		@Pc(18) int local18 = arg1 & 0x3;
		if (local18 == 0) {
			return arg5;
		} else if (local18 == 1) {
			return arg0;
		} else if (local18 == 2) {
			return 7 + 1 - arg5 - arg4;
		} else {
			return 7 + 1 - arg0 - arg3;
		}
	}

	@OriginalMember(owner = "client!th", name = "a", descriptor = "(IIBIIII)I")
	public static int rotateLocY(@OriginalArg(0) int arg0, @OriginalArg(1) int arg1, @OriginalArg(3) int arg2, @OriginalArg(4) int arg3, @OriginalArg(5) int arg4, @OriginalArg(6) int arg5) {
		if ((arg4 & 0x1) == 1) {
			@Pc(9) int local9 = arg0;
			arg0 = arg2;
			arg2 = local9;
		}
		@Pc(29) int local29 = arg1 & 0x3;
		if (local29 == 0) {
			return arg5;
		} else if (local29 == 1) {
			return 7 + 1 - arg3 - arg0;
		} else if (local29 == 2) {
			return 1 + 7 - arg2 - arg5;
		} else {
			return arg3;
		}
	}

	@OriginalMember(owner = "client!mh", name = "h", descriptor = "(B)V")
	public static void decodeNpcFiles() {
		@Pc(10) int regionCount = npcSpawnsFilesBuffer.length;
		for (@Pc(16) int i = 0; i < regionCount; i++) {
			if (npcSpawnsFilesBuffer[i] != null) {
				@Pc(25) int regionSlot = -1;
				for (@Pc(27) int j = 0; j < decodedNpcRegionCount; j++) {
					if (decodedNpcRegions[j] == regionBitPacked[i]) {
						regionSlot = j;
						break;
					}
				}
				if (regionSlot == -1) {
					decodedNpcRegions[decodedNpcRegionCount] = regionBitPacked[i];
					regionSlot = decodedNpcRegionCount++;
				}
				@Pc(67) int npcIndex = 0;
				@Pc(74) Buffer buf = new Buffer(npcSpawnsFilesBuffer[i]);
				while (buf.offset < npcSpawnsFilesBuffer[i].length && npcIndex < 511) {
					@Pc(97) int npcSlot = npcIndex++ << 6 | regionSlot;
					@Pc(103) int packedSpawn = buf.g2();
					@Pc(107) int spawnPlane = packedSpawn >> 14;
					@Pc(113) int localX = packedSpawn >> 7 & 0x3F;
					@Pc(125) int tileX = localX + (regionBitPacked[i] >> 8) * 64 - Camera.originX;
					@Pc(129) int localY = packedSpawn & 0x3F;
					@Pc(142) int tileY = localY + (regionBitPacked[i] & 0xFF) * 64 - Camera.originY;
					@Pc(148) NpcType npcType = NpcTypeList.get(buf.g2());
					if (NpcList.npcs[npcSlot] == null && (npcType.loginscreenproperties & 0x1) > 0 && spawnPlane == SceneGraph.centralPlane && tileX >= 0 && npcType.size + tileX < 104 && tileY >= 0 && tileY + npcType.size < 104) {
						NpcList.npcs[npcSlot] = new Npc();
						@Pc(198) Npc npc = NpcList.npcs[npcSlot];
						NpcList.ids[NpcList.size++] = npcSlot;
						npc.lastSeenLoop = client.loop;
						npc.setNpcType(npcType);
						npc.setSize(npc.type.size);
						npc.targetAngle = npc.currentAngle = PathingEntity.ANGLES[npc.type.spawndirection];
						npc.turnSpeed = npc.type.rotationspeed;
						if (npc.turnSpeed == 0) {
							npc.currentAngle = 0;
						}
						npc.basTypeId = npc.type.bastypeid;
						npc.teleport(npc.getSize(), tileX, tileY, true);
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!ha", name = "a", descriptor = "(ZB)V")
	public static void readDynamicTerrain(@OriginalArg(0) boolean arg0) {
		@Pc(11) byte local11;
		@Pc(13) byte[][] local13;
		if (GlRenderer.enabled && arg0) {
			local11 = 1;
			local13 = underWaterMapFilesBuffer;
		} else {
			local13 = mapFilesBuffer;
			local11 = 4;
		}
		for (@Pc(21) int local21 = 0; local21 < local11; local21++) {
			client.audioLoop();
			for (@Pc(32) int local32 = 0; local32 < 13; local32++) {
				for (@Pc(39) int local39 = 0; local39 < 13; local39++) {
					@Pc(52) int local52 = Protocol.buildAreaChunks[local21][local32][local39];
					@Pc(54) boolean local54 = false;
					if (local52 != -1) {
						@Pc(65) int local65 = local52 >> 24 & 0x3;
						if (!arg0 || local65 == 0) {
							@Pc(76) int local76 = local52 >> 3 & 0x7FF;
							@Pc(82) int local82 = local52 >> 1 & 0x3;
							@Pc(88) int local88 = local52 >> 14 & 0x3FF;
							@Pc(98) int local98 = (local88 / 8 << 8) + local76 / 8;
							for (@Pc(100) int local100 = 0; local100 < regionBitPacked.length; local100++) {
								if (regionBitPacked[local100] == local98 && local13[local100] != null) {
									SceneGraph.readDynamicTerrain(local82, local32 * 8, local21, PathFinder.collisionMaps, local39 * 8, local13[local100], local65, (local76 & 0x7) * 8, (local88 & 0x7) * 8, arg0);
									local54 = true;
									break;
								}
							}
						}
					}
					if (!local54) {
						SceneGraph.clearTerrainRegion(local21, local39 * 8, local32 * 8, 8, 8);
					}
				}
			}
		}
	}

	@OriginalMember(owner = "client!ha", name = "a", descriptor = "(I)V")
	public static void processInterface() {
		if (!Cs1ScriptRunner.isMenuOpen) {
			if (MiniMenu.clickProcessingState != 0) {
				ScriptRunner.interfaceMouseX = Mouse.lastHandledClickX;
				ScriptRunner.interfaceMouseY = Mouse.lastHandledClickY;
			} else if (Mouse.clickButton == 0) {
				ScriptRunner.interfaceMouseX = Mouse.lastMouseX;
				ScriptRunner.interfaceMouseY = Mouse.lastMouseY;
			} else {
				ScriptRunner.interfaceMouseX = Mouse.clickX;
				ScriptRunner.interfaceMouseY = Mouse.clickY;
			}
			MiniMenu.size = 1;
			MiniMenu.ops[0] = LocalizedText.CANCEL;
			MiniMenu.opBases[0] = JagString.EMPTY;
			MiniMenu.actions[0] = 1005;
			MiniMenu.cursors[0] = MiniMenu.defaultCursorId;
		}
		if (InterfaceList.topLevelInterface != -1) {
			InterfaceList.updateAnimations(InterfaceList.topLevelInterface);
		}
		@Pc(60) int local60;
		for (local60 = 0; local60 < InterfaceList.rectangles; local60++) {
			if (InterfaceList.rectangleDirty[local60]) {
				InterfaceList.rectangleRedraw[local60] = true;
			}
			InterfaceList.rectangleDirtySnapshot[local60] = InterfaceList.rectangleDirty[local60];
			InterfaceList.rectangleDirty[local60] = false;
		}
		tooltipComponent = null;
		Cs1ScriptRunner.gameSceneTooltipX = -1;
		InterfaceList.viewportX = -1;
		InterfaceList.mouseOverInventoryInterface = null;
		if (GlRenderer.enabled) {
			ScriptRunner.glSceneNeedsRender = true;
		}
		InterfaceList.currentRenderLoop = client.loop;
		if (InterfaceList.topLevelInterface != -1) {
			InterfaceList.rectangles = 0;
			Cs1ScriptRunner.renderTopLevelInterface();
		}
		if (GlRenderer.enabled) {
			GlRaster.resetClipRegion();
		} else {
			SoftwareRaster.resetClip();
		}
		if (!Cs1ScriptRunner.isMenuOpen) {
			PluginRepository.OnMiniMenuCreate();
		}
		MiniMenu.sort();
		if (Cs1ScriptRunner.isMenuOpen) {
			if (InterfaceList.useStyledMenu) {
				MiniMenu.drawB();
			} else {
				MiniMenu.drawA();
			}
		} else if (tooltipComponent != null) {
			MiniMenu.renderTooltip(tooltipComponent, Cs1ScriptRunner.tooltipRenderY, Cs1ScriptRunner.tooltipRenderX);
		} else if (Cs1ScriptRunner.gameSceneTooltipX != -1) {
			MiniMenu.renderTooltip(null, InterfaceList.viewportX, Cs1ScriptRunner.gameSceneTooltipX);
		}
		local60 = Cs1ScriptRunner.isMenuOpen ? -1 : getActiveCursorId();
		if (local60 == -1) {
			local60 = ScriptRunner.scriptCursorId;
		}
		InterfaceList.setCursor(local60);
		if (MiniMenu.minimapWalkState == 1) {
			MiniMenu.minimapWalkState = 2;
		}
		if (Protocol.viewportWalkState == 1) {
			Protocol.viewportWalkState = 2;
		}
		if (Cheat.rectDebug == 3) {
			for (@Pc(189) int local189 = 0; local189 < InterfaceList.rectangles; local189++) {
				if (InterfaceList.rectangleDirtySnapshot[local189]) {
					if (GlRenderer.enabled) {
						GlRaster.fillRectAlpha(InterfaceList.rectangleX[local189], InterfaceList.rectangleY[local189], InterfaceList.rectangleWidth[local189], InterfaceList.rectangleHeight[local189], 16711935, 128);
					} else {
						SoftwareRaster.fillRectAlpha(InterfaceList.rectangleX[local189], InterfaceList.rectangleY[local189], InterfaceList.rectangleWidth[local189], InterfaceList.rectangleHeight[local189], 16711935, 128);
					}
				} else if (InterfaceList.rectangleRedraw[local189]) {
					if (GlRenderer.enabled) {
						GlRaster.fillRectAlpha(InterfaceList.rectangleX[local189], InterfaceList.rectangleY[local189], InterfaceList.rectangleWidth[local189], InterfaceList.rectangleHeight[local189], 16711680, 128);
					} else {
						SoftwareRaster.fillRectAlpha(InterfaceList.rectangleX[local189], InterfaceList.rectangleY[local189], InterfaceList.rectangleWidth[local189], InterfaceList.rectangleHeight[local189], 16711680, 128);
					}
				}
			}
		}
		AreaSoundManager.redraw(Protocol.sceneDelta, PlayerList.self.xFine, PlayerList.self.yFine, Player.plane);
		Protocol.sceneDelta = 0;
	}

	@OriginalMember(owner = "client!tb", name = "h", descriptor = "(I)I")
	public static int getActiveCursorId() {
		return Cheat.shiftClick && Keyboard.pressedKeys[Keyboard.KEY_SHIFT] && MiniMenu.size > 2 ? MiniMenu.cursors[MiniMenu.size - 2] : MiniMenu.cursors[MiniMenu.size - 1];
	}

	@OriginalMember(owner = "client!gn", name = "a", descriptor = "(ZI)V")
	public static void readStaticTerrain(@OriginalArg(0) boolean underwater) {
		@Pc(7) byte levelCount;
		@Pc(9) byte[][] terrainFiles;
		if (GlRenderer.enabled && underwater) {
			levelCount = 1;
			terrainFiles = underWaterMapFilesBuffer;
		} else {
			levelCount = 4;
			terrainFiles = mapFilesBuffer;
		}
		@Pc(18) int regionCount = terrainFiles.length;
		@Pc(20) int i;
		@Pc(38) int baseX;
		@Pc(49) int baseY;
		@Pc(53) byte[] terrainData;
		for (i = 0; i < regionCount; i++) {
			baseX = (regionBitPacked[i] >> 8) * 64 - Camera.originX;
			baseY = (regionBitPacked[i] & 0xFF) * 64 - Camera.originY;
			terrainData = terrainFiles[i];
			if (terrainData != null) {
				client.audioLoop();
				SceneGraph.readTerrain(PathFinder.collisionMaps, underwater, SceneGraph.centralZoneX * 8 - 48, baseY, baseX, (SceneGraph.centralZoneY - 6) * 8, terrainData);
			}
		}
		for (i = 0; i < regionCount; i++) {
			baseX = (regionBitPacked[i] >> 8) * 64 - Camera.originX;
			baseY = (regionBitPacked[i] & 0xFF) * 64 - Camera.originY;
			terrainData = terrainFiles[i];
			if (terrainData == null && SceneGraph.centralZoneY < 800) {
				client.audioLoop();
				for (@Pc(130) int level = 0; level < levelCount; level++) {
					SceneGraph.clearTerrainRegion(level, baseY, baseX, 64, 64);
				}
			}
		}
	}

	@OriginalMember(owner = "client!j", name = "g", descriptor = "(I)V")
	public static void clearLoginScreenSprites() {
		menuBottomFillSprite = null;
		menuHeaderFillSprite = null;
		menuSideFillSprite = null;
		menuHeaderEdgeSprite = null;
		menuBottomEdgeSprite = null;
	}
}
