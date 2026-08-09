package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;

public class UpgradeInventory extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeInventory(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
