package li.cil.oc.core.impl.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.impl.common.blockentity.Printer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import org.jetbrains.annotations.NotNull;

public class PrinterRenderer implements BlockEntityRenderer<Printer> {
    @SuppressWarnings("unused")
    public PrinterRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Printer printer, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (printer.data != null && !printer.data.stateOff.isEmpty()) {
            var stack = printer.data.createItemStack();

            poseStack.pushPose();
            poseStack.translate(0.5, 0.5 + 0.3, 0.5);

            long time = System.currentTimeMillis();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees((time % 20000) / 20000f * 360));
            poseStack.scale(0.75f, 0.75f, 0.75f);

            var level = printer.getLevel();
            if (level != null) {
                var light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, printer.getBlockPos());
                Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                        light, packedOverlay, poseStack, bufferSource, level, 0);
            }

            poseStack.popPose();
        }
    }
}
