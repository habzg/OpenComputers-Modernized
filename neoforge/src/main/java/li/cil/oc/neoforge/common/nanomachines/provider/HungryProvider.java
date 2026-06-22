package li.cil.oc.neoforge.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.nanomachines.provider.ScalaProvider;
import li.cil.oc.neoforge.integration.util.DamageSourceWithRandomCause;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

public class HungryProvider extends ScalaProvider {
    public static final int FillCount = 10;

    public static final DamageSourceWithRandomCause HungryDamage = new DamageSourceWithRandomCause("oc.nanomachinesHungry", 3);

    public HungryProvider() {
        super("d697c24a-014c-4773-a288-23084a59e9e8");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        List<Behavior> list = new ArrayList<>();
        for (int i = 0; i < FillCount; i++) {
            list.add(new HungryBehavior(player));
        }
        return list;
    }

    @Override
    protected Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new HungryBehavior(player);
    }

    public static class HungryBehavior extends AbstractBehavior {
        public HungryBehavior(Player player) {
            super(player);
        }

        @Override
        public void onDisable(DisableReason reason) {
            if (reason == DisableReason.OutOfEnergy) {
                player.hurt(HungryDamage, Settings.get().nanomachinesHungryDamage);
                li.cil.oc.api.Nanomachines.getController(player).changeBuffer(Settings.get().nanomachinesHungryEnergyRestored);
            }
        }
    }
}
