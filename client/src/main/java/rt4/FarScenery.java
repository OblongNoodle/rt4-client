package rt4;

// Not part of the original client. The scenery half of FarTerrain: walls,
// trees, rocks, buildings and roofs on the far ground, so distant towns look
// like towns rather than floor plans.
//
// Loc files are encrypted; only keys the server sent or that Js5.checkKey has
// verified are used (XteaKeyStore), otherwise the region just has no far
// scenery. Level 0 only, decorations and ground clutter skipped, first
// animation frame, multilocs resolved as they currently are. Each model is
// built and lit like the real scene builds it (a GlModel with the loc's
// lighting, see model()), placed like SceneGraph.addLoc places it and merged,
// per texture and 8x8 zone, into a few meshes per region, so distant trees
// and buildings are textured and shaded like nearby ones.
final class FarScenery {

	// Loc shapes drawn far away: walls (0-3, 9), centrepieces (10, 11), roofs (12-21)
	private static boolean drawnShape(int shape) {
		return shape <= 3 || shape >= 9 && shape <= 21;
	}

	// Unlit raw models (LocType.getFarRawModel) and the lit models built from them
	private static final SoftLruHashTable rawModels = new SoftLruHashTable(512);
	private static final SoftLruHashTable models = new SoftLruHashTable(256);

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
		// Texture id (-1 = none) to its per-zone vertex data
		final java.util.HashMap<Integer, Buffer[]> textures = new java.util.HashMap<>();
		int vertices;
		int missingModels;
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
		GlModel model = model(type, shape, rotation);
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
		sink.build = build;
		sink.zone = (x >> 3) * 8 + (y >> 3);
		model.exportTriangles(sink, fineX, height, fineY);
	}

	// The model the real scene would draw (LocType.getStaticEntity in HD):
	// same raw model, rotation and lighting, cached per loc/shape/rotation
	private static GlModel model(LocType type, int shape, int rotation) {
		long key = (long) type.id << 8 | shape << 3 | rotation;
		GlModel model = (GlModel) models.get(key);
		if (model != null) {
			return model;
		}
		RawModel raw;
		if (shape == 11) {
			// Diagonal centrepiece: the straight one turned 45 degrees
			raw = type.getFarRawModel(rotation + 4, 10, rawModels);
			if (raw != null) {
				raw.rotate256();
			}
		} else {
			raw = type.getFarRawModel(rotation, shape, rawModels);
		}
		if (raw == null) {
			return null;
		}
		model = new GlModel(raw, type.getFarAmbient(), type.getFarContrast(), false);
		models.put(model, key);
		return model;
	}

	// Collects exported triangles into the current build's per-texture zones.
	// Far away, vertices snap to a grid (FAR_SCENERY_SIMPLIFY) and triangles
	// that collapse are dropped: leaves and other small detail thin out while
	// walls, roofs and tree shapes stay.
	private static final class Sink implements GlModel.TriangleSink {
		Build build;
		int zone;
		private int corner;
		private final float[] data = new float[3 * 10];
		private final int[] colours = new int[3];

		@Override
		public void vertex(int texture, float x, float y, float z, int rgba, float nx, float ny, float nz, float s, float t) {
			int grid = GlobalConfig.FAR_SCENERY_SIMPLIFY;
			if (grid > 1) {
				x = snap(x, grid);
				y = snap(y, grid);
				z = snap(z, grid);
			}
			int o = corner * 10;
			data[o] = x;
			data[o + 1] = y;
			data[o + 2] = z;
			data[o + 3] = nx;
			data[o + 4] = ny;
			data[o + 5] = nz;
			data[o + 6] = s;
			data[o + 7] = t;
			colours[corner] = rgba;
			if (++corner < 3) {
				return;
			}
			corner = 0;
			// Nearly invisible faces aren't worth sorting out far away
			if ((rgba & 0xFF) < 56) {
				return;
			}
			if (same(0, 10) || same(0, 20) || same(10, 20)) {
				return;
			}
			Buffer[] zones = build.textures.get(texture);
			if (zones == null) {
				zones = new Buffer[64];
				build.textures.put(texture, zones);
			}
			Buffer buf = zones[zone];
			if (buf == null || buf.offset + 3 * STRIDE > buf.data.length) {
				Buffer bigger = new Buffer(buf == null ? STRIDE * 768 : buf.data.length * 2);
				if (buf != null) {
					System.arraycopy(buf.data, 0, bigger.data, 0, buf.offset);
					bigger.offset = buf.offset;
				}
				buf = bigger;
				zones[zone] = buf;
			}
			for (int c = 0; c < 3; c++) {
				int i = c * 10;
				put(buf, data[i], data[i + 1], data[i + 2], colours[c], data[i + 3], data[i + 4], data[i + 5], data[i + 6], data[i + 7]);
			}
			build.vertices += 3;
		}

		private boolean same(int a, int b) {
			return data[a] == data[b] && data[a + 1] == data[b + 1] && data[a + 2] == data[b + 2];
		}

		private static float snap(float v, int grid) {
			return (float) Math.floor(v / grid + 0.5F) * grid;
		}
	}

	private static final Sink sink = new Sink();

	private static void put(Buffer buf, float x, float y, float z, int rgba, float nx, float ny, float nz, float s, float t) {
		if (GlRenderer.bigEndian) {
			buf.pFloat(x);
			buf.pFloat(y);
			buf.pFloat(z);
			buf.p4(rgba);
			buf.pFloat(nx);
			buf.pFloat(ny);
			buf.pFloat(nz);
			buf.pFloat(s);
			buf.pFloat(t);
		} else {
			buf.gFloat(x);
			buf.gFloat(y);
			buf.gFloat(z);
			buf.p4(rgba);
			buf.gFloat(nx);
			buf.gFloat(ny);
			buf.gFloat(nz);
			buf.gFloat(s);
			buf.gFloat(t);
		}
	}
}
