package li.cil.oc.core.impl.server.fs;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.core.impl.util.FilePathUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public abstract class InputStreamFileSystem implements li.cil.oc.api.fs.FileSystem {
    private final Map<Integer, Handle> handles = new HashMap<>();

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean delete(String path) {
        return false;
    }

    @Override
    public boolean makeDirectory(String path) {
        return false;
    }

    @Override
    public boolean rename(String from, String to) {
        return false;
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return false;
    }

    @Override
    public int open(String path, Mode mode) {
        FilePathUtil.validatePath(path);
        synchronized (this) {
            if (mode == Mode.Read && exists(path) && !isDirectory(path)) {
                int handle;
                do {
                    handle = (int) (Math.random() * Integer.MAX_VALUE) + 1;
                } while (handles.containsKey(handle));
                InputChannel channel = openInputChannel(path);
                if (channel != null) {
                    handles.put(handle, new Handle(this, handle, path, channel));
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
            return handles.get(handle);
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            var copy = java.util.List.copyOf(handles.values());
            handles.clear();
            for (Handle h : copy) h.close();
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag handlesNbt = nbt.getList("input", Tag.TAG_COMPOUND);
        for (int i = 0; i < handlesNbt.size(); i++) {
            CompoundTag handleNbt = handlesNbt.getCompound(i);
            int handle = handleNbt.getInt("handle");
            String path = handleNbt.getString("path");
            long position = handleNbt.getLong("position");
            InputChannel channel = openInputChannel(path);
            if (channel != null) {
                channel.position(position);
                handles.put(handle, new Handle(this, handle, path, channel));
            }
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (this) {
            ListTag handlesNbt = new ListTag();
            for (Handle file : handles.values()) {
                assert file.channel.isOpen();
                CompoundTag handleNbt = new CompoundTag();
                handleNbt.putInt("handle", file.handle);
                handleNbt.putString("path", file.path);
                handleNbt.putLong("position", file.position());
                handlesNbt.add(handleNbt);
            }
            nbt.put("input", handlesNbt);
        }
    }

    protected abstract InputChannel openInputChannel(String path) ;

    public interface InputChannel extends ReadableByteChannel {
        boolean isOpen();

        void close();

        long position();

        long position(long newPosition);

        int read(byte[] dst);

        default int read(ByteBuffer dst) {
            if (dst.hasArray()) {
                return read(dst.array());
            }
            int count = Math.max(0, dst.limit() - dst.position());
            byte[] buffer = new byte[count];
            int n = read(buffer);
            if (n > 0) dst.put(buffer, 0, n);
            return n;
        }
    }

    public static class InputStreamChannel implements InputChannel {
        private final java.io.InputStream inputStream;
        public boolean isOpen = true;
        private long position = 0L;

        public InputStreamChannel(java.io.InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void close() {
            if (isOpen) {
                isOpen = false;
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public long position() {
            return position;
        }

        @Override
        public long position(long newPosition) {
            try {
                inputStream.reset();
                position = inputStream.skip(newPosition);
            } catch (IOException ignored) {
            }
            return position;
        }

        @Override
        public int read(byte[] dst) {
            try {
                int read = inputStream.read(dst);
                if (read > 0) position += read;
                return read;
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public boolean isOpen() {
            return isOpen;
        }

    }

    private record Handle(InputStreamFileSystem owner, int handle, String path,
                          InputChannel channel) implements li.cil.oc.api.fs.Handle {

        @Override
        public long position() {
            return channel.position();
        }

        @Override
        public long length() {
            return owner.size(path);
        }

        @Override
        public void close() {
            if (channel.isOpen()) {
                owner.handles.remove(handle);
                channel.close();
            }
        }

        @Override
        public int read(byte[] into) {
            return channel.read(into);
        }

        @Override
        public long seek(long to) {
            return channel.position(to);
        }

        @Override
        public void write(byte[] value) {
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }
}
