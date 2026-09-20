package li.cil.oc.fabric.model;

import java.util.List;
import java.util.function.Supplier;

import li.cil.oc.fabric.OpenComputers;
import li.cil.oc.fabric.common.blockentity.RackFabric;
import net.fabricmc.fabric.api.renderer.v1.Renderer;
import net.fabricmc.fabric.api.renderer.v1.RendererAccess;
import net.fabricmc.fabric.api.renderer.v1.material.RenderMaterial;
import net.fabricmc.fabric.api.renderer.v1.mesh.QuadEmitter;
import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.fabricmc.fabric.api.renderer.v1.render.RenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class RackModel implements BakedModel, FabricBakedModel {
  private static final FaceBakery BAKERY = new FaceBakery();

  private final BakedModel originalModel;

  @SuppressWarnings("unused")
  public RackModel(BakedModel originalModel) {
    this.originalModel = originalModel;
  }

  @Override
  public boolean isVanillaAdapter() {
    return false;
  }

  @Override
  public void emitBlockQuads(@NotNull BlockAndTintGetter blockView, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Supplier<RandomSource> randomSupplier, @NotNull RenderContext context) {
    Object renderData = blockView.getBlockEntityRenderData(pos);
    ResourceLocation[] slotTextures = null;
    if (renderData instanceof RackFabric.RackRenderData(ResourceLocation[] textures)) {
      slotTextures = textures;
    }

    RenderMaterial material = getStandardMaterial();
    QuadEmitter emitter = context.getEmitter();

    for (BakedQuad quad : originalModel.getQuads(state, null, randomSupplier.get())) {
      emitter.fromVanilla(quad, material, null).emit();
    }
    for (Direction dir : Direction.values()) {
      for (BakedQuad quad : originalModel.getQuads(state, dir, randomSupplier.get())) {
        emitter.fromVanilla(quad, material, null).emit();
      }
    }

    if (slotTextures != null) {
      BlockModelRotation modelRotation = getRotation(state);
      for (int i = 0; i < 4; i++) {
        if (slotTextures[i] != null) {
          addMountableFace(emitter, material, i, slotTextures[i], modelRotation);
        }
      }
    }
  }

  private static BlockModelRotation getRotation(BlockState state) {
    Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
    return switch (facing) {
      case EAST -> BlockModelRotation.X0_Y90;
      case SOUTH -> BlockModelRotation.X0_Y180;
      case WEST -> BlockModelRotation.X0_Y270;
      default -> BlockModelRotation.X0_Y0;
    };
  }

  private static final ResourceLocation GENERIC_TOP = ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/generic_top");

  private static void addMountableFace(QuadEmitter emitter, RenderMaterial material, int slot, ResourceLocation texture, BlockModelRotation modelRotation) {
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
      emitter.fromVanilla(quad, material, null).emit();
    }
  }

  private static RenderMaterial standardMaterial;

  private static RenderMaterial getStandardMaterial() {
    if (standardMaterial == null) {
      Renderer renderer = RendererAccess.INSTANCE.getRenderer();
      if (renderer != null) {
        standardMaterial = renderer.materialFinder().find();
      }
    }
    return standardMaterial;
  }

  private static TextureAtlasSprite getSprite(ResourceLocation loc) {
    var atlas = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS);
    TextureAtlasSprite sprite = atlas.apply(loc);
    TextureAtlasSprite missing = atlas.apply(ResourceLocation.fromNamespaceAndPath("minecraft", "missingno"));
    return sprite != missing ? sprite : atlas.apply(ResourceLocation.fromNamespaceAndPath("minecraft", "block/stone"));
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
    return List.of();
  }

  @Override
  public @NotNull ItemOverrides getOverrides() {
    return overrides;
  }

  @SuppressWarnings("DataFlowIssue")
  private final ItemOverrides overrides = new ItemOverrides(null, null, List.of()) {
    @Override
    public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
      return originalModel;
    }
  };

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
    return originalModel.getParticleIcon();
  }

  @Override
  public @NotNull ItemTransforms getTransforms() {
    return originalModel.getTransforms();
  }
}
