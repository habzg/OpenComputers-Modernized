package li.cil.oc.neoforge.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.impl.common.blockentity.Cable;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import org.jetbrains.annotations.NotNull;

public class CableRenderer implements BlockEntityRenderer<Cable> {
    @SuppressWarnings("unused")
    public CableRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(Cable cable, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        if (cable.getLevel() == null) return;
        CableRenderHelper.render(cable.getLevel(), cable.getBlockPos(), cable.color(),
                poseStack, buffer, packedLight, packedOverlay);
    }
}
