package li.cil.oc.neoforge.common.item.traits;

import li.cil.oc.core.impl.common.item.AbstractItem;
import li.cil.oc.neoforge.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class SimpleItem extends AbstractItem {
    @SuppressWarnings("unused")
    public SimpleItem() {
        super(new Properties().stacksTo(64));
    }

    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
        tooltip.addAll(Tooltip.get(getClass().getSimpleName()));
    }
}
