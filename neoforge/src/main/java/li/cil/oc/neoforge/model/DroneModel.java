package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.impl.Settings;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public class DroneModel implements IDynamicBakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();
    private static final ResourceLocation DRONE_TEXTURE = ResourceLocation.fromNamespaceAndPath(Settings.resourceDomain, "item/drone");

    private final ItemOverrides overrides;

    @SuppressWarnings("unused")
    public DroneModel() {
        this.overrides = new ItemOverrides() {
            @Override
            public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                return DroneModel.this;
            }
        };
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
        var sprite = droneSprite();
        List<BakedQuad> quads = new ArrayList<>();

        quads.addAll(bakeBox(new Vector3f(1, 7, 1), new Vector3f(7, 8, 7), sprite));
        quads.addAll(bakeBox(new Vector3f(1, 7, 9), new Vector3f(7, 8, 15), sprite));
        quads.addAll(bakeBox(new Vector3f(9, 7, 1), new Vector3f(15, 8, 7), sprite));
        quads.addAll(bakeBox(new Vector3f(9, 7, 9), new Vector3f(15, 8, 15), sprite));
        quads.addAll(bakeRotatedBox(new Vector3f(6, 6, 6), new Vector3f(10, 9, 10), 45, sprite));

        return quads;
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    private static TextureAtlasSprite droneSprite() {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.apply(DRONE_TEXTURE);
        TextureAtlasSprite missing = atlas.apply(ResourceLocation.withDefaultNamespace("missingno"));
        if (sprite != missing) {
            return sprite;
        }
        return atlas.apply(ResourceLocation.withDefaultNamespace("block/stone"));
    }

    private static List<BakedQuad> bakeBox(Vector3f from, Vector3f to, TextureAtlasSprite sprite) {
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction faceDir : Direction.values()) {
            BlockElementFace face = new BlockElementFace(faceDir, -1, "", new BlockFaceUV(getUVs(from, to, faceDir), 0));
            BakedQuad quad = BAKERY.bakeQuad(from, to, face, sprite, faceDir, BlockModelRotation.X0_Y0, null, false);
            int[] vertices = quad.getVertices();
            for (int i = 0; i < 4; i++) {
                vertices[i * 8 + 3] = 0xFFFFFFFF;
                vertices[i * 8 + 6] = 0;
            }
            quads.add(quad);
        }
        return quads;
    }

    private static float[] getUVs(Vector3f from, Vector3f to, Direction dir) {
        float x1 = from.x();
        float y1 = from.y();
        float z1 = from.z();
        float x2 = to.x();
        float y2 = to.y();
        float z2 = to.z();
        return switch (dir) {
            case DOWN, UP -> new float[]{x1, z1, x2, z2};
            case NORTH, SOUTH -> new float[]{x1, 16 - y2, x2, 16 - y1};
            case WEST, EAST -> new float[]{z1, 16 - y2, z2, 16 - y1};
        };
    }

    @SuppressWarnings({"SameParameterValue", "unused"})
    private static List<BakedQuad> bakeRotatedBox(Vector3f from, Vector3f to, float angle, TextureAtlasSprite sprite) {
        float cx = (from.x() + to.x()) / 2.0f;
        float cy = (from.y() + to.y()) / 2.0f;
        float cz = (from.z() + to.z()) / 2.0f;

        Vector3f[][] unitCube = {
                {new Vector3f(0, 0, 1), new Vector3f(0, 0, 0), new Vector3f(1, 0, 0), new Vector3f(1, 0, 1)}, // DOWN
                {new Vector3f(0, 1, 0), new Vector3f(0, 1, 1), new Vector3f(1, 1, 1), new Vector3f(1, 1, 0)}, // UP
                {new Vector3f(1, 1, 0), new Vector3f(1, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0, 1, 0)}, // NORTH
                {new Vector3f(0, 1, 1), new Vector3f(0, 0, 1), new Vector3f(1, 0, 1), new Vector3f(1, 1, 1)}, // SOUTH
                {new Vector3f(0, 1, 0), new Vector3f(0, 0, 0), new Vector3f(0, 0, 1), new Vector3f(0, 1, 1)}, // WEST
                {new Vector3f(1, 1, 1), new Vector3f(1, 0, 1), new Vector3f(1, 0, 0), new Vector3f(1, 1, 0)}  // EAST
        };

        float minX = from.x() / 16.0f;
        float minY = from.y() / 16.0f;
        float minZ = from.z() / 16.0f;
        float maxX = to.x() / 16.0f;
        float maxY = to.y() / 16.0f;
        float maxZ = to.z() / 16.0f;

        float ocx = cx / 16.0f;
        float ocy = cy / 16.0f;
        float ocz = cz / 16.0f;

        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);

        List<BakedQuad> quads = new ArrayList<>();
        Direction[] dirs = Direction.values();

        for (int fi = 0; fi < 6; fi++) {
            Direction faceDir = dirs[fi];
            Vector3f[] face = unitCube[fi];

            int[] vertices = new int[32];

            float[] rxArr = new float[4], ryArr = new float[4], rzArr = new float[4];
            float[] uArr = new float[4], vtArr = new float[4];

            for (int vi = 0; vi < 4; vi++) {
                float vx = minX + face[vi].x() * (maxX - minX);
                float vy = minY + face[vi].y() * (maxY - minY);
                float vz = minZ + face[vi].z() * (maxZ - minZ);
                double dx = vx - ocx;
                double dz = vz - ocz;
                rxArr[vi] = (float) (dx * cos - dz * sin) + ocx;
                ryArr[vi] = vy;
                rzArr[vi] = (float) (dx * sin + dz * cos) + ocz;

                float uPixel, vPixel;
                switch (faceDir) {
                    case DOWN, UP -> {
                        uPixel = from.x() + face[vi].x() * (to.x() - from.x());
                        vPixel = from.z() + face[vi].z() * (to.z() - from.z());
                    }
                    case NORTH, SOUTH -> {
                        uPixel = from.x() + face[vi].x() * (to.x() - from.x());
                        vPixel = from.y() + (1 - face[vi].y()) * (to.y() - from.y());
                    }
                    default -> { // WEST, EAST
                        uPixel = from.z() + face[vi].z() * (to.z() - from.z());
                        vPixel = from.y() + (1 - face[vi].y()) * (to.y() - from.y());
                    }
                }
                uArr[vi] = sprite.getU(uPixel / 16.0f);
                vtArr[vi] = sprite.getV(vPixel / 16.0f);
            }

            int packedNormal = computePackedNormal(rxArr, ryArr, rzArr);

            for (int vi = 0; vi < 4; vi++) {
                int offset = vi * 8;
                vertices[offset] = Float.floatToRawIntBits(rxArr[vi]);
                vertices[offset + 1] = Float.floatToRawIntBits(ryArr[vi]);
                vertices[offset + 2] = Float.floatToRawIntBits(rzArr[vi]);
                vertices[offset + 3] = 0xFFFFFFFF;
                vertices[offset + 4] = Float.floatToRawIntBits(uArr[vi]);
                vertices[offset + 5] = Float.floatToRawIntBits(vtArr[vi]);
                vertices[offset + 6] = 0x00F000F0;
                vertices[offset + 7] = packedNormal;
            }

            quads.add(new BakedQuad(vertices, -1, faceDir, sprite, false));
        }

        return quads;
    }

    private static int computePackedNormal(float[] rxArr, float[] ryArr, float[] rzArr) {
        float e1x = rxArr[1] - rxArr[0], e1y = ryArr[1] - ryArr[0], e1z = rzArr[1] - rzArr[0];
        float e2x = rxArr[2] - rxArr[0], e2y = ryArr[2] - ryArr[0], e2z = rzArr[2] - rzArr[0];
        float nx = e1y * e2z - e1z * e2y;
        float ny = e1z * e2x - e1x * e2z;
        float nz = e1x * e2y - e1y * e2x;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0001f) {
            nx /= len;
            ny /= len;
            nz /= len;
        }
        return ((int) (nx * 127) & 0xFF) | (((int) (ny * 127) & 0xFF) << 8) | (((int) (nz * 127) & 0xFF) << 16);
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
        return droneSprite();
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

    @Override
    public @NotNull List<RenderType> getRenderTypes(@NotNull ItemStack itemStack, boolean fabulous) {
        return List.of(RenderType.cutout());
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        switch (transformType) {
            case THIRD_PERSON_RIGHT_HAND ->
                    new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.15625f, 0), new Vector3f(0.375f, 0.375f, 0.375f)).apply(applyLeftHandTransform, poseStack);
            case FIRST_PERSON_LEFT_HAND ->
                    new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
            case FIRST_PERSON_RIGHT_HAND ->
                    new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)).apply(applyLeftHandTransform, poseStack);
            case GUI ->
                    new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)).apply(applyLeftHandTransform, poseStack);
            case GROUND ->
                    new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0.1875f, 0), new Vector3f(0.25f, 0.25f, 0.25f)).apply(applyLeftHandTransform, poseStack);
            case FIXED ->
                    new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f)).apply(applyLeftHandTransform, poseStack);
        }
        return this;
    }
}
