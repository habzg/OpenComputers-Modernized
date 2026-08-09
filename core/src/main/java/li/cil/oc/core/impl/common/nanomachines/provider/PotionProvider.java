package li.cil.oc.core.impl.common.nanomachines.provider;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

public class PotionProvider extends ScalaProvider {
    public static Set<MobEffect> PotionWhitelist = null;

    public PotionProvider() {
        super("c29e4eec-5a46-479a-9b3d-ad0f06da784a");
    }

    public static Set<MobEffect> getWhitelist() {
        if (PotionWhitelist == null) {
            PotionWhitelist = filterPotions(OCSettings.get().nanomachinePotionWhitelist);
        }
        return PotionWhitelist;
    }

    public static Set<MobEffect> filterPotions(Iterable<?> list) {
        Set<MobEffect> result = new HashSet<>();
        for (var entry : list) {
            if (entry instanceof String name) {
                var potion = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(net.minecraft.resources.ResourceLocation.tryParse(name));
                if (potion != null) {
                    result.add(potion);
                }
            } else if (entry instanceof Number id) {
                int intId = id.intValue();
                var potion = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.byId(intId);
                if (potion != null) {
                    result.add(potion);
                }
            }
        }
        return result;
    }

    public static boolean isPotionEligible(MobEffect potion) {
        return potion != null && getWhitelist().contains(potion);
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        List<Behavior> list = new ArrayList<>();
        for (var potion : net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT) {
            if (isPotionEligible(potion)) {
                list.add(new PotionBehavior(potion, player));
            }
        }
        return list;
    }

    @Override
    public void writeBehaviorToNBT(Behavior behavior, CompoundTag nbt) {
        if (behavior instanceof PotionBehavior potionBehavior) {
            var key = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(potionBehavior.potion.value());
            if (key != null) {
                nbt.putString("potionId", key.toString());
            }
        }
    }

    @Override
    public Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        var key = net.minecraft.resources.ResourceLocation.tryParse(nbt.getString("potionId"));
        if (key != null) {
            var potion = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.get(key);
            if (potion != null) {
                return new PotionBehavior(potion, player);
            }
        }
        return null;
    }

    public static class PotionBehavior extends AbstractBehavior {
        public static final int Duration = 600;
        public final Holder<MobEffect> potion;

        public PotionBehavior(MobEffect potion, Player player) {
            super(player);
            this.potion = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(potion);
        }

        public int amplifier(Player player) {
            return li.cil.oc.api.Nanomachines.getController(player).getInputCount(this) - 1;
        }

        @Override
        public String getNameHint() {
            var key = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getKey(potion.value());
            return key != null ? key.getPath() : potion.value().getDescriptionId().replace("effect.", "");
        }

        @Override
        public void onDisable(DisableReason reason) {
            player.removeEffect(potion);
        }

        @Override
        public void update() {
            player.addEffect(new MobEffectInstance(potion, Duration, amplifier(player), true, OCSettings.get().enableNanomachinePfx));
        }
    }
}
