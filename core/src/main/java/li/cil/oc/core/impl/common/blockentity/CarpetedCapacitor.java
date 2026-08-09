package li.cil.oc.core.impl.common.blockentity;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Ocelot;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class CarpetedCapacitor extends Capacitor implements DeviceInfo {


    public static BlockEntityType<CarpetedCapacitor> TYPE;
    private final Random rng = new Random();
    private final double chance = OCSettings.get().carpetDamageChance;
    private final Map<String, String> deviceInfo = Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Battery",
            DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
            DeviceInfo.DeviceAttribute.Product, "CarpetedCapBank3x",
            DeviceInfo.DeviceAttribute.Capacity, String.valueOf(maxCapacity())
    );
    private long nextChanceTime = 0;

    public CarpetedCapacitor(BlockPos pos, BlockState state) {
        super(TYPE, pos, state);
    }

    @Override
    public java.util.Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    public void updateEntity() {
        super.updateEntity();
        if ((level().getGameTime() + hashCode()) % 20 == 0) {
            var entities = level().getEntitiesOfClass(LivingEntity.class, capacitorPowerBounds());
            var sheepEntities = new HashSet<LivingEntity>();
            for (var e : entities) if (e.isAlive() && e instanceof Sheep) sheepEntities.add(e);
            var sheepPower = energyFromGroup(sheepEntities, OCSettings.get().sheepPower);
            var ocelotEntities = new HashSet<LivingEntity>();
            for (var e : entities) if (e.isAlive() && e instanceof Ocelot) ocelotEntities.add(e);
            var ocelotPower = energyFromGroup(ocelotEntities, OCSettings.get().ocelotPower);
            var totalPower = sheepPower + ocelotPower;
            if (totalPower > 0) {
                ((li.cil.oc.api.network.Connector) node).changeBuffer(totalPower);
            }
        }
    }

    private double energyFromGroup(java.util.HashSet<LivingEntity> entities, double power) {
        if (entities.size() < 2) return 0;
        if (chance > 0 && nextChanceTime < level().getGameTime()) {
            for (var entity : entities) {
                if (rng.nextDouble() < chance) {
                    entity.hurt(level().damageSources().generic(), 1);
                    if (entity instanceof net.minecraft.world.entity.Mob mob) {
                        mob.setLastHurtByMob(mob);
                    }
                    var motion = entity.getDeltaMovement();
                    entity.setDeltaMovement(motion.x / 2, motion.y / 2 + 0.4, motion.z / 2);
                    nextChanceTime = level().getGameTime() + (20 * 60);
                    break;
                }
            }
        }
        return power;
    }

    private AABB capacitorPowerBounds() {
        return new AABB(worldPosition.relative(Direction.UP));
    }
}
