package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.traits.GPULike;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.common.item.traits.CPULike;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class APU extends DelegateItem implements ItemTier, CPULike, GPULike {
    private final int tier;

    public APU(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int cpuTier() {
        return Math.min(Tier.Three, tier + 1);
    }

    @Override
    public int cpuTierForComponents() {
        return tier + 1;
    }

    @Override
    public int gpuTier() {
        return tier;
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        if (tier == Tier.Three) return Rarity.byTier(Tier.Four);
        return super.getRarity(stack);
    }

    @Override
    protected List<Object> tooltipData() {
        List<Object> data = new ArrayList<>();
        data.add(Settings.get().cpuComponentSupport[cpuTierForComponents()]);
        data.addAll(gpuTooltipData());
        return data;
    }
}
