package rt4;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.CRC32;

// Not part of the original client. Caches generated HD texture pixels on
// disk, since generating procedural textures above the original 128px is
// slow enough to hitch the render thread whenever a new area loads. Each
// texture is generated once, then read back from disk in later sessions.
public final class GlTextureDiskCache {

	// Bump whenever texture generation output changes, so stale files are
	// never read back.
	private static final int VERSION = 1;

	private static File directory;

	private static boolean disabled = false;

	private static final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
		Thread thread = new Thread(runnable, "texture-disk-cache");
		thread.setDaemon(true);
		return thread;
	});

	private GlTextureDiskCache() {
	}

	public static String createKey(byte[] definition, boolean mirrored) {
		CRC32 crc = new CRC32();
		crc.update(definition);
		return VERSION + "_" + Long.toHexString(crc.getValue()) + "_" + definition.length + (mirrored ? "_m" : "");
	}

	private static File getDirectory() {
		if (directory == null && !disabled) {
			String homeDir = SignLink.getHomeDir();
			if (homeDir == null) {
				disabled = true;
				return null;
			}
			File dir = new File(homeDir, "texture_cache");
			if (!dir.isDirectory() && !dir.mkdirs()) {
				disabled = true;
				return null;
			}
			directory = dir;
		}
		return directory;
	}

	public static byte[] read(String key, int size) {
		File dir = getDirectory();
		if (dir == null) {
			return null;
		}
		File file = new File(dir, key + "_" + size + ".bin");
		int length = size * size * 4;
		if (file.length() != length) {
			return null;
		}
		try (RandomAccessFile in = new RandomAccessFile(file, "r")) {
			byte[] bytes = new byte[length];
			in.readFully(bytes);
			return bytes;
		} catch (IOException ex) {
			return null;
		}
	}

	public static void write(String key, int size, byte[] bytes) {
		File dir = getDirectory();
		if (dir == null) {
			return;
		}
		writer.execute(() -> {
			File file = new File(dir, key + "_" + size + ".bin");
			File temp = new File(dir, key + "_" + size + ".tmp");
			try (FileOutputStream out = new FileOutputStream(temp)) {
				out.write(bytes);
			} catch (IOException ex) {
				temp.delete();
				return;
			}
			// Write-then-rename so a crash never leaves a partial file behind
			if (!temp.renameTo(file)) {
				temp.delete();
			}
		});
	}
}
