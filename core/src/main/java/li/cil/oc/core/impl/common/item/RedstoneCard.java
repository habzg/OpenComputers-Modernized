package li.cil.oc.core.impl.common.item;

import java.util.List;
import java.util.function.BooleanSupplier;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.util.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RedstoneCard extends DelegateItem implements ItemTier {
    public final int tier;

    private static BooleanSupplier projectRedAvailable = () -> false;

    public static void setProjectRedAvailable(BooleanSupplier supplier) {
        projectRedAvailable = supplier;
    }

    public RedstoneCard(Item.Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (tier == Tier.Two) {
            if (projectRedAvailable.getAsBoolean()) {
                tooltip.addAll(Tooltip.get(unlocalizedName() + ".ProjectRed"));
            }
        }
    }
}
