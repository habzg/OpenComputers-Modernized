package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.common.item.traits.GPULike;

public class GraphicsCard extends DelegateItem implements ItemTier, GPULike {
    private final int tier;

    public GraphicsCard(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int gpuTier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return gpuTooltipData();
    }
}
