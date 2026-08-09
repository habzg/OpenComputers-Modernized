package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CableModel implements IDynamicBakedModel {
    private final ResourceLocation cableLoc;
    private final ResourceLocation capLoc;
    private final BakedModel originalModel;
    private TextureAtlasSprite cableSprite;
    private TextureAtlasSprite capSprite;
    private static final FaceBakery BAKERY = new FaceBakery();

    @SuppressWarnings("unused")
    private record BoxDef(float x1, float y1, float z1, float x2, float y2, float z2) {
    }

    private static final BoxDef MIDDLE = new BoxDef(6, 6, 6, 10, 10, 10);
    private static final BoxDef UP_SHORT = new BoxDef(6, 10, 6, 10, 15, 10);
    private static final BoxDef UP_PLUG = new BoxDef(5, 15, 5, 11, 16, 11);
    private static final BoxDef DOWN_SHORT = new BoxDef(6, 1, 6, 10, 6, 10);
    private static final BoxDef DOWN_PLUG = new BoxDef(5, 0, 5, 11, 1, 11);

    @SuppressWarnings("unused")
    public CableModel(ResourceLocation cableLoc, ResourceLocation capLoc, BakedModel originalModel) {
        this.cableLoc = cableLoc;
        this.capLoc = capLoc;
        this.originalModel = originalModel;
    }

    private TextureAtlasSprite getCableSprite() {
        if (cableSprite == null)
            cableSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(cableLoc);
        return cableSprite;
    }

    private TextureAtlasSprite getCapSprite() {
        if (capSprite == null)
            capSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(capLoc);
        return capSprite;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        List<BakedQuad> quads = new ArrayList<>();

        buildBox(quads, MIDDLE, getCableSprite(), true);
        buildBox(quads, UP_SHORT, getCableSprite(), true);
        buildBox(quads, UP_PLUG, getCapSprite(), false);
        buildBox(quads, DOWN_SHORT, getCableSprite(), true);
        buildBox(quads, DOWN_PLUG, getCapSprite(), false);

        return quads;
    }

    static void buildBox(List<BakedQuad> quads, BoxDef box, TextureAtlasSprite sprite, boolean colored) {
        Vector3f from = new Vector3f(box.x1(), box.y1(), box.z1());
        Vector3f to = new Vector3f(box.x2(), box.y2(), box.z2());

        for (Direction faceDir : Direction.values()) {
            float[] uv = getUVs(box, faceDir);
            BlockElementFace face = new BlockElementFace(faceDir, colored ? 0 : -1, "", new BlockFaceUV(uv, 0));
            quads.add(BAKERY.bakeQuad(from, to, face, sprite, faceDir, BlockModelRotation.X0_Y0, null, true));
        }
    }

    private static float[] getUVs(BoxDef box, Direction dir) {
        return switch (dir) {
            case DOWN, UP -> new float[]{box.x1(), box.z1(), box.x2(), box.z2()};
            case NORTH, SOUTH -> new float[]{box.x1(), 16 - box.y2(), box.x2(), 16 - box.y1()};
            case WEST, EAST -> new float[]{box.z1(), 16 - box.y2(), box.z2(), 16 - box.y1()};
        };
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
        this.originalModel.applyTransform(transformType, poseStack, applyLeftHandTransform);
        return this;
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
        return getCableSprite();
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return ItemOverrides.EMPTY;
    }

    @Override
    public @NotNull ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        return ChunkRenderTypeSet.of(RenderType.cutout());
    }

}
