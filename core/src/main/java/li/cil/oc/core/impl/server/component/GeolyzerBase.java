package li.cil.oc.core.impl.server.component;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.traits.WorldControl;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public abstract class GeolyzerBase extends AbstractManagedEnvironment implements WorldControl, DeviceInfo {
    public final EnvironmentHost host;
    public final Connector node = Network.newNode(this, Visibility.Network)
            .withComponent("geolyzer")
            .withConnector()
            .create();

    private final Map<String, String> deviceInfo;

    public GeolyzerBase(EnvironmentHost host) {
        this.host = host;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Generic, DeviceAttribute.Description, "Geolyzer", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Terrain Analyzer MkII", DeviceAttribute.Capacity, String.valueOf(OCSettings.get().geolyzerRange));
        setNode(this.node);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public abstract BlockPosition position();

    @Override
    public abstract Direction checkSideForAction(Arguments args, int n);

    private boolean canSeeSky() {
        BlockPosition pos = position().offset(Direction.UP);
        if (!host.level().dimensionType().hasSkyLight()) return false;
        return host.level().canSeeSky(new BlockPos(pos.x(), pos.y(), pos.z()));
    }

    @Callback(doc = "function():boolean -- Returns whether there is a clear line of sight to the sky directly above.")
    public Object[] canSeeSky(Context context, Arguments args) {
        return ResultWrapper.result(canSeeSky());
    }

    @Callback(doc = "function():boolean -- Return whether the sun is currently visible directly above.")
    public Object[] isSunVisible(Context context, Arguments args) {
        BlockPosition hostPos = BlockPosition.apply(host).offset(Direction.UP);
        BlockPos blockPos = new BlockPos(hostPos.x(), hostPos.y(), hostPos.z());
        return ResultWrapper.result(
                host.level().isDay() &&
                        canSeeSky() &&
                        (!host.level().getBiome(blockPos).value().hasPrecipitation() ||
                                (!host.level().isRaining() && !host.level().isThundering())));
    }

    @SuppressWarnings("unchecked")
    @Callback(doc = "function(x:number, z:number[, y:number, w:number, d:number, h:number][, ignoreReplaceable:boolean|options:table]):table -- Analyzes the density of the column at the specified relative coordinates.")
    public Object[] scan(Context context, Arguments args) {
        int minX, minY, minZ, maxX, maxY, maxZ, optIndex;
        minX = args.checkInteger(0);
        minZ = args.checkInteger(1);
        if (args.isInteger(2) && args.isInteger(3) && args.isInteger(4) && args.isInteger(5)) {
            minY = args.checkInteger(2);
            int w = args.checkInteger(3);
            int d = args.checkInteger(4);
            int h = args.checkInteger(5);
            maxX = minX + w - 1;
            maxY = minY + h - 1;
            maxZ = minZ + d - 1;
            int mx = Math.min(minX, maxX);
            int my = Math.min(minY, maxY);
            int mz = Math.min(minZ, maxZ);
            maxX = Math.max(minX, maxX);
            maxY = Math.max(minY, maxY);
            maxZ = Math.max(minZ, maxZ);
            minX = mx;
            minY = my;
            minZ = mz;
            optIndex = 6;
        } else {
            minY = -32;
            maxX = minX;
            maxY = 31;
            maxZ = minZ;
            optIndex = 2;
        }
        int volume = (maxX - minX + 1) * (maxZ - minZ + 1) * (maxY - minY + 1);
        if (volume > 64) throw new IllegalArgumentException("volume too large (maximum is 64)");

        Map<String, Object> options;
        if (args.isBoolean(optIndex)) {
            options = new HashMap<>();
            options.put("includeReplaceable", !args.checkBoolean(optIndex));
        } else {
            options = args.optTable(optIndex, new HashMap<>());
        }

        if (Math.abs(minX) > OCSettings.get().geolyzerRange || Math.abs(maxX) > OCSettings.get().geolyzerRange ||
                Math.abs(minY) > OCSettings.get().geolyzerRange || Math.abs(maxY) > OCSettings.get().geolyzerRange ||
                Math.abs(minZ) > OCSettings.get().geolyzerRange || Math.abs(maxZ) > OCSettings.get().geolyzerRange) {
            throw new IllegalArgumentException("location out of bounds");
        }

        if (!node.tryChangeBuffer(-OCSettings.get().geolyzerScanCost))
            return ResultWrapper.result(null, "not enough energy");

        var delegate = EventHandlerDelegate.get();
        float[] data = delegate != null
                ? delegate.postGeolyzerScan(host, options, minX, minY, minZ, maxX, maxY, maxZ)
                : new float[64];
        if (data == null)
            return ResultWrapper.result(null, "scan was canceled");
        return ResultWrapper.result((Object) data);
    }

    @SuppressWarnings("rawtypes")
    @Callback(doc = "function(side:number[,options:table]):table -- Get some information on a directly adjacent block.")
    public Object[] analyze(Context context, Arguments args) {
        if (!OCSettings.get().allowItemStackInspection)
            return ResultWrapper.result(null, "not enabled in config");

        Direction side = ExtendedArguments.checkSideAny(args, 0);
        Direction globalSide = host instanceof li.cil.oc.api.internal.Rotatable ?
                ((li.cil.oc.api.internal.Rotatable) host).toGlobal(side) : side;
        Map options = args.optTable(1, new HashMap<>());

        if (!node.tryChangeBuffer(-OCSettings.get().geolyzerScanCost))
            return ResultWrapper.result(null, "not enough energy");

        BlockPosition globalPos = position().offset(globalSide);
        var delegate = EventHandlerDelegate.get();
        Map<String, Object> data = delegate != null
                ? delegate.postGeolyzerAnalyze(host, options, globalPos.x(), globalPos.y(), globalPos.z())
                : new HashMap<>();
        if (data == null)
            return ResultWrapper.result(null, "scan was canceled");
        return ResultWrapper.result(data);
    }

    @Callback(doc = "function(side:number, dbAddress:string, dbSlot:number):boolean -- Store an item stack representation of the block on the specified side in a database component.")
    public Object[] store(Context context, Arguments args) {
        Direction side = ExtendedArguments.checkSideAny(args, 0);
        Direction globalSide = host instanceof li.cil.oc.api.internal.Rotatable ?
                ((li.cil.oc.api.internal.Rotatable) host).toGlobal(side) : side;

        if (!node.tryChangeBuffer(-OCSettings.get().geolyzerScanCost))
            return ResultWrapper.result(null, "not enough energy");

        BlockPosition blockPos = position().offset(globalSide);
        BlockPos pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
        BlockState blockState = host.level().getBlockState(pos);
        Block block = blockState.getBlock();
        Item item = block.asItem();

        if (item == net.minecraft.world.item.Items.AIR)
            return ResultWrapper.result(null, "block has no registered item representation");

        final ItemStack finalStack = new ItemStack(item, 1);

        return DatabaseAccess.withDatabase(node, args.checkString(1), database -> {
            int toSlot = ExtendedArguments.checkSlot(args, database.data(), 2);
            boolean nonEmpty = database.getStackInSlot(toSlot) != null;
            database.setStackInSlot(toSlot, finalStack);
            return ResultWrapper.result(nonEmpty);
        });
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            if (machine.host() instanceof li.cil.oc.api.internal.Tablet && message.data().length >= 5) {
                CompoundTag nbt = (CompoundTag) message.data()[0];
                li.cil.oc.core.impl.util.BlockPosition blockPos = (li.cil.oc.core.impl.util.BlockPosition) message.data()[3];
                if (node.tryChangeBuffer(-OCSettings.get().geolyzerScanCost)) {
                    var delegate = EventHandlerDelegate.get();
                    Map<String, Object> data = delegate != null
                            ? delegate.postGeolyzerAnalyze(host, new HashMap<>(), blockPos.x(), blockPos.y(), blockPos.z())
                            : null;
                    if (data != null) {
                        for (Map.Entry<String, Object> e : data.entrySet()) {
                            if (e.getValue() instanceof Number)
                                nbt.putDouble(e.getKey(), ((Number) e.getValue()).doubleValue());
                            else if (e.getValue() instanceof String && !((String) e.getValue()).isEmpty())
                                nbt.putString(e.getKey(), (String) e.getValue());
                        }
                    }
                }
            }
        }
    }
}
