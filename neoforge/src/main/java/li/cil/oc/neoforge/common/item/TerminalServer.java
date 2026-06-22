package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.Item;

import java.util.List;

public class TerminalServer extends DelegateItem {

    @SuppressWarnings("unused")
    public TerminalServer(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(Settings.get().terminalsPerServer);
    }
}
