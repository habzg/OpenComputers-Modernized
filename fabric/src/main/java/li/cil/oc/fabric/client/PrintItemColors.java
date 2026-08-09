package li.cil.oc.fabric.client;

import li.cil.oc.core.impl.common.item.data.PrintData;
import net.minecraft.world.item.ItemStack;

public final class PrintItemColors {
    private static final int LIME = 0x66FF66;

    private PrintItemColors() {
    }

    public static int getColor(ItemStack stack, int tintIndex) {
        var shapes = PrintData.getRenderShapes(stack, KeyBindings.showExtendedTooltips());
        if (shapes.isEmpty()) return tintIndex == 0 ? LIME : -1;
        if (tintIndex >= 0 && tintIndex < shapes.size()) {
            Integer tint = shapes.get(tintIndex).tint();
            return tint != null ? tint : -1;
        }
        return -1;
    }
}
