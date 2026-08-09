package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.world.item.Item;

public class TerminalServer extends DelegateItem {

    @SuppressWarnings("unused")
    public TerminalServer(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(OCSettings.get().terminalsPerServer);
    }
}
