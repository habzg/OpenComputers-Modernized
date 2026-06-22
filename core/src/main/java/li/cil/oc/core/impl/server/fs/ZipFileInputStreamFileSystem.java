package li.cil.oc.core.impl.server.fs;

import com.google.common.cache.CacheBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileInputStreamFileSystem extends InputStreamFileSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(ZipFileInputStreamFileSystem.class);
    private static final com.google.common.cache.Cache<String, ArchiveDirectory> cache =
            CacheBuilder.newBuilder().weakValues().build();
    private final ArchiveDirectory archive;
    private volatile Long spaceUsed_ = null;

    public ZipFileInputStreamFileSystem(ArchiveDirectory archive) {
        this.archive = archive;
    }

    public static ZipFileInputStreamFileSystem fromFile(File file, String innerPath) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            try {
                ArchiveDirectory archive = cache.get(file.getPath() + ":" + innerPath, () -> {
                    try (ZipFile zip = new ZipFile(file.getPath())) {
                        String cleanedPath = innerPath.replaceAll("^/+", "").replaceAll("/+$", "") + "/";
                        ZipEntry rootEntry = zip.getEntry(cleanedPath);
                        if (rootEntry == null || !rootEntry.isDirectory()) {
                            throw new IllegalArgumentException("Root path " + innerPath + " doesn't exist or is not a directory in ZIP file " + file.getName() + ".");
                        }
                        Set<ArchiveDirectory> directories = new HashSet<>();
                        Set<ArchiveFile> files = new HashSet<>();
                        java.util.Enumeration<? extends ZipEntry> iterator = zip.entries();
                        while (iterator.hasMoreElements()) {
                            ZipEntry entry = iterator.nextElement();
                            if (entry.getName().startsWith(cleanedPath)) {
                                if (entry.isDirectory()) directories.add(new ArchiveDirectory(entry, cleanedPath));
                                else files.add(new ArchiveFile(zip, entry, cleanedPath));
                            }
                        }
                        ArchiveDirectory root = null;
                        for (ArchiveDirectory entry : directories) {
                            if (!entry.path.isEmpty()) {
                                String parent = entry.path.substring(0, Math.max(entry.path.lastIndexOf('/'), 0));
                                for (ArchiveDirectory d : directories) {
                                    if (d.path.equals(parent)) {
                                        d.children.add(entry);
                                        break;
                                    }
                                }
                            } else {
                                root = entry;
                            }
                        }
                        for (ArchiveFile entry : files) {
                            if (!entry.path.isEmpty()) {
                                String parent = entry.path.substring(0, Math.max(entry.path.lastIndexOf('/'), 0));
                                for (ArchiveDirectory d : directories) {
                                    if (d.path.equals(parent)) {
                                        d.children.add(entry);
                                        break;
                                    }
                                }
                            }
                        }
                        if (root == null) throw new IllegalStateException("No root directory found");
                        return root;
                    }
                });
                return new ZipFileInputStreamFileSystem(archive);
            } catch (Throwable e) {
                LOGGER.warn("Failed creating ZIP file system.", e);
            }
            return null;
        }
    }

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    @Override
    public long spaceUsed() {
        if (spaceUsed_ == null) {
            synchronized (this) {
                if (spaceUsed_ == null) {
                    spaceUsed_ = recurseSize(archive);
                }
            }
        }
        return spaceUsed_;
    }

    private long recurseSize(ArchiveDirectory d) {
        long acc = 0;
        for (Archive c : d.children) {
            if (c instanceof ArchiveDirectory dir) acc += recurseSize(dir);
            else if (c instanceof ArchiveFile file) acc += file.size;
        }
        return acc;
    }

    @Override
    public boolean exists(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            return entry(path) != null;
        }
    }

    @Override
    public long size(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Archive a = entry(path);
            if (a != null && !a.isDirectory) return a.size();
            return 0L;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Archive a = entry(path);
            return a != null && a.isDirectory;
        }
    }

    @Override
    public long lastModified(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Archive a = entry(path);
            return a != null ? a.lastModified : 0L;
        }
    }

    @Override
    public String[] list(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Archive a = entry(path);
            if (a != null && a.isDirectory) return a.list();
            return null;
        }
    }

    @Override
    protected InputChannel openInputChannel(String path) {
        synchronized (ZipFileInputStreamFileSystem.class) {
            Archive a = entry(path);
            if (a != null) {
                InputStream stream = a.openStream();
                if (stream != null) return new InputStreamChannel(stream);
            }
            return null;
        }
    }

    private Archive entry(String path) {
        String cleanPath = "/" + path.replace("\\", "/").replace("//", "/").replaceAll("^/+", "").replaceAll("/+$", "");
        if (cleanPath.equals("/")) return archive;
        return archive.find(cleanPath.split("/"));
    }

    public abstract static class Archive {
        public final String path;
        public final String name;
        public final long lastModified;
        public final boolean isDirectory;

        public Archive(ZipEntry entry, String root) {
            this.path = entry.getName().replaceFirst("^" + root.replaceAll("\\$", "\\\\\\$"), "").replaceAll("/+$", "");
            String p = this.path;
            this.name = p.substring(p.lastIndexOf('/') + 1);
            this.lastModified = entry.getTime();
            this.isDirectory = entry.isDirectory();
        }

        public abstract int size();

        public abstract String[] list();

        public abstract InputStream openStream();

        public abstract Archive find(String[] path);
    }

    public static class ArchiveFile extends Archive {
        public final int size;
        private byte[] data;

        public ArchiveFile(ZipFile zip, ZipEntry entry, String root) {
            super(entry, root);
            InputStream in;
            try {
                in = zip.getInputStream(entry);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int r;
                while ((r = in.read(buf)) != -1) out.write(buf, 0, r);
                this.data = out.toByteArray();
            } catch (IOException e) {
                this.data = new byte[0];
            }
            this.size = data.length;
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public String[] list() {
            return null;
        }

        @Override
        public InputStream openStream() {
            return new ByteArrayInputStream(data);
        }

        @Override
        public Archive find(String[] path) {
            if (path.length == 1 && path[0].equals(name)) return this;
            return null;
        }
    }

    public static class ArchiveDirectory extends Archive {
        public final Set<Archive> children = new HashSet<>();

        public ArchiveDirectory(ZipEntry entry, String root) {
            super(entry, root);
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String[] list() {
            String[] result = new String[children.size()];
            int i = 0;
            for (Archive c : children) {
                result[i++] = c.name + (c.isDirectory ? "/" : "");
            }
            return result;
        }

        @Override
        public InputStream openStream() {
            return null;
        }

        @Override
        public Archive find(String[] path) {
            if (path.length > 0 && path[0].equals(name)) {
                if (path.length == 1) return this;
                String[] subPath = new String[path.length - 1];
                System.arraycopy(path, 1, subPath, 0, subPath.length);
                for (Archive c : children) {
                    Archive result = c.find(subPath);
                    if (result != null) return result;
                }
            }
            return null;
        }
    }
}
