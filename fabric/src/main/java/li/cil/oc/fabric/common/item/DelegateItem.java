package li.cil.oc.fabric.common.item;

import java.util.List;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class DelegateItem extends AbstractItem {
    @SuppressWarnings("unused")
    public DelegateItem(Properties properties) {
        super(properties);
    }

    public String unlocalizedName() {
        return getClass().getSimpleName();
    }

    protected List<Object> tooltipData() {
        return List.of();
    }

    @SuppressWarnings("unused")
    @Override
    public void tooltipBody(ItemStack stack, List<Component> tooltip) {
        tooltip.addAll(Tooltip.get(unlocalizedName(), tooltipData().toArray()));
    }
}
