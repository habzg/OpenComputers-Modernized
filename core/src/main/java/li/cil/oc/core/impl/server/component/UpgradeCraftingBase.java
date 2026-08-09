package li.cil.oc.core.impl.server.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import org.jetbrains.annotations.NotNull;

public abstract class UpgradeCraftingBase extends AbstractManagedEnvironment implements DeviceInfo {
    public final li.cil.oc.api.internal.Robot host;

    @SuppressWarnings("unused")
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("crafting")
            .create();
    private final Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Assembly controller");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "MultiCombinator-9S");
    }};
    private final CraftingInventory craftingInventory = new CraftingInventory();

    public UpgradeCraftingBase(li.cil.oc.api.internal.Robot host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    protected abstract void postItemCraftedEvent(Player player, ItemStack result, CraftingInventory inventory);

    @SuppressWarnings("unused")
    protected abstract void postPlayerDestroyItemEvent(Player ignoredPlayer, ItemStack ignoredStack);

    @Callback(doc = "function([count:number]):number -- Tries to craft the specified number of items in the top left area of the inventory.")
    public Object[] craft(Context context, Arguments args) {
        int count = Math.clamp(args.optInteger(0, 64), 0, 64);
        Object[] result = craftingInventory.craft(count);
        return ResultWrapper.result(result);
    }

    public class CraftingInventory extends TransientCraftingContainer {
        int amountPossible = 0;

        CraftingInventory() {
            super(new AbstractContainerMenu(null, 0) {
                @Override
                public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
                    return ItemStack.EMPTY;
                }

                @Override
                public boolean stillValid(@NotNull Player player) {
                    return true;
                }
            }, 3, 3);
        }

        Object[] craft(int wantedCount) {
            Player player = host.player();
            load(player.getInventory());
            int countCrafted = 0;
            var level = host.level();
            var input = CraftingInput.of(getWidth(), getHeight(), getItems());
            var recipeOpt = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level);
            if (recipeOpt.isEmpty()) {
                return new Object[]{false, 0};
            }
            var originalHolder = recipeOpt.get();
            ItemStack originalCraft = originalHolder.value().assemble(input, level.registryAccess());
            if (originalCraft.isEmpty()) {
                return new Object[]{false, 0};
            }
            while (countCrafted < wantedCount) {
                CraftingInput currentInput = CraftingInput.of(getWidth(), getHeight(), getItems());
                var currentRecipeOpt = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, currentInput, level);
                if (currentRecipeOpt.isEmpty()) break;
                if (currentRecipeOpt.get() != originalHolder) break;
                ItemStack result = currentRecipeOpt.get().value().assemble(currentInput, level.registryAccess());
                if (result.isEmpty()) break;
                countCrafted += result.getCount();
                postItemCraftedEvent(player, result, this);
                List<ItemStack> surplus = new ArrayList<>();
                for (int slot = 0; slot < getContainerSize(); slot++) {
                    ItemStack stack = getItem(slot);
                    if (!stack.isEmpty()) {
                        removeItem(slot, 1);
                        Item containerItem = stack.getItem().getCraftingRemainingItem();
                        ItemStack container = containerItem != null ? containerItem.getDefaultInstance() : ItemStack.EMPTY;
                        if (!container.isEmpty()) {
                            surplus.add(container);
                        }
                    }
                }
                save(player.getInventory());
                InventoryUtils.addToPlayerInventory(result, player);
                for (ItemStack stack : surplus) {
                    InventoryUtils.addToPlayerInventory(stack, player);
                }
                load(player.getInventory());
            }
            return new Object[]{true, countCrafted};
        }

        void load(Container inventory) {
            amountPossible = Integer.MAX_VALUE;
            for (int slot = 0; slot < getContainerSize(); slot++) {
                ItemStack stack = inventory.getItem(toParentSlot(slot));
                setItem(slot, stack);
                if (!stack.isEmpty()) {
                    amountPossible = Math.min(amountPossible, stack.getCount());
                }
            }
        }

        void save(Container inventory) {
            for (int slot = 0; slot < getContainerSize(); slot++) {
                inventory.setItem(toParentSlot(slot), getItem(slot));
            }
        }

        private int toParentSlot(int slot) {
            int col = slot % 3;
            int row = slot / 3;
            return row * 4 + col;
        }
    }
}
