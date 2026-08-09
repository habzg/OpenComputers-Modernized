package li.cil.oc.fabric.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class CableRenderHelper {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/block/cable.png");
    private static final ResourceLocation TEXTURE_CAP = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/block/cablecap.png");
    private static final double BASE = 2.0 / 16.0;
    private static final double PLUG_HALF = 6.0 / 16.0 / 2.0 - 1e-4;
    private static final double OFFSET = 0.25;

    private static RenderType renderType;
    private static RenderType renderTypeCap;
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite bodySprite;
    private static net.minecraft.client.renderer.texture.TextureAtlasSprite capSprite;

    private CableRenderHelper() {
    }

    private static RenderType getRenderType() {
        if (renderType == null) {
            renderType = RenderType.entityCutoutNoCull(TEXTURE);
        }
        return renderType;
    }

    private static RenderType getRenderTypeCap() {
        if (renderTypeCap == null) {
            renderTypeCap = RenderType.entityCutoutNoCull(TEXTURE_CAP);
        }
        return renderTypeCap;
    }

    private static TextureAtlasSprite getBodySprite() {
        if (bodySprite == null) {
            bodySprite = net.minecraft.client.Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(TEXTURE);
        }
        return bodySprite;
    }

    private static TextureAtlasSprite getCapSprite() {
        if (capSprite == null) {
            capSprite = net.minecraft.client.Minecraft.getInstance()
                    .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                    .apply(TEXTURE_CAP);
        }
        return capSprite;
    }

    private static TextureAtlasSprite safeSprite(TextureAtlasSprite sprite) {
        if (sprite != null) return sprite;
        return net.minecraft.client.Minecraft.getInstance()
                .getTextureAtlas(net.minecraft.world.inventory.InventoryMenu.BLOCK_ATLAS)
                .apply(MissingTextureAtlasSprite.getLocation());
    }

    public static void render(Level level, BlockPos pos, int color, PoseStack poseStack,
                              MultiBufferSource buffer,
                              int packedLight, int packedOverlay) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        int mask = computeConnections(level, pos);

        poseStack.pushPose();
        var bodyConsumer = buffer.getBuffer(getRenderType());
        renderBody(poseStack, bodyConsumer, mask, r, g, b, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        var capConsumer = buffer.getBuffer(getRenderTypeCap());
        renderCaps(poseStack, capConsumer, mask, pos, level, r, g, b, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @SuppressWarnings("unused")
    public static void render(Level level, BlockPos pos, int color, PoseStack poseStack,
                              VertexConsumer bodyConsumer, VertexConsumer capConsumer,
                              int packedLight, int packedOverlay) {
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        int mask = computeConnections(level, pos);

        poseStack.pushPose();
        renderBody(poseStack, bodyConsumer, mask, r, g, b, packedLight, packedOverlay);
        renderCaps(poseStack, capConsumer, mask, pos, level, r, g, b, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderBody(PoseStack poseStack, VertexConsumer consumer, int mask,
                                   float r, float g, float b, int packedLight, int packedOverlay) {
        TextureAtlasSprite bodySprite = safeSprite(getBodySprite());

        renderBox(poseStack, consumer, 0.375, 0.625, 0.375, 0.625, 0.375, 0.625,
                r, g, b, packedLight, packedOverlay, bodySprite);

        for (Direction side : Direction.values()) {
            if ((mask & (1 << side.ordinal())) != 0) {
                double[] armBox = computeArmBox(side);
                renderBox(poseStack, consumer,
                        armBox[0], armBox[1], armBox[2], armBox[3], armBox[4], armBox[5],
                        r, g, b, packedLight, packedOverlay, bodySprite);
            }
        }
    }

    private static void renderCaps(PoseStack poseStack, VertexConsumer consumer, int mask,
                                   BlockPos pos, Level level,
                                   float r, float g, float b, int packedLight, int packedOverlay) {
        TextureAtlasSprite capSprite = safeSprite(getCapSprite());

        for (Direction side : Direction.values()) {
            int bit = 1 << side.ordinal();

            if ((bit & mask) != 0) {
                BlockPos neighborPos = pos.relative(side);
                if (!isCable(level, neighborPos)) {
                    double[] plugBox = computePlugBox(side);
                    renderBox(poseStack, consumer,
                            plugBox[0], plugBox[1], plugBox[2], plugBox[3], plugBox[4], plugBox[5],
                            r, g, b, packedLight, packedOverlay, capSprite);
                }
            } else if ((1 << side.getOpposite().ordinal() & mask) == mask || mask == 0) {
                double[] capBox = computeCapBox(side);
                renderBox(poseStack, consumer,
                        capBox[0], capBox[1], capBox[2], capBox[3], capBox[4], capBox[5],
                        r, g, b, packedLight, packedOverlay, capSprite);
            }
        }
    }

    private static boolean isCable(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockEntity te = level.getBlockEntity(pos);
        return te instanceof Cable;
    }

    private static int computeConnections(Level level, BlockPos pos) {
        int selfColor = getCableColor(level, pos);
        int connections = 0;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity neighborTE = level.getBlockEntity(neighbor);
            if (isOCNeighbor(neighborTE, side.getOpposite())) {
                int neighborColor = getCableColor(level, neighbor);
                if (selfColor == neighborColor || selfColor == Color.LightGray || neighborColor == Color.LightGray) {
                    connections |= 1 << side.ordinal();
                }
            }
        }
        return connections;
    }

    private static boolean isOCNeighbor(BlockEntity te, Direction side) {
        if (te instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy) return false;
        if (te instanceof li.cil.oc.api.network.Environment || te instanceof SidedEnvironment) {
            if (te instanceof SidedEnvironment sideEnv) return sideEnv.canConnect(side);
            return true;
        }
        return false;
    }

    private static int getCableColor(Level level, BlockPos pos) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof Cable cable) return cable.color();
        return Color.LightGray;
    }

    private static double[] computeArmBox(Direction side) {
        double sx = side.getStepX(), sy = side.getStepY(), sz = side.getStepZ();
        double minX = -BASE + sx * OFFSET;
        double maxX = BASE + sx * OFFSET;
        double minY = -BASE + sy * OFFSET;
        double maxY = BASE + sy * OFFSET;
        double minZ = -BASE + sz * OFFSET;
        double maxZ = BASE + sz * OFFSET;
        minX = Math.min(minX, sx * 0.5);
        maxX = Math.max(maxX, sx * 0.5);
        minY = Math.min(minY, sy * 0.5);
        maxY = Math.max(maxY, sy * 0.5);
        minZ = Math.min(minZ, sz * 0.5);
        maxZ = Math.max(maxZ, sz * 0.5);
        return new double[]{minX + 0.5, maxX + 0.5, minY + 0.5, maxY + 0.5, minZ + 0.5, maxZ + 0.5};
    }

    private static double[] computePlugBox(Direction side) {
        double sx = side.getStepX(), sy = side.getStepY(), sz = side.getStepZ();
        double minX = -PLUG_HALF + sx * OFFSET;
        double maxX = PLUG_HALF + sx * OFFSET;
        double minY = -PLUG_HALF + sy * OFFSET;
        double maxY = PLUG_HALF + sy * OFFSET;
        double minZ = -PLUG_HALF + sz * OFFSET;
        double maxZ = PLUG_HALF + sz * OFFSET;
        minX = Math.clamp(minX + sx * 10.0 / 16.0, -0.5 - 1e-4, 7.0 / 16.0);
        maxX = Math.clamp(maxX + sx * 10.0 / 16.0, -7.0 / 16.0, 0.5 + 1e-4);
        minY = Math.clamp(minY + sy * 10.0 / 16.0, -0.5 - 1e-4, 7.0 / 16.0);
        maxY = Math.clamp(maxY + sy * 10.0 / 16.0, -7.0 / 16.0, 0.5 + 1e-4);
        minZ = Math.clamp(minZ + sz * 10.0 / 16.0, -0.5 - 1e-4, 7.0 / 16.0);
        maxZ = Math.clamp(maxZ + sz * 10.0 / 16.0, -7.0 / 16.0, 0.5 + 1e-4);
        return new double[]{minX + 0.5, maxX + 0.5, minY + 0.5, maxY + 0.5, minZ + 0.5, maxZ + 0.5};
    }

    private static double[] computeCapBox(Direction side) {
        double sx = side.getStepX(), sy = side.getStepY(), sz = side.getStepZ();
        double minX = -BASE + sx * OFFSET;
        double maxX = BASE + sx * OFFSET;
        double minY = -BASE + sy * OFFSET;
        double maxY = BASE + sy * OFFSET;
        double minZ = -BASE + sz * OFFSET;
        double maxZ = BASE + sz * OFFSET;
        minX = Math.max(minX, -PLUG_HALF);
        maxX = Math.min(maxX, PLUG_HALF);
        minY = Math.max(minY, -PLUG_HALF);
        maxY = Math.min(maxY, PLUG_HALF);
        minZ = Math.max(minZ, -PLUG_HALF);
        maxZ = Math.min(maxZ, PLUG_HALF);
        return new double[]{minX + 0.5, maxX + 0.5, minY + 0.5, maxY + 0.5, minZ + 0.5, maxZ + 0.5};
    }

    private static void renderBox(PoseStack poseStack, VertexConsumer c,
                                  double x0, double x1, double y0, double y1, double z0, double z1,
                                  float r, float g, float b, int packedLight, int packedOverlay,
                                  TextureAtlasSprite ignoredSprite) {
        var m = poseStack.last().pose();
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255);
        float fx0 = (float) x0, fx1 = (float) x1, fy0 = (float) y0, fy1 = (float) y1, fz0 = (float) z0, fz1 = (float) z1;

        // North (-Z)
        c.addVertex(m, fx0, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fx0, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        c.addVertex(m, fx0, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fx0, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        c.addVertex(m, fx1, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fx1, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        c.addVertex(m, fx1, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fx1, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, -1);
        // South (+Z)
        c.addVertex(m, fx1, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fx1, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        c.addVertex(m, fx1, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fx1, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        c.addVertex(m, fx0, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fx0, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        c.addVertex(m, fx0, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fx0, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 0, 1);
        // West (-X)
        c.addVertex(m, fx0, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fz1, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        c.addVertex(m, fx0, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fz1, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        c.addVertex(m, fx0, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fz0, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        c.addVertex(m, fx0, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fz0, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(-1, 0, 0);
        // East (+X)
        c.addVertex(m, fx1, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fz0, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        c.addVertex(m, fx1, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fz0, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        c.addVertex(m, fx1, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fz1, fy0).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        c.addVertex(m, fx1, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fz1, fy1).setOverlay(packedOverlay).setLight(packedLight).setNormal(1, 0, 0);
        // Down (-Y)
        c.addVertex(m, fx0, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fx0, fz0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        c.addVertex(m, fx1, fy0, fz0).setColor(ri, gi, bi, 255).setUv(fx1, fz0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        c.addVertex(m, fx1, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fx1, fz1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        c.addVertex(m, fx0, fy0, fz1).setColor(ri, gi, bi, 255).setUv(fx0, fz1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, -1, 0);
        // Up (+Y)
        c.addVertex(m, fx1, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fx1, fz0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        c.addVertex(m, fx1, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fx1, fz1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        c.addVertex(m, fx0, fy1, fz1).setColor(ri, gi, bi, 255).setUv(fx0, fz1).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
        c.addVertex(m, fx0, fy1, fz0).setColor(ri, gi, bi, 255).setUv(fx0, fz0).setOverlay(packedOverlay).setLight(packedLight).setNormal(0, 1, 0);
    }
}
