package li.cil.oc.neoforge.common.recipe;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

public final class ExtendedRecipe {
    private ExtendedRecipe() {
    }

    public static void init() {
        li.cil.oc.core.impl.common.recipe.ExtendedRecipe.setAe2Check(Mods.AppliedEnergistics2::isAvailable);
        li.cil.oc.core.impl.common.recipe.ExtendedRecipe.setPrintHandler(ExtendedRecipe::handlePrintBlock);
    }

    @Nullable
    private static ItemStack handlePrintBlock(ItemStack craftedStack, CraftingInput inventory) {
        if (!(craftedStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof li.cil.oc.neoforge.common.block.Print)) {
            return null;
        }
        var data = new PrintData(craftedStack);
        var inputs = getItems(inventory);
        boolean isBeaconBaseInput = false;

        for (var stack : inputs) {
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof li.cil.oc.neoforge.common.block.Print) {
                data.load(stack);
                if (stack.is(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get())) {
                    isBeaconBaseInput = true;
                }
            }
        }

        var glowstoneDust = new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST);
        var glowstone = new ItemStack(Blocks.GLOWSTONE);

        for (var stack : inputs) {
            if (ItemStack.isSameItem(glowstoneDust, stack)) {
                if (data.lightLevel >= 15) return ItemStack.EMPTY;
                data.lightLevel = Math.min(15, data.lightLevel + 1);
            }
            if (ItemStack.isSameItem(glowstone, stack)) {
                if (data.lightLevel >= 15) return ItemStack.EMPTY;
                data.lightLevel = Math.min(15, data.lightLevel + 4);
            }
        }

        if (isBeaconBaseInput && !craftedStack.is(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get())) {
            var correctStack = new ItemStack(li.cil.oc.neoforge.common.init.Items.BEACON_BASE_PRINT.get(), craftedStack.getCount());
            data.save(correctStack);
            return correctStack;
        }

        data.save(craftedStack);
        return null;
    }

    private static ItemStack[] getItems(CraftingInput inventory) {
        List<ItemStack> list = new ArrayList<>();
        for (int i = 0; i < inventory.size(); i++) {
            var stack = inventory.getItem(i);
            if (!stack.isEmpty()) list.add(stack);
        }
        return list.toArray(new ItemStack[0]);
    }
}
