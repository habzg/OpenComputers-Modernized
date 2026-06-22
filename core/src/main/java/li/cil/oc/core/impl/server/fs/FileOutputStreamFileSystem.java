package li.cil.oc.core.impl.server.fs;

import li.cil.oc.api.fs.Mode;
import li.cil.oc.core.impl.util.FilePathUtil;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public abstract class FileOutputStreamFileSystem extends OutputStreamFileSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileOutputStreamFileSystem.class);

    protected abstract File root();

    @Override
    public long spaceTotal() {
        return -1;
    }

    @Override
    public long spaceUsed() {
        return -1;
    }

    @Override
    public boolean delete(String path) {
        File file = new File(root(), FilePathUtil.validatePath(path));
        return file.equals(root()) || file.delete();
    }

    @Override
    public boolean makeDirectory(String path) {
        return new File(root(), FilePathUtil.validatePath(path)).mkdir();
    }

    @Override
    public boolean rename(String from, String to) {
        try {
            Files.move(new File(root(), FilePathUtil.validatePath(from)).toPath(),
                    new File(root(), FilePathUtil.validatePath(to)).toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean setLastModified(String path, long time) {
        return new File(root(), FilePathUtil.validatePath(path)).setLastModified(time);
    }

    @Override
    protected OutputHandle openOutputHandle(int id, String path, Mode mode) {
        String modeStr;
        if (mode == Mode.Append || mode == Mode.Write) modeStr = "rw";
        else throw new IllegalArgumentException();
        try {
            return new FileHandle(new RandomAccessFile(new File(root(), path), modeStr), this, id, path, mode);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (!root().mkdirs()) {
            LOGGER.warn("Failed to create root directory: {}", root());
        }
        if (!root().setLastModified(System.currentTimeMillis())) {
            LOGGER.warn("Failed to set last modified on root directory: {}", root());
        }
    }

    public static class FileHandle extends OutputHandle {
        private final RandomAccessFile file;

        public FileHandle(RandomAccessFile file, OutputStreamFileSystem owner, int handle, String path, Mode mode) {
            super(owner, handle, path);
            this.file = file;
            if (mode == Mode.Write) {
                try {
                    file.setLength(0);
                } catch (IOException ignored) {
                }
            }
        }

        @Override
        public long position() {
            try {
                return file.getFilePointer();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public long length() {
            try {
                return file.length();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public void close() {
            super.close();
            try {
                file.close();
            } catch (IOException ignored) {
            }
        }

        @Override
        public long seek(long to) {
            try {
                file.seek(to);
                return to;
            } catch (IOException e) {
                return -1;
            }
        }

        @Override
        public void write(byte[] value) {
            try {
                file.write(value);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
