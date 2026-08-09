package li.cil.oc.core.impl.server.fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import li.cil.oc.api.detail.FileSystemEnvironmentFactory;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.common.item.traits.FileSystemLike;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.computercraft.ComputerCraftFileSystem;
import li.cil.oc.core.impl.integration.computercraft.ComputerCraftWritableFileSystem;
import li.cil.oc.core.impl.util.FilePathUtil;
import li.cil.oc.core.impl.util.SafeThreadPool;
import li.cil.oc.core.impl.util.ThreadPoolFactory;
import li.cil.oc.core.server.fs.Buffered;
import li.cil.oc.core.server.fs.Capacity;
import li.cil.oc.core.server.fs.Volatile;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class FileSystem implements li.cil.oc.api.detail.FileSystemAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileSystem.class);
    public static final FileSystem INSTANCE = new FileSystem();
    private static FileSystemEnvironmentFactory environmentFactory;

    private FileSystem() {
    }

    public static void setEnvironmentFactory(FileSystemEnvironmentFactory factory) {
        environmentFactory = factory;
    }

    public static void removeAddress(ItemStack fsStack) {
        var item = fsStack.getItem();
        if (item instanceof FileSystemLike) {
            var customData = fsStack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            CompoundTag data;
            if (customData != null && !customData.isEmpty()) {
                data = customData.copyTag();
            } else {
                data = new CompoundTag();
                fsStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
            }
            if (!data.contains(OCSettings.namespace + "data")) {
                data.put(OCSettings.namespace + "data", new CompoundTag());
            }
            CompoundTag tagData = data.getCompound(OCSettings.namespace + "data");
            if (tagData.contains("node")) {
                CompoundTag nodeData = tagData.getCompound("node");
                if (nodeData.contains("address")) {
                    nodeData.remove("address");
                }
            }
        }
    }

    public li.cil.oc.api.fs.FileSystem fromMemory(long capacity) {
        return new RamFileSystem(capacity);
    }

    public li.cil.oc.api.fs.FileSystem fromComputerCraft(Object mount) {
        if (mount instanceof dan200.computercraft.api.filesystem.WritableMount writable) {
            return new ComputerCraftWritableFileSystem(writable);
        }
        if (mount instanceof dan200.computercraft.api.filesystem.Mount readOnly) {
            return new ComputerCraftFileSystem(readOnly);
        }
        return null;
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label,
                                                                         EnvironmentHost host, String accessSound, int speed) {
        if (fileSystem == null) return null;
        int clampedSpeed = Math.clamp(speed - 1, 0, 5);
        if (environmentFactory != null)
            return environmentFactory.create(fileSystem, label, host, accessSound, clampedSpeed);
        return null;
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label,
                                                                         EnvironmentHost host, String accessSound, int speed) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), host, accessSound, speed);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label,
                                                                         EnvironmentHost host, String sound) {
        return asManagedEnvironment(fileSystem, label, host, sound, 1);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label,
                                                                         EnvironmentHost host, String sound) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), host, sound, 1);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, Label label) {
        return asManagedEnvironment(fileSystem, label, null, null, 1);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem, String label) {
        return asManagedEnvironment(fileSystem, new ReadOnlyLabel(label), null, null, 1);
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment asManagedEnvironment(li.cil.oc.api.fs.FileSystem fileSystem) {
        return asManagedEnvironment(fileSystem, (Label) null, null, null, 1);
    }

    @Override
    public li.cil.oc.api.fs.FileSystem fromClass(Class<?> clazz, String domain, String root) {
        String innerPath = "assets/" + domain + "/" + root.trim().replaceAll("/$", "") + "/";

        String codeSource = null;
        try {
            codeSource = clazz.getProtectionDomain().getCodeSource() != null
                    ? clazz.getProtectionDomain().getCodeSource().getLocation().getPath() : null;
        } catch (Throwable ignored) {
        }

        File codeSourceFile = fileFromUrlString(codeSource);
        if (codeSourceFile != null) {
            li.cil.oc.api.fs.FileSystem fs = ZipFileInputStreamFileSystem.fromFile(codeSourceFile, innerPath);
            if (fs != null) return fs;
        }

        java.net.URL resourceUrl = clazz.getClassLoader().getResource(innerPath);
        if (resourceUrl != null) {
            if ("file".equals(resourceUrl.getProtocol())) {
                try {
                    File resourceDir = new File(resourceUrl.toURI());
                    if (resourceDir.isDirectory()) return new ReadOnlyFileSystem(resourceDir);
                } catch (URISyntaxException e) {
                    LOGGER.warn("fromClass: URISyntaxException for {}", resourceUrl, e);
                }
            } else {
                File jarFile = fileFromUrlString(resourceUrl.toString());
                if (jarFile != null) {
                    li.cil.oc.api.fs.FileSystem fs = ZipFileInputStreamFileSystem.fromFile(jarFile, innerPath);
                    if (fs != null) return fs;
                }
            }
        }

        String classPath = System.getProperty("java.class.path");
        String separator = File.pathSeparator;
        if (classPath != null) {
            for (String cp : classPath.split(separator)) {
                File fsp = new File(new File(cp), innerPath);
                if (fsp.exists() && fsp.isDirectory()) {
                    return new ReadOnlyFileSystem(fsp);
                }
            }
        }

        LOGGER.warn("fromClass: filesystem not found for innerPath={}", innerPath);
        return null;
    }

    private static File fileFromUrlString(String urlString) {
        if (urlString == null) return null;
        String s = urlString;
        int bang = s.indexOf('!');
        if (bang >= 0) {
            s = s.substring(0, bang);
        }
        while (s.startsWith("jar:") || s.startsWith("zip:")) {
            s = s.substring(4);
        }
        if (s.startsWith("union:")) {
            s = s.substring(6);
        }
        if (s.startsWith("file:")) {
            s = s.substring(5);
        }
        if (s.contains("%")) {
            try {
                s = java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8);
            } catch (IllegalArgumentException ignored) {
            }
        }
        int hash = s.indexOf('#');
        if (hash >= 0) {
            s = s.substring(0, hash);
        }
        if (s.isEmpty()) return null;
        File file = new File(s);
        return file.exists() && file.isFile() ? file : null;
    }

    @Override
    public li.cil.oc.api.fs.FileSystem fromSaveDirectory(String root, long capacity, boolean buffered) {
        var server = li.cil.oc.core.impl.util.SideTracker.getCurrentServer();
        File baseDir = server != null ?
                server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile() :
                new File(".");
        File path = new File(baseDir, OCSettings.savePath + root);
        if (path.exists() && !path.isDirectory()) {
            if (!path.delete()) {
                LOGGER.warn("Failed to delete path: {}", path);
            }
        }
        if (!path.mkdirs() && !path.isDirectory()) {
            LOGGER.warn("Failed to create save directory: {}", path);
        }
        if (path.exists() && path.isDirectory()) {
            if (buffered) return new BufferedFileSystem(path, capacity);
            else return new ReadWriteFileSystem(path, capacity);
        }
        return null;
    }

    @Override
    public li.cil.oc.api.fs.FileSystem asReadOnly(li.cil.oc.api.fs.FileSystem fileSystem) {
        if (fileSystem.isReadOnly()) return fileSystem;
        return new ReadOnlyWrapper(fileSystem);
    }

    public abstract static class ItemLabel implements Label {
    }

    public record ReadOnlyLabel(String label) implements Label {

        @Override
        public String getLabel() {
            return label;
        }

        @Override
        public void setLabel(String value) {
            throw new IllegalArgumentException("label is read only");
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            if (label != null) {
                nbt.putString(OCSettings.namespace + "fs.label", label);
            }
        }
    }

    private static class ReadOnlyFileSystem extends FileInputStreamFileSystem {
        private final File root;

        ReadOnlyFileSystem(File root) {
            this.root = root;
        }

        @Override
        protected File root() {
            return root;
        }

    }

    private static class ReadWriteFileSystem extends FileOutputStreamFileSystem implements Capacity {
        private final File root;
        private final long capacity;
        private long used;
        private boolean ignoreCapacity = false;

        ReadWriteFileSystem(File root, long capacity) {
            this.root = root;
            this.capacity = capacity;
            this.used = computeSize("/");
        }

        @Override
        public boolean exists(String path) {
            return new File(root(), FilePathUtil.validatePath(path)).exists();
        }

        @Override
        public long size(String path) {
            File file = new File(root(), FilePathUtil.validatePath(path));
            if (file.isFile()) return file.length();
            return 0L;
        }

        @Override
        public boolean isDirectory(String path) {
            return new File(root(), FilePathUtil.validatePath(path)).isDirectory();
        }

        @Override
        public long lastModified(String path) {
            return new File(root(), FilePathUtil.validatePath(path)).lastModified();
        }

        @Override
        public String[] list(String path) {
            File file = new File(root(), FilePathUtil.validatePath(path));
            if (file.exists()) {
                if (file.isFile()) return new String[]{file.getName()};
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    if (files != null) {
                        String[] result = new String[files.length];
                        for (int i = 0; i < files.length; i++) {
                            result[i] = files[i].isDirectory() ? files[i].getName() + "/" : files[i].getName();
                        }
                        return result;
                    }
                }
            }
            throw new RuntimeException(new FileNotFoundException("no such file or directory: " + path));
        }

        @Override
        protected InputChannel openInputChannel(String path) {
            return new li.cil.oc.core.impl.server.fs.FileInputStreamFileSystem.FileChannel(new File(root(), FilePathUtil.validatePath(path)));
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            try {
                ignoreCapacity = true;
                super.load(nbt, provider);
            } finally {
                ignoreCapacity = false;
            }
            used = computeSize("/");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putLong("capacity.used", used);
        }

        @Override
        protected File root() {
            return root;
        }

        @Override
        public long capacity() {
            return capacity;
        }

        @Override
        public long spaceTotal() {
            return capacity();
        }

        @Override
        public long spaceUsed() {
            return used;
        }

        @Override
        public boolean delete(String path) {
            long freed = OCSettings.get().fileCost + size(path);
            if (super.delete(path)) {
                used = Math.max(0, used - freed);
                return true;
            }
            return false;
        }

        @Override
        public boolean rename(String from, String to) {
            if (exists(to)) {
                long freed = OCSettings.get().fileCost + size(to);
                if (super.rename(from, to)) {
                    used = Math.max(0, used - freed);
                    return true;
                }
                return false;
            }
            return super.rename(from, to);
        }

        @Override
        public boolean makeDirectory(String path) {
            if (capacity() - used < OCSettings.get().fileCost && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            if (super.makeDirectory(path)) {
                used += OCSettings.get().fileCost;
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            super.close();
            used = computeSize("/");
        }

        @Override
        protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
            long delta;
            if (exists(path)) {
                delta = mode == Mode.Write ? -size(path) : 0;
            } else {
                delta = OCSettings.get().fileCost;
            }
            if (capacity() - used < delta && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            OutputHandle stream = super.openOutputHandle(id, path, mode);
            if (stream != null) {
                used = Math.max(0, used + delta);
                if (mode == Mode.Append) {
                    stream.seek(stream.length());
                }
                return new CountingOutputHandle(this, stream);
            }
            return null;
        }

        private long computeSize(String path) {
            long acc = OCSettings.get().fileCost + size(path);
            if (isDirectory(path)) {
                String[] children = list(path);
                for (String child : children) {
                    acc += computeSize(path + child);
                }
            }
            return acc;
        }

        private static class CountingOutputHandle extends OutputHandle {
            private final ReadWriteFileSystem owner;
            private final OutputHandle inner;

            CountingOutputHandle(ReadWriteFileSystem owner, OutputHandle inner) {
                super(inner.owner, inner.handle, inner.path);
                this.owner = owner;
                this.inner = inner;
            }

            @Override
            public boolean isClosed() {
                return inner.isClosed();
            }

            @Override
            public long length() {
                return inner.length();
            }

            @Override
            public long position() {
                return inner.position();
            }

            @Override
            public void close() {
                inner.close();
            }

            @Override
            public long seek(long to) {
                return inner.seek(to);
            }

            @Override
            public void write(byte[] b) {
                long oldLen = inner.length();
                long newLen = Math.max(oldLen, inner.position() + b.length);
                long delta = newLen - oldLen;
                if (owner.capacity() - owner.used < delta && !owner.ignoreCapacity)
                    throw new RuntimeException(new IOException("not enough space"));
                inner.write(b);
                owner.used += delta;
            }
        }
    }

    private static class RamFileSystem extends VirtualFileSystem implements Volatile, Capacity {
        private final long capacity;
        private long used;
        private boolean ignoreCapacity = false;

        RamFileSystem(long capacity) {
            this.capacity = capacity;
            this.used = computeSize("/");
        }

        @Override
        public long capacity() {
            return capacity;
        }

        @Override
        public long spaceTotal() {
            return capacity();
        }

        @Override
        public long spaceUsed() {
            return used;
        }

        @Override
        public boolean delete(String path) {
            long freed = OCSettings.get().fileCost + size(path);
            if (super.delete(path)) {
                used = Math.max(0, used - freed);
                return true;
            }
            return false;
        }

        @Override
        public boolean rename(String from, String to) {
            if (exists(to)) {
                long freed = OCSettings.get().fileCost + size(to);
                if (super.rename(from, to)) {
                    used = Math.max(0, used - freed);
                    return true;
                }
                return false;
            }
            return super.rename(from, to);
        }

        @Override
        public boolean makeDirectory(String path) {
            if (capacity() - used < OCSettings.get().fileCost && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            if (super.makeDirectory(path)) {
                used += OCSettings.get().fileCost;
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            root.children.clear();
            super.close();
            used = computeSize("/");
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            try {
                ignoreCapacity = true;
                super.load(nbt, provider);
            } finally {
                ignoreCapacity = false;
            }
            used = computeSize("/");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putLong("capacity.used", used);
        }

        @Override
        protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
            long delta;
            if (exists(path)) {
                delta = mode == Mode.Write ? -size(path) : 0;
            } else {
                delta = OCSettings.get().fileCost;
            }
            if (capacity() - used < delta && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            OutputHandle stream = super.openOutputHandle(id, path, mode);
            if (stream != null) {
                used = Math.max(0, used + delta);
                if (mode == Mode.Append) {
                    stream.seek(stream.length());
                }
                return new CountingOutputHandle(this, stream);
            }
            return null;
        }

        private long computeSize(String path) {
            long acc = OCSettings.get().fileCost + size(path);
            if (isDirectory(path)) {
                String[] children = list(path);
                if (children != null) {
                    for (String child : children) {
                        acc += computeSize(path + child);
                    }
                }
            }
            return acc;
        }

        private static class CountingOutputHandle extends OutputHandle {
            private final RamFileSystem owner;
            private final OutputHandle inner;

            CountingOutputHandle(RamFileSystem owner, OutputHandle inner) {
                super(inner.owner, inner.handle, inner.path);
                this.owner = owner;
                this.inner = inner;
            }

            @Override
            public boolean isClosed() {
                return inner.isClosed();
            }

            @Override
            public long length() {
                return inner.length();
            }

            @Override
            public long position() {
                return inner.position();
            }

            @Override
            public void close() {
                inner.close();
            }

            @Override
            public long seek(long to) {
                return inner.seek(to);
            }

            @Override
            public void write(byte[] b) {
                long oldLen = inner.length();
                long newLen = Math.max(oldLen, inner.position() + b.length);
                long delta = newLen - oldLen;
                if (owner.capacity() - owner.used < delta && !owner.ignoreCapacity)
                    throw new RuntimeException(new IOException("not enough space"));
                inner.write(b);
                owner.used += delta;
            }
        }
    }

    private static class BufferedFileSystem extends VirtualFileSystem implements Buffered, Capacity {
        public static final SafeThreadPool fileSaveHandler = ThreadPoolFactory.createSafePool("FileSystem", 1);

        private final File fileRoot;
        private final long capacity;
        private final Map<String, Long> deletions = new HashMap<>();
        private long used;
        private boolean ignoreCapacity = false;
        private Future<?> saving = null;

        BufferedFileSystem(File fileRoot, long capacity) {
            this.fileRoot = fileRoot;
            this.capacity = capacity;
            this.used = computeSize("/");
        }

        @Override
        public File fileRoot() {
            return fileRoot;
        }

        @Override
        public long capacity() {
            return capacity;
        }

        @Override
        public long spaceTotal() {
            return capacity();
        }

        @Override
        public long spaceUsed() {
            return used;
        }

        @Override
        public boolean delete(String path) {
            long freed = OCSettings.get().fileCost + size(path);
            if (super.delete(path)) {
                used = Math.max(0, used - freed);
                deletions.put(path, System.currentTimeMillis());
                return true;
            }
            return false;
        }

        @Override
        public boolean rename(String from, String to) {
            long freed = 0;
            boolean hadTarget = false;
            if (exists(to)) {
                hadTarget = true;
                freed = OCSettings.get().fileCost + size(to);
            }
            if (super.rename(from, to)) {
                if (hadTarget) {
                    used = Math.max(0, used - freed);
                }
                deletions.put(from, System.currentTimeMillis());
                return true;
            }
            return false;
        }

        @Override
        public boolean makeDirectory(String path) {
            if (capacity() - used < OCSettings.get().fileCost && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            if (super.makeDirectory(path)) {
                used += OCSettings.get().fileCost;
                return true;
            }
            return false;
        }

        @Override
        public void close() {
            super.close();
            used = computeSize("/");
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            if (saving != null) {
                try {
                    saving.get(120L, TimeUnit.SECONDS);
                } catch (TimeoutException e) {
                    LOGGER.warn("Waiting for filesystem to save took two minutes! Aborting.");
                } catch (CancellationException | InterruptedException | ExecutionException ignored) {
                }
            }
            try {
                ignoreCapacity = true;
                loadFiles(nbt);
                super.load(nbt, provider);
            } finally {
                ignoreCapacity = false;
            }
            used = computeSize("/");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putLong("capacity.used", used);
            var f = fileSaveHandler.withPool(pool -> pool.submit(this::saveFiles));
            if (f != null) saving = f;
        }

        private synchronized void loadFiles(CompoundTag ignoredNbt) {
            recurseLoad("", fileRoot());
        }

        private void recurseLoad(String path, File directory) {
            makeDirectory(path);
            File[] children = directory.listFiles();
            if (children != null) {
                for (File child : children) {
                    if (FilePathUtil.isValidFilename(child.getName())) {
                        String childPath = path + child.getName();
                        if (child.exists() && child.isDirectory() && child.list() != null) {
                            recurseLoad(childPath + "/", child);
                        } else if (!exists(childPath) || !isDirectory(childPath)) {
                            OutputHandle stream = openOutputHandle(0, childPath, Mode.Write);
                            if (stream != null) {
                                try {
                                    FileInputStream in = new FileInputStream(child);
                                    byte[] buffer = new byte[8 * 1024];
                                    int read;
                                    while ((read = in.read(buffer)) >= 0) {
                                        if (read > 0) {
                                            if (read == buffer.length) stream.write(buffer);
                                            else {
                                                byte[] trimmed = new byte[read];
                                                System.arraycopy(buffer, 0, trimmed, 0, read);
                                                stream.write(trimmed);
                                            }
                                        }
                                    }
                                    in.close();
                                } catch (IOException ignored) {
                                }
                                stream.close();
                                setLastModified(childPath, child.lastModified());
                            }
                        }
                    }
                }
            }
            setLastModified(path, directory.lastModified());
            String[] rootListing = fileRoot().list();
            if (rootListing == null || rootListing.length == 0) {
                if (!fileRoot().delete()) {
                    LOGGER.warn("Failed to delete file root: {}", fileRoot());
                }
            }
        }

        @Override
        protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
            long delta;
            if (exists(path)) {
                delta = mode == Mode.Write ? -size(path) : 0;
            } else {
                delta = OCSettings.get().fileCost;
            }
            if (capacity() - used < delta && !ignoreCapacity) {
                throw new RuntimeException(new IOException("not enough space"));
            }
            OutputHandle stream = super.openOutputHandle(id, path, mode);
            if (stream != null) {
                used = Math.max(0, used + delta);
                if (mode == Mode.Append) {
                    stream.seek(stream.length());
                }
                return new CountingOutputHandle(this, stream);
            }
            return null;
        }

        public synchronized void saveFiles() {
            for (Map.Entry<String, Long> entry : deletions.entrySet()) {
                File file = new File(fileRoot(), entry.getKey());
                if (FileUtils.isFileOlder(file, entry.getValue()))
                    FileUtils.deleteQuietly(file);
            }
            deletions.clear();
            recurseSave("", fileRoot());
        }

        private void recurseSave(String path, File directory) {
            if (!directory.exists()) {
                if (!directory.mkdirs() && !directory.isDirectory()) {
                    LOGGER.warn("Failed to create directory during save: {}", directory);
                }
            }
            String[] children = list(path);
            if (children != null) {
                for (String child : children) {
                    String childPath = path + child;
                    String childName = child.endsWith("/") ? child.substring(0, child.length() - 1) : child;
                    File childFile = new File(directory, childName);
                    if (isDirectory(childPath)) {
                        recurseSave(childPath + "/", childFile);
                    } else {
                        try {
                            long len = size(childPath);
                            byte[] data = new byte[(int) len];
                            int handleId = open(childPath, Mode.Read);
                            if (handleId >= 0) {
                                li.cil.oc.api.fs.Handle h = getHandle(handleId);
                                h.read(data);
                                h.close();
                            }
                            try (FileOutputStream fos = new FileOutputStream(childFile)) {
                                fos.write(data);
                            }
                            if (!childFile.setLastModified(lastModified(childPath))) {
                                LOGGER.warn("Failed to set last modified on: {}", childFile);
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        @Override
        protected List<String> segments(String path) {
            List<String> parts = super.segments(path);
            if (FilePathUtil.isCaseInsensitive()) return toCaseInsensitive(parts);
            return parts;
        }

        private List<String> toCaseInsensitive(List<String> path) {
            VirtualDirectory node = root;
            List<String> result = new java.util.ArrayList<>();
            for (String segment : path) {
                assert node != null : "corrupted virtual file system";
                boolean found = false;
                for (Map.Entry<String, VirtualObject> entry : node.children.entrySet()) {
                    if (entry.getKey().equalsIgnoreCase(segment)) {
                        result.add(entry.getKey());
                        if (entry.getValue() instanceof VirtualDirectory) {
                            node = (VirtualDirectory) entry.getValue();
                        } else {
                            node = null;
                        }
                        found = true;
                        break;
                    }
                }
                if (!found) result.add(segment);
            }
            return result;
        }

        private long computeSize(String path) {
            long acc = OCSettings.get().fileCost + size(path);
            if (isDirectory(path)) {
                String[] children = list(path);
                if (children != null) {
                    for (String child : children) {
                        acc += computeSize(path + child);
                    }
                }
            }
            return acc;
        }

        private static class CountingOutputHandle extends OutputHandle {
            private final BufferedFileSystem owner;
            private final OutputHandle inner;

            CountingOutputHandle(BufferedFileSystem owner, OutputHandle inner) {
                super(inner.owner, inner.handle, inner.path);
                this.owner = owner;
                this.inner = inner;
            }

            @Override
            public boolean isClosed() {
                return inner.isClosed();
            }

            @Override
            public long length() {
                return inner.length();
            }

            @Override
            public long position() {
                return inner.position();
            }

            @Override
            public void close() {
                inner.close();
            }

            @Override
            public long seek(long to) {
                return inner.seek(to);
            }

            @Override
            public void write(byte[] b) {
                long oldLen = inner.length();
                long newLen = Math.max(oldLen, inner.position() + b.length);
                long delta = newLen - oldLen;
                if (owner.capacity() - owner.used < delta && !owner.ignoreCapacity)
                    throw new RuntimeException(new IOException("not enough space"));
                inner.write(b);
                owner.used += delta;
            }
        }
    }
}
