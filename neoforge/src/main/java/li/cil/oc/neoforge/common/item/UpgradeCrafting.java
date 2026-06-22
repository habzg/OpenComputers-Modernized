package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;

public class UpgradeCrafting extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeCrafting(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
