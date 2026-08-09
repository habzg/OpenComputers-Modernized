package li.cil.oc.core.impl.common.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ButtonGroup extends DelegateItem {
    public ButtonGroup(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
    }
}
