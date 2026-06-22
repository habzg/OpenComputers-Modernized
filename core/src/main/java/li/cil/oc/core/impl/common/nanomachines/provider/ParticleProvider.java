package li.cil.oc.core.impl.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.PlayerUtils;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ParticleProvider extends ScalaProvider {
    public ParticleProvider() {
        super("b48c4bbd-51bb-4915-9367-16cff3220e4b");
    }

    private static final Set<String> VANILLA_PARTICLES = Set.of(
            "minecraft:firework", "minecraft:entity_effect", "minecraft:witch",
            "minecraft:smoke", "minecraft:note", "minecraft:enchant",
            "minecraft:flame", "minecraft:lava", "minecraft:splash",
            "minecraft:dust", "minecraft:item_slime", "minecraft:heart",
            "minecraft:happy_villager"
    );

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        List<Behavior> list = new ArrayList<>();
        for (var entry : BuiltInRegistries.PARTICLE_TYPE.entrySet()) {
            if (entry.getValue() instanceof ParticleOptions && VANILLA_PARTICLES.contains(entry.getKey().toString())) {
                list.add(new ParticleBehavior(entry.getKey().toString(), player));
            }
        }
        return list;
    }

    @Override
    public void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof ParticleBehavior particles) {
            nbt.putString("effectName", particles.effectName);
        }
    }

    @Override
    public Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        String effectName = nbt.getString("effectName");
        return new ParticleBehavior(effectName, player);
    }

    public static class ParticleBehavior extends AbstractBehavior {
        public final String effectName;

        public ParticleBehavior(String effectName, Player player) {
            super(player);
            this.effectName = effectName;
        }

        @Override
        public String getNameHint() {
            var idx = effectName.indexOf(':');
            return "particles." + (idx >= 0 ? effectName.substring(idx + 1) : effectName);
        }

        @Override
        public void update() {
            var world = player.level();
            if (world.isClientSide && Settings.get().enableNanomachinePfx) {
                PlayerUtils.spawnParticleAround(player, effectName, li.cil.oc.api.Nanomachines.getController(player).getInputCount(this) * 0.25);
            }
        }
    }
}
