package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;

import java.util.List;

public class UpgradeSolarGenerator extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeSolarGenerator(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of((int) (Settings.get().solarGeneratorEfficiency * 100));
    }
}
