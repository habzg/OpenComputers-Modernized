package li.cil.oc.core.impl.common.template;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.item.data.NavigationUpgradeData;
import net.minecraft.world.item.ItemStack;

public final class NavigationUpgradeTemplate {
    private NavigationUpgradeTemplate() {
    }

    @SuppressWarnings("unused")

    public static boolean selectDisassembler(ItemStack stack) {
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.NavigationUpgrade);
    }

    @SuppressWarnings("unused")

    public static ItemStack[] disassemble(ItemStack stack, ItemStack[] ingredients) {
        var info = new NavigationUpgradeData(stack);
        for (int i = 0; i < ingredients.length; i++) {
            if (ingredients[i].getItem() == net.minecraft.world.item.Items.FILLED_MAP) {
                ingredients[i] = info.map;
            }
        }
        return ingredients;
    }

    public static void register() {
        li.cil.oc.core.impl.common.Registrar.registerDisassemblerTemplate(
                "Navigation Upgrade",
                "li.cil.oc.core.impl.common.template.NavigationUpgradeTemplate.selectDisassembler",
                "li.cil.oc.core.impl.common.template.NavigationUpgradeTemplate.disassemble");
    }
}
