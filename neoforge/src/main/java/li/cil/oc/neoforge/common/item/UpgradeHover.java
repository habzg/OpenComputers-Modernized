package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;

import java.util.List;

public class UpgradeHover extends DelegateItem implements ItemTier {

    private final int tier;

    public UpgradeHover(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(Settings.get().upgradeFlightHeight[tier]);
    }
}
