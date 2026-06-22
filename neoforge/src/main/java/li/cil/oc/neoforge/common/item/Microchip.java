package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class Microchip extends DelegateItem {
    private final int tier;

    public Microchip(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        return Rarity.byTier(tier);
    }
}
