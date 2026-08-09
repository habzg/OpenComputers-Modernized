package li.cil.oc.fabric.server.component;

import li.cil.oc.api.internal.Robot;
import li.cil.oc.core.impl.common.recipe.ExtendedRecipe;
import li.cil.oc.core.impl.common.recipe.ExtendedShapedRecipe;
import li.cil.oc.core.impl.common.recipe.ExtendedShapelessOreRecipe;
import li.cil.oc.core.impl.server.component.UpgradeCraftingBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

public class UpgradeCrafting extends UpgradeCraftingBase {
    public UpgradeCrafting(Robot host) {
        super(host);
    }

    @Override
    protected void postItemCraftedEvent(@NotNull Player player, @NotNull ItemStack result, @NotNull CraftingInventory inventory) {
        var level = player.level();
        var input = CraftingInput.of(inventory.getWidth(), inventory.getHeight(), inventory.getItems());
        var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(RecipeHolder::value).orElse(null);
        if (!(recipe instanceof ExtendedShapedRecipe) && !(recipe instanceof ExtendedShapelessOreRecipe)) {
            ExtendedRecipe.addNBTToResult(recipe, result, input, level.registryAccess());
        }
    }

    @Override
    protected void postPlayerDestroyItemEvent(@NotNull Player player, @NotNull ItemStack stack) {
      // Fabric has no central event for this.
    }
}
