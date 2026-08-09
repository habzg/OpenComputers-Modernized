package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;

public class DataCard extends DelegateItem implements ItemTier {
    private final int tier;

    public DataCard(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public String unlocalizedName() {
        return "DataCard" + tier;
    }
}
