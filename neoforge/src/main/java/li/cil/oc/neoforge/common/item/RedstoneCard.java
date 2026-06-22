package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class RedstoneCard extends DelegateItem implements ItemTier {
    public final int tier;

    public RedstoneCard(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (tier == Tier.Two) {
            if (Mods.ProjectRedTransmission.isAvailable()) {
                tooltip.addAll(Tooltip.get(unlocalizedName() + ".ProjectRed"));
            }
        }
    }
}
