package li.cil.oc.core.impl.common.template;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.Registrar;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public final class ServerTemplate {
    private ServerTemplate() {
    }

    @SuppressWarnings("unused")

    public static boolean selectDisassembler(ItemStack stack) {
        return Items.get(stack) == Items.get(Constants.ItemName.ServerTier1) ||
                Items.get(stack) == Items.get(Constants.ItemName.ServerTier2) ||
                Items.get(stack) == Items.get(Constants.ItemName.ServerTier3);
    }

    @SuppressWarnings("unused")

    public static Object[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        var info = new ServerInventory() {
            @Override
            public ItemStack container() {
                return stack;
            }

            @Override
            public void updateItems(int slot, ItemStack stack) {
            }
        };
        var drops = new ArrayList<ItemStack>();
        for (int i = 0; i < info.getContainerSize(); i++) {
            var item = info.getItem(i);
            if (!item.isEmpty()) {
                drops.add(item);
            }
        }
        return new Object[]{ingredients, drops.toArray(new ItemStack[0])};
    }

    public static void register() {
        Registrar.registerDisassemblerTemplate("Server",
                "li.cil.oc.core.impl.common.template.ServerTemplate.selectDisassembler",
                "li.cil.oc.core.impl.common.template.ServerTemplate.disassemble");
    }
}
