package li.cil.oc.neoforge.common.item;

import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class NumPad extends DelegateItem {
    public NumPad(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
    }
}
