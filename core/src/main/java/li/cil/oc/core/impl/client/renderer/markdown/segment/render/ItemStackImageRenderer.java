package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
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
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int mouseX, int mouseY) {
        if (stacks.length == 0) return;
        int index = (int) ((System.currentTimeMillis() % (CYCLE_SPEED * stacks.length)) / CYCLE_SPEED);
        var stack = stacks[index];
        if (stack.isEmpty()) return;

        var mc = Minecraft.getInstance();
        var itemRenderer = mc.getItemRenderer();

        poseStack.pushPose();
        poseStack.translate(getWidth() / 2f, getHeight() / 2f, 150);
        poseStack.scale((float) getWidth(), (float) -getHeight(), (float) getWidth());

        var model = itemRenderer.getModel(stack, mc.level, null, 0);
        itemRenderer.render(stack, ItemDisplayContext.GUI, false, poseStack, bufferSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);

        poseStack.popPose();
    }
}
