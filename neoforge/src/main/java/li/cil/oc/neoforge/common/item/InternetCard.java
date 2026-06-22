package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.Item;

public class InternetCard extends DelegateItem implements ItemTier {
    public InternetCard(Item.Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
