package li.cil.oc.neoforge.client.renderer.blockentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Hologram;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class HologramRenderer implements BlockEntityRenderer<Hologram> {
    private static final Random random = new Random();

    private static final int STRIDE = 9;
    private static final int FLOATS_PER_QUAD = STRIDE * 4;

    private static final RenderStateShard.TransparencyStateShard ADDITIVE_TRANSPARENCY =
            new RenderStateShard.TransparencyStateShard(
                    "additive_transparency",
                    () -> {
                        RenderSystem.enableBlend();
                        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                    },
                    () -> {
                        RenderSystem.disableBlend();
                        RenderSystem.defaultBlendFunc();
                    }
            );

    public static final RenderType HOLOGRAM = RenderType.create(
            "oc_hologram",
            DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS,
            786432, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(Textures.blockHologram, false, false))
                    .setTransparencyState(ADDITIVE_TRANSPARENCY)
                    .setCullState(RenderStateShard.CULL)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setOverlayState(RenderStateShard.OVERLAY)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .createCompositeState(true)
    );

    @SuppressWarnings("unused")
    private record CachedData(float[] vertexData, int quadCount) {
    }

    private record PendingDraw(org.joml.Matrix4f matrix, float[] verts, int quadCount,
                               int alphaInt, int packedOverlay) {
        PendingDraw {
            verts = verts.clone();
            matrix = new org.joml.Matrix4f(matrix);
        }
    }

    private static final java.util.List<PendingDraw> pendingDraws = new java.util.ArrayList<>();

    public static void drawPending(MultiBufferSource.BufferSource bufferSource) {
        if (pendingDraws.isEmpty()) return;
        var draws = java.util.List.copyOf(pendingDraws);
        pendingDraws.clear();
        for (var holo : draws) {
            VertexConsumer consumer = bufferSource.getBuffer(HOLOGRAM);
            submitQuadsStatic(consumer, holo.matrix, holo.verts, holo.quadCount, holo.alphaInt, holo.packedOverlay);
        }
        bufferSource.endBatch(HOLOGRAM);
    }

    private static void submitQuadsStatic(VertexConsumer consumer, org.joml.Matrix4f matrix,
                                          float[] verts, int quadCount, int alphaInt, int packedOverlay) {
        int idx = 0;
        for (int q = 0; q < quadCount; q++) {
            for (int v = 0; v < 4; v++) {
                float x = verts[idx];
                float y = verts[idx + 1];
                float z = verts[idx + 2];
                float nx = verts[idx + 3];
                float ny = verts[idx + 4];
                float nz = verts[idx + 5];
                int r = (int) verts[idx + 6];
                int g = (int) verts[idx + 7];
                int b = (int) verts[idx + 8];
                idx += STRIDE;

                float u = (v == 0 || v == 3) ? 0 : 1;
                float vv = (v == 0 || v == 1) ? 0 : 1;

                consumer.addVertex(matrix, x, y, z)
                        .setColor(r, g, b, alphaInt)
                        .setUv(u, vv)
                        .setOverlay(packedOverlay)
                        .setLight(0xF000F0)
                        .setNormal(nx, ny, nz);
            }
        }
    }

    private final com.google.common.cache.Cache<BlockPos, CachedData> cache =
            com.google.common.cache.CacheBuilder.newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .build();

    @SuppressWarnings("unused")
    public HologramRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    @Override
    public void render(@NotNull Hologram hologram, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!hologram.hasPower) return;

        var level = hologram.getLevel();
        if (level == null) return;

        var player = Minecraft.getInstance().player;
        if (player == null) return;
        double playerDistSq = ClientDistanceHelper.distanceSquared(level,
                hologram.getBlockPos().getX() + 0.5, hologram.getBlockPos().getY() + 0.5, hologram.getBlockPos().getZ() + 0.5, player);

        double maxScale = Arrays.stream(OCSettings.get().hologramMaxScaleByTier).max().orElse(4.0);
        double maxDistSq = hologram.scale / maxScale * OCSettings.get().hologramRenderDistance * OCSettings.get().hologramRenderDistance;
        double fadeDistSq = hologram.scale / maxScale * OCSettings.get().hologramFadeStartDistance * OCSettings.get().hologramFadeStartDistance;

        float alpha;
        if (playerDistSq > maxDistSq) return;
        else if (playerDistSq > fadeDistSq)
            alpha = 0.75f * (float) Math.max(0, 1 - (playerDistSq - fadeDistSq) / (maxDistSq - fadeDistSq));
        else alpha = 0.75f;

        if (alpha <= 0) return;

        var pos = hologram.getBlockPos();
        var data = cache.getIfPresent(pos);
        if (data == null || hologram.needsRendering) {
            data = rebuildQuads(hologram);
            cache.put(pos, data);
            hologram.needsRendering = false;
        }

        if (data.quadCount() == 0) return;
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        var yaw = hologram.yaw();
        if (yaw == Direction.WEST) poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
        else if (yaw == Direction.NORTH) poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
        else if (yaw == Direction.EAST) poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));

        var pitch = hologram.pitch();
        if (pitch == Direction.DOWN) poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(90)));
        else if (pitch == Direction.UP) poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(-90)));

        if (hologram.rotationAngle != 0) {
            float len = (float) Math.sqrt(hologram.rotationX * hologram.rotationX + hologram.rotationY * hologram.rotationY + hologram.rotationZ * hologram.rotationZ);
            if (len > 1e-6f)
                poseStack.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(hologram.rotationAngle), hologram.rotationX / len, hologram.rotationY / len, hologram.rotationZ / len));
        }

        if (hologram.rotationSpeed != 0) {
            float angle = hologram.rotationSpeed * ((level.getGameTime() % (360 * 20 - 1) + partialTick) / 20f);
            float len = (float) Math.sqrt(hologram.rotationSpeedX * hologram.rotationSpeedX + hologram.rotationSpeedY * hologram.rotationSpeedY + hologram.rotationSpeedZ * hologram.rotationSpeedZ);
            if (len > 1e-6f)
                poseStack.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(angle), hologram.rotationSpeedX / len, hologram.rotationSpeedY / len, hologram.rotationSpeedZ / len));
        }

        poseStack.scale(1.001f, 1.001f, 1.001f);
        poseStack.translate(
                (float) ((hologram.translationX * Hologram.WIDTH / 16 - 1.5) * hologram.scale),
                (float) (hologram.translationY * Hologram.HEIGHT / 16 * hologram.scale),
                (float) ((hologram.translationZ * Hologram.WIDTH / 16 - 1.5) * hologram.scale));

        if (OCSettings.get().hologramFlickerFrequency > 0 && random.nextDouble() < OCSettings.get().hologramFlickerFrequency) {
            poseStack.scale(1 + (float) random.nextGaussian() * 0.01f, 1 + (float) random.nextGaussian() * 0.001f, 1 + (float) random.nextGaussian() * 0.01f);
            poseStack.translate((float) random.nextGaussian() * 0.01f, (float) random.nextGaussian() * 0.01f, (float) random.nextGaussian() * 0.01f);
        }

        poseStack.scale((float) (hologram.scale / 16f), (float) (hologram.scale / 16f), (float) (hologram.scale / 16f));

        int alphaInt = (int) (alpha * 255);
        if (alphaInt > 255) alphaInt = 255;
        if (alphaInt < 0) alphaInt = 0;

        var matrix = poseStack.last().pose();
        var verts = data.vertexData();
        int quadCount = data.quadCount();

        pendingDraws.add(new PendingDraw(matrix, verts, quadCount, alphaInt, packedOverlay));

        poseStack.popPose();
    }

    private CachedData rebuildQuads(Hologram hologram) {
        int w = Hologram.WIDTH;
        int h = Hologram.HEIGHT;
        int maxQuads = w * w * h * 6;
        int maxFloats = maxQuads * FLOATS_PER_QUAD;
        float[] newData = new float[maxFloats];
        int q = 0;

        for (int x = 0; x < w; x++) {
            for (int z = 0; z < w; z++) {
                for (int y = 0; y < h; y++) {
                    int colorIndex = hologram.getColor(x, y, z);
                    if (colorIndex == 0) continue;

                    int color = hologram.colors[colorIndex - 1];
                    float r = (color >> 16) & 0xFF;
                    float g = (color >> 8) & 0xFF;
                    float b = color & 0xFF;

                    if (isExposed(hologram, x, y, z + 1)) {
                        addQuad(newData, q++, x + 1, y + 1, z + 1, x, y + 1, z + 1, x, y, z + 1, x + 1, y, z + 1, 0, 0, 1, r, g, b);
                    }
                    if (isExposed(hologram, x, y, z - 1)) {
                        addQuad(newData, q++, x + 1, y, z, x, y, z, x, y + 1, z, x + 1, y + 1, z, 0, 0, -1, r, g, b);
                    }
                    if (isExposed(hologram, x + 1, y, z)) {
                        addQuad(newData, q++, x + 1, y + 1, z + 1, x + 1, y, z + 1, x + 1, y, z, x + 1, y + 1, z, 1, 0, 0, r, g, b);
                    }
                    if (isExposed(hologram, x - 1, y, z)) {
                        addQuad(newData, q++, x, y, z + 1, x, y + 1, z + 1, x, y + 1, z, x, y, z, -1, 0, 0, r, g, b);
                    }
                    if (isExposed(hologram, x, y + 1, z)) {
                        addQuad(newData, q++, x + 1, y + 1, z, x, y + 1, z, x, y + 1, z + 1, x + 1, y + 1, z + 1, 0, 1, 0, r, g, b);
                    }
                    if (isExposed(hologram, x, y - 1, z)) {
                        addQuad(newData, q++, x + 1, y, z + 1, x, y, z + 1, x, y, z, x + 1, y, z, 0, -1, 0, r, g, b);
                    }
                }
            }
        }

        return new CachedData(Arrays.copyOf(newData, q * FLOATS_PER_QUAD), q);
    }

    private void addQuad(float[] data, int quadIndex,
                         float x0, float y0, float z0,
                         float x1, float y1, float z1,
                         float x2, float y2, float z2,
                         float x3, float y3, float z3,
                         float nx, float ny, float nz,
                         float r, float g, float b) {
        int base = quadIndex * FLOATS_PER_QUAD;
        putVertex(data, base, x0, y0, z0, nx, ny, nz, r, g, b);
        putVertex(data, base + STRIDE, x1, y1, z1, nx, ny, nz, r, g, b);
        putVertex(data, base + STRIDE * 2, x2, y2, z2, nx, ny, nz, r, g, b);
        putVertex(data, base + STRIDE * 3, x3, y3, z3, nx, ny, nz, r, g, b);
    }

    private void putVertex(float[] data, int offset,
                           float x, float y, float z,
                           float nx, float ny, float nz,
                           float r, float g, float b) {
        data[offset] = x;
        data[offset + 1] = y;
        data[offset + 2] = z;
        data[offset + 3] = nx;
        data[offset + 4] = ny;
        data[offset + 5] = nz;
        data[offset + 6] = r;
        data[offset + 7] = g;
        data[offset + 8] = b;
    }

    private static boolean isExposed(Hologram hologram, int x, int y, int z) {
        if (x < 0 || y < 0 || z < 0 || x >= Hologram.WIDTH || y >= Hologram.HEIGHT || z >= Hologram.WIDTH) return true;
        return hologram.getColor(x, y, z) == 0;
    }

    @Override
    public @NotNull net.minecraft.world.phys.AABB getRenderBoundingBox(@NotNull Hologram blockEntity) {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
}
