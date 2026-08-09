package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;

public class UpgradeTankController extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeTankController(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }
}
