package li.cil.oc.core.impl.server.component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Rotatable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.NavigationUpgradeData;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.phys.Vec3;

public abstract class UpgradeNavigationBase extends AbstractManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;

    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("navigation", Visibility.Neighbors)
            .withConnector()
            .create();
    public final NavigationUpgradeData data = new NavigationUpgradeData();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Navigation upgrade");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "PathFinder v3");
    }};

    public UpgradeNavigationBase(EnvironmentHost host) {
        this.host = host;
        deviceInfo.put(DeviceAttribute.Capacity, String.valueOf(data.getSize(host.level())));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    protected abstract boolean consumeEnergy(double amount);

    protected abstract List<WaypointInfo> queryWaypoints(BlockPosition pos, double range);

    @Callback(doc = "function():number, number, number -- Get the current relative position of the robot.")
    public Object[] getPosition(Context context, Arguments args) {
        NavigationUpgradeData.MapData info = data.mapData(host.level());
        int size = data.getSize(host.level());
        double relativeX = host.xPosition() - info.xCenter();
        double relativeZ = host.zPosition() - info.zCenter();

        if (Math.abs(relativeX) <= (double) size / 2 && Math.abs(relativeZ) <= (double) size / 2)
            return ResultWrapper.result(relativeX, host.yPosition(), relativeZ);
        else
            return ResultWrapper.result(null, "out of range");
    }

    @Callback(doc = "function():number -- Get the current orientation of the robot.")
    public Object[] getFacing(Context context, Arguments args) {
        if (host instanceof Rotatable) {
            return ResultWrapper.result(((Rotatable) host).facing().ordinal());
        }
        return ResultWrapper.result(0);
    }

    @Callback(doc = "function():number -- Get the operational range of the navigation upgrade.")
    public Object[] getRange(Context context, Arguments args) {
        return ResultWrapper.result(data.getSize(host.level()) / 2);
    }

    @Callback(doc = "function(range:number):table -- Find waypoints in the specified range.")
    public Object[] findWaypoints(Context context, Arguments args) {
        double range = Math.clamp(args.checkDouble(0), 0, OCSettings.get().maxWirelessRange[Tier.Two]);
        if (range <= 0) return ResultWrapper.result();
        if (!consumeEnergy(range * OCSettings.get().wirelessCostPerRange[Tier.Two] * 0.25))
            return ResultWrapper.result(null, "not enough energy");
        context.pause(0.5);
        BlockPosition position = BlockPosition.apply(host);
        Vec3 positionVec = position.toVec3();
        List<WaypointInfo> waypoints = queryWaypoints(position, range);
        Object[] waypointsArray = waypoints.stream()
                .filter(waypoint -> new Vec3(waypoint.position().x(), waypoint.position().y(), waypoint.position().z())
                        .distanceTo(positionVec) <= range)
                .map(waypoint -> {
                    Vec3 waypointPos = waypoint.position().offset(waypoint.facing()).toVec3();
                    Vec3 delta = new Vec3(
                            waypointPos.x - positionVec.x,
                            waypointPos.y - positionVec.y,
                            waypointPos.z - positionVec.z
                    );
                    Map<String, Object> map = new HashMap<>();
                    map.put("position", new double[]{delta.x, delta.y, delta.z});
                    map.put("redstone", waypoint.maxInput());
                    map.put("label", waypoint.label());
                    map.put("address", waypoint.address());
                    return map;
                })
                .toArray();
        return ResultWrapper.result(waypointsArray);
    }

    @Override
    public void onMessage(Message message) {
        super.onMessage(message);
        if ("tablet.use".equals(message.name()) && message.source().host() instanceof li.cil.oc.api.machine.Machine machine) {
            if (machine.host() instanceof li.cil.oc.api.internal.Tablet && message.data().length >= 7) {
                Object[] data = message.data();
                CompoundTag nbt = (CompoundTag) data[0];
                BlockPosition blockPos = (BlockPosition) data[3];
                NavigationUpgradeData.MapData info = this.data.mapData(host.level());
                nbt.putInt("posX", blockPos.x() - info.xCenter());
                nbt.putInt("posY", blockPos.y());
                nbt.putInt("posZ", blockPos.z() - info.zCenter());
            }
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        data.load(nbt, provider);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        data.save(nbt, provider);
    }
}
