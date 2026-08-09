package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import net.minecraft.world.item.Item;

public class TabletCase extends DelegateItem implements ItemTier {
    private final int tier;

    @SuppressWarnings("unused")
    public TabletCase(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }
}
