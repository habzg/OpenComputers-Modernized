package li.cil.oc.core.impl.client.renderer.tileentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.tileentity.Adapter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.NotNull;

public class AdapterRenderer implements BlockEntityRenderer<Adapter> {
    @SuppressWarnings("unused")
    public AdapterRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Adapter adapter, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        boolean[] openSides = adapter.openSides();
        boolean anyOpen = false;
        for (boolean open : openSides) {
            if (open) {
                anyOpen = true;
                break;
            }
        }
        if (!anyOpen) return;

        TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Textures.blockAdapterOnSprite);
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.scale(1.0025f, -1.0025f, 1.0025f);
        poseStack.translate(-0.5, -0.5, -0.5);

        int fullBright = 0xF000F0;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(InventoryMenu.BLOCK_ATLAS));
        var matrix = poseStack.last().pose();

        for (Direction side : Direction.values()) {
            if (!adapter.isSideOpen(side)) continue;

            switch (side) {
                case DOWN -> {
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case UP -> {
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case NORTH -> {
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case SOUTH -> {
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case WEST -> {
                    consumer.addVertex(matrix, 0, 1, 0).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 1, 1).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 1).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 0, 0, 0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
                case EAST -> {
                    consumer.addVertex(matrix, 1, 1, 1).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 1, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                    consumer.addVertex(matrix, 1, 0, 1).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
                }
            }
        }

        poseStack.popPose();
    }
}
