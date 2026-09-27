package rt4;

import com.jogamp.opengl.GL2;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Iterator;

// Not part of the original client. Draws the ground beyond the loaded 104x104
// area, so zooming out no longer shows the world ending in a hard edge.
//
// Display only: level-0 terrain decoded straight from the (unencrypted) map
// files, with heights, underlay colour blending and lighting done the way
// SceneGraph.buildScene does them, so it lines up with the real terrain at
// the seam. No scenery, no textures (textured floors use their average
// colour, which is what a mipmapped texture looks like from far away).
//
// Meshes are built per 64x64 map region, one region per frame, cached, and
// drawn in 8x8 zones: a zone the real scene covers is skipped, and zones that
// overlap its outer edge are drawn at a deeper depth layer so the real
// terrain always wins.
public final class FarTerrain {

	private static final int ZONE_TILES = 8;
	private static final int ZONES_PER_SIDE = 64 / ZONE_TILES;
	private static final int VERTS_PER_ZONE = ZONE_TILES * ZONE_TILES * 6;
	private static final int STRIDE = 36;
	// Behind the real ground (level 0 draws at ~201)
	private static final float DEPTH_LAYER = 301.5F;
	private static final int BLEND = 5;

	private static final class Region {
		final int id;
		final int baseX;
		final int baseY;
		boolean decoded;
		boolean missing;
		final int[] heights = new int[64 * 64];
		final byte[] underlays = new byte[64 * 64];
		final byte[] overlays = new byte[64 * 64];
		final byte[] shapes = new byte[64 * 64];
		GlVertexBufferObject vbo;
		ByteBuffer vertexData;
		boolean meshed;
		// Which zones have any triangles
		final boolean[] zoneFilled = new boolean[ZONES_PER_SIDE * ZONES_PER_SIDE];
		// Meshed with all 8 neighbours decoded, so its edges are final
		boolean meshComplete;

		Region(int id) {
			this.id = id;
			this.baseX = (id >> 8) * 64;
			this.baseY = (id & 0xFF) * 64;
		}
	}

	private static final HashMap<Integer, Region> regions = new HashMap<>();

	// Real average colour (RGB) of each texture, from its generated pixels.
	// The cache's stored average is a placeholder for many HD-era textures.
	private static final HashMap<Integer, Integer> textureAverages = new HashMap<>();


	private FarTerrain() {
	}

	public static boolean isEnabled() {
		return GlobalConfig.FAR_TERRAIN_TILES > 0 && GlRenderer.enabled && !SceneGraph.dynamicMapRegion && client.gameState == 30;
	}

	// Extra distance the far clip plane needs
	public static int extraViewDistance() {
		return isEnabled() ? GlobalConfig.FAR_TERRAIN_TILES * 128 : 0;
	}

	private static Region region(int worldX, int worldY) {
		return regions.get(((worldX >> 6) << 8) | (worldY >> 6));
	}

	private static boolean decode(Region r) {
		int group = client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.MAP_PREFIX, JagString.parseInt(r.baseX >> 6), LoginManager.UNDERSCORE, JagString.parseInt(r.baseY >> 6)}));
		if (group == -1) {
			r.missing = true;
			r.decoded = true;
			return true;
		}
		byte[] data = client.js5Archive5.fetchFile(group, 0);
		if (data == null) {
			// Still downloading; the fetch requested it
			return false;
		}
		Buffer buffer = new Buffer(data);
		// Level 0 comes first, x-major, as SceneGraph.readTerrain reads it
		for (int x = 0; x < 64; x++) {
			for (int y = 0; y < 64; y++) {
				int i = x * 64 + y;
				int worldX = r.baseX + x;
				int worldY = r.baseY + y;
				while (true) {
					int opcode = buffer.g1();
					if (opcode == 0) {
						r.heights[i] = -PerlinNoise.getTileHeight(worldY + 556238, worldX + 932731) * 8;
						break;
					}
					if (opcode == 1) {
						int height = buffer.g1();
						if (height == 1) {
							height = 0;
						}
						r.heights[i] = -height * 8;
						break;
					}
					if (opcode <= 49) {
						r.overlays[i] = buffer.g1b();
						r.shapes[i] = (byte) ((opcode - 2) / 4);
					} else if (opcode > 81) {
						r.underlays[i] = (byte) (opcode - 81);
					}
				}
			}
		}
		r.decoded = true;
		return true;
	}

	private static int heightAt(Region home, int worldX, int worldY) {
		Region r = region(worldX, worldY);
		if (r == null || !r.decoded || r.missing) {
			// Neighbour not loaded: clamp to this region's edge
			r = home;
			worldX = Math.max(home.baseX, Math.min(home.baseX + 63, worldX));
			worldY = Math.max(home.baseY, Math.min(home.baseY + 63, worldY));
		}
		return r.heights[(worldX - r.baseX) * 64 + (worldY - r.baseY)];
	}

	private static int underlayAt(int worldX, int worldY) {
		Region r = region(worldX, worldY);
		if (r == null || !r.decoded || r.missing) {
			return 0;
		}
		return r.underlays[(worldX - r.baseX) * 64 + (worldY - r.baseY)] & 0xFF;
	}

	// -1 while the texture isn't loaded yet
	private static int textureAverage(int textureId) {
		Integer cached = textureAverages.get(textureId);
		if (cached != null) {
			return cached;
		}
		int[] pixels = Rasteriser.textureProvider.getPixels(textureId);
		if (pixels == null) {
			return -1;
		}
		long red = 0, green = 0, blue = 0;
		int count = 0;
		for (int pixel : pixels) {
			if (pixel == 0) {
				// Transparent
				continue;
			}
			red += pixel >> 16 & 0xFF;
			green += pixel >> 8 & 0xFF;
			blue += pixel & 0xFF;
			count++;
		}
		if (count == 0) {
			textureAverages.put(textureId, 0);
			return 0;
		}
		// getPixels generates at brightness 1.0; GL textures at 0.7
		int rgb = gamma(red / count) << 16 | gamma(green / count) << 8 | gamma(blue / count);
		textureAverages.put(textureId, rgb);
		return rgb;
	}

	private static int gamma(long channel) {
		return (int) (Math.pow(channel / 255.0, 0.7) * 255.0);
	}

	private static int multiply(int rgb, int by) {
		int red = (rgb >> 16 & 0xFF) * (by >> 16 & 0xFF) / 255;
		int green = (rgb >> 8 & 0xFF) * (by >> 8 & 0xFF) / 255;
		int blue = (rgb & 0xFF) * (by & 0xFF) / 255;
		return red << 16 | green << 8 | blue;
	}

	// Colour for an overlay (paths, water, ...) or -1 if it's invisible.
	// Same lightness (74) the HD terrain uses before GL lighting; brighter
	// colours saturate to white once lit.
	private static int overlayColor(int overlay) {
		FloType type = FloTypeList.get(overlay - 1);
		// As SceneGraph.buildGlShapedTiles: overlays without a base colour
		// aren't drawn in HD at all, and the vertex colour is the base colour
		// run through getTexturedColor, which the texture is multiplied by
		if (type.baseColor == -1) {
			return -1;
		}
		int rgb = SceneGraph.getTexturedColor(type.texture >= 0 ? type.texture : 0, type.baseColor, 74);
		if (type.texture >= 0) {
			int average = textureAverage(type.texture);
			rgb = multiply(rgb, average != -1 ? average : Rasteriser.palette[Rasteriser.textureProvider.getAverageColor(type.texture)]);
		}
		return rgb;
	}

	private static void mesh(Region r) {
		int size = 64 + 2 * BLEND + 1;
		// Underlay HSL sums over the region plus a BLEND border, as a summed
		// area table, for buildScene's 11x11 colour blend
		long[] hue = new long[(size + 1) * (size + 1)];
		long[] sat = new long[(size + 1) * (size + 1)];
		long[] light = new long[(size + 1) * (size + 1)];
		long[] chroma = new long[(size + 1) * (size + 1)];
		int[] count = new int[(size + 1) * (size + 1)];
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				int u = underlayAt(r.baseX + x - BLEND, r.baseY + y - BLEND);
				int idx = (x + 1) * (size + 1) + (y + 1);
				int left = x * (size + 1) + (y + 1);
				int down = (x + 1) * (size + 1) + y;
				int diag = x * (size + 1) + y;
				long h = 0, s = 0, l = 0, c = 0;
				int n = 0;
				if (u > 0) {
					FluType type = FluTypeList.get(u - 1);
					h = type.weightedHue;
					s = type.saturation;
					l = type.lightness;
					c = type.chroma;
					n = 1;
				}
				hue[idx] = h + hue[left] + hue[down] - hue[diag];
				sat[idx] = s + sat[left] + sat[down] - sat[diag];
				light[idx] = l + light[left] + light[down] - light[diag];
				chroma[idx] = c + chroma[left] + chroma[down] - chroma[diag];
				count[idx] = n + count[left] + count[down] - count[diag];
			}
		}

		// Colour at each tile corner (0..64): the blended underlay of the tile
		// that corner belongs to, like buildGlPlainTiles' per-corner colours
		int[] cornerColor = new int[65 * 65];
		for (int x = 0; x <= 64; x++) {
			for (int y = 0; y <= 64; y++) {
				int u = underlayAt(r.baseX + x, r.baseY + y);
				if (u == 0) {
					cornerColor[x * 65 + y] = -1;
					continue;
				}
				int x0 = x, y0 = y, x1 = x + 2 * BLEND + 1, y1 = y + 2 * BLEND + 1;
				int n = sum(count, size, x0, y0, x1, y1);
				long c = sumL(chroma, size, x0, y0, x1, y1);
				if (n == 0 || c == 0) {
					cornerColor[x * 65 + y] = -1;
					continue;
				}
				int hsl = ColorUtils.packHsl((int) (sumL(light, size, x0, y0, x1, y1) / n), (int) (sumL(sat, size, x0, y0, x1, y1) / n), (int) (sumL(hue, size, x0, y0, x1, y1) * 256 / c));
				FluType type = FluTypeList.get(u - 1);
				int rgb = SceneGraph.getTexturedColor(type.texture >= 0 ? type.texture : 0, hsl, 74);
				if (type.texture >= 0) {
					int average = textureAverage(type.texture);
					rgb = multiply(rgb, average != -1 ? average : Rasteriser.palette[Rasteriser.textureProvider.getAverageColor(type.texture)]);
				}
				cornerColor[x * 65 + y] = rgb;
			}
		}

		// Normals from central differences of the corner heights
		float[] normals = new float[65 * 65 * 3];
		for (int x = 0; x <= 64; x++) {
			for (int y = 0; y <= 64; y++) {
				int q = heightAt(r, r.baseX + x + 1, r.baseY + y) - heightAt(r, r.baseX + x - 1, r.baseY + y);
				int s = heightAt(r, r.baseX + x, r.baseY + y + 1) - heightAt(r, r.baseX + x, r.baseY + y - 1);
				float len = (float) Math.sqrt(q * q + s * s + 65536);
				int i = (x * 65 + y) * 3;
				normals[i] = q / len;
				normals[i + 1] = -256.0F / len;
				normals[i + 2] = s / len;
			}
		}
		int[] heights = new int[65 * 65];
		for (int x = 0; x <= 64; x++) {
			for (int y = 0; y <= 64; y++) {
				heights[x * 65 + y] = heightAt(r, r.baseX + x, r.baseY + y);
			}
		}

		Buffer buf = new Buffer(64 * 64 * 6 * STRIDE);
		for (int zone = 0; zone < ZONES_PER_SIDE * ZONES_PER_SIDE; zone++) {
			int zoneX = zone / ZONES_PER_SIDE * ZONE_TILES;
			int zoneY = zone % ZONES_PER_SIDE * ZONE_TILES;
			boolean filled = false;
			for (int tx = zoneX; tx < zoneX + ZONE_TILES; tx++) {
				for (int ty = zoneY; ty < zoneY + ZONE_TILES; ty++) {
					int i = tx * 64 + ty;
					int u = r.underlays[i] & 0xFF;
					int o = r.overlays[i] & 0xFF;
					int flat = -1;
					if (o > 0 && r.shapes[i] == 0) {
						flat = overlayColor(o);
					}
					int cSW, cSE, cNE, cNW;
					if (flat != -1) {
						cSW = cSE = cNE = cNW = flat;
					} else if (u > 0) {
						int own = cornerColor[tx * 65 + ty];
						if (own == -1) {
							own = 0;
						}
						cSW = own;
						cSE = orElse(cornerColor[(tx + 1) * 65 + ty], own);
						cNE = orElse(cornerColor[(tx + 1) * 65 + ty + 1], own);
						cNW = orElse(cornerColor[tx * 65 + ty + 1], own);
					} else if (o > 0) {
						flat = overlayColor(o);
						if (flat == -1) {
							degenerateTile(buf);
							continue;
						}
						cSW = cSE = cNE = cNW = flat;
					} else {
						degenerateTile(buf);
						continue;
					}
					filled = true;
					// Same fan as a plain HD tile: NW, SW, SE, then NW, SE, NE
					vertex(buf, tx, ty + 1, heights, normals, cNW);
					vertex(buf, tx, ty, heights, normals, cSW);
					vertex(buf, tx + 1, ty, heights, normals, cSE);
					vertex(buf, tx, ty + 1, heights, normals, cNW);
					vertex(buf, tx + 1, ty, heights, normals, cSE);
					vertex(buf, tx + 1, ty + 1, heights, normals, cNE);
				}
			}
			r.zoneFilled[zone] = filled;
		}
		if (GlRenderer.arbVboSupported) {
			r.vbo = new GlVertexBufferObject();
			r.vbo.setArrayBuffer(ByteBuffer.wrap(buf.data, 0, buf.offset));
			r.vertexData = null;
		} else {
			ByteBuffer data = ByteBuffer.allocateDirect(buf.offset).order(ByteOrder.nativeOrder());
			data.put(buf.data, 0, buf.offset);
			data.flip();
			r.vbo = null;
			r.vertexData = data;
		}
		r.meshed = true;
		r.meshComplete = neighboursDecoded(r);
	}

	private static int orElse(int value, int fallback) {
		return value == -1 ? fallback : value;
	}

	private static int sum(int[] table, int size, int x0, int y0, int x1, int y1) {
		int w = size + 1;
		return table[x1 * w + y1] - table[x0 * w + y1] - table[x1 * w + y0] + table[x0 * w + y0];
	}

	private static long sumL(long[] table, int size, int x0, int y0, int x1, int y1) {
		int w = size + 1;
		return table[x1 * w + y1] - table[x0 * w + y1] - table[x1 * w + y0] + table[x0 * w + y0];
	}

	// Keeps every zone the same size so zones can be drawn by index
	private static void degenerateTile(Buffer buf) {
		for (int i = 0; i < 6 * STRIDE; i++) {
			buf.data[buf.offset++] = 0;
		}
	}

	private static void vertex(Buffer buf, int x, int y, int[] heights, float[] normals, int rgb) {
		int ni = (x * 65 + y) * 3;
		float px = x * 128;
		float py = heights[x * 65 + y];
		float pz = y * 128;
		if (GlRenderer.bigEndian) {
			buf.pFloat(px);
			buf.pFloat(py);
			buf.pFloat(pz);
			buf.p4(rgb << 8 | 0xFF);
			buf.pFloat(normals[ni]);
			buf.pFloat(normals[ni + 1]);
			buf.pFloat(normals[ni + 2]);
			buf.pFloat(0.0F);
			buf.pFloat(0.0F);
		} else {
			buf.gFloat(px);
			buf.gFloat(py);
			buf.gFloat(pz);
			buf.p4(rgb << 8 | 0xFF);
			buf.gFloat(normals[ni]);
			buf.gFloat(normals[ni + 1]);
			buf.gFloat(normals[ni + 2]);
			buf.gFloat(0.0F);
			buf.gFloat(0.0F);
		}
	}

	// Loads or meshes at most one region per call (once per frame)
	private static void update(int cameraWorldX, int cameraWorldY) {
		int reach = (GlobalConfig.VIEW_DISTANCE >> 7) + GlobalConfig.FAR_TERRAIN_TILES;
		int minRx = (cameraWorldX - reach) >> 6;
		int maxRx = (cameraWorldX + reach) >> 6;
		int minRy = (cameraWorldY - reach) >> 6;
		int maxRy = (cameraWorldY + reach) >> 6;
		// Forget regions well out of range
		for (Iterator<Region> it = regions.values().iterator(); it.hasNext(); ) {
			Region r = it.next();
			int rx = r.id >> 8, ry = r.id & 0xFF;
			if (rx < minRx - 2 || rx > maxRx + 2 || ry < minRy - 2 || ry > maxRy + 2) {
				it.remove();
			}
		}
		// Decode first (nearest first isn't worth sorting for ~25 regions)
		for (int rx = minRx - 1; rx <= maxRx + 1; rx++) {
			for (int ry = minRy - 1; ry <= maxRy + 1; ry++) {
				int id = rx << 8 | ry;
				Region r = regions.get(id);
				if (r == null) {
					r = new Region(id);
					regions.put(id, r);
				}
				if (!r.decoded) {
					decode(r);
					return;
				}
			}
		}
		// Then mesh (re-mesh when neighbours arrived since, for the seams)
		for (int rx = minRx; rx <= maxRx; rx++) {
			for (int ry = minRy; ry <= maxRy; ry++) {
				Region r = regions.get(rx << 8 | ry);
				if (r != null && !r.missing && (!r.meshed || !r.meshComplete && neighboursDecoded(r))) {
					mesh(r);
					return;
				}
			}
		}
	}

	private static boolean neighboursDecoded(Region r) {
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				Region n = regions.get(((r.id >> 8) + dx) << 8 | ((r.id & 0xFF) + dy));
				if (n == null || !n.decoded) {
					return false;
				}
			}
		}
		return true;
	}

	// SceneGraph's scene draw, after the camera transform, before the ground.
	// camX/camY are the camera's scene coordinates in fine units.
	public static void render(int camX, int camY) {
		if (!isEnabled()) {
			return;
		}
		int cameraWorldX = Camera.originX + (camX >> 7);
		int cameraWorldY = Camera.originY + (camY >> 7);
		update(cameraWorldX, cameraWorldY);

		GL2 gl = GlRenderer.gl;
		GlRenderer.setTextureId(-1);
		MaterialManager.setMaterial(0, 0);
		GlRenderer.setDepthLayer(DEPTH_LAYER);
		float near = GlobalConfig.VIEW_DISTANCE * 0.9F;
		float far = GlobalConfig.VIEW_DISTANCE + GlobalConfig.FAR_TERRAIN_TILES * 128;
		for (Region r : regions.values()) {
			if (!r.meshed || r.missing) {
				continue;
			}
			// Region base in scene coordinates (fine units)
			int sceneBaseX = (r.baseX - Camera.originX) << 7;
			int sceneBaseY = (r.baseY - Camera.originY) << 7;
			boolean bound = false;
			for (int zone = 0; zone < ZONES_PER_SIDE * ZONES_PER_SIDE; zone++) {
				if (!r.zoneFilled[zone]) {
					continue;
				}
				int zx = sceneBaseX + (zone / ZONES_PER_SIDE * ZONE_TILES << 7);
				int zy = sceneBaseY + (zone % ZONES_PER_SIDE * ZONE_TILES << 7);
				int span = ZONE_TILES << 7;
				// Nearest and farthest horizontal distance from the camera
				float nx = Math.max(Math.max(zx - camX, 0), camX - (zx + span));
				float ny = Math.max(Math.max(zy - camY, 0), camY - (zy + span));
				float nearest = (float) Math.sqrt(nx * nx + ny * ny);
				if (nearest > far) {
					continue;
				}
				boolean insideScene = zx >= 0 && zy >= 0 && zx + span <= 104 << 7 && zy + span <= 104 << 7;
				if (insideScene) {
					float fx = Math.max(Math.abs(zx - camX), Math.abs(zx + span - camX));
					float fy = Math.max(Math.abs(zy - camY), Math.abs(zy + span - camY));
					if ((float) Math.sqrt(fx * fx + fy * fy) < near) {
						// Entirely within what the real scene draws
						continue;
					}
				}
				if (!bound) {
					bind(gl, r, sceneBaseX, sceneBaseY);
					bound = true;
				}
				gl.glDrawArrays(GL2.GL_TRIANGLES, zone * VERTS_PER_ZONE, VERTS_PER_ZONE);
			}
			if (bound) {
				gl.glPopMatrix();
			}
		}
	}

	private static void bind(GL2 gl, Region r, int sceneBaseX, int sceneBaseY) {
		gl.glPushMatrix();
		gl.glTranslatef(sceneBaseX, 0.0F, sceneBaseY);
		if (r.vbo != null) {
			r.vbo.bindArray();
			gl.glVertexPointer(3, GL2.GL_FLOAT, STRIDE, 0L);
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, STRIDE, 12L);
			if (Preferences.highDetailLighting) {
				gl.glNormalPointer(GL2.GL_FLOAT, STRIDE, 16L);
			}
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, STRIDE, 28L);
		} else {
			if (GlRenderer.arbVboSupported) {
				gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, 0);
			}
			r.vertexData.position(0);
			gl.glVertexPointer(3, GL2.GL_FLOAT, STRIDE, r.vertexData);
			r.vertexData.position(12);
			gl.glColorPointer(4, GL2.GL_UNSIGNED_BYTE, STRIDE, r.vertexData);
			if (Preferences.highDetailLighting) {
				r.vertexData.position(16);
				gl.glNormalPointer(GL2.GL_FLOAT, STRIDE, r.vertexData);
			}
			r.vertexData.position(28);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, STRIDE, r.vertexData);
		}
	}
}
