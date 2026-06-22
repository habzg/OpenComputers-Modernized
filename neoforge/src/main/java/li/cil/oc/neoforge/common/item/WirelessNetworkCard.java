package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;

public class WirelessNetworkCard extends DelegateItem implements ItemTier {

    private final int tier;

    public WirelessNetworkCard(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public String unlocalizedName() {
        return getClass().getSimpleName() + tier();
    }
}
