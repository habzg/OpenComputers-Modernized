package li.cil.oc.core.impl.common.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ArrowKeys extends DelegateItem {
    public ArrowKeys(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
    }
}
