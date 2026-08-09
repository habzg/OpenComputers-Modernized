package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;

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
        return List.of((int) (OCSettings.get().solarGeneratorEfficiency * 100));
    }
}
