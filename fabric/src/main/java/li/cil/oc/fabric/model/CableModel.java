package li.cil.oc.fabric.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.fabric.OpenComputers;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CableModel implements BakedModel, FabricBakedModel {
  private static final FaceBakery BAKERY = new FaceBakery();

  private static final double BASE = 2.0;
  private static final double PLUG_HALF = 3.0 - 1e-4;
  private static final double OFFSET = 4.0;

  private static final double CENTER_MIN = 6.0;
  private static final double CENTER_MAX = 10.0;

  private final BakedModel originalModel;
  private TextureAtlasSprite cableSprite;
  private TextureAtlasSprite capSprite;

  public CableModel(BakedModel originalModel) {
    this.originalModel = originalModel;
  }

  private TextureAtlasSprite getCableSprite() {
    if (cableSprite == null)
      cableSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/cable"));
    return cableSprite;
  }

  private TextureAtlasSprite getCapSprite() {
    if (capSprite == null)
      capSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/cablecap"));
    return capSprite;
  }

  private static TextureAtlasSprite getBodySprite() {
    return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "block/cable"));
  }

  @Override
  public boolean isVanillaAdapter() {
    return false;
  }

  @Override
  public void emitBlockQuads(@NotNull BlockAndTintGetter blockView, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Supplier<RandomSource> randomSupplier, @NotNull RenderContext context) {
    int mask = computeConnections(blockView, pos);
    RenderMaterial material = tintedMaterial();
    QuadEmitter emitter = context.getEmitter();

    emitBox(emitter, material, getBodySprite(), CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX);

    for (Direction side : Direction.values()) {
      if ((mask & (1 << side.ordinal())) != 0) {
        double[] arm = armBox(side);
        emitBox(emitter, material, getBodySprite(), arm[0], arm[1], arm[2], arm[3], arm[4], arm[5]);

        if (!isCableAt(blockView, pos.relative(side))) {
          double[] plug = plugBox(side);
          emitBox(emitter, material, getCapSprite(), plug[0], plug[1], plug[2], plug[3], plug[4], plug[5]);
        }
      } else if (isUnconnectedCap(mask, side)) {
        double[] cap = capBox(side);
        emitBox(emitter, material, getCapSprite(), cap[0], cap[1], cap[2], cap[3], cap[4], cap[5]);
      }
    }
  }

  private static boolean isUnconnectedCap(int mask, Direction side) {
    return mask == 0 || (1 << side.getOpposite().ordinal() & mask) == mask;
  }

  private static void emitBox(QuadEmitter emitter, RenderMaterial material, TextureAtlasSprite sprite, double x0, double x1, double y0, double y1, double z0, double z1) {
    Vector3f from = new Vector3f((float) x0, (float) y0, (float) z0);
    Vector3f to = new Vector3f((float) x1, (float) y1, (float) z1);
    for (Direction face : Direction.values()) {
      float[] uv = boxUV(x0, y0, z0, x1, y1, z1, face);
      BlockElementFace faceDef = new BlockElementFace(null, 0, "", new BlockFaceUV(uv, 0));
      BakedQuad quad = BAKERY.bakeQuad(from, to, faceDef, sprite, face, BlockModelRotation.X0_Y0, null, true);
      emitter.fromVanilla(quad, material, null).emit();
    }
  }

  private static float[] boxUV(double x0, double y0, double z0, double x1, double y1, double z1, Direction dir) {
    return switch (dir) {
      case DOWN, UP -> new float[]{(float) x0, (float) z0, (float) x1, (float) z1};
      case NORTH, SOUTH -> new float[]{(float) x0, (float) (16 - y1), (float) x1, (float) (16 - y0)};
      case WEST, EAST -> new float[]{(float) z0, (float) (16 - y1), (float) z1, (float) (16 - y0)};
    };
  }

  private static int computeConnections(BlockAndTintGetter blockView, BlockPos pos) {
    int selfColor = getColor(blockView, pos);
    int connections = 0;
    for (Direction side : Direction.values()) {
      BlockPos neighbor = pos.relative(side);
      BlockEntity neighborTE = blockView.getBlockEntity(neighbor);
      if (isOCNeighbor(neighborTE, side.getOpposite())) {
        int neighborColor = getColor(blockView, neighbor);
        if (selfColor == neighborColor || selfColor == Color.LightGray || neighborColor == Color.LightGray) {
          connections |= 1 << side.ordinal();
        }
      }
    }
    return connections;
  }

  private static boolean isOCNeighbor(BlockEntity te, Direction side) {
    if (te instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy) return false;
    if (te instanceof li.cil.oc.api.network.Environment || te instanceof SidedEnvironment) {
      if (te instanceof SidedEnvironment sideEnv) return sideEnv.canConnect(side);
      return true;
    }
    return false;
  }

  private static boolean isCableAt(BlockAndTintGetter blockView, BlockPos pos) {
    BlockEntity te = blockView.getBlockEntity(pos);
    return te instanceof Cable;
  }

  private static int getColor(BlockAndTintGetter blockView, BlockPos pos) {
    BlockEntity te = blockView.getBlockEntity(pos);
    if (te instanceof Cable cable) return cable.color();
    return Color.LightGray;
  }

  private static double[] armBox(Direction side) {
    double sx = side.getStepX(), sy = side.getStepY(), sz = side.getStepZ();
    double minX = -BASE + sx * OFFSET;
    double maxX = BASE + sx * OFFSET;
    double minY = -BASE + sy * OFFSET;
    double maxY = BASE + sy * OFFSET;
    double minZ = -BASE + sz * OFFSET;
    double maxZ = BASE + sz * OFFSET;
    minX = Math.min(minX, sx * 8.0);
    maxX = Math.max(maxX, sx * 8.0);
    minY = Math.min(minY, sy * 8.0);
    maxY = Math.max(maxY, sy * 8.0);
    minZ = Math.min(minZ, sz * 8.0);
    maxZ = Math.max(maxZ, sz * 8.0);
    return new double[]{minX + 8.0, maxX + 8.0, minY + 8.0, maxY + 8.0, minZ + 8.0, maxZ + 8.0};
  }

  private static double[] plugBox(Direction side) {
    double sx = side.getStepX(), sy = side.getStepY(), sz = side.getStepZ();
    double minX = -PLUG_HALF + sx * OFFSET;
    double maxX = PLUG_HALF + sx * OFFSET;
    double minY = -PLUG_HALF + sy * OFFSET;
    double maxY = PLUG_HALF + sy * OFFSET;
    double minZ = -PLUG_HALF + sz * OFFSET;
    double maxZ = PLUG_HALF + sz * OFFSET;
    minX = Math.clamp(minX + sx * 10.0, -0.5 - 1e-4, 7.0);
    maxX = Math.clamp(maxX + sx * 10.0, -7.0, 0.5 + 1e-4);
    minY = Math.clamp(minY + sy * 10.0, -0.5 - 1e-4, 7.0);
    maxY = Math.clamp(maxY + sy * 10.0, -7.0, 0.5 + 1e-4);
    minZ = Math.clamp(minZ + sz * 10.0, -0.5 - 1e-4, 7.0);
    maxZ = Math.clamp(maxZ + sz * 10.0, -7.0, 0.5 + 1e-4);
    return new double[]{minX + 8.0, maxX + 8.0, minY + 8.0, maxY + 8.0, minZ + 8.0, maxZ + 8.0};
  }

  private static double[] capBox(Direction side) {
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
    return new double[]{minX + 8.0, maxX + 8.0, minY + 8.0, maxY + 8.0, minZ + 8.0, maxZ + 8.0};
  }

  private static RenderMaterial tintedMaterial;

  private static RenderMaterial tintedMaterial() {
    if (tintedMaterial == null) {
      Renderer renderer = RendererAccess.INSTANCE.getRenderer();
      if (renderer != null) {
        tintedMaterial = renderer.materialFinder().find();
      }
    }
    return tintedMaterial;
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
    List<BakedQuad> quads = new ArrayList<>();
    TextureAtlasSprite body = getCableSprite();
    TextureAtlasSprite cap = getCapSprite();

    emitItemBox(quads, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, body, 0);
    emitItemBox(quads, 6, 10, 10, 15, 6, 10, body, 0);
    emitItemBox(quads, 5, 11, 15, 16, 5, 11, cap, -1);
    emitItemBox(quads, 6, 10, 1, 6, 6, 10, body, 0);
    emitItemBox(quads, 5, 11, 0, 1, 5, 11, cap, -1);

    return quads;
  }

  private static void emitItemBox(List<BakedQuad> quads, double x0, double x1, double y0, double y1, double z0, double z1, TextureAtlasSprite sprite, int tintIndex) {
    Vector3f from = new Vector3f((float) x0, (float) y0, (float) z0);
    Vector3f to = new Vector3f((float) x1, (float) y1, (float) z1);
    for (Direction face : Direction.values()) {
      float[] uv = boxUV(x0, y0, z0, x1, y1, z1, face);
      BlockElementFace faceDef = new BlockElementFace(null, tintIndex, "", new BlockFaceUV(uv, 0));
      quads.add(BAKERY.bakeQuad(from, to, faceDef, sprite, face, BlockModelRotation.X0_Y0, null, true));
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
  public @NotNull TextureAtlasSprite getParticleIcon() {
    return getCableSprite();
  }

  @Override
  public @NotNull ItemOverrides getOverrides() {
    return OVERRIDES;
  }

  @SuppressWarnings("DataFlowIssue")
  private static final ItemOverrides OVERRIDES = new ItemOverrides(null, null, List.of()) {
    @Override
    public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
      return original;
    }
  };

  @Override
  public @NotNull ItemTransforms getTransforms() {
    return originalModel.getTransforms();
  }
}
