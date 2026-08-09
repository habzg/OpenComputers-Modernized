package li.cil.oc.core.impl.server.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.core.impl.util.FilePathUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public abstract class OutputStreamFileSystem extends InputStreamFileSystem {
    private final Map<Integer, OutputHandle> handles = new HashMap<>();

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public int open(String path, Mode mode) {
        synchronized (this) {
            if (mode == Mode.Read) return super.open(path, mode);
            FilePathUtil.validatePath(path);
            if (!isDirectory(path)) {
                int handle;
                do {
                    handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
                } while (handles.containsKey(handle));
                OutputHandle fileHandle = openOutputHandle(handle, path, mode);
                if (fileHandle != null) {
                    handles.put(handle, fileHandle);
                    return handle;
                }
                throw new RuntimeException(new FileNotFoundException(path));
            }
            throw new RuntimeException(new FileNotFoundException(path));
        }
    }

    @Override
    public li.cil.oc.api.fs.Handle getHandle(int handle) {
        synchronized (this) {
            li.cil.oc.api.fs.Handle h = super.getHandle(handle);
            if (h != null) return h;
            return handles.get(handle);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            super.close();
            for (OutputHandle h : handles.values()) h.close();
            handles.clear();
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        ListTag handlesNbt = nbt.getList("output", Tag.TAG_COMPOUND);
        for (int i = 0; i < handlesNbt.size(); i++) {
            CompoundTag handleNbt = handlesNbt.getCompound(i);
            int handle = handleNbt.getInt("handle");
            String path = handleNbt.getString("path");
            OutputHandle fileHandle = openOutputHandle(handle, path, Mode.Append);
            if (fileHandle != null) {
                handles.put(handle, fileHandle);
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            super.save(nbt, provider);
            ListTag handlesNbt = new ListTag();
            for (OutputHandle file : handles.values()) {
                assert !file.isClosed();
                CompoundTag handleNbt = new CompoundTag();
                handleNbt.putInt("handle", file.handle);
                handleNbt.putString("path", file.path);
                handlesNbt.add(handleNbt);
            }
            nbt.put("output", handlesNbt);
        }
    }

    protected abstract OutputHandle openOutputHandle(int id, String path, Mode mode) ;

    public abstract static class OutputHandle implements li.cil.oc.api.fs.Handle {
        public final OutputStreamFileSystem owner;
        public final int handle;
        public final String path;
        protected boolean _isClosed = false;

        public OutputHandle(OutputStreamFileSystem owner, int handle, String path) {
            this.owner = owner;
            this.handle = handle;
            this.path = path;
        }

        public boolean isClosed() {
            return _isClosed;
        }

        @Override
        public void close() {
            if (!isClosed()) {
                _isClosed = true;
                owner.handles.remove(handle);
            }
        }

        @Override
        public int read(byte[] into) {
            throw new RuntimeException(new IOException("bad file descriptor"));
        }

        @Override
        public long seek(long to) {
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }
}
