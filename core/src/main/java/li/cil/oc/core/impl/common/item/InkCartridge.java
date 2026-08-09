package li.cil.oc.core.impl.common.item;

import net.minecraft.world.item.Item;

public class InkCartridge extends DelegateItem {
    public InkCartridge(Item.Properties properties) {
        super(properties.stacksTo(1));
    }


}
