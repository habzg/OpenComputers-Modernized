package li.cil.oc.api.prefab;

import li.cil.oc.api.manual.TabIconRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/**
 * Simple implementation of a tab icon renderer using an item stack as its graphic.
 */

public class ItemStackTabIconRenderer implements TabIconRenderer {
    private final ItemStack stack;

    @SuppressWarnings("unused")
    public ItemStackTabIconRenderer(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public void render(GuiGraphics guiGraphics) {
        guiGraphics.renderItem(stack, 0, 0);
    }
}
