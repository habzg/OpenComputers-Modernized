package li.cil.oc.core.impl.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public final class PlayerUtils {

    public static CompoundTag persistedData(Player player) {
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains(Player.PERSISTED_NBT_TAG)) {
            nbt.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return nbt.getCompound(Player.PERSISTED_NBT_TAG);
    }

    public static void spawnParticleAround(Player player, String effectName, double chance) {
        RandomSource rng = player.level().random;
        if (chance >= 1 || rng.nextDouble() < chance) {
            AABB bounds = player.getBoundingBox();
            double x = bounds.minX + (bounds.maxX - bounds.minX) * rng.nextDouble() * 1.5;
            double y = bounds.minY + (bounds.maxY - bounds.minY) * rng.nextDouble() * 0.5;
            double z = bounds.minZ + (bounds.maxZ - bounds.minZ) * rng.nextDouble() * 1.5;
            var key = net.minecraft.resources.ResourceLocation.tryParse(effectName);
            var particleType = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(key);
            if (particleType instanceof net.minecraft.core.particles.ParticleOptions particle) {
                player.level().addParticle(particle, x, y, z, 0, 0, 0);
            }
        }
    }
}
