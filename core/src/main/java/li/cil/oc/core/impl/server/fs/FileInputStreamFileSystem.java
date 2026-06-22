package li.cil.oc.core.impl.server.fs;

import li.cil.oc.core.impl.util.FilePathUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;

public abstract class FileInputStreamFileSystem extends InputStreamFileSystem {
    protected abstract File root();

    @Override
    public long spaceTotal() {
        return spaceUsed();
    }

    @Override
    public long spaceUsed() {
        return spaceUsed_();
    }

    private Long spaceUsed_() {
        return recurse(root());
    }

    private long recurse(File path) {
        if (path.isDirectory()) {
            long acc = 0;
            File[] files = path.listFiles();
            if (files != null) {
                for (File f : files) acc += recurse(f);
            }
            return acc;
        }
        return path.length();
    }

    @Override
    public boolean exists(String path) {
        File f = new File(root(), FilePathUtil.validatePath(path));
        return f.exists();
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
        return new FileChannel(new File(root(), path));
    }

    public static class FileChannel implements InputChannel {
        private final java.nio.channels.FileChannel channel;
        private final RandomAccessFile file;

        public FileChannel(File file) {
            RandomAccessFile f;
            try {
                f = new RandomAccessFile(file, "r");
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            this.file = f;
            this.channel = this.file.getChannel();
        }

        @Override
        public long position(long newPosition) {
            try {
                channel.position(newPosition);
                return channel.position();
            } catch (java.io.IOException e) {
                return -1;
            }
        }

        @Override
        public long position() {
            try {
                return channel.position();
            } catch (java.io.IOException e) {
                return -1;
            }
        }

        @Override
        public void close() {
            try {
                channel.close();
            } catch (java.io.IOException ignored) {
            }
            try {
                file.close();
            } catch (java.io.IOException ignored) {
            }
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public int read(byte[] dst) {
            try {
                return channel.read(java.nio.ByteBuffer.wrap(dst));
            } catch (java.io.IOException e) {
                return -1;
            }
        }
    }
}
