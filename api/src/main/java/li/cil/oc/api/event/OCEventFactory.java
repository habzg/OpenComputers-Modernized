package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;

import java.util.Map;

/**
 * Loader-independent factory for creating OC event instances.
 */
public abstract class OCEventFactory {
    private static OCEventFactory instance;

    /**
     * Set the loader-specific factory instance. Called by the
     * loader-specific module during initialization.
     *
     * @param factory the factory instance, or {@code null} to clear
     */
    public static void setInstance(OCEventFactory factory) {
        instance = factory;
    }

    /**
     * Get the loader-specific factory instance.
     *
     * @return the factory instance, or {@code null} if not yet set
     */
    @SuppressWarnings("unused")
    public static OCEventFactory getInstance() {
        return instance;
    }

    /**
     * Convenience accessor that throws if no factory has been set.
     *
     * @return the factory instance
     * @throws IllegalStateException if no factory has been registered
     */
    public static OCEventFactory get() {
        if (instance == null) {
            throw new IllegalStateException("OCEventFactory has not been initialized");
        }
        return instance;
    }

    public abstract RobotMoveEvent.Pre createRobotMoveEventPre(Agent agent, net.minecraft.core.Direction direction);

    public abstract RobotMoveEvent.Post createRobotMoveEventPost(Agent agent, net.minecraft.core.Direction direction);

    @SuppressWarnings("unused")
    public abstract RobotAttackEntityEvent.Pre createRobotAttackEntityEventPre(Agent agent, net.minecraft.world.entity.Entity target);

    @SuppressWarnings("unused")
    public abstract RobotAttackEntityEvent.Post createRobotAttackEntityEventPost(Agent agent, net.minecraft.world.entity.Entity target);

    @SuppressWarnings("unused")
    public abstract RobotBreakBlockEvent.Pre createRobotBreakBlockEventPre(Agent agent, Level world, int x, int y, int z, double breakTime);

    @SuppressWarnings("unused")
    public abstract RobotBreakBlockEvent.Post createRobotBreakBlockEventPost(Agent agent, double experience);

    @SuppressWarnings("unused")
    public abstract RobotPlaceBlockEvent.Pre createRobotPlaceBlockEventPre(Agent agent, net.minecraft.world.item.ItemStack stack, Level world, int x, int y, int z);

    @SuppressWarnings("unused")
    public abstract RobotPlaceBlockEvent.Post createRobotPlaceBlockEventPost(Agent agent, net.minecraft.world.item.ItemStack stack, Level world, int x, int y, int z);

    @SuppressWarnings("unused")
    public abstract RobotAnalyzeEvent createRobotAnalyzeEvent(Agent agent, Player player);

    @SuppressWarnings("unused")
    public abstract RobotExhaustionEvent createRobotExhaustionEvent(Agent agent, double exhaustion);

    public abstract RobotPlaceInAirEvent createRobotPlaceInAirEvent(Agent agent);

    @SuppressWarnings("unused")
    public abstract RobotUsedToolEvent.ComputeDamageRate createRobotUsedToolComputeDamageRateEvent(Agent agent, net.minecraft.world.item.ItemStack toolBeforeUse, net.minecraft.world.item.ItemStack toolAfterUse, double damageRate);

    @SuppressWarnings("unused")
    public abstract RobotUsedToolEvent.ApplyDamageRate createRobotUsedToolApplyDamageRateEvent(Agent agent, net.minecraft.world.item.ItemStack toolBeforeUse, net.minecraft.world.item.ItemStack toolAfterUse, double damageRate);

    @SuppressWarnings("unused")
    public abstract RobotRenderEvent createRobotRenderEvent(Agent agent, RobotRenderEvent.MountPoint[] mountPoints);

    public abstract FileSystemAccessEvent.Server createFileSystemAccessEventServer(String sound, BlockEntity tileEntity, Node node);

    public abstract FileSystemAccessEvent.Server createFileSystemAccessEventServer(String sound, Level world, double x, double y, double z, Node node);

    @SuppressWarnings("unused")
    public abstract FileSystemAccessEvent.Client createFileSystemAccessEventClient(String sound, BlockEntity tileEntity, CompoundTag data);

    @SuppressWarnings("unused")
    public abstract FileSystemAccessEvent.Client createFileSystemAccessEventClient(String sound, Level world, double x, double y, double z, CompoundTag data);

    public abstract NetworkActivityEvent.Server createNetworkActivityEventServer(BlockEntity tileEntity, Node node);

    public abstract NetworkActivityEvent.Server createNetworkActivityEventServer(Level world, double x, double y, double z, Node node);

    @SuppressWarnings("unused")
    public abstract NetworkActivityEvent.Client createNetworkActivityEventClient(BlockEntity tileEntity, CompoundTag data);

    @SuppressWarnings("unused")
    public abstract NetworkActivityEvent.Client createNetworkActivityEventClient(Level world, double x, double y, double z, CompoundTag data);

    public abstract GeolyzerEvent.Scan createGeolyzerScanEvent(EnvironmentHost host, Map<?, ?> options, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    public abstract GeolyzerEvent.Analyze createGeolyzerAnalyzeEvent(EnvironmentHost host, Map<?, ?> options, int x, int y, int z);

    @SuppressWarnings("unused")
    public abstract RackMountableRenderEvent.Block createRackMountableRenderEventBlock(li.cil.oc.api.internal.Rack rack, int mountable, CompoundTag data, net.minecraft.core.Direction side, Object renderer);

    @SuppressWarnings("unused")
    public abstract RackMountableRenderEvent.BlockEntity createRackMountableRenderEventBlockEntity(li.cil.oc.api.internal.Rack rack, int mountable, CompoundTag data, float v0, float v1);

    @SuppressWarnings("unused")
    public abstract SignChangeEvent.Pre createSignChangeEventPre(SignBlockEntity tileEntity, String[] lines);

    @SuppressWarnings("unused")
    public abstract SignChangeEvent.Post createSignChangeEventPost(SignBlockEntity tileEntity, String[] lines);
}
