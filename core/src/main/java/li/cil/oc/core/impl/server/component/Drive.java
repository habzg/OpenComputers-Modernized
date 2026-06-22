package li.cil.oc.core.impl.server.component;

import com.google.common.io.Files;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.core.util.ServerNetwork;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;

import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class Drive extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(Drive.class);
    public final int capacity;
    public final int platterCount;
    public final Label label;
    public final String sound;
    public final int speed;
    public final boolean isLocked;
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("drive", Visibility.Neighbors)
            .withConnector()
            .create();
    public final double[] readSectorCosts = {1.0 / 10, 1.0 / 20, 1.0 / 30, 1.0 / 40, 1.0 / 50, 1.0 / 60};
    public final double[] writeSectorCosts = {1.0 / 5, 1.0 / 10, 1.0 / 15, 1.0 / 20, 1.0 / 25, 1.0 / 30};
    public final double[] readByteCosts = {1.0 / 48, 1.0 / 64, 1.0 / 80, 1.0 / 96, 1.0 / 112, 1.0 / 128};
    public final double[] writeByteCosts = {1.0 / 24, 1.0 / 32, 1.0 / 40, 1.0 / 48, 1.0 / 56, 1.0 / 64};
    private final int sectorSize = 512;
    private final byte[] data;
    private final int sectorCount;
    private final int sectorsPerPlatter;
    private final Map<String, String> deviceInfo;
    private int headPos = 0;

    public Drive(int capacity, int platterCount, Label label, EnvironmentHost host, String sound, int speed, boolean isLocked) {
        super(host);
        this.capacity = capacity;
        this.platterCount = platterCount;
        this.label = label;
        this.sound = sound;
        this.speed = speed;
        this.isLocked = isLocked;
        this.data = new byte[capacity];
        this.sectorCount = capacity / sectorSize;
        this.sectorsPerPlatter = sectorCount / platterCount;

        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Disk, DeviceAttribute.Description, "Hard disk drive", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "MPD" + (capacity / 1024) + "L" + platterCount, DeviceAttribute.Capacity, String.valueOf((int) (capacity * 1.024)), DeviceAttribute.Size, String.valueOf(capacity), DeviceAttribute.Clock, ((2000 / readSectorCosts[speed]) / 100) + "/" +
                ((2000 / writeSectorCosts[speed]) / 100) + "/" +
                ((2000 / readByteCosts[speed]) / 100) + "/" +
                ((2000 / writeByteCosts[speed]) / 100));
    }

    private File savePath() {
        var level = host().level();
        if (level != null && level.getServer() != null) {
            var saveDir = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
            return new File(saveDir.toFile(), Settings.savePath + node.address() + ".bin");
        }
        return new File("saves", Settings.savePath + node.address() + ".bin");
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(direct = true, doc = "function():string -- Get the current label of the drive.")
    public synchronized Object @Nullable [] getLabel(Context context, Arguments args) {
        if (label != null) return ResultWrapper.result(label.getLabel());
        return null;
    }

    @Callback(doc = "function(value:string):string -- Sets the label of the drive.")
    public synchronized Object[] setLabel(Context context, Arguments args) {
        if (isLocked) throw new RuntimeException("drive is read only");
        if (label == null) throw new RuntimeException("drive does not support labeling");
        if (args.checkAny(0) == null) label.setLabel(null);
        else label.setLabel(args.checkString(0));
        return ResultWrapper.result(label.getLabel());
    }

    @Callback(direct = true, doc = "function():number -- Returns the total capacity of the drive, in bytes.")
    public Object[] getCapacity(Context context, Arguments args) {
        return ResultWrapper.result((double) capacity);
    }

    @Callback(direct = true, doc = "function():number -- Returns the size of a single sector on the drive, in bytes.")
    public Object[] getSectorSize(Context context, Arguments args) {
        return ResultWrapper.result((double) sectorSize);
    }

    @Callback(direct = true, doc = "function():number -- Returns the number of platters in the drive.")
    public Object[] getPlatterCount(Context context, Arguments args) {
        return ResultWrapper.result((double) platterCount);
    }

    @Callback(direct = true, doc = "function(sector:number):string -- Read the current contents of the specified sector.")
    public synchronized Object[] readSector(Context context, Arguments args) {
        context.consumeCallBudget(readSectorCosts[speed]);
        int sector = moveToSector(context, checkSector(args));
        diskActivity();
        byte[] sectorData = new byte[sectorSize];
        System.arraycopy(data, sectorOffset(sector), sectorData, 0, sectorSize);
        return ResultWrapper.result((Object) sectorData);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(direct = true, doc = "function(sector:number, value:string) -- Write the specified contents to the specified sector.")
    public synchronized Object @Nullable [] writeSector(Context context, Arguments args) {
        if (isLocked) throw new RuntimeException("drive is read only");
        context.consumeCallBudget(writeSectorCosts[speed]);
        byte[] sectorData = args.checkByteArray(1);
        int sector = moveToSector(context, checkSector(args));
        diskActivity();
        System.arraycopy(sectorData, 0, data, sectorOffset(sector), Math.min(sectorSize, sectorData.length));
        return null;
    }

    @Callback(direct = true, doc = "function(offset:number):number -- Read a single byte at the specified offset.")
    public synchronized Object[] readByte(Context context, Arguments args) {
        context.consumeCallBudget(readByteCosts[speed]);
        int offset = args.checkInteger(0) - 1;
        moveToSector(context, checkSector(offset));
        diskActivity();
        return ResultWrapper.result(data[offset] & 0xFF);
    }

    @SuppressWarnings("SameReturnValue")
    @Callback(direct = true, doc = "function(offset:number, value:number) -- Write a single byte to the specified offset.")
    public synchronized Object @Nullable [] writeByte(Context context, Arguments args) {
        if (isLocked) throw new RuntimeException("drive is read only");
        context.consumeCallBudget(writeByteCosts[speed]);
        int offset = args.checkInteger(0) - 1;
        int value = args.checkInteger(1);
        moveToSector(context, checkSector(offset));
        diskActivity();
        data[offset] = (byte) value;
        return null;
    }

    @Override
    public synchronized void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (node != null && node.address() != null) try {
            File path = savePath();
            if (path.exists()) {
                ByteArrayInputStream bin = new ByteArrayInputStream(Files.toByteArray(path));
                GZIPInputStream zin = new GZIPInputStream(bin);
                int offset = 0;
                int read;
                while ((read = zin.read(data, offset, data.length - offset)) >= 0 && offset < data.length) {
                    offset += read;
                }
                zin.close();
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed loading drive contents for '{}'.", node.address(), t);
        }
        headPos = Math.clamp(nbt.getInt("headPos"), 0, sectorToHeadPos(sectorCount));
        if (label != null) {
            label.load(nbt, provider);
        }
    }

    @Override
    public synchronized void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (node != null && node.address() != null) try {
            File path = savePath();
            if (!path.getParentFile().mkdirs()) {
                LOGGER.warn("Failed to create directory: {}", path.getParentFile());
            }
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            GZIPOutputStream zos = new GZIPOutputStream(bos);
            zos.write(data);
            zos.close();
            Files.write(bos.toByteArray(), path);
        } catch (Throwable t) {
            LOGGER.warn("Failed saving drive contents for '{}'.", node.address(), t);
        }
        nbt.putInt("headPos", headPos);
        if (label != null) {
            label.save(nbt, provider);
        }
    }

    private int validateSector(int sector) {
        if (sector < 0 || sector >= sectorCount)
            throw new IllegalArgumentException("invalid offset, not in a usable sector");
        return sector;
    }

    private int checkSector(int offset) {
        return validateSector(offset / sectorSize);
    }

    private int checkSector(Arguments args) {
        return validateSector(args.checkInteger(0) - 1);
    }

    private int moveToSector(Context context, int sector) {
        int newHeadPos = sectorToHeadPos(sector);
        if (headPos != newHeadPos) {
            int delta = Math.abs(headPos - newHeadPos);
            if (delta > Settings.get().sectorSeekThreshold) context.pause(Settings.get().sectorSeekTime);
            headPos = newHeadPos;
        }
        return sector;
    }

    private int sectorToHeadPos(int sector) {
        return sector % sectorsPerPlatter;
    }

    private int sectorOffset(int sector) {
        return sector * sectorSize;
    }

    private void diskActivity() {
        if (sound != null) {
            ServerNetwork.get().sendFileSystemActivity(node, host(), sound);
        }
    }
}
