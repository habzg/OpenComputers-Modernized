package li.cil.oc.core.impl.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
            if (server == null) return new ItemStack[0];
            var recipeManager = server.getRecipeManager();
            var craftingRecipes = recipeManager.getAllRecipesFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING);

            for (var holder : craftingRecipes) {
                var recipe = holder.value();
                var result = recipe.getResultItem(server.registryAccess());
                if (ItemStack.isSameItem(result, stack) && ItemStack.isSameItemSameComponents(result, stack)) {
                    var ings = recipe.getIngredients();
                    List<ItemStack> stacks = new ArrayList<>();
                    boolean blacklisted = false;
                    for (var ingr : ings) {
                        var items = ingr.getItems();
                        if (items.length > 0) {
                            var chosen = items[rng.nextInt(items.length)].copy();
                            if (!chosen.isEmpty()) {
                                if (isInputBlacklisted(chosen)) {
                                    blacklisted = true;
                                    break;
                                }
                                stacks.add(chosen);
                            }
                        }
                    }
                    if (!blacklisted) {
                        return stacks.toArray(new ItemStack[0]);
                    }
                }
            }
            return new ItemStack[0];
        } catch (Throwable t) {
            LOGGER.warn("Whoops, something went wrong when trying to figure out an item's parts.", t);
            return new ItemStack[0];
        }
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
