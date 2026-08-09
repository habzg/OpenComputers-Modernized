package li.cil.oc.core.impl.server.component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class MotionSensor extends AbstractManagedEnvironment implements DeviceInfo {
    private static final String SensitivityTag = OCSettings.namespace + "sensitivity";
    public final EnvironmentHost host;
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("motion_sensor")
            .withConnector()
            .create();
    private final int radius = 8;
    private final Map<LivingEntity, double[]> trackedEntities = new HashMap<>();
    private final Map<String, String> deviceInfo;
    private double sensitivity = 0.4;

    public MotionSensor(EnvironmentHost host) {
        this.host = host;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Generic, DeviceAttribute.Description, "Motion sensor", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor, DeviceAttribute.Product, "Blinker M1K0", DeviceAttribute.Capacity, String.valueOf(radius));
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    private boolean isServer() {
        return host.level() != null && !host.level().isClientSide;
    }

    @Override
    public boolean canUpdate() {
        return isServer();
    }

    @Override
    public void update() {
        super.update();
        if (host.level().getGameTime() % 10 == 0) {
            List<LivingEntity> entities = host.level().getEntitiesOfClass(LivingEntity.class, sensorBounds());
            Set<LivingEntity> visible = new HashSet<>();
            for (LivingEntity entity : entities) {
                if (entity.isAlive() && isInRange(entity) && isVisible(entity)) {
                    visible.add(entity);
                }
            }
            trackedEntities.keySet().retainAll(visible);
            for (LivingEntity entity : visible) {
                double[] prev = trackedEntities.get(entity);
                if (prev != null) {
                    if (entity.distanceToSqr(prev[0], prev[1], prev[2]) > sensitivity * sensitivity * 2) {
                        sendSignal(entity);
                    }
                } else {
                    sendSignal(entity);
                }
                trackedEntities.put(entity, new double[]{entity.getX(), entity.getY(), entity.getZ()});
            }
        }
    }

    private AABB sensorBounds() {
        double cx = host.xPosition() + 0.5;
        double cy = host.yPosition() + 0.5;
        double cz = host.zPosition() + 0.5;
        return new AABB(
                cx - radius, cy - radius, cz - radius,
                cx + radius, cy + radius, cz + radius);
    }

    private boolean isInRange(LivingEntity entity) {
        return entity.distanceToSqr(host.xPosition() + 0.5, host.yPosition() + 0.5, host.zPosition() + 0.5) <= radius * radius;
    }

    private boolean isClearPath(Vec3 target) {
        Vec3 origin = new Vec3(host.xPosition(), host.yPosition(), host.zPosition());
        Vec3 path = target.subtract(origin).normalize();
        Vec3 eye = origin.add(path.x, path.y, path.z);
        ClipContext ctx = new ClipContext(eye, target, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        BlockHitResult hit = host.level().clip(ctx);
        return hit.getType() == HitResult.Type.MISS;
    }

    private boolean isVisible(LivingEntity entity) {
        MobEffectInstance invis = entity.getEffect(MobEffects.INVISIBILITY);
        if (invis != null) return false;
        Vec3 target = new Vec3(entity.getX(), entity.getY(), entity.getZ());
        return isClearPath(target) || isClearPath(target.add(0, entity.getEyeHeight(), 0));
    }

    private void sendSignal(LivingEntity entity) {
        if (OCSettings.get().inputUsername) {
            node.sendToReachable("computer.signal", "motion",
                    entity.getX() - (host.xPosition() + 0.5),
                    entity.getY() - (host.yPosition() + 0.5),
                    entity.getZ() - (host.zPosition() + 0.5),
                    entity.getName().getString());
        } else {
            node.sendToReachable("computer.signal", "motion",
                    entity.getX() - (host.xPosition() + 0.5),
                    entity.getY() - (host.yPosition() + 0.5),
                    entity.getZ() - (host.zPosition() + 0.5));
        }
    }

    @Callback(direct = true, doc = "function():number -- Gets the current sensor sensitivity.")
    public Object[] getSensitivity(Context context, Arguments args) {
        return ResultWrapper.result(sensitivity);
    }

    @Callback(direct = true, doc = "function(value:number):number -- Sets the sensor's sensitivity. Returns the old value.")
    public Object[] setSensitivity(Context context, Arguments args) {
        double oldValue = sensitivity;
        sensitivity = Math.max(0.2, args.checkDouble(0));
        return ResultWrapper.result(oldValue);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        sensitivity = nbt.getDouble(SensitivityTag);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putDouble(SensitivityTag, sensitivity);
    }
}
