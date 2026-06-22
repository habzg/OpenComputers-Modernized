package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.GeolyzerEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.event.OCEventFactory;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.event.RobotAttackEntityEvent;
import li.cil.oc.api.event.RobotBreakBlockEvent;
import li.cil.oc.api.event.RobotExhaustionEvent;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.api.event.RobotPlaceBlockEvent;
import li.cil.oc.api.event.RobotPlaceInAirEvent;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.event.SignChangeEvent;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Map;

public final class NeoForgeOCEventFactory extends OCEventFactory {
    @Override
    public RobotMoveEvent.Pre createRobotMoveEventPre(Agent agent, Direction direction) {
        return new RobotMoveEventImpl.Pre(agent, direction);
    }

    @Override
    public RobotMoveEvent.Post createRobotMoveEventPost(Agent agent, Direction direction) {
        return new RobotMoveEventImpl.Post(agent, direction);
    }

    @Override
    public RobotAttackEntityEvent.Pre createRobotAttackEntityEventPre(Agent agent, net.minecraft.world.entity.Entity target) {
        return new RobotAttackEntityEventImpl.Pre(agent, target);
    }

    @Override
    public RobotAttackEntityEvent.Post createRobotAttackEntityEventPost(Agent agent, net.minecraft.world.entity.Entity target) {
        return new RobotAttackEntityEventImpl.Post(agent, target);
    }

    @Override
    public RobotBreakBlockEvent.Pre createRobotBreakBlockEventPre(Agent agent, Level world, int x, int y, int z, double breakTime) {
        return new RobotBreakBlockEventImpl.Pre(agent, world, x, y, z, breakTime);
    }

    @Override
    public RobotBreakBlockEvent.Post createRobotBreakBlockEventPost(Agent agent, double experience) {
        return new RobotBreakBlockEventImpl.Post(agent, experience);
    }

    @Override
    public RobotPlaceBlockEvent.Pre createRobotPlaceBlockEventPre(Agent agent, ItemStack stack, Level world, int x, int y, int z) {
        return new RobotPlaceBlockEventImpl.Pre(agent, stack, world, x, y, z);
    }

    @Override
    public RobotPlaceBlockEvent.Post createRobotPlaceBlockEventPost(Agent agent, ItemStack stack, Level world, int x, int y, int z) {
        return new RobotPlaceBlockEventImpl.Post(agent, stack, world, x, y, z);
    }

    @Override
    public RobotAnalyzeEvent createRobotAnalyzeEvent(Agent agent, Player player) {
        return new RobotAnalyzeEventImpl(agent, player);
    }

    @Override
    public RobotExhaustionEvent createRobotExhaustionEvent(Agent agent, double exhaustion) {
        return new RobotExhaustionEventImpl(agent, exhaustion);
    }

    @Override
    public RobotPlaceInAirEvent createRobotPlaceInAirEvent(Agent agent) {
        return new RobotPlaceInAirEventImpl(agent);
    }

    @Override
    public RobotUsedToolEvent.ComputeDamageRate createRobotUsedToolComputeDamageRateEvent(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
        return new RobotUsedToolEventImpl.ComputeDamageRate(agent, toolBeforeUse, toolAfterUse, damageRate);
    }

    @Override
    public RobotUsedToolEvent.ApplyDamageRate createRobotUsedToolApplyDamageRateEvent(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
        return new RobotUsedToolEventImpl.ApplyDamageRate(agent, toolBeforeUse, toolAfterUse, damageRate);
    }

    @Override
    public li.cil.oc.api.event.RobotRenderEvent createRobotRenderEvent(Agent agent, RobotRenderEvent.MountPoint[] mountPoints) {
        return new RobotRenderEventImpl(agent, mountPoints);
    }

    @Override
    public FileSystemAccessEvent.Server createFileSystemAccessEventServer(String sound, BlockEntity tileEntity, Node node) {
        return new FileSystemAccessEventImpl.Server(sound, tileEntity, node);
    }

    @Override
    public FileSystemAccessEvent.Server createFileSystemAccessEventServer(String sound, Level world, double x, double y, double z, Node node) {
        return new FileSystemAccessEventImpl.Server(sound, world, x, y, z, node);
    }

    @Override
    public FileSystemAccessEvent.Client createFileSystemAccessEventClient(String sound, BlockEntity tileEntity, CompoundTag data) {
        return new FileSystemAccessEventImpl.Client(sound, tileEntity, data);
    }

    @Override
    public FileSystemAccessEvent.Client createFileSystemAccessEventClient(String sound, Level world, double x, double y, double z, CompoundTag data) {
        return new FileSystemAccessEventImpl.Client(sound, world, x, y, z, data);
    }

    @Override
    public NetworkActivityEvent.Server createNetworkActivityEventServer(BlockEntity tileEntity, Node node) {
        return new NetworkActivityEventImpl.Server(tileEntity, node);
    }

    @Override
    public NetworkActivityEvent.Server createNetworkActivityEventServer(Level world, double x, double y, double z, Node node) {
        return new NetworkActivityEventImpl.Server(world, x, y, z, node);
    }

    @Override
    public NetworkActivityEvent.Client createNetworkActivityEventClient(BlockEntity tileEntity, CompoundTag data) {
        return new NetworkActivityEventImpl.Client(tileEntity, data);
    }

    @Override
    public NetworkActivityEvent.Client createNetworkActivityEventClient(Level world, double x, double y, double z, CompoundTag data) {
        return new NetworkActivityEventImpl.Client(world, x, y, z, data);
    }

    @Override
    public GeolyzerEvent.Scan createGeolyzerScanEvent(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new GeolyzerEventImpl.Scan(host, options, minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public GeolyzerEvent.Analyze createGeolyzerAnalyzeEvent(EnvironmentHost host, Map<?, ?> options, int x, int y, int z) {
        return new GeolyzerEventImpl.Analyze(host, options, x, y, z);
    }

    @Override
    public RackMountableRenderEvent.Block createRackMountableRenderEventBlock(Rack rack, int mountable, CompoundTag data, Direction side, Object renderer) {
        return new RackMountableRenderEventImpl.Block(rack, mountable, data, side, renderer);
    }

    @Override
    public RackMountableRenderEvent.BlockEntity createRackMountableRenderEventBlockEntity(Rack rack, int mountable, CompoundTag data, float v0, float v1) {
        return new RackMountableRenderEventImpl.BlockEntity(rack, mountable, data, v0, v1);
    }

    @Override
    public SignChangeEvent.Pre createSignChangeEventPre(SignBlockEntity tileEntity, String[] lines) {
        return new SignChangeEventImpl.Pre(tileEntity, lines);
    }

    @Override
    public SignChangeEvent.Post createSignChangeEventPost(SignBlockEntity tileEntity, String[] lines) {
        return new SignChangeEventImpl.Post(tileEntity, lines);
    }
}
