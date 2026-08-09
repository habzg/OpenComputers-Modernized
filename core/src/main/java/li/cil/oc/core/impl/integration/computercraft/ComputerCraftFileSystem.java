package li.cil.oc.core.impl.integration.computercraft;

import dan200.computercraft.api.filesystem.Mount;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.util.ArrayList;
import li.cil.oc.core.impl.server.fs.InputStreamFileSystem;

@SuppressWarnings("unused")
public class ComputerCraftFileSystem extends InputStreamFileSystem {
    protected final Mount mount;

    public ComputerCraftFileSystem(Mount mount) {
        this.mount = mount;
    }

    @Override
    public long spaceTotal() {
        return 0;
    }

    @Override
    public long spaceUsed() {
        return 0;
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
}
