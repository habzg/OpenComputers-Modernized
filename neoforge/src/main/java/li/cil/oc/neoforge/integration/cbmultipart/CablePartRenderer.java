package li.cil.oc.neoforge.integration.cbmultipart;

import codechicken.lib.render.buffer.BakedQuadVertexBuilder;
import codechicken.multipart.api.part.MultiPart;
import codechicken.multipart.api.part.render.PartRenderer;
import codechicken.multipart.block.TileMultipart;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.Cable;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class CablePartRenderer implements PartRenderer<CablePart> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "block/cable");
    private static final ResourceLocation TEXTURE_CAP = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "block/cablecap");
    private static final double BASE = 2.0 / 16.0;
    private static final double PLUG_HALF = 6.0 / 16.0 / 2.0 - 1e-4;
    private static final double OFFSET = 0.25;

    private static TextureAtlasSprite bodySprite;
    private static TextureAtlasSprite capSprite;

    @Override
    public @NotNull List<BakedQuad> getQuads(@NotNull CablePart part, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        if (side != null) return List.of();
        if (renderType != null && renderType != RenderType.cutout() && renderType != RenderType.translucent())
            return List.of();
        if (!part.hasLevel()) return List.of();

        ensureSprites();
        if (bodySprite == null || capSprite == null) return List.of();

        var level = part.level();
        var pos = part.pos();
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        int packedLight = LightTexture.pack(blockLight, skyLight);
        int color = part.getColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        int mask = computeConnections(level, pos);

        var bodyBuilder = new BakedQuadVertexBuilder();
        var capBuilder = new BakedQuadVertexBuilder();
        var poseStack = new PoseStack();

        renderBody(poseStack, bodyBuilder, mask, r, g, b, packedLight);
        renderCaps(poseStack, capBuilder, mask, pos, level, r, g, b, packedLight);

        bodyBuilder.sprite(bodySprite);
        var result = new ArrayList<BakedQuad>();
        for (var quad : bodyBuilder.bake()) {
            result.add(new BakedQuad(quad.getVertices(), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), false, quad.hasAmbientOcclusion()));
        }
        capBuilder.sprite(capSprite);
        for (var quad : capBuilder.bake()) {
            result.add(new BakedQuad(quad.getVertices(), quad.getTintIndex(), quad.getDirection(), quad.getSprite(), false, quad.hasAmbientOcclusion()));
        }
        return result;
    }

    private static void renderBody(PoseStack poseStack, BakedQuadVertexBuilder builder,
                                   int mask, float r, float g, float b, int packedLight) {
        renderBox(poseStack, builder, 0.375, 0.625, 0.375, 0.625, 0.375, 0.625,
                r, g, b, packedLight, bodySprite);

        for (Direction side : Direction.values()) {
            if ((mask & (1 << side.ordinal())) != 0) {
                double[] arm = computeArmBox(side);
                renderBox(poseStack, builder,
                        arm[0], arm[1], arm[2], arm[3], arm[4], arm[5],
                        r, g, b, packedLight, bodySprite);
            }
        }
    }

    private static void renderCaps(PoseStack poseStack, BakedQuadVertexBuilder builder,
                                   int mask, BlockPos pos, Level level,
                                   float r, float g, float b, int packedLight) {
        for (Direction side : Direction.values()) {
            int bit = 1 << side.ordinal();

            if ((bit & mask) != 0) {
                BlockPos neighborPos = pos.relative(side);
                if (!isCable(level, neighborPos)) {
                    double[] plug = computePlugBox(side);
                    renderBox(poseStack, builder,
                            plug[0], plug[1], plug[2], plug[3], plug[4], plug[5],
                            r, g, b, packedLight, capSprite);
                }
            } else if ((1 << side.getOpposite().ordinal() & mask) == mask || mask == 0) {
                double[] cap = computeCapBox(side);
                renderBox(poseStack, builder,
                        cap[0], cap[1], cap[2], cap[3], cap[4], cap[5],
                        r, g, b, packedLight, capSprite);
            }
        }
    }

    private static void renderBox(PoseStack poseStack, BakedQuadVertexBuilder builder,
                                  double x0, double x1, double y0, double y1, double z0, double z1,
                                  float r, float g, float b, int packedLight,
                                  TextureAtlasSprite sprite) {
        var m = poseStack.last().pose();
        int ri = (int) (r * 255), gi = (int) (g * 255), bi = (int) (b * 255);
        float fx0 = (float) x0, fx1 = (float) x1, fy0 = (float) y0, fy1 = (float) y1, fz0 = (float) z0, fz1 = (float) z1;

        // North (-Z)
        builder.sprite(sprite);
        addVertex(m, builder, fx0, fy0, fz0, sprite, fx0, fy1, ri, gi, bi, packedLight, 0.8f, 0, 0, -1);
        addVertex(m, builder, fx0, fy1, fz0, sprite, fx0, fy0, ri, gi, bi, packedLight, 0.8f, 0, 0, -1);
        addVertex(m, builder, fx1, fy1, fz0, sprite, fx1, fy0, ri, gi, bi, packedLight, 0.8f, 0, 0, -1);
        addVertex(m, builder, fx1, fy0, fz0, sprite, fx1, fy1, ri, gi, bi, packedLight, 0.8f, 0, 0, -1);

        // South (+Z)
        builder.sprite(sprite);
        addVertex(m, builder, fx1, fy0, fz1, sprite, fx1, fy1, ri, gi, bi, packedLight, 0.8f, 0, 0, 1);
        addVertex(m, builder, fx1, fy1, fz1, sprite, fx1, fy0, ri, gi, bi, packedLight, 0.8f, 0, 0, 1);
        addVertex(m, builder, fx0, fy1, fz1, sprite, fx0, fy0, ri, gi, bi, packedLight, 0.8f, 0, 0, 1);
        addVertex(m, builder, fx0, fy0, fz1, sprite, fx0, fy1, ri, gi, bi, packedLight, 0.8f, 0, 0, 1);

        // West (-X)
        builder.sprite(sprite);
        addVertex(m, builder, fx0, fy0, fz1, sprite, fz1, fy1, ri, gi, bi, packedLight, 0.6f, -1, 0, 0);
        addVertex(m, builder, fx0, fy1, fz1, sprite, fz1, fy0, ri, gi, bi, packedLight, 0.6f, -1, 0, 0);
        addVertex(m, builder, fx0, fy1, fz0, sprite, fz0, fy0, ri, gi, bi, packedLight, 0.6f, -1, 0, 0);
        addVertex(m, builder, fx0, fy0, fz0, sprite, fz0, fy1, ri, gi, bi, packedLight, 0.6f, -1, 0, 0);

        // East (+X)
        builder.sprite(sprite);
        addVertex(m, builder, fx1, fy0, fz0, sprite, fz0, fy1, ri, gi, bi, packedLight, 0.6f, 1, 0, 0);
        addVertex(m, builder, fx1, fy1, fz0, sprite, fz0, fy0, ri, gi, bi, packedLight, 0.6f, 1, 0, 0);
        addVertex(m, builder, fx1, fy1, fz1, sprite, fz1, fy0, ri, gi, bi, packedLight, 0.6f, 1, 0, 0);
        addVertex(m, builder, fx1, fy0, fz1, sprite, fz1, fy1, ri, gi, bi, packedLight, 0.6f, 1, 0, 0);

        // Down (-Y)
        builder.sprite(sprite);
        addVertex(m, builder, fx0, fy0, fz0, sprite, fx0, fz0, ri, gi, bi, packedLight, 0.5f, 0, -1, 0);
        addVertex(m, builder, fx1, fy0, fz0, sprite, fx1, fz0, ri, gi, bi, packedLight, 0.5f, 0, -1, 0);
        addVertex(m, builder, fx1, fy0, fz1, sprite, fx1, fz1, ri, gi, bi, packedLight, 0.5f, 0, -1, 0);
        addVertex(m, builder, fx0, fy0, fz1, sprite, fx0, fz1, ri, gi, bi, packedLight, 0.5f, 0, -1, 0);

        // Up (+Y)
        builder.sprite(sprite);
        addVertex(m, builder, fx0, fy1, fz0, sprite, fx0, fz0, ri, gi, bi, packedLight, 1.0f, 0, 1, 0);
        addVertex(m, builder, fx0, fy1, fz1, sprite, fx0, fz1, ri, gi, bi, packedLight, 1.0f, 0, 1, 0);
        addVertex(m, builder, fx1, fy1, fz1, sprite, fx1, fz1, ri, gi, bi, packedLight, 1.0f, 0, 1, 0);
        addVertex(m, builder, fx1, fy1, fz0, sprite, fx1, fz0, ri, gi, bi, packedLight, 1.0f, 0, 1, 0);
    }

    private static void addVertex(org.joml.Matrix4f m, VertexConsumer c,
                                  double x, double y, double z,
                                  TextureAtlasSprite sprite,
                                  double u, double v,
                                  int r, int g, int b, int light,
                                  float brightness,
                                  float nx, float ny, float nz) {
        var pos = m.transformPosition((float) x, (float) y, (float) z, new org.joml.Vector3f());
        c.addVertex(pos.x, pos.y, pos.z);
        c.setColor((int) (r * brightness), (int) (g * brightness), (int) (b * brightness), 255);
        c.setUv(sprite.getU((float) u), sprite.getV((float) v));
        c.setOverlay(OverlayTexture.NO_OVERLAY);
        c.setLight(light);
        c.setNormal(nx, ny, nz);
    }

    private static int computeConnections(Level level, BlockPos pos) {
        int selfColor = getCableColor(level, pos);
        int connections = 0;
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (!level.isLoaded(neighbor)) continue;
            BlockEntity neighborTE = level.getBlockEntity(neighbor);
            if (isOCNeighbor(neighborTE)) {
                BlockEntity localTE = level.getBlockEntity(pos);
                if (localTE instanceof TileMultipart localTileMP) {
                    if (!MultipartNetworkBridge.canConnectFromSide(localTileMP, side)) {
                        continue;
                    }
                }
                if (neighborTE instanceof TileMultipart neighborTileMP) {
                    if (!MultipartNetworkBridge.canConnectFromSide(neighborTileMP, side.getOpposite())) {
                        continue;
                    }
                }
                int neighborColor = getCableColor(level, neighbor);
                if (selfColor == neighborColor || selfColor == Color.LightGray || neighborColor == Color.LightGray) {
                    connections |= 1 << side.ordinal();
                }
            }
        }
        return connections;
    }

    private static boolean isOCNeighbor(BlockEntity te) {
        if (te instanceof Environment || te instanceof SidedEnvironment) return true;
        if (te instanceof TileMultipart tileMP) {
            for (MultiPart part : tileMP.getPartList()) {
                if (part instanceof Environment) return true;
            }
        }
        return false;
    }

    private static boolean isCable(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) return false;
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof Cable) return true;
        if (te instanceof TileMultipart tileMP) {
            for (MultiPart part : tileMP.getPartList()) {
                if (part instanceof CablePart) return true;
            }
        }
        return false;
    }

    private static int getCableColor(Level level, BlockPos pos) {
        BlockEntity te = level.getBlockEntity(pos);
        if (te instanceof Cable cable) return cable.color();
        if (te instanceof TileMultipart tileMP) {
            for (MultiPart part : tileMP.getPartList()) {
                if (part instanceof CablePart cablePart) return cablePart.getColor();
            }
        }
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

    private static void ensureSprites() {
        if (bodySprite != null && capSprite != null) return;
        bodySprite = safeGetSprite(TEXTURE);
        capSprite = safeGetSprite(TEXTURE_CAP);
    }

    private static TextureAtlasSprite safeGetSprite(ResourceLocation texture) {
        try {
            var atlasFn = Minecraft.getInstance()
                    .getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
            var sprite = atlasFn.apply(texture);
            if (sprite != null) return sprite;
            return atlasFn.apply(MissingTextureAtlasSprite.getLocation());
        } catch (Exception ignored) {
        }
        return null;
    }
}
