package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Rack;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class RackModel implements IDynamicBakedModel {
  private static final FaceBakery BAKERY = new FaceBakery();

  private final BakedModel originalModel;

  public RackModel(BakedModel originalModel) {
    this.originalModel = originalModel;
  }

  // --- ModelData keys ---

  public static final ModelProperty<ResourceLocation[]> SLOT_TEXTURES = new ModelProperty<>();

  @Override
  public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
    BlockEntity be = level.getBlockEntity(pos);
    if (be instanceof Rack rack) {
      ResourceLocation[] textures = new ResourceLocation[rack.getContainerSize()];
      for (int i = 0; i < rack.getContainerSize(); i++) {
        var stack = rack.getItem(i);
        if (!stack.isEmpty()) {
          textures[i] = baseTextureFor(li.cil.oc.api.Items.get(stack));
        }
      }
      return ModelData.builder().with(SLOT_TEXTURES, textures).build();
    }
    return modelData;
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
    List<BakedQuad> quads = new ArrayList<>();

    if (side == null) {
      quads.addAll(originalModel.getQuads(state, null, rand, extraData, renderType));
    } else {
      quads.addAll(originalModel.getQuads(state, side, rand, extraData, renderType));
    }

    ResourceLocation[] slotTextures = extraData.get(SLOT_TEXTURES);
    if (slotTextures != null && state != null) {
      BlockModelRotation modelRotation = getRotation(state);
      for (int i = 0; i < 4; i++) {
        if (slotTextures[i] != null) {
          addMountableFace(quads, i, slotTextures[i], modelRotation);
        }
      }
    }

    return quads;
  }

  private static BlockModelRotation getRotation(BlockState state) {
    if (!state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) return BlockModelRotation.X0_Y0;
    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    return switch (facing) {
      case EAST -> BlockModelRotation.X0_Y90;
      case SOUTH -> BlockModelRotation.X0_Y180;
      case WEST -> BlockModelRotation.X0_Y270;
      default -> BlockModelRotation.X0_Y0;
    };
  }

  private static final ResourceLocation GENERIC_TOP = ResourceLocation.fromNamespaceAndPath("opencomputers", "block/generic_top");

  private void addMountableFace(List<BakedQuad> quads, int slot, ResourceLocation texture, BlockModelRotation modelRotation) {
    TextureAtlasSprite slotSprite = getSprite(texture);
    TextureAtlasSprite topSprite = getSprite(GENERIC_TOP);
    float y0 = 11 - slot * 3f;
    float y1 = y0 + 3f;
    Vector3f from = new Vector3f(0.5f, y0, 0.5f);
    Vector3f to = new Vector3f(15.5f, y1, 15.5f);
    float[] uvs = new float[]{0.5f, 2 + slot * 3f, 15.5f, 5 + slot * 3f};
    float[] topUvs = new float[]{0.5f, 0.5f, 15.5f, 15.5f};
    for (Direction direction : Direction.values()) {
      boolean isTopBottom = direction.getAxis() == Direction.Axis.Y;
      TextureAtlasSprite sprite = isTopBottom ? topSprite : slotSprite;
      float[] faceUvs = isTopBottom ? topUvs : uvs;
      BlockElementFace face = new BlockElementFace(null, -1, "", new BlockFaceUV(faceUvs, 0));
      BakedQuad quad = BAKERY.bakeQuad(from, to, face, sprite, direction, modelRotation, null, true);
      quads.add(quad);
    }
  }

  private static TextureAtlasSprite getSprite(ResourceLocation loc) {
    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    TextureAtlasSprite sprite = atlas.apply(loc);
    TextureAtlasSprite missing = atlas.apply(ResourceLocation.fromNamespaceAndPath("minecraft", "missingno"));
    return sprite != missing ? sprite : atlas.apply(ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"));
  }

  private static final li.cil.oc.api.detail.ItemInfo DISK_DRIVE_MOUNTABLE = li.cil.oc.api.Items.get(Constants.ItemName.DiskDriveMountable);
  private static final li.cil.oc.api.detail.ItemInfo[] SERVERS = new li.cil.oc.api.detail.ItemInfo[]{
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier1),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier2),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerTier3),
    li.cil.oc.api.Items.get(Constants.ItemName.ServerCreative)
  };
  private static final li.cil.oc.api.detail.ItemInfo TERMINAL_SERVER = li.cil.oc.api.Items.get(Constants.ItemName.TerminalServer);

  private static ResourceLocation baseTextureFor(li.cil.oc.api.detail.ItemInfo itemInfo) {
    if (itemInfo == null) return null;
    if (itemInfo == DISK_DRIVE_MOUNTABLE) return Textures.blockRackDiskDrive;
    for (var server : SERVERS) {
      if (itemInfo == server) return Textures.blockRackServer;
    }
    if (itemInfo == TERMINAL_SERVER) return Textures.blockRackTerminalServer;
    return null;
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

  @SuppressWarnings("deprecation")
  @Override
  public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
    return originalModel.getParticleIcon();
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
