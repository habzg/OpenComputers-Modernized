package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class NetSplitterModel implements IDynamicBakedModel {
    public static final ModelProperty<Byte> OPEN_SIDES = new ModelProperty<>();

    private static final FaceBakery BAKERY = new FaceBakery();

    private static final ResourceLocation TOP_TEX = ResourceLocation.fromNamespaceAndPath("opencomputers", "block/netsplitter_top");
    private static final ResourceLocation SIDE_TEX = ResourceLocation.fromNamespaceAndPath("opencomputers", "block/netsplitter_side");

    private static TextureAtlasSprite topSprite;
    private static TextureAtlasSprite sideSprite;

    private final ItemOverrides overrides;

    @SuppressWarnings("unused")
    public NetSplitterModel() {
        this.overrides = new ItemOverrides() {
            @Override
            public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                return new ItemModel();
            }
        };
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof li.cil.oc.core.impl.common.tileentity.NetSplitter splitter) {
            byte sides = 0;
            for (Direction dir : Direction.values()) {
                if (splitter.isSideOpen(dir)) {
                    sides |= (byte) (1 << dir.get3DDataValue());
                }
            }
            return ModelData.builder().with(OPEN_SIDES, sides).build();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        Byte openSidesRaw = extraData.get(OPEN_SIDES);
        boolean[] openSides;
        if (openSidesRaw != null) {
            openSides = new boolean[6];
            for (int i = 0; i < 6; i++) {
                openSides[i] = (openSidesRaw & (1 << i)) != 0;
            }
        } else {
            openSides = new boolean[]{false, false, false, false, false, false};
        }

        List<BakedQuad> quads = new ArrayList<>();
        addBaseQuads(quads);
        addSideQuads(quads, openSides);
        return quads;
    }

    private static void addBaseQuads(List<BakedQuad> quads) {
        addBox(quads, 0, 0, 5, 5, 5, 11);
        addBox(quads, 11, 0, 5, 16, 5, 11);
        addBox(quads, 5, 0, 0, 11, 5, 5);
        addBox(quads, 5, 0, 11, 11, 5, 16);
        addBox(quads, 0, 0, 0, 5, 16, 5);
        addBox(quads, 11, 0, 0, 16, 16, 5);
        addBox(quads, 0, 0, 11, 5, 16, 16);
        addBox(quads, 11, 0, 11, 16, 16, 16);
        addBox(quads, 0, 11, 5, 5, 16, 11);
        addBox(quads, 11, 11, 5, 16, 16, 11);
        addBox(quads, 5, 11, 0, 11, 16, 5);
        addBox(quads, 5, 11, 11, 11, 16, 16);
    }

    private static void addSideQuads(List<BakedQuad> quads, boolean[] openSides) {
        // Down
        boolean down = openSides[Direction.DOWN.get3DDataValue()];
        addBox(quads, 5, down ? 0 : 2, 5, 11, 5, 11);

        // Up
        boolean up = openSides[Direction.UP.get3DDataValue()];
        addBox(quads, 5, 11, 5, 11, up ? 16 : 14, 11);

        // North
        boolean north = openSides[Direction.NORTH.get3DDataValue()];
        addBox(quads, 5, 5, north ? 0 : 2, 11, 11, 5);

        // South
        boolean south = openSides[Direction.SOUTH.get3DDataValue()];
        addBox(quads, 5, 5, 11, 11, 11, south ? 16 : 14);

        // West
        boolean west = openSides[Direction.WEST.get3DDataValue()];
        addBox(quads, west ? 0 : 2, 5, 5, 5, 11, 11);

        // East
        boolean east = openSides[Direction.EAST.get3DDataValue()];
        addBox(quads, 11, 5, 5, east ? 16 : 14, 11, 11);
    }

    private static void addBox(List<BakedQuad> quads, float x1, float y1, float z1, float x2, float y2, float z2) {
        Vector3f from = new Vector3f(x1, y1, z1);
        Vector3f to = new Vector3f(x2, y2, z2);
        for (Direction faceDir : Direction.values()) {
            TextureAtlasSprite sprite = switch (faceDir) {
                case DOWN, UP -> getTopSprite();
                default -> getSideSprite();
            };
            BlockElementFace face = new BlockElementFace(faceDir, -1, "", new BlockFaceUV(getUVs(from, to, faceDir), 0));
            quads.add(BAKERY.bakeQuad(from, to, face, sprite, faceDir, BlockModelRotation.X0_Y0, null, true));
        }
    }

    private static float[] getUVs(Vector3f from, Vector3f to, Direction dir) {
        float x1 = from.x(), y1 = from.y(), z1 = from.z();
        float x2 = to.x(),   y2 = to.y(),   z2 = to.z();
        return switch (dir) {
            case DOWN  -> new float[]{x1,      16 - z2, x2,      16 - z1};
            case UP    -> new float[]{x1,      z1,      x2,      z2     };
            case NORTH -> new float[]{16 - x2, 16 - y2, 16 - x1, 16 - y1};
            case SOUTH -> new float[]{x1,      16 - y2, x2,      16 - y1};
            case WEST  -> new float[]{z1,      16 - y2, z2,      16 - y1};
            case EAST  -> new float[]{16 - z2, 16 - y2, 16 - z1, 16 - y1};
        };
    }

    private static TextureAtlasSprite getTopSprite() {
        if (topSprite == null)
            topSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(TOP_TEX);
        return topSprite;
    }

    private static TextureAtlasSprite getSideSprite() {
        if (sideSprite == null)
            sideSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SIDE_TEX);
        return sideSprite;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
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
        return getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return getSideSprite();
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        switch (transformType) {
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                    new ItemTransform(new org.joml.Vector3f(75, 45, 0), new org.joml.Vector3f(0, 0.1f, 0), new org.joml.Vector3f(0.375f, 0.375f, 0.375f)).apply(applyLeftHandTransform, poseStack);
            case FIRST_PERSON_LEFT_HAND ->
                    new ItemTransform(new org.joml.Vector3f(0, 225, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
            case FIRST_PERSON_RIGHT_HAND ->
                    new ItemTransform(new org.joml.Vector3f(0, 45, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
            case GUI ->
                    new ItemTransform(new org.joml.Vector3f(30, 225, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.625f, 0.625f, 0.625f)).apply(applyLeftHandTransform, poseStack);
            case GROUND ->
                    new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0.1875f, 0), new org.joml.Vector3f(0.25f, 0.25f, 0.25f)).apply(applyLeftHandTransform, poseStack);
            case FIXED ->
                    new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.5f, 0.5f, 0.5f)).apply(applyLeftHandTransform, poseStack);
        }
        return this;
    }

    @SuppressWarnings("unused")
    private static class ItemModel implements IDynamicBakedModel {
        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            List<BakedQuad> quads = new ArrayList<>();
            addBaseQuads(quads);
            addSideQuads(quads, new boolean[]{false, false, false, false, false, false});
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
            return getParticleIcon(ModelData.EMPTY);
        }

        @Override
        public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
            return getSideSprite();
        }

        @Override
        public @NotNull ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return ChunkRenderTypeSet.of(RenderType.cutout());
        }

        @Override
        public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
            switch (transformType) {
                case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                        new ItemTransform(new org.joml.Vector3f(75, 45, 0), new org.joml.Vector3f(0, 0.1f, 0), new org.joml.Vector3f(0.375f, 0.375f, 0.375f)).apply(applyLeftHandTransform, poseStack);
                case FIRST_PERSON_LEFT_HAND ->
                        new ItemTransform(new org.joml.Vector3f(0, 225, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
                case FIRST_PERSON_RIGHT_HAND ->
                        new ItemTransform(new org.joml.Vector3f(0, 45, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
                case GUI ->
                        new ItemTransform(new org.joml.Vector3f(30, 225, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.625f, 0.625f, 0.625f)).apply(applyLeftHandTransform, poseStack);
                case GROUND ->
                        new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0.1875f, 0), new org.joml.Vector3f(0.25f, 0.25f, 0.25f)).apply(applyLeftHandTransform, poseStack);
                case FIXED ->
                        new ItemTransform(new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0, 0, 0), new org.joml.Vector3f(0.5f, 0.5f, 0.5f)).apply(applyLeftHandTransform, poseStack);
            }
            return this;
        }
    }
}
