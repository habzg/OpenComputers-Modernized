package li.cil.oc.neoforge.common.item.traits;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.common.item.AbstractItem;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class DelegateItem extends AbstractItem {
    public DelegateItem(Properties properties) {
        super(properties);
    }

    public String unlocalizedName() {
        return getClass().getSimpleName();
    }

    protected List<Object> tooltipData() {
        return List.of();
    }

    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
        tooltip.addAll(Tooltip.get(unlocalizedName(), tooltipData().toArray()));
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        if (this instanceof ItemTier tiered) {
            return Rarity.byTier(tiered.tier());
        }
        return super.getRarity(stack);
    }
}
