package rt4;

// Not part of the original client. The scenery half of FarTerrain: walls,
// trees, rocks, buildings and roofs on the far ground, so distant towns look
// like towns rather than floor plans.
//
// Loc files are encrypted; only keys the server sent or that Js5.checkKey has
// verified are used (XteaKeyStore), otherwise the region just has no far
// scenery. Level 0 only, decorations and ground clutter skipped, first
// animation frame, multilocs resolved as they currently are. Each model is
// placed like SceneGraph.addLoc places it and merged, per 8x8 zone, into one
// untextured, flat-shaded mesh per region (textured faces use the texture's
// average colour), lit by GL like the rest of the far terrain.
final class FarScenery {

	// Loc shapes drawn far away: walls (0-3, 9), centrepieces (10, 11), roofs (12-21)
	private static boolean drawnShape(int shape) {
		return shape <= 3 || shape >= 9 && shape <= 21;
	}

	private static final SoftLruHashTable models = new SoftLruHashTable(512);

	static final int STRIDE = 36;

	// Growing per-zone vertex data while a region's scenery is built
	static final class Build {
		int[] locIds;
		int[] locX;
		int[] locY;
		int[] locShape;
		int[] locRotation;
		int count;
		int next;
		final Buffer[] zones = new Buffer[64];
		int vertices;
		int missingModels;
		// Textured faces whose texture wasn't downloaded yet (drawn in their
		// plain colour, which for textured faces is usually white)
		int missingTextures;
	}

	private FarScenery() {
	}

	private static int locGroup(int regionId) {
		return client.js5Archive5.getGroupId(JagString.concatenate(new JagString[]{LoginManager.LOC_PREFIX, JagString.parseInt(regionId >> 8), LoginManager.UNDERSCORE, JagString.parseInt(regionId & 0xFF)}));
	}

	// Returns null while waiting (download or key check), an empty build when
	// the region has no usable scenery
	static Build start(int regionId) {
		Build build = new Build();
		int group = locGroup(regionId);
		if (group == -1) {
			return build;
		}
		int[] key = XteaKeyStore.get(regionId, group);
		if (key == XteaKeyStore.NOT_READY) {
			return null;
		}
		if (key == null) {
			// Key unknown or wrong: no far scenery for this region
			return build;
		}
		byte[] data = client.js5Archive5.fetchFile(group, key, 0);
		if (data == null) {
			return null;
		}
		// As SceneGraph.readLocs
		int capacity = 256;
		build.locIds = new int[capacity];
		build.locX = new int[capacity];
		build.locY = new int[capacity];
		build.locShape = new int[capacity];
		build.locRotation = new int[capacity];
		Buffer buffer = new Buffer(data);
		int locId = -1;
		while (true) {
			int idDelta = buffer.gVarSmart();
			if (idDelta == 0) {
				break;
			}
			locId += idDelta;
			int position = 0;
			while (true) {
				int posDelta = buffer.gsmarts();
				if (posDelta == 0) {
					break;
				}
				position += posDelta - 1;
				int y = position & 0x3F;
				int x = position >> 6 & 0x3F;
				int level = position >> 12;
				int flags = buffer.g1();
				int shape = flags >> 2;
				int rotation = flags & 0x3;
				if (level != 0 || !drawnShape(shape)) {
					continue;
				}
				if (build.count == capacity) {
					capacity *= 2;
					build.locIds = java.util.Arrays.copyOf(build.locIds, capacity);
					build.locX = java.util.Arrays.copyOf(build.locX, capacity);
					build.locY = java.util.Arrays.copyOf(build.locY, capacity);
					build.locShape = java.util.Arrays.copyOf(build.locShape, capacity);
					build.locRotation = java.util.Arrays.copyOf(build.locRotation, capacity);
				}
				build.locIds[build.count] = locId;
				build.locX[build.count] = x;
				build.locY[build.count] = y;
				build.locShape[build.count] = shape;
				build.locRotation[build.count] = rotation;
				build.count++;
			}
		}
		return build;
	}

	interface Heights {
		// World tile corner height, like FarTerrain.heightAt
		int at(int worldX, int worldY);
	}

	// Adds locs until the deadline; true once all are added
	static boolean step(Build build, int baseX, int baseY, Heights heights, long deadline) {
		while (build.next < build.count) {
			if (System.nanoTime() >= deadline) {
				return false;
			}
			int i = build.next++;
			addLoc(build, baseX, baseY, heights, build.locIds[i], build.locX[i], build.locY[i], build.locShape[i], build.locRotation[i]);
		}
		return true;
	}

	private static void addLoc(Build build, int baseX, int baseY, Heights heights, int id, int x, int y, int shape, int rotation) {
		LocType type = LocTypeList.get(id);
		if (type.multiLocs != null) {
			type = type.getMultiLoc();
			if (type == null) {
				return;
			}
		}
		RawModel model = type.getFarRawModel(rotation, shape, models);
		if (model == null) {
			build.missingModels++;
			return;
		}
		if (shape == 10 || shape == 11) {
			// Small clutter (plants, small rocks, ...) isn't worth its
			// triangles at this distance; heights are negative going up
			int top = 0;
			for (int v = 0; v < model.vertexCount; v++) {
				if (model.vertexY[v] < top) {
					top = model.vertexY[v];
				}
			}
			if (-top < GlobalConfig.FAR_SCENERY_MIN_HEIGHT) {
				return;
			}
		}
		int width = type.width;
		int length = type.length;
		if (rotation == 1 || rotation == 3) {
			width = type.length;
			length = type.width;
		}
		// Position as SceneGraph.addLoc: centrepieces sit centred on their
		// footprint, everything else on the tile centre; height is the
		// footprint's average corner height
		int fineX;
		int fineY;
		int height;
		int wx = baseX + x;
		int wy = baseY + y;
		if (shape == 10 || shape == 11) {
			fineX = (x << 7) + (width << 6);
			fineY = (y << 7) + (length << 6);
			height = heights.at(wx, wy) + heights.at(wx + width, wy) + heights.at(wx, wy + length) + heights.at(wx + width, wy + length) >> 2;
		} else {
			fineX = (x << 7) + 64;
			fineY = (y << 7) + 64;
			height = heights.at(wx, wy) + heights.at(wx + 1, wy) + heights.at(wx, wy + 1) + heights.at(wx + 1, wy + 1) >> 2;
		}
		int zone = (x >> 3) * 8 + (y >> 3);
		Buffer buf = build.zones[zone];
		for (int f = 0; f < model.triangleCount; f++) {
			if (model.triangleInfo != null && model.triangleInfo[f] == 2) {
				// Hidden
				continue;
			}
			if (model.triangleAlpha != null && (model.triangleAlpha[f] & 0xFF) >= 200) {
				continue;
			}
			int a = model.triangleVertexA[f];
			int b = model.triangleVertexB[f];
			int c = model.triangleVertexC[f];
			float ax = model.vertexX[a] + fineX, ay = model.vertexY[a] + height, az = model.vertexZ[a] + fineY;
			float bx = model.vertexX[b] + fineX, by = model.vertexY[b] + height, bz = model.vertexZ[b] + fineY;
			float cx = model.vertexX[c] + fineX, cy = model.vertexY[c] + height, cz = model.vertexZ[c] + fineY;
			// Flat face normal
			float ux = bx - ax, uy = by - ay, uz = bz - az;
			float vx = cx - ax, vy = cy - ay, vz = cz - az;
			float nx = uy * vz - uz * vy;
			float ny = uz * vx - ux * vz;
			float nz = ux * vy - uy * vx;
			float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (len == 0.0F) {
				continue;
			}
			nx /= len;
			ny /= len;
			nz /= len;
			int texture = model.triangleTextures == null ? -1 : model.triangleTextures[f];
			int rgb;
			int average = texture == -1 ? -1 : FarTerrain.textureAverage(texture);
			if (texture != -1 && average == -1) {
				build.missingTextures++;
			}
			int hsl = model.triangleColors[f] & 0xFFFF;
			if (average != -1) {
				// Most HD model textures are greyscale detail maps: the face
				// colour (through getTexturedColor, which also handles the few
				// coloured "brightness" textures) is multiplied by the texture
				rgb = FarTerrain.multiply(SceneGraph.getTexturedColor(texture, hsl, GlobalConfig.FAR_SCENERY_LIGHTNESS), average);
			} else {
				// Untextured, or the texture isn't loaded yet
				rgb = Rasteriser.palette[ColorUtils.multiplyLightness2(hsl, GlobalConfig.FAR_SCENERY_LIGHTNESS)];
			}
			if (buf == null || buf.offset + 3 * STRIDE > buf.data.length) {
				Buffer bigger = new Buffer(buf == null ? 3 * STRIDE * 256 : buf.data.length * 2);
				if (buf != null) {
					System.arraycopy(buf.data, 0, bigger.data, 0, buf.offset);
					bigger.offset = buf.offset;
				}
				buf = bigger;
				build.zones[zone] = buf;
			}
			put(buf, ax, ay, az, rgb, nx, ny, nz);
			put(buf, bx, by, bz, rgb, nx, ny, nz);
			put(buf, cx, cy, cz, rgb, nx, ny, nz);
			build.vertices += 3;
		}
	}

	private static void put(Buffer buf, float x, float y, float z, int rgb, float nx, float ny, float nz) {
		if (GlRenderer.bigEndian) {
			buf.pFloat(x);
			buf.pFloat(y);
			buf.pFloat(z);
			buf.p4(rgb << 8 | 0xFF);
			buf.pFloat(nx);
			buf.pFloat(ny);
			buf.pFloat(nz);
			buf.pFloat(0.0F);
			buf.pFloat(0.0F);
		} else {
			buf.gFloat(x);
			buf.gFloat(y);
			buf.gFloat(z);
			buf.p4(rgb << 8 | 0xFF);
			buf.gFloat(nx);
			buf.gFloat(ny);
			buf.gFloat(nz);
			buf.gFloat(0.0F);
			buf.gFloat(0.0F);
		}
	}
}
