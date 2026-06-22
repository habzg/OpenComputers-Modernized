package li.cil.oc.neoforge.integration.appeng;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageCells;
import appeng.api.storage.cells.IBasicCellItem;
import appeng.me.cells.BasicCellInventory;
import li.cil.oc.api.driver.Converter;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterCellInventory implements Converter {
    @Override
    public void convert(final Object value, final Map<Object, Object> output) {
        if (value instanceof BasicCellInventory cell) {
            output.put("storedItemTypes", cell.getStoredItemTypes());
            output.put("storedItemCount", cell.getStoredItemCount());
            output.put("remainingItemCount", cell.getRemainingItemCount());
            output.put("remainingItemTypes", cell.getRemainingItemTypes());

            output.put("getTotalItemTypes", cell.getTotalItemTypes());
            var availableItems = new KeyCounter();
            cell.getAvailableStacks(availableItems);
            var itemsList = new ArrayList<String>();
            for (var entry : availableItems) {
                if (entry.getKey() instanceof AEItemKey itemKey) {
                    long amount = entry.getLongValue();
                    String descriptionId = itemKey.getItem().getDescriptionId();
                    int damage = itemKey.getReadOnlyStack().getDamageValue();
                    itemsList.add(amount + "x" + descriptionId + "@" + damage);
                }
            }
            output.put("getAvailableItems", itemsList);

            output.put("totalBytes", cell.getTotalBytes());
            output.put("freeBytes", cell.getFreeBytes());
            output.put("usedBytes", cell.getUsedBytes());
            output.put("unusedItemCount", cell.getUnusedItemCount());
            output.put("canHoldNewItem", cell.canHoldNewItem());

            output.put("fuzzyMode", cell.getFuzzyMode().toString());
            output.put("name", cell.getDescription().getString());
        } else if ((value instanceof ItemStack stack) && (stack.getItem() instanceof IBasicCellItem)) {
            var cell = StorageCells.getCellInventory(stack, null);
            if (cell != null) {
                convert(cell, output);
            }
        }
    }
}
