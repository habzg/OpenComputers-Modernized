package li.cil.oc.fabric.common.recipe;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.fabric.common.block.Print;
import li.cil.oc.fabric.common.init.Blocks;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import org.jetbrains.annotations.Nullable;

public final class ExtendedRecipe {
    @SuppressWarnings("unused")
    private ExtendedRecipe() {
    }

    public static void init() {
        li.cil.oc.core.impl.common.recipe.ExtendedRecipe.setAe2Check(() -> FabricLoader.getInstance().isModLoaded("ae2"));
        li.cil.oc.core.impl.common.recipe.ExtendedRecipe.setPrintHandler(ExtendedRecipe::handlePrintBlock);
    }

    @Nullable
    private static ItemStack handlePrintBlock(ItemStack craftedStack, CraftingInput inventory) {
        if (!(craftedStack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof Print)) {
            return null;
        }
        var data = new PrintData(craftedStack);
        var inputs = getItems(inventory);
        boolean isBeaconBaseInput = false;

        for (var stack : inputs) {
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() instanceof Print) {
                data.load(stack);
                if (stack.is(Blocks.BEACON_BASE_PRINT.asItem())) {
                    isBeaconBaseInput = true;
                }
            }
        }

        var glowstoneDust = new ItemStack(net.minecraft.world.item.Items.GLOWSTONE_DUST);
        var glowstone = new ItemStack(net.minecraft.world.level.block.Blocks.GLOWSTONE);

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

        if (isBeaconBaseInput && !craftedStack.is(Blocks.BEACON_BASE_PRINT.asItem())) {
            var correctStack = new ItemStack(Blocks.BEACON_BASE_PRINT.asItem(), craftedStack.getCount());
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
