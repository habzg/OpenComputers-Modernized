package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;

public class WirelessNetworkCard extends DelegateItem implements ItemTier {

    private final int tier;

    public WirelessNetworkCard(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

}
