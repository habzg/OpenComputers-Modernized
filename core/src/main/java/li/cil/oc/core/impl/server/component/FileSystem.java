package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.fs.Mode;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.api.prefab.ManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.traits.TileEntity;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.core.util.ServerNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FileSystem extends ManagedEnvironment implements DeviceInfo {
    public final li.cil.oc.api.fs.FileSystem fileSystem;
    public final Label label;
    public final String sound;
    public final int speed;
    public final Connector node = Network.newNode(this, Visibility.Network)
            .withComponent("filesystem", Visibility.Neighbors)
            .withConnector()
            .create();
    public final double[] readCosts = {1.0, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    public final double[] seekCosts = {1.0, 1.0 / 4, 1.0 / 7, 1.0 / 10, 1.0 / 13, 1.0 / 15};
    public final double[] writeCosts = {1.0, 1.0 / 2, 1.0 / 3, 1.0 / 4, 1.0 / 5, 1.0 / 6};
    private final Map<String, Set<Integer>> owners = new HashMap<>();
    private final Map<String, String> deviceInfo;

    public FileSystem(li.cil.oc.api.fs.FileSystem fileSystem, Label label, EnvironmentHost host, String sound, int speed) {
        super(host);
        this.fileSystem = fileSystem;
        this.label = label;
        this.sound = sound;
        this.speed = speed;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Volume, DeviceAttribute.Description, "Filesystem", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "MPFS.21.6", DeviceAttribute.Capacity, String.valueOf((int) (fileSystem.spaceTotal() * 1.024)), DeviceAttribute.Size, String.valueOf(fileSystem.spaceTotal()), DeviceAttribute.Clock, ((2000 / readCosts[speed]) / 100) + "/" +
                ((2000 / seekCosts[speed]) / 100) + "/" +
                ((2000 / writeCosts[speed]) / 100));
        setNode(this.node);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():string -- Get the current label.")
    public Object @Nullable [] getLabel(Context ignoredContext, Arguments ignoredArgs) {
        synchronized (fileSystem) {
            if (label != null) return ResultWrapper.result(label.getLabel());
            return null;
        }
    }

    @Callback(doc = "function(value:string):string -- Sets the label.")
    public Object[] setLabel(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            if (label == null) throw new RuntimeException("drive does not support labeling");
            if (args.checkAny(0) == null) label.setLabel(null);
            else label.setLabel(args.checkString(0));
            return ResultWrapper.result(label.getLabel());
        }
    }

    @Callback(direct = true, doc = "function():boolean -- Returns whether the file system is read-only.")
    public Object[] isReadOnly(Context ignoredContext, Arguments ignoredArgs) {
        synchronized (fileSystem) {
            return ResultWrapper.result(fileSystem.isReadOnly());
        }
    }

    @Callback(direct = true, doc = "function():number -- The overall capacity.")
    public Object[] spaceTotal(Context ignoredContext, Arguments ignoredArgs) {
        synchronized (fileSystem) {
            long space = fileSystem.spaceTotal();
            if (space < 0) return ResultWrapper.result(Double.POSITIVE_INFINITY);
            return ResultWrapper.result((double) space);
        }
    }

    @Callback(direct = true, doc = "function():number -- The currently used capacity.")
    public Object[] spaceUsed(Context ignoredContext, Arguments ignoredArgs) {
        synchronized (fileSystem) {
            return ResultWrapper.result((double) fileSystem.spaceUsed());
        }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether an object exists.")
    public Object[] exists(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            diskActivity();
            return ResultWrapper.result(fileSystem.exists(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns the size.")
    public Object[] size(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            diskActivity();
            return ResultWrapper.result((double) fileSystem.size(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):boolean -- Returns whether it is a directory.")
    public Object[] isDirectory(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            diskActivity();
            return ResultWrapper.result(fileSystem.isDirectory(clean(args.checkString(0))));
        }
    }

    @Callback(direct = true, doc = "function(path:string):number -- Returns last modified timestamp.")
    public Object[] lastModified(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            diskActivity();
            return ResultWrapper.result((double) fileSystem.lastModified(clean(args.checkString(0))));
        }
    }

    @Callback(doc = "function(path:string):table -- Returns a list of names in the directory.")
    public Object @Nullable [] list(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            String[] list = fileSystem.list(clean(args.checkString(0)));
            if (list != null) {
                diskActivity();
                return new Object[]{list};
            }
            return null;
        }
    }

    @Callback(doc = "function(path:string):boolean -- Creates a directory.")
    public Object[] makeDirectory(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            boolean success = recurseMkdir(clean(args.checkString(0)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    private boolean recurseMkdir(String path) {
        if (fileSystem.exists(path)) return true;
        if (fileSystem.makeDirectory(path)) return true;
        String parent = path.contains("/") ? path.substring(0, path.lastIndexOf('/')) : "";
        if (!parent.isEmpty()) {
            recurseMkdir(parent);
            return fileSystem.makeDirectory(path);
        }
        return fileSystem.makeDirectory(path);
    }

    @Callback(doc = "function(path:string):boolean -- Removes the object.")
    public Object[] remove(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            boolean success = recurseDelete(clean(args.checkString(0)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    private boolean recurseDelete(String path) {
        if (!fileSystem.exists(path)) return true;
        if (fileSystem.isDirectory(path)) {
            String[] children = fileSystem.list(path);
            if (children != null) {
                for (String child : children) {
                    if (!recurseDelete(path + "/" + child)) return false;
                }
            }
        }
        return fileSystem.delete(path);
    }

    @Callback(doc = "function(from:string, to:string):boolean -- Renames/moves an object.")
    public Object[] rename(Context ignoredContext, Arguments args) {
        synchronized (fileSystem) {
            boolean success = fileSystem.rename(clean(args.checkString(0)), clean(args.checkString(1)));
            diskActivity();
            return ResultWrapper.result(success);
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata) -- Closes an open file descriptor.")
    @SuppressWarnings("SameReturnValue")
    public Object @Nullable [] close(Context context, Arguments args) {
        synchronized (fileSystem) {
            close(context, checkHandle(args, 0));
            return null;
        }
    }

    @Callback(direct = true, limit = 4, doc = "function(path:string[, mode:string='r']):userdata -- Opens a new file descriptor.")
    public Object[] open(Context context, Arguments args) {
        synchronized (fileSystem) {
            Set<Integer> ctxOwners = owners.get(context.node().address());
            if (ctxOwners != null && ctxOwners.size() >= Settings.get().maxHandles) {
                throw new RuntimeException(new IOException("too many open handles"));
            }
            String path = args.checkString(0);
            String mode = args.optString(1, "r");
            int handle = fileSystem.open(clean(path), parseMode(mode));
            if (handle > 0) {
                owners.computeIfAbsent(context.node().address(), k -> new HashSet<>()).add(handle);
            }
            diskActivity();
            return ResultWrapper.result(new HandleValue(node.address(), handle));
        }
    }

    @Callback(direct = true, limit = 15, doc = "function(handle:userdata, count:number):string -- Reads data.")
    public Object @Nullable [] read(Context context, Arguments args) {
        synchronized (fileSystem) {
            context.consumeCallBudget(readCosts[speed]);
            int handle = checkHandle(args, 0);
            int n = Math.clamp(args.checkInteger(1), 0, Settings.get().maxReadBuffer);
            checkOwner(context.node().address(), handle);
            li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                byte[] buffer = new byte[n];
                int read = file.read(buffer);
                if (read >= 0) {
                    byte[] bytes = read == buffer.length ? buffer : java.util.Arrays.copyOf(buffer, read);
                    if (!node.tryChangeBuffer(-Settings.get().hddReadCost * bytes.length)) {
                        throw new RuntimeException(new IOException("not enough energy"));
                    }
                    diskActivity();
                    return ResultWrapper.result((Object) bytes);
                }
                return null;
            }
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, whence:string, offset:number):number -- Seeks in an open file descriptor.")
    public Object[] seek(Context context, Arguments args) {
        synchronized (fileSystem) {
            context.consumeCallBudget(seekCosts[speed]);
            int handle = checkHandle(args, 0);
            String whence = args.checkString(1);
            int offset = args.checkInteger(2);
            checkOwner(context.node().address(), handle);
            li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                switch (whence) {
                    case "cur":
                        file.seek(file.position() + offset);
                        break;
                    case "set":
                        file.seek(offset);
                        break;
                    case "end":
                        file.seek(file.length() + offset);
                        break;
                    default:
                        throw new IllegalArgumentException("invalid mode");
                }
                return ResultWrapper.result((double) file.position());
            }
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }

    @Callback(direct = true, doc = "function(handle:userdata, value:string):boolean -- Writes data.")
    public Object[] write(Context context, Arguments args) {
        synchronized (fileSystem) {
            context.consumeCallBudget(writeCosts[speed]);
            int handle = checkHandle(args, 0);
            byte[] value = args.checkByteArray(1);
            if (!node.tryChangeBuffer(-Settings.get().hddWriteCost * value.length)) {
                throw new RuntimeException(new IOException("not enough energy"));
            }
            checkOwner(context.node().address(), handle);
            li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                file.write(value);
                diskActivity();
                return ResultWrapper.result(true);
            }
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }

    public int checkHandle(Arguments args, int index) {
        if (args.isInteger(index)) {
            return args.checkInteger(index);
        }
        Object value = args.checkAny(index);
        if (value instanceof HandleValue) {
            return ((HandleValue) value).handle;
        }
        if (value instanceof java.util.Map<?, ?> table && table.containsKey("handle")) {
            Object raw = table.get("handle");
            if (raw instanceof Number num) {
                return num.intValue();
            }
        }
        throw new RuntimeException(new IOException("bad file descriptor"));
    }

    public void close(Context context, int handle) {
        synchronized (fileSystem) {
            li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
            if (file != null) {
                Set<Integer> set = owners.get(context.node().address());
                if (set != null && set.remove(handle)) {
                    file.close();
                    return;
                }
            }
            throw new RuntimeException(new IOException("bad file descriptor"));
        }
    }

    @Override
    public void onMessage(Message message) {
        synchronized (fileSystem) {
            super.onMessage(message);
            if ("computer.stopped".equals(message.name()) || "computer.started".equals(message.name())) {
                Set<Integer> set = owners.get(message.source().address());
                if (set != null) {
                    for (int handle : set) {
                        li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
                        if (file != null) file.close();
                    }
                    set.clear();
                }
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        synchronized (fileSystem) {
            super.onDisconnect(node);
            if (node == this.node) {
                fileSystem.close();
            } else if (owners.containsKey(node.address())) {
                for (int handle : owners.get(node.address())) {
                    li.cil.oc.api.fs.Handle file = fileSystem.getHandle(handle);
                    if (file != null) file.close();
                }
                owners.remove(node.address());
            }
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (fileSystem) {
            super.load(nbt, provider);
            ListTag ownersList = nbt.getList("owners", Tag.TAG_COMPOUND);
            for (int i = 0; i < ownersList.size(); i++) {
                CompoundTag ownerNbt = ownersList.getCompound(i);
                String address = ownerNbt.getString("address");
                if (!address.isEmpty()) {
                    int[] handles = ownerNbt.getIntArray("handles");
                    Set<Integer> set = new HashSet<>();
                    for (int h : handles) set.add(h);
                    owners.put(address, set);
                }
            }
            if (label != null) {
                label.load(nbt, provider);
            }
        }
        fileSystem.load(nbt.getCompound("fs"), provider);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        synchronized (fileSystem) {
            super.save(nbt, provider);
            if (label != null) {
                label.save(nbt, provider);
            }
            if (!TileEntity.savingForClients) {
                ListTag ownersNbt = new ListTag();
                for (Map.Entry<String, Set<Integer>> entry : owners.entrySet()) {
                    CompoundTag ownerNbt = new CompoundTag();
                    ownerNbt.putString("address", entry.getKey());
                    int[] handles = entry.getValue().stream().mapToInt(Integer::intValue).toArray();
                    ownerNbt.put("handles", new IntArrayTag(handles));
                    ownersNbt.add(ownerNbt);
                }
                nbt.put("owners", ownersNbt);
                CompoundTag fsNbt = new CompoundTag();
                fileSystem.save(fsNbt, provider);
                nbt.put("fs", fsNbt);
            }
        }
    }

    private String clean(String path) {
        String result = com.google.common.io.Files.simplifyPath(path);
        if (result.startsWith("../") || "..".equals(result))
            throw new RuntimeException(new FileNotFoundException(path));
        if ("/".equals(result) || ".".equals(result)) return "";
        return result;
    }

    private Mode parseMode(String value) {
        return switch (value) {
            case "r", "rb" -> Mode.Read;
            case "w", "wb" -> Mode.Write;
            case "a", "ab" -> Mode.Append;
            default -> throw new IllegalArgumentException("unsupported mode");
        };
    }

    private void checkOwner(String owner, int handle) {
        Set<Integer> set = owners.get(owner);
        if (set == null || !set.contains(handle))
            throw new RuntimeException(new IOException("bad file descriptor"));
    }

    private void diskActivity() {
        if (sound != null) {
            ServerNetwork.get().sendFileSystemActivity(node, host(), sound);
        }
    }

    public static class HandleValue extends AbstractValue {
        public String owner;
        public int handle;

        @SuppressWarnings("unused")
        public HandleValue() {
            this.owner = "";
            this.handle = 0;
        }

        public HandleValue(String owner, int handle) {
            this.owner = owner;
            this.handle = handle;
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            if (context.node() != null && context.node().network() != null) {
                Node node = context.node().network().node(owner);
                if (node != null && node.host() instanceof FileSystem) {
                    try {
                        ((FileSystem) node.host()).close(context, handle);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            owner = nbt.getString("owner");
            handle = nbt.getInt("handle");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putInt("handle", handle);
            nbt.putString("owner", owner);
        }

        @Override
        public String toString() {
            return String.valueOf(handle);
        }

    }
}
