package li.cil.oc.fabric.model;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class ScreenModel implements BakedModel {
    private static final FaceBakery BAKERY = new FaceBakery();

    private final ItemOverrides overrides;

    @SuppressWarnings("unused")
    public ScreenModel() {
        this.overrides = ItemOverrides.EMPTY;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        List<BakedQuad> quads = new ArrayList<>();

        var frontSprite = sprite("opencomputers", "block/screen/f");
        var backSprite = sprite("opencomputers", "block/screen/b");
        var sideSprite = sprite("opencomputers", "block/screen/b2");

        Vector3f from = new Vector3f(0, 0, 0);
        Vector3f to = new Vector3f(16, 16, 16);

        for (Direction faceDir : Direction.values()) {
            TextureAtlasSprite sprite;
            switch (faceDir) {
                case SOUTH, NORTH -> sprite = frontSprite;
                case UP, DOWN -> sprite = backSprite;
                default -> sprite = sideSprite;
            }
            BlockElementFace face = new BlockElementFace(faceDir, 0, "", new BlockFaceUV(getUVs(from, to, faceDir), 0));
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

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    @SuppressWarnings("SameParameterValue")
    private static TextureAtlasSprite sprite(String namespace, String path) {
        var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
        TextureAtlasSprite sprite = atlas.apply(ResourceLocation.fromNamespaceAndPath(namespace, path));
        TextureAtlasSprite missing = atlas.apply(ResourceLocation.withDefaultNamespace("missingno"));
        if (sprite != missing) {
            return sprite;
        }
        return atlas.apply(ResourceLocation.withDefaultNamespace("block/stone"));
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
        return sprite("opencomputers", "block/screen/f");
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        return ITEM_TRANSFORMS;
    }

    private static final ItemTransforms ITEM_TRANSFORMS = new ItemTransforms(
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.1f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
            new ItemTransform(new Vector3f(75, 45, 0), new Vector3f(0, 0.1f, 0), new Vector3f(0.375f, 0.375f, 0.375f)),
            new ItemTransform(new Vector3f(0, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
            new ItemTransform(new Vector3f(0, 45, 0), new Vector3f(0, 0, 0), new Vector3f(0.4f, 0.4f, 0.4f)),
            ItemTransform.NO_TRANSFORM,
            new ItemTransform(new Vector3f(30, 225, 0), new Vector3f(0, 0, 0), new Vector3f(0.625f, 0.625f, 0.625f)),
            new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0.1875f, 0), new Vector3f(0.25f, 0.25f, 0.25f)),
            new ItemTransform(new Vector3f(0, 0, 0), new Vector3f(0, 0, 0), new Vector3f(0.5f, 0.5f, 0.5f))
    );
}
