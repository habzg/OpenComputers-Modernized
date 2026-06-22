package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;

import java.util.List;

public class UpgradeRITEG extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeRITEG(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of((int) (Settings.get().ritegUpgradeEfficiency * 100));
    }
}
