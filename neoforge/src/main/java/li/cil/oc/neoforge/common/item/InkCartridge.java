package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class InkCartridge extends DelegateItem {
    public InkCartridge(Item.Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull ItemStack getCraftingRemainingItem(@NotNull ItemStack stack) {
        if (Items.get(stack) == Items.get(Constants.ItemName.InkCartridge))
            return Items.get(Constants.ItemName.InkCartridgeEmpty).createItemStack(1);
        return super.getCraftingRemainingItem(stack);
    }

    @Override
    public boolean hasCraftingRemainingItem(@NotNull ItemStack stack) {
        return true;
    }
}
