package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;


import java.util.Map;
import java.util.function.Consumer;

public abstract class UpgradeMFBase extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    public final EnvironmentHost host;
    public final BlockPosition coord;
    public final Direction dir;

    public final Node node = Network.newNode(this, Visibility.None)
            .withConnector()
            .create();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Bus);
        put(DeviceAttribute.Description, "Remote Adapter");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.Scummtech);
        put(DeviceAttribute.Product, "ERR NAME NOT FOUND");
    }};
    protected Environment otherEnv = null;
    protected li.cil.oc.api.network.ManagedEnvironment otherDrvEnv = null;
    protected li.cil.oc.api.driver.SidedBlock otherDrvDriver = null;
    protected BlockData blockData = null;

    public UpgradeMFBase(EnvironmentHost host, BlockPosition coord, Direction dir) {
        this.host = host;
        this.coord = coord;
        this.dir = dir;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    protected abstract void connectToTileNode(BlockEntity tile, Consumer<Node> consumer);

    protected abstract void registerBlockChangeListener();

    protected abstract void unregisterBlockChangeListener();

    protected abstract boolean consumeEnergy(double amount);

    protected void updateBoundState() {
        if (node.network() != null && coord.level() != null && coord.level().dimension().location().hashCode() == host.level().dimension().location().hashCode() && coord.toVec3().distanceTo(new Vec3(host.xPosition(), host.yPosition(), host.zPosition())) <= Settings.get().mfuRange) {
            BlockPos blockPos = new BlockPos(coord.x(), coord.y(), coord.z());
            BlockEntity te = host.level().getBlockEntity(blockPos);
            if (otherEnv != null && otherEnv instanceof BlockEntity) {
                connectToTileNode((BlockEntity) otherEnv, node::disconnect);
                otherEnv = null;
            }
            if (te instanceof Environment) {
                if (otherDrvEnv != null) {
                    node.disconnect(otherDrvEnv.node());
                    otherDrvEnv.save(blockData.data, host.level().registryAccess());
                    if (otherDrvEnv.node() != null) otherDrvEnv.node().remove();
                    otherDrvEnv = null;
                    otherDrvDriver = null;
                }
                otherEnv = (Environment) te;
                connectToTileNode(te, node::connect);
            } else {
                var w = coord.level();
                li.cil.oc.api.driver.SidedBlock newDriver = li.cil.oc.api.API.driver.driverFor(w, blockPos, dir);
                if (newDriver != null) {
                    if (otherDrvDriver != newDriver) {
                        if (otherDrvEnv != null) {
                            node.disconnect(otherDrvEnv.node());
                            otherDrvEnv = null;
                            otherDrvDriver = null;
                            blockData = null;
                        }
                        var environment = newDriver.createEnvironment(w, coord.x(), coord.y(), coord.z(), dir);
                        if (environment != null) {
                            otherDrvEnv = environment;
                            otherDrvDriver = newDriver;
                            blockData = new BlockData(environment.getClass().getName(), new CompoundTag());
                            node.connect(environment.node());
                        }
                    }
                } else {
                    if (otherDrvEnv != null) {
                        node.disconnect(otherDrvEnv.node());
                        otherDrvEnv.save(blockData.data, host.level().registryAccess());
                        if (otherDrvEnv.node() != null) otherDrvEnv.node().remove();
                        otherDrvEnv = null;
                        otherDrvDriver = null;
                    }
                }
            }
        }
    }

    private void disconnect() {
        if (otherEnv != null && otherEnv instanceof BlockEntity) {
            connectToTileNode((BlockEntity) otherEnv, node::disconnect);
            otherEnv = null;
        }
        if (otherDrvEnv != null) {
            node.disconnect(otherDrvEnv.node());
            otherDrvEnv.save(blockData.data, host.level().registryAccess());
            if (otherDrvEnv.node() != null) otherDrvEnv.node().remove();
            otherDrvEnv = null;
            otherDrvDriver = null;
        }
    }

    @Override
    public void update() {
        super.update();
        if (otherDrvEnv != null && otherDrvEnv.canUpdate()) {
            otherDrvEnv.update();
        }
        if (host.level().getGameTime() % Settings.get().tickFrequency == 0) {
            if (!consumeEnergy(Settings.get().mfuCost * Settings.get().tickFrequency *
                    coord.toVec3().distanceTo(new Vec3(host.xPosition(), host.yPosition(), host.zPosition())))) {
                disconnect();
            }
        }
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            registerBlockChangeListener();
            updateBoundState();
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (otherEnv != null && otherEnv instanceof BlockEntity) {
            connectToTileNode((BlockEntity) otherEnv, otherNode -> {
                if (node == otherNode) otherEnv = null;
            });
        }
        if (otherDrvEnv != null && node == otherDrvEnv.node()) {
            otherDrvEnv = null;
            otherDrvDriver = null;
        }
        if (node == this.node) {
            unregisterBlockChangeListener();
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        CompoundTag blockNbt = nbt.getCompound(Settings.namespace + "adapter.block");
        if (blockNbt.contains("name") && blockNbt.contains("data")) {
            blockData = new BlockData(blockNbt.getString("name"), blockNbt.getCompound("data"));
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        CompoundTag blockNbt = new CompoundTag();
        if (blockData != null) {
            if (otherDrvEnv != null) otherDrvEnv.save(blockData.data, provider);
            blockNbt.putString("name", blockData.name);
            blockNbt.put("data", blockData.data);
        }
        nbt.put(Settings.namespace + "adapter.block", blockNbt);
    }

    protected record BlockData(String name, CompoundTag data) {
    }
}
