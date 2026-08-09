package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;

public class UpgradeContainerUpgrade extends DelegateItem implements ItemTier {

    private final int tier;

    public UpgradeContainerUpgrade(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(tier + 1);
    }
}
