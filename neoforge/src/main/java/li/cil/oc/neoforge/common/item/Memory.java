package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.Item;

public class Memory extends DelegateItem implements ItemTier {
    private final int tier;

    public Memory(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }
}
