package li.cil.oc.core.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public final class ItemUtils {
  private static final Logger LOGGER = LoggerFactory.getLogger(ItemUtils.class);

  private static final Random rng = new Random();

  public static int caseTier(ItemStack stack) {
    var descriptor = li.cil.oc.api.Items.get(stack);
    if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier1)) return Tier.One;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier2)) return Tier.Two;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseTier3)) return Tier.Three;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.BlockName.CaseCreative)) return Tier.Four;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier1)) return Tier.One;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseTier2)) return Tier.Two;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.MicrocontrollerCaseCreative))
      return Tier.Four;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseTier1)) return Tier.One;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseTier2)) return Tier.Two;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.DroneCaseCreative)) return Tier.Four;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1)) return Tier.One;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2)) return Tier.Two;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3)) return Tier.Three;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.ServerCreative)) return Tier.Four;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier1)) return Tier.One;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseTier2)) return Tier.Two;
    else if (descriptor == li.cil.oc.api.Items.get(Constants.ItemName.TabletCaseCreative)) return Tier.Four;
    else return Tier.None;
  }

  public static ItemStack[] getIngredients(ItemStack stack) {
    try {
      var server = li.cil.oc.core.impl.util.SideTracker.getCurrentServer();
      if (server == null || stack.isEmpty()) return new ItemStack[0];
      var recipeManager = server.getRecipeManager();
      var craftingRecipes = recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);

      for (var holder : craftingRecipes) {
        var recipe = holder.value();
        if (!(recipe instanceof ShapedRecipe) && !(recipe instanceof ShapelessRecipe)) continue;
        var result = recipe.getResultItem(server.registryAccess());
        if (result.isEmpty() || !isItemEqual(result, stack)) continue;
        int outputSize = result.getCount();
        List<ItemStack> inputs = new ArrayList<>();
        for (var ingr : recipe.getIngredients()) {
          var items = ingr.getItems();
          if (items.length == 0) continue;
          var chosen = items[rng.nextInt(items.length)].copy();
          if (chosen.isEmpty()) continue;
          if (chosen.getCount() / outputSize <= 0) continue;
          if (chosen.getItem() instanceof BucketItem) continue;
          inputs.add(chosen);
        }
        boolean blacklisted = false;
        for (var input : inputs) {
          if (isInputBlacklisted(input)) {
            blacklisted = true;
            break;
          }
        }
        if (blacklisted) continue;
        for (var input : inputs) {
          if (isItemEqual(input, stack)) return new ItemStack[0];
        }
        List<ItemStack> merged = new ArrayList<>();
        for (var ingredient : inputs) {
          ItemStack entry = null;
          for (var m : merged) {
            if (isItemEqual(m, ingredient)) {
              entry = m;
              break;
            }
          }
          if (entry != null) entry.grow(ingredient.getCount());
          else merged.add(ingredient.copy());
        }
        List<ItemStack> distinct = new ArrayList<>();
        for (var ingredient : merged) {
          int size = Math.max(1, ingredient.getCount() / outputSize);
          ingredient.setCount(1);
          for (int i = 0; i < size; i++) distinct.add(ingredient.copy());
        }
        return distinct.toArray(new ItemStack[0]);
      }
      return new ItemStack[0];
    } catch (Throwable t) {
      LOGGER.warn("Whoops, something went wrong when trying to figure out an item's parts.", t);
      return new ItemStack[0];
    }
  }

  private static boolean isItemEqual(ItemStack a, ItemStack b) {
    return !a.isEmpty() && !b.isEmpty() && ItemStack.isSameItem(a, b) && a.getDamageValue() == b.getDamageValue();
  }

  private static boolean isInputBlacklisted(ItemStack stack) {
    var blacklist = OCSettings.get().disassemblerInputBlacklist;
    Item item = stack.getItem();
    if (item instanceof BlockItem blockItem) {
      return blacklist.contains(BuiltInRegistries.BLOCK.getKey(blockItem.getBlock()).toString());
    } else {
      return blacklist.contains(BuiltInRegistries.ITEM.getKey(item).toString());
    }
  }
}
