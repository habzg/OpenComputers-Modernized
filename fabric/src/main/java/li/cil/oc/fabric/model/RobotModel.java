package li.cil.oc.fabric.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import li.cil.oc.core.impl.OCSettings;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.BlendMode;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class RobotModel implements BakedModel, FabricBakedModel {
    private static final ResourceLocation ROBOT_TEXTURE = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "item/robot");

    private final BakedModel originalModel;

    @SuppressWarnings("unused")
    public RobotModel(BakedModel originalModel) {
        this.originalModel = originalModel;
    }

    @Override
    public boolean isVanillaAdapter() {
        return false;
    }

    @Override
    public void emitBlockQuads(@NotNull BlockAndTintGetter blockView, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Supplier<RandomSource> randomSupplier, @NotNull RenderContext context) {
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        return Collections.emptyList();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return OVERRIDES;
    }

    private static final ItemOverrides OVERRIDES = new ItemOverrides(null, null, List.of()) {
        @Override
        public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull net.minecraft.world.item.ItemStack stack, @Nullable net.minecraft.client.multiplayer.ClientLevel level, @Nullable net.minecraft.world.entity.LivingEntity entity, int seed) {
            return new ItemModel();
        }
    };

    private static TextureAtlasSprite robotSprite() {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.apply(ROBOT_TEXTURE);
        TextureAtlasSprite missing = atlas.apply(ResourceLocation.withDefaultNamespace("missingno"));
        if (sprite != missing) {
            return sprite;
        }
        return atlas.apply(ResourceLocation.withDefaultNamespace("block/stone"));
    }

    private static class ItemModel implements BakedModel, FabricBakedModel {
        private final List<BakedQuad> quads;
        private static RenderMaterial cutoutMaterial;

        @SuppressWarnings("unused")
        ItemModel() {
            this.quads = buildQuads(0x00007F00, 0xFF7F7F7F);
        }

        @SuppressWarnings({"SameParameterValue", "unused"})
        private ItemModel(int packedNormal, int color) {
            this.quads = buildQuads(packedNormal, color);
        }

        @Override
        public boolean isVanillaAdapter() {
            return false;
        }

        @Override
        public void emitItemQuads(@NotNull ItemStack stack, @NotNull Supplier<RandomSource> randomSupplier, @NotNull RenderContext context) {
            Renderer renderer = RendererAccess.INSTANCE.getRenderer();
            if (renderer == null) return;
            if (cutoutMaterial == null) {
                cutoutMaterial = renderer.materialFinder().blendMode(BlendMode.CUTOUT).find();
            }
            QuadEmitter emitter = context.getEmitter();
            for (BakedQuad quad : quads) {
                emitter.fromVanilla(quad, cutoutMaterial, null);
                emitter.emit();
            }
        }

        @Override
        public void emitBlockQuads(@NotNull BlockAndTintGetter blockView, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Supplier<RandomSource> randomSupplier, @NotNull RenderContext context) {
        }

        private static List<BakedQuad> buildQuads(int packedNormal, int color) {
            float size = 0.4f;
            float l = 0.5f - size;
            float h = 0.5f + size;

            float[] top = {0.5f, 1f, 0.5f, 0.25f, 0.25f};
            float[] top1 = {l, 0.5f, h, 0f, 0f};
            float[] top2 = {h, 0.5f, h, 0f, 0.5f};
            float[] top3 = {h, 0.5f, l, 0.5f, 0.5f};
            float[] top4 = {l, 0.5f, l, 0.5f, 0f};

            float[] bottom = {0.5f, 0f, 0.5f, 0.75f, 0.25f};
            float[] bottom1 = {l, 0.5f, l, 0.5f, 0.5f};
            float[] bottom2 = {h, 0.5f, l, 0.5f, 0f};
            float[] bottom3 = {h, 0.5f, h, 1f, 0f};
            float[] bottom4 = {l, 0.5f, h, 1f, 0.5f};

            var sprite = robotSprite();
            List<BakedQuad> quads = new ArrayList<>();

            quads.add(buildPyramidQuad(top, top1, top2, sprite, Direction.NORTH, packedNormal, color));
            quads.add(buildPyramidQuad(top, top2, top3, sprite, Direction.EAST, packedNormal, color));
            quads.add(buildPyramidQuad(top, top3, top4, sprite, Direction.SOUTH, packedNormal, color));
            quads.add(buildPyramidQuad(top, top4, top1, sprite, Direction.WEST, packedNormal, color));

            quads.add(buildPyramidQuad(bottom, bottom1, bottom2, sprite, Direction.NORTH, packedNormal, color));
            quads.add(buildPyramidQuad(bottom, bottom2, bottom3, sprite, Direction.EAST, packedNormal, color));
            quads.add(buildPyramidQuad(bottom, bottom3, bottom4, sprite, Direction.SOUTH, packedNormal, color));
            quads.add(buildPyramidQuad(bottom, bottom4, bottom1, sprite, Direction.WEST, packedNormal, color));

            return quads;
        }

        private static BakedQuad buildPyramidQuad(float[] v0, float[] v1, float[] v2, TextureAtlasSprite sprite, Direction facing, int packedNormal, int color) {
            float[] v3 = {
                    (v2[0] + v0[0]) * 0.5f, (v2[1] + v0[1]) * 0.5f, (v2[2] + v0[2]) * 0.5f,
                    (v2[3] + v0[3]) * 0.5f, (v2[4] + v0[4]) * 0.5f
            };

            int[] vertices = new int[32];
            addVertexData(vertices, 0, v0, sprite, packedNormal, color);
            addVertexData(vertices, 1, v1, sprite, packedNormal, color);
            addVertexData(vertices, 2, v2, sprite, packedNormal, color);
            addVertexData(vertices, 3, v3, sprite, packedNormal, color);

            return new BakedQuad(vertices, -1, facing, sprite, true);
        }

        private static void addVertexData(int[] data, int index, float[] v, TextureAtlasSprite sprite, int packedNormal, int vertexColor) {
            float x = (v[0] - 0.5f) * 1.4f + 0.5f;
            float y = (v[1] - 0.5f) * 1.4f + 0.5f;
            float z = (v[2] - 0.5f) * 1.4f + 0.5f;
            float u = sprite.getU(v[3]);
            float vt = sprite.getV(v[4]);

            int offset = index * 8;
            data[offset] = Float.floatToRawIntBits(x);
            data[offset + 1] = Float.floatToRawIntBits(y);
            data[offset + 2] = Float.floatToRawIntBits(z);
            data[offset + 3] = vertexColor;
            data[offset + 4] = Float.floatToRawIntBits(u);
            data[offset + 5] = Float.floatToRawIntBits(vt);
            data[offset + 6] = 0x00000000;
            data[offset + 7] = packedNormal;
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
            return quads;
        }

        @Override
        public boolean useAmbientOcclusion() {
            return true;
        }

        @Override
        public boolean isGui3d() {
            return true;
        }

        @Override
        public boolean usesBlockLight() {
            return true;
        }

        @Override
        public boolean isCustomRenderer() {
            return false;
        }

        @Override
        public @NotNull TextureAtlasSprite getParticleIcon() {
            return robotSprite();
        }

        @Override
        public @NotNull ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public @NotNull ItemTransforms getTransforms() {
            return ITEM_TRANSFORMS;
        }
    }

    private static final ItemTransforms ITEM_TRANSFORMS = new ItemTransforms(
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.15625f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.15625f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
            new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
            new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
            ItemTransform.NO_TRANSFORM,
            new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
            new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0.1875f, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
            new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
    );

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return true;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return robotSprite();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return originalModel.getTransforms();
    }
}
