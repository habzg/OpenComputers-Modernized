package li.cil.oc.core.impl.server.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.core.impl.util.FilePathUtil;
import li.cil.oc.core.server.fs.Buffered;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.NotNull;

public abstract class VirtualFileSystem extends OutputStreamFileSystem {
    protected final VirtualDirectory root = new VirtualDirectory();

    @Override
    public boolean exists(String path) {
        return root.get(segments(path)) != null;
    }

    @Override
    public boolean isDirectory(String path) {
        VirtualObject obj = root.get(segments(path));
        return obj != null && obj.isDirectory;
    }

    @Override
    public long size(String path) {
        VirtualObject obj = root.get(segments(path));
        if (obj instanceof VirtualFile vf) return vf.size();
        return 0L;
    }

    @Override
    public long lastModified(String path) {
        VirtualObject obj = root.get(segments(path));
        return obj != null ? obj.lastModified : 0L;
    }

    @Override
    public String[] list(String path) {
        VirtualObject obj = root.get(segments(path));
        if (obj instanceof VirtualDirectory dir) {
            return dir.list();
        }
        return null;
    }

    @Override
    public boolean delete(String path) {
        List<String> parts = segments(path);
        if (parts.isEmpty()) return true;
        VirtualObject parent = root.get(parts.subList(0, parts.size() - 1));
        if (parent instanceof VirtualDirectory dir) {
            return dir.delete(parts.getLast());
        }
        return false;
    }

    @Override
    public boolean makeDirectory(String path) {
        List<String> parts = segments(path);
        if (parts.isEmpty()) return false;
        VirtualObject parent = root.get(parts.subList(0, parts.size() - 1));
        if (parent instanceof VirtualDirectory dir) {
            return dir.makeDirectory(parts.getLast());
        }
        return false;
    }

    @Override
    public boolean rename(String from, String to) {
        if (from.isEmpty() || !exists(from)) throw new RuntimeException(new FileNotFoundException(from));
        List<String> segmentsTo = segments(to);
        VirtualObject toParentOpt = root.get(segmentsTo.subList(0, segmentsTo.size() - 1));
        if (toParentOpt instanceof VirtualDirectory toParent) {
            String toName = segmentsTo.getLast();
            List<String> segmentsFrom = segments(from);
            VirtualObject fromParentObj = root.get(segmentsFrom.subList(0, segmentsFrom.size() - 1));
            VirtualDirectory fromParent = (VirtualDirectory) fromParentObj;
            String fromName = segmentsFrom.getLast();
            VirtualObject obj = fromParent.children.get(fromName);
            if (toParent.get(Collections.singletonList(toName)) != null) {
                toParent.delete(toName);
            }
            fromParent.children.remove(fromName);
            fromParent.lastModified = System.currentTimeMillis();
            toParent.children.put(toName, obj);
            toParent.lastModified = System.currentTimeMillis();
            obj.lastModified = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        VirtualObject obj = root.get(segments(path));
        if (obj != null && time >= 0) {
            obj.lastModified = time;
            return true;
        }
        return false;
    }

    @Override
    protected InputChannel openInputChannel(String path) {
        VirtualObject obj = root.get(segments(path));
        if (obj instanceof VirtualFile vf) {
            InputStream is = vf.openInputStream();
            if (is != null) {
                return new InputStreamChannel(is);
            }
        }
        return null;
    }

    @Override
    protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
        List<String> parts = segments(path);
        if (parts.isEmpty()) return null;
        VirtualObject dir = root.get(parts.subList(0, parts.size() - 1));
        if (dir instanceof VirtualDirectory directory) {
            VirtualFile file = directory.touch(parts.getLast());
            if (file != null) {
                return file.openOutputHandle(this, id, path, mode);
            }
        }
        return null;
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        if (!(this instanceof Buffered)) {
            root.load(nbt, provider);
        }
        super.load(nbt, provider);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (!(this instanceof Buffered)) {
            root.save(nbt, provider);
        }
    }

    protected List<String> segments(String path) {
        String validated = FilePathUtil.validatePath(path);
        String[] parts = validated.split("/");
        List<String> result = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) result.add(p);
        }
        return result;
    }

    public abstract static class VirtualObject {
        public boolean isDirectory;
        public long lastModified = System.currentTimeMillis();

        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            if (nbt.contains("lastModified"))
                lastModified = nbt.getLong("lastModified");
        }

        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            nbt.putLong("lastModified", lastModified);
        }

        public VirtualObject get(List<String> path) {
            if (path.isEmpty()) return this;
            return null;
        }

        public abstract boolean canDelete();
    }

    public static class VirtualFile extends VirtualObject {
        byte[] data = new byte[0];
        int dataLength = 0;
        public VirtualOutputHandle handle = null;

        public VirtualFile() {
            isDirectory = false;
        }

        public long size() {
            return dataLength;
        }

        public InputStream openInputStream() {
            return new VirtualFileInputStream(this);
        }

        public VirtualOutputHandle openOutputHandle(OutputStreamFileSystem owner, int id, String path, Mode mode) {
            if (handle != null) return null;
            if (mode == Mode.Write) {
                data = new byte[0];
                dataLength = 0;
                lastModified = System.currentTimeMillis();
            }
            VirtualOutputHandle h = new VirtualOutputHandle(this, owner, id, path);
            handle = h;
            return h;
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            byte[] bytes = nbt.getByteArray("data");
            data = bytes;
            dataLength = bytes.length;
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            byte[] bytes = new byte[dataLength];
            System.arraycopy(data, 0, bytes, 0, dataLength);
            nbt.putByteArray("data", bytes);
        }

        void grow(int needed) {
            if (needed > data.length) {
                int newLen = data.length == 0 ? needed : Math.max(data.length * 2, needed);
                byte[] newData = new byte[newLen];
                System.arraycopy(data, 0, newData, 0, dataLength);
                data = newData;
            }
        }

        @Override
        public boolean canDelete() {
            return handle == null;
        }

    }

    public static class VirtualDirectory extends VirtualObject {
        public final Map<String, VirtualObject> children = new LinkedHashMap<>();

        public VirtualDirectory() {
            isDirectory = true;
        }

        public String[] list() {
            String[] result = new String[children.size()];
            int i = 0;
            for (Map.Entry<String, VirtualObject> e : children.entrySet()) {
                result[i++] = e.getValue().isDirectory ? e.getKey() + "/" : e.getKey();
            }
            return result;
        }

        public boolean makeDirectory(String name) {
            if (children.containsKey(name)) return false;
            children.put(name, new VirtualDirectory());
            lastModified = System.currentTimeMillis();
            return true;
        }

        public boolean delete(String name) {
            VirtualObject child = children.get(name);
            if (child != null && child.canDelete()) {
                children.remove(name);
                lastModified = System.currentTimeMillis();
                return true;
            }
            return false;
        }

        public VirtualFile touch(String name) {
            VirtualObject obj = children.get(name);
            if (obj instanceof VirtualFile file) return file;
            if (obj == null) {
                VirtualFile child = new VirtualFile();
                children.put(name, child);
                lastModified = System.currentTimeMillis();
                return child;
            }
            return null;
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            ListTag childrenNbt = nbt.getList("children", Tag.TAG_COMPOUND);
            for (int i = 0; i < childrenNbt.size(); i++) {
                CompoundTag childNbt = childrenNbt.getCompound(i);
                VirtualObject child = childNbt.getBoolean("isDirectory") ? new VirtualDirectory() : new VirtualFile();
                child.load(childNbt, provider);
                children.put(childNbt.getString("name"), child);
            }
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            ListTag childrenNbt = new ListTag();
            for (Map.Entry<String, VirtualObject> e : children.entrySet()) {
                CompoundTag childNbt = new CompoundTag();
                childNbt.putBoolean("isDirectory", e.getValue().isDirectory);
                childNbt.putString("name", e.getKey());
                e.getValue().save(childNbt, provider);
                childrenNbt.add(childNbt);
            }
            nbt.put("children", childrenNbt);
        }

        @Override
        public VirtualObject get(List<String> path) {
            if (path.isEmpty()) return this;
            VirtualObject child = children.get(path.getFirst());
            if (child != null) return child.get(path.subList(1, path.size()));
            return null;
        }

        @Override
        public boolean canDelete() {
            return children.isEmpty();
        }

    }

    public static class VirtualFileInputStream extends InputStream {
        private final VirtualFile file;
        private boolean closed = false;
        private int pos = 0;

        public VirtualFileInputStream(VirtualFile file) {
            this.file = file;
        }

        @Override
        public int available() {
            if (closed) return 0;
            return Math.max(file.dataLength - pos, 0);
        }

        @Override
        public void close() {
            closed = true;
        }

        @Override
        public int read() {
            if (closed) throw new RuntimeException(new IOException("file is closed"));
            if (available() == 0) return -1;
            return file.data[pos++] & 0xFF;
        }

        @Override
        public int read(byte @NotNull [] b, int off, int len) {
            if (closed) throw new RuntimeException(new IOException("file is closed"));
            int count = available();
            if (count == 0) return -1;
            int n = Math.min(len, count);
            System.arraycopy(file.data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public synchronized void reset() {
            if (closed) throw new RuntimeException(new IOException("file is closed"));
            pos = 0;
        }

        @Override
        public long skip(long n) {
            if (closed) throw new RuntimeException(new IOException("file is closed"));
            pos = (int) Math.min(pos + n, Integer.MAX_VALUE);
            return pos;
        }
    }

    public static class VirtualOutputHandle extends OutputHandle {
        private static final long MAX_SEEK = 64 * 1024 * 1024;

        public final VirtualFile file;
        public long position;

        public VirtualOutputHandle(VirtualFile file, OutputStreamFileSystem owner, int handle, String path) {
            super(owner, handle, path);
            this.file = file;
            this.position = file.dataLength;
        }

        @Override
        public long length() {
            return file.size();
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public void close() {
            if (!isClosed()) {
                super.close();
                assert file.handle == this;
                file.handle = null;
            }
        }

        @Override
        public long seek(long to) {
            if (to < 0) throw new RuntimeException(new IOException("invalid offset"));
            if (to > MAX_SEEK) throw new RuntimeException(new IOException("offset too large"));
            position = to;
            return position;
        }

        @Override
        public void write(byte[] b) {
            if (isClosed()) throw new RuntimeException(new IOException("file is closed"));
            if (position > Integer.MAX_VALUE) throw new RuntimeException(new IOException("file too large"));
            int pos = (int) position;
            int newLen = pos + b.length;
            file.grow(newLen);
            if (newLen > file.dataLength) {
                file.dataLength = newLen;
            }
            System.arraycopy(b, 0, file.data, pos, b.length);
            position += b.length;
            file.lastModified = System.currentTimeMillis();
        }
    }
}
