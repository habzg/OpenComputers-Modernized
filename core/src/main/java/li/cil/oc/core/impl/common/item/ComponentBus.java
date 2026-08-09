package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;

public class ComponentBus extends DelegateItem implements ItemTier {
    private final int tier;

    public ComponentBus(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(OCSettings.get().cpuComponentSupport[tier]);
    }
}
