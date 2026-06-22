package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class ComponentBus extends DelegateItem implements ItemTier {
    private final int tier;

    public ComponentBus(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        if (tier == Tier.Four) return Rarity.byTier(Tier.Four);
        return super.getRarity(stack);
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(Settings.get().cpuComponentSupport[tier]);
    }
}
