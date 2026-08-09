package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.PrintData;
import li.cil.oc.core.impl.util.ExtendedAABB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PrintModel implements IDynamicBakedModel {
    private static final Vec3[][] UNIT_CUBE = {
            {new Vec3(0, 0, 1), new Vec3(0, 0, 0), new Vec3(1, 0, 0), new Vec3(1, 0, 1)}, // DOWN
            {new Vec3(0, 1, 0), new Vec3(0, 1, 1), new Vec3(1, 1, 1), new Vec3(1, 1, 0)}, // UP
            {new Vec3(1, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 0), new Vec3(0, 1, 0)}, // NORTH
            {new Vec3(0, 1, 1), new Vec3(0, 0, 1), new Vec3(1, 0, 1), new Vec3(1, 1, 1)}, // SOUTH
            {new Vec3(0, 1, 0), new Vec3(0, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 1)}, // WEST
            {new Vec3(1, 1, 1), new Vec3(1, 0, 1), new Vec3(1, 0, 0), new Vec3(1, 1, 0)}  // EAST
    };

    private static final Vec3[][] PLANES = {
            {new Vec3(1, 0, 0), new Vec3(0, 0, -1)},  // DOWN
            {new Vec3(1, 0, 0), new Vec3(0, 0, 1)},   // UP
            {new Vec3(-1, 0, 0), new Vec3(0, -1, 0)}, // NORTH
            {new Vec3(1, 0, 0), new Vec3(0, -1, 0)},  // SOUTH
            {new Vec3(0, 0, 1), new Vec3(0, -1, 0)},  // WEST
            {new Vec3(0, 0, -1), new Vec3(0, -1, 0)}  // EAST
    };

    private final BakedModel originalModel;

    @SuppressWarnings("unused")
    public PrintModel(BakedModel originalModel) {
        this.originalModel = originalModel;
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.neoforge.common.blockentity.PrintNeoForge print) {
            return print.getModelData();
        }
        return modelData;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        PrintData data = extraData.get(li.cil.oc.neoforge.common.blockentity.PrintNeoForge.PRINT_DATA);
        if (data != null && !(data.stateOff.isEmpty() && data.stateOn.isEmpty())) {
            Direction facing = extraData.get(li.cil.oc.neoforge.common.blockentity.PrintNeoForge.FACING);
            if (facing == null) facing = Direction.NORTH;
            Boolean st = extraData.get(li.cil.oc.neoforge.common.blockentity.PrintNeoForge.STATE);
            boolean activeState = st != null && st;
            return bakeQuads(data, activeState, facing);
        }
        return List.of();
    }

    public static List<BakedQuad> bakeQuads(PrintData data, boolean state, Direction facing) {
        List<BakedQuad> quads = new ArrayList<>();
        var shapes = state ? data.stateOn : data.stateOff;
        for (var shape : shapes) {
            if (shape.texture() == null || shape.texture().isEmpty()) continue;
            AABB bounds = ExtendedAABB.rotateTowards(shape.bounds(), facing);
            TextureAtlasSprite sprite = resolveTexture(shape.texture());
            Integer t = shape.tint();
            int tintRGB = (t != null) ? t : -1;
            buildBox(quads, bounds, sprite, -1, tintRGB);
        }
        return quads;
    }

    private static Vec3[][] makeBox(AABB bounds) {
        double minX = bounds.minX;
        double minY = bounds.minY;
        double minZ = bounds.minZ;
        double maxX = bounds.maxX;
        double maxY = bounds.maxY;
        double maxZ = bounds.maxZ;
        Vec3[][] result = new Vec3[6][4];
        for (int face = 0; face < 6; face++) {
            for (int vert = 0; vert < 4; vert++) {
                Vec3 v = UNIT_CUBE[face][vert];
                result[face][vert] = new Vec3(
                        Math.clamp(v.x, minX, maxX),
                        Math.clamp(v.y, minY, maxY),
                        Math.clamp(v.z, minZ, maxZ)
                );
            }
        }
        return result;
    }

    private static int[] quadData(Vec3[] vertices, Direction face, TextureAtlasSprite sprite, int packedColor) {
        int[] data = new int[32];
        Vec3 uAxis = PLANES[face.get3DDataValue()][0];
        Vec3 vAxis = PLANES[face.get3DDataValue()][1];
        for (int i = 0; i < 4; i++) {
            Vec3 v = vertices[i];
            double u = v.dot(uAxis);
            double vv = v.dot(vAxis);
            if (uAxis.x + uAxis.y + uAxis.z < 0) u = 1 + u;
            if (vAxis.x + vAxis.y + vAxis.z < 0) vv = 1 + vv;
            int idx = i * 8;
            data[idx] = Float.floatToRawIntBits((float) v.x);
            data[idx + 1] = Float.floatToRawIntBits((float) v.y);
            data[idx + 2] = Float.floatToRawIntBits((float) v.z);
            data[idx + 3] = packedColor;
            data[idx + 4] = Float.floatToRawIntBits(sprite.getU((float) u));
            data[idx + 5] = Float.floatToRawIntBits(sprite.getV((float) vv));
            data[idx + 6] = 0;
        }
        ClientHooks.fillNormal(data, face);
        return data;
    }

    @SuppressWarnings("SameParameterValue")
    private static void buildBox(List<BakedQuad> quads, AABB bounds, TextureAtlasSprite sprite, int tintIndex, int tintRGB) {
        int packedColor;
        if (tintRGB == -1) {
            packedColor = -1;
        } else {
            int r = (tintRGB >> 16) & 0xFF;
            int g = (tintRGB >> 8) & 0xFF;
            int b = tintRGB & 0xFF;
            packedColor = (0xFF << 24) | (b << 16) | (g << 8) | r;
        }
        Vec3[][] box = makeBox(bounds);
        for (Direction faceDir : Direction.values()) {
            Vec3[] face = box[faceDir.get3DDataValue()];
            int[] data = quadData(face, faceDir, sprite, packedColor);
            quads.add(new BakedQuad(data, tintIndex, faceDir, sprite, true));
        }
    }

    private static TextureAtlasSprite resolveTexture(String texture) {
        var getter = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite missing = getter.apply(ResourceLocation.withDefaultNamespace("missingno"));
        TextureAtlasSprite result = tryResolve(getter, missing, texture);
        if (result != null) return result;
        result = tryResolve(getter, missing, OCSettings.resourceDomain + ":block/" + texture);
        if (result != null) return result;
        result = tryResolve(getter, missing, "minecraft:block/" + texture);
        if (result != null) return result;
        return getter.apply(ResourceLocation.withDefaultNamespace("block/stone"));
    }

    @Nullable
    private static TextureAtlasSprite tryResolve(Function<ResourceLocation, TextureAtlasSprite> getter, TextureAtlasSprite missing, String path) {
        ResourceLocation loc = ResourceLocation.tryParse(path);
        if (loc == null) return null;
        TextureAtlasSprite sprite = getter.apply(loc);
        return sprite != missing ? sprite : null;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return OVERRIDES;
    }

    private static final ItemOverrides OVERRIDES = new ItemOverrides() {
        @Override
        public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
            return new ItemModel(stack);
        }
    };

    private record ItemModel(PrintData data) implements IDynamicBakedModel {
        private ItemModel(ItemStack stack) {
            this(new PrintData(stack));
        }

        @Override
        public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
            var shapes = data.hasActiveState() && li.cil.oc.neoforge.client.KeyBindings.showExtendedTooltips() ? data.stateOn : data.stateOff;
            List<BakedQuad> quads = new ArrayList<>();
            if (shapes.isEmpty()) {
                TextureAtlasSprite sprite = resolveTexture("white");
                buildBox(quads, ExtendedAABB.unitBounds(), sprite, -1, 0x66FF66);
                return quads;
            }
            for (var shape : shapes) {
                if (shape.texture() == null || shape.texture().isEmpty()) continue;
                TextureAtlasSprite sprite = resolveTexture(shape.texture());
                Integer t = shape.tint();
                int tintRGB = (t != null) ? t : -1;
                buildBox(quads, shape.bounds(), sprite, -1, tintRGB);
            }
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
            return resolveTexture(OCSettings.resourceDomain + ":block/white");
        }

        @Override
        public @NotNull ItemOverrides getOverrides() {
            return ItemOverrides.EMPTY;
        }

        @Override
        public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
            return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
        }

        @Override
        public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
            return List.of(RenderType.cutout());
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
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        PrintData printData = data.get(li.cil.oc.neoforge.common.blockentity.PrintNeoForge.PRINT_DATA);
        if (printData != null) {
            var shape = printData.stateOff.stream().findFirst().orElse(null);
            if (shape != null && shape.texture() != null) {
                return resolveTexture(shape.texture());
            }
        }
        return originalModel.getParticleIcon(data);
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        originalModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
        return this;
    }
}
