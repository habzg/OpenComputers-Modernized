package li.cil.oc.neoforge.common.item;

import java.util.List;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class SimpleItem extends AbstractItem {
    @SuppressWarnings("unused")
    public SimpleItem() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public void tooltipBody(ItemStack ignoredStack, List<Component> tooltip) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName()));
    }
}
