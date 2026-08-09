package li.cil.oc.core.impl.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StateSaveManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(StateSaveManager.class);
    public static final SafeThreadPool stateSaveHandler = ThreadPoolFactory.createSafePool("SaveHandler", 1);
    public static final ConcurrentLinkedDeque<File> chunkDirs = new ConcurrentLinkedDeque<>();
    static final Map<String, Future<?>> saving = new HashMap<>();
    private static final String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
    private static final long TimeToHoldOntoOldSaves = 60 * 1000;

    private static File saveRoot = null;

    private StateSaveManager() {
    }

    public static void setSaveRoot(File root) {
        saveRoot = root;
    }

    public static File savePath() {
        return new File(saveRoot, OCSettings.savePath);
    }

    public static File statePath() {
        return new File(savePath(), "state");
    }

    public static void scheduleSave(MachineHost host, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPosition.apply(host), nbt, name, data);
    }

    public static void scheduleSave(EnvironmentHost host, CompoundTag nbt, String name, byte[] data) {
        scheduleSave(BlockPosition.apply(host), nbt, name, data);
    }

    public static void scheduleSave(BlockPosition position, CompoundTag nbt, String name, byte[] data) {
        Level world = position.level();
        if (world instanceof ServerLevel) {
            String dimension = world.dimension().location().toString().replace(':', '_');
            ChunkPos chunk = new ChunkPos(position.x() >> 4, position.z() >> 4);
            nbt.putString("dimension", dimension);
            nbt.putInt("chunkX", chunk.x);
            nbt.putInt("chunkZ", chunk.z);
            scheduleSave(dimension, chunk, name, data);
        }
    }

    public static CompoundTag loadNBT(CompoundTag nbt, String name) {
        byte[] data = load(nbt, name);
        if (data.length > 0) {
            try {
                ByteArrayInputStream bais = new ByteArrayInputStream(data);
                DataInputStream dis = new DataInputStream(bais);
                return NbtIo.read(dis);
            } catch (Throwable t) {
                LOGGER.warn("Error restoring block state from external data.", t);
                return new CompoundTag();
            }
        }
        return new CompoundTag();
    }

    public static byte[] load(CompoundTag nbt, String name) {
        String dimension = nbt.getString("dimension");
        ChunkPos chunk = new ChunkPos(nbt.getInt("chunkX"), nbt.getInt("chunkZ"));
        Future<?> f = saving.get(name);
        if (f != null) {
            try {
                f.get(120L, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                LOGGER.warn("Waiting for state data to save took two minutes!");
            } catch (Exception ignored) {
            }
        }
        saving.remove(name);
        return load(dimension, chunk, name);
    }

    public static void scheduleSave(String dimension, ChunkPos chunk, String name, byte[] data) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");
        Future<?> f = stateSaveHandler.withPool(pool -> pool.submit(new SaveDataEntry(data, chunk, name, dimension)));
        if (f != null) saving.put(name, f);
    }

    public static byte[] load(String dimension, ChunkPos chunk, String name) {
        if (chunk == null) throw new IllegalArgumentException("chunk is null");
        File path = statePath();
        File dimPath = new File(path, dimension);
        File chunkPath = new File(dimPath, chunk.x + "." + chunk.z);
        File file;
        try {
            file = new File(chunkPath, sanitizeName(name)).getCanonicalFile();
            if (!file.toPath().startsWith(statePath().getCanonicalFile().toPath())) {
                throw new IOException("save path escapes state directory: " + name);
            }
        } catch (IOException e) {
            LOGGER.warn("Invalid save name '{}': {}", name, e.getMessage());
            return new byte[0];
        }
        if (!file.exists()) return new byte[0];
        try {
            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8 * 1024];
            int read;
            while ((read = bis.read(buffer)) >= 0) {
                bos.write(buffer, 0, read);
            }
            bis.close();
            return bos.toByteArray();
        } catch (IOException e) {
            LOGGER.warn("Error loading auxiliary block entity data.", e);
            return new byte[0];
        }
    }

    public static void cleanSaveData() {
        File[] emptyDirs = savePath().listFiles(file -> {
            if (!(file.isDirectory() &&
                    file.getName().matches(uuidRegex) &&
                    System.currentTimeMillis() - file.lastModified() > TimeToHoldOntoOldSaves)) {
                return false;
            }
            String[] listing = file.list();
            return listing == null || listing.length == 0;
        });
        if (emptyDirs != null) {
            for (File dir : emptyDirs) {
                if (dir != null && !dir.delete()) {
                    LOGGER.warn("Failed to delete empty save directory: {}", dir);
                }
            }
        }
    }

    public static void touchStateFiles() {
        try {
            Files.walkFileTree(statePath().toPath(), new SimpleFileVisitor<>() {
                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    if (!file.toFile().setLastModified(System.currentTimeMillis())) {
                        LOGGER.warn("Failed to set last modified time on file (walk): {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFileFailed(@NotNull Path file, @NotNull IOException exc) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult postVisitDirectory(@NotNull Path dir, IOException exc) {
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            recurse(statePath());
        }
    }

    private static void recurse(File file) {
        if (!file.setLastModified(System.currentTimeMillis())) {
            LOGGER.warn("Failed to set last modified time on file (recurse): {}", file);
        }
        if (file.exists() && file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    recurse(f);
                }
            }
        }
    }

    private static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) throw new IllegalArgumentException("empty name");
        if (name.contains("/") || name.contains("\\") || name.contains("..") || name.contains("\0")) {
            throw new IllegalArgumentException("invalid save name: " + name);
        }
        return name;
    }

    public record SaveDataEntry(byte[] data, ChunkPos pos, String name, String dimension) implements Runnable {
        @Override
        public void run() {
            File path = statePath();
            File dimPath = new File(path, dimension);
            File chunkPath = new File(dimPath, pos.x + "." + pos.z);
            chunkDirs.add(chunkPath);
            if (!chunkPath.exists()) {
                if (!chunkPath.mkdirs()) {
                    LOGGER.warn("Failed to create chunk directory: {}", chunkPath);
                }
            }
            File file;
            try {
                file = new File(chunkPath, sanitizeName(name)).getCanonicalFile();
                if (!file.toPath().startsWith(statePath().getCanonicalFile().toPath())) {
                    throw new IOException("save path escapes state directory: " + name);
                }
            } catch (IOException e) {
                LOGGER.warn("Invalid save name '{}': {}", name, e.getMessage());
                return;
            }
            try {
                BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream(file));
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                LOGGER.warn("Error saving auxiliary block entity data to '{}'.", file.getAbsolutePath(), e);
            }
        }
    }
}
