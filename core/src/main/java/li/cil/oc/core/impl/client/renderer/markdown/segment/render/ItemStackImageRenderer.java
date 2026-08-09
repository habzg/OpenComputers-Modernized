package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class ItemStackImageRenderer implements ImageRenderer {
    private final ItemStack[] stacks;
    private static final long CYCLE_SPEED = 1000;

    @SuppressWarnings("unused")
    public ItemStackImageRenderer(ItemStack... stacks) {
        this.stacks = stacks;
    }

    @SuppressWarnings("unused")
    public ItemStackImageRenderer(ItemStack stack) {
        this.stacks = new ItemStack[]{stack};
    }

    @Override
    public int getWidth() {
        return 32;
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (stacks.length == 0) return;
        int index = (int) ((System.currentTimeMillis() % (CYCLE_SPEED * stacks.length)) / CYCLE_SPEED);
        var stack = stacks[index];
        if (stack.isEmpty()) return;

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 150);
        graphics.pose().scale(2f, 2f, 2f);
        graphics.renderItem(stack, 0, 0);
        graphics.pose().popPose();
    }
}
