package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import net.minecraft.world.item.Item;

public class NetworkCard extends DelegateItem implements ItemTier {
    public NetworkCard(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
