package li.cil.oc.neoforge.integration.util;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;
import li.cil.oc.core.impl.common.item.DelegateItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public final class JEI {
    private static final Set<Supplier<ItemStack>> hiddenItems = new LinkedHashSet<>();

    private JEI() {
    }

    public static void hide(Block block) {
        hiddenItems.add(() -> new ItemStack(block));
    }

    public static void hide(DelegateItem item) {
        hiddenItems.add(() -> item.createItemStack(1));
    }

    public static void hide(ItemStack stack) {
        hiddenItems.add(() -> stack);
    }

    public static Set<Supplier<ItemStack>> getHiddenItems() {
        return hiddenItems;
    }
}
