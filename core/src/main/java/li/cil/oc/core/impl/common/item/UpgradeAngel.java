package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;

public class UpgradeAngel extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeAngel(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
