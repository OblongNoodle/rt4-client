package rt4;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

// Not part of the original client. Map (loc file) decryption keys, so
// AsyncSceneLoader can build the next area before the server asks for it.
// Keys the server has sent in rebuild packets are remembered across sessions.
// Otherwise 2009scape's public key list (bundled xteas.json), or no key.
// Whichever is used, Js5.checkKey first confirms it actually decrypts the
// file: a wrong key would decrypt to garbage, and the cache's decompressor has
// no length cap. (The server itself sends wrong keys for a few regions.)
public final class XteaKeyStore {

	private static final HashMap<Integer, int[]> keys = new HashMap<>();

	private static final HashMap<Integer, int[]> bundled = new HashMap<>();

	// Checked key per region (null = no key decrypts it)
	private static final HashMap<Integer, int[]> resolved = new HashMap<>();

	private static boolean loaded = false;

	private XteaKeyStore() {
	}

	private static File getFile() {
		String homeDir = SignLink.getHomeDir();
		return homeDir == null ? null : new File(homeDir, "xtea_keys.txt");
	}

	private static void load() {
		if (loaded) {
			return;
		}
		loaded = true;
		loadBundled();
		File file = getFile();
		if (file == null || !file.isFile()) {
			return;
		}
		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = in.readLine()) != null) {
				String[] parts = line.trim().split(",");
				if (parts.length != 5) {
					continue;
				}
				try {
					int region = Integer.parseInt(parts[0]);
					int[] key = new int[4];
					for (int i = 0; i < 4; i++) {
						key[i] = Integer.parseInt(parts[i + 1]);
					}
					keys.put(region, key);
				} catch (NumberFormatException ignored) {
				}
			}
		} catch (IOException ignored) {
		}
	}

	private static void loadBundled() {
		try (java.io.InputStream in = XteaKeyStore.class.getResourceAsStream("/xteas.json")) {
			if (in == null) {
				return;
			}
			com.google.gson.JsonObject root = new com.google.gson.JsonParser().parse(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
			for (com.google.gson.JsonElement element : root.getAsJsonArray("xteas")) {
				com.google.gson.JsonObject entry = element.getAsJsonObject();
				String[] parts = entry.get("keys").getAsString().split(",");
				if (parts.length != 4) {
					continue;
				}
				int[] key = new int[4];
				for (int i = 0; i < 4; i++) {
					key[i] = Integer.parseInt(parts[i].trim());
				}
				bundled.put(Integer.parseInt(entry.get("regionId").getAsString()), key);
			}
		} catch (Exception ignored) {
		}
	}

	// Region id is (regionX << 8) | regionY, as in LoginManager.regionBitPacked.
	// locGroup is the region's loc file in archive 5, used to check a bundled
	// key. Returns the key, null if it's unknown or wrong, or NOT_READY while
	// the loc file is still downloading.
	public static final int[] NOT_READY = new int[4];

	public static int[] get(int region, int locGroup) {
		load();
		if (resolved.containsKey(region)) {
			return resolved.get(region);
		}
		// Every candidate is checked before use, including keys the server
		// sent: 2009scape sends wrong keys for a few regions, and a wrong key
		// must never reach Js5.unpackGroup (see class comment)
		int[][] candidates = {keys.get(region), bundled.get(region), new int[4]};
		for (int[] candidate : candidates) {
			if (candidate == null) {
				continue;
			}
			Boolean valid = client.js5Archive5.checkKey(locGroup, candidate);
			if (valid == null) {
				return NOT_READY;
			}
			if (valid) {
				resolved.put(region, candidate);
				return candidate;
			}
		}
		resolved.put(region, null);
		return null;
	}


	// Called with every non-dynamic rebuild packet's regions and keys
	public static void record(int[] regions, int[][] regionKeys) {
		if (regions == null || regionKeys == null) {
			return;
		}
		load();
		StringBuilder added = new StringBuilder();
		for (int i = 0; i < regions.length && i < regionKeys.length; i++) {
			int[] key = regionKeys[i];
			if (key == null || key.length != 4) {
				continue;
			}
			int[] known = keys.get(regions[i]);
			if (known != null && known[0] == key[0] && known[1] == key[1] && known[2] == key[2] && known[3] == key[3]) {
				continue;
			}
			keys.put(regions[i], key.clone());
			resolved.remove(regions[i]);
			added.append(regions[i]).append(',').append(key[0]).append(',').append(key[1]).append(',').append(key[2]).append(',').append(key[3]).append('\n');
		}
		if (added.length() == 0) {
			return;
		}
		// Appended; a later line for the same region wins when loading
		File file = getFile();
		if (file == null) {
			return;
		}
		try (FileWriter out = new FileWriter(file, true)) {
			out.write(added.toString());
		} catch (IOException ignored) {
		}
	}
}
