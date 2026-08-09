package li.cil.oc.core.impl.server.fs;

import java.io.FileNotFoundException;
import li.cil.oc.api.fs.Mode;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

class ReadOnlyWrapper implements li.cil.oc.api.fs.FileSystem {
    private final li.cil.oc.api.fs.FileSystem fileSystem;

    ReadOnlyWrapper(li.cil.oc.api.fs.FileSystem fileSystem) {
        this.fileSystem = fileSystem;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public long spaceTotal() {
        return fileSystem.spaceUsed();
    }

    @Override
    public long spaceUsed() {
        return fileSystem.spaceUsed();
    }

    @Override
    public boolean exists(String path) {
        return fileSystem.exists(path);
    }

    @Override
    public long size(String path) {
        return fileSystem.size(path);
    }

    @Override
    public boolean isDirectory(String path) {
        return fileSystem.isDirectory(path);
    }

    @Override
    public long lastModified(String path) {
        return fileSystem.lastModified(path);
    }

    @Override
    public String[] list(String path) {
        return fileSystem.list(path);
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
        if (mode == Mode.Read) return fileSystem.open(path, mode);
        throw new RuntimeException(new FileNotFoundException("read-only filesystem; cannot open for writing: " + path));
    }

    @Override
    public li.cil.oc.api.fs.Handle getHandle(int handle) {
        return fileSystem.getHandle(handle);
    }

    @Override
    public void close() {
        fileSystem.close();
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        fileSystem.load(nbt, provider);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        fileSystem.save(nbt, provider);
    }

}
