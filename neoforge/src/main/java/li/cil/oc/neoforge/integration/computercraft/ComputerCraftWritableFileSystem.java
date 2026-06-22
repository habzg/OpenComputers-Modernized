package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.filesystem.WritableMount;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.core.impl.server.fs.OutputStreamFileSystem;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Set;

@SuppressWarnings("unused")
public class ComputerCraftWritableFileSystem extends OutputStreamFileSystem {
    protected final WritableMount mount;

    @SuppressWarnings("unused")
    public ComputerCraftWritableFileSystem(WritableMount mount) {
        this.mount = mount;
    }

    @Override
    public long spaceTotal() {
        try {
            return mount.getCapacity();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public long spaceUsed() {
        try {
            return mount.getCapacity() - mount.getRemainingSpace();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean exists(String path) {
        try {
            return mount.exists(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean isDirectory(String path) {
        try {
            return mount.isDirectory(path);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long lastModified(String path) {
        return 0L;
    }

    @Override
    public String[] list(String path) {
        try {
            ArrayList<String> result = new ArrayList<>();
            mount.list(path, result);
            return result.toArray(new String[0]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public long size(String path) {
        try {
            return mount.getSize(path);
        } catch (Exception e) {
            return 0L;
        }
    }

    @Override
    protected InputChannel openInputChannel(String path) {
        try {
            SeekableByteChannel channel = mount.openForRead(path);
            return new SeekableByteChannelInput(channel);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean delete(String path) {
        try {
            mount.delete(path);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public boolean makeDirectory(String path) {
        try {
            mount.makeDirectory(path);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
        try {
            Set<OpenOption> options;
            if (mode == Mode.Append) {
                options = Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } else if (mode == Mode.Write) {
                options = Set.of(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            } else {
                throw new IllegalArgumentException();
            }
            SeekableByteChannel channel = mount.openFile(path, options);
            return new SeekableByteOutputHandle(channel, this, id, path);
        } catch (Throwable e) {
            return null;
        }
    }

    private record SeekableByteChannelInput(SeekableByteChannel channel) implements InputChannel {

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public void close() {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
        }

        @Override
        public long position() {
            try {
                return channel.position();
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public long position(long newPosition) {
            try {
                channel.position(newPosition);
                return newPosition;
            } catch (Exception e) {
                return position();
            }
        }

        @Override
        public int read(byte[] dst) {
            try {
                return channel.read(ByteBuffer.wrap(dst));
            } catch (Exception e) {
                return -1;
            }
        }
    }

    public static class SeekableByteOutputHandle extends OutputHandle {
        private final SeekableByteChannel channel;

        public SeekableByteOutputHandle(SeekableByteChannel channel,
                                        OutputStreamFileSystem owner, int handle, String path) {
            super(owner, handle, path);
            this.channel = channel;
        }

        @Override
        public long length() {
            try {
                return ((ComputerCraftWritableFileSystem) owner).mount.getSize(path);
            } catch (Exception e) {
                return 0;
            }
        }

        @Override
        public long position() {
            return 0;
        }

        @Override
        public void write(byte[] value) {
            try {
                channel.write(ByteBuffer.wrap(value));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
