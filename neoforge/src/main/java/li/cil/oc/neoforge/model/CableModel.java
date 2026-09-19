package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.util.Color;
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
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class CableModel implements IDynamicBakedModel {
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
      cableSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
        ResourceLocation.fromNamespaceAndPath("opencomputers", "block/cable"));
    return cableSprite;
  }

  private TextureAtlasSprite getCapSprite() {
    if (capSprite == null)
      capSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(
        ResourceLocation.fromNamespaceAndPath("opencomputers", "block/cablecap"));
    return capSprite;
  }

  public static final ModelProperty<Integer> CABLE_CONNECTIONS = new ModelProperty<>();
  public static final ModelProperty<Set<Direction>> CABLE_NEIGHBORS = new ModelProperty<>();

  @Override
  public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
    int connections = computeConnections(level, pos);
    Set<Direction> cableNeighbors = computeCableNeighbors(level, pos);
    return ModelData.builder()
      .with(CABLE_CONNECTIONS, connections)
      .with(CABLE_NEIGHBORS, cableNeighbors)
      .build();
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
    List<BakedQuad> quads = new ArrayList<>();

    if (state != null) {
      Integer maskObj = extraData.get(CABLE_CONNECTIONS);
      int mask = maskObj != null ? maskObj : 0;
      Set<Direction> cableNeighbors = extraData.get(CABLE_NEIGHBORS);

      emitBox(quads, getCableSprite(), CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, 0);

      for (Direction dir : Direction.values()) {
        if ((mask & (1 << dir.ordinal())) != 0) {
          double[] arm = armBox(dir);
          emitBox(quads, getCableSprite(), arm[0], arm[1], arm[2], arm[3], arm[4], arm[5], 0);

          boolean isCableNeighbor = cableNeighbors != null && cableNeighbors.contains(dir);
          if (!isCableNeighbor) {
            double[] plug = plugBox(dir);
            emitBox(quads, getCapSprite(), plug[0], plug[1], plug[2], plug[3], plug[4], plug[5], 0);
          }
        } else if (isUnconnectedCap(mask, dir)) {
          double[] cap = capBox(dir);
          emitBox(quads, getCapSprite(), cap[0], cap[1], cap[2], cap[3], cap[4], cap[5], 0);
        }
      }
    } else {
      emitBox(quads, getCableSprite(), CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, CENTER_MIN, CENTER_MAX, 0);
      emitBox(quads, getCableSprite(), 6, 10, 10, 15, 6, 10, 0);
      emitBox(quads, getCapSprite(), 5, 11, 15, 16, 5, 11, -1);
      emitBox(quads, getCableSprite(), 6, 10, 1, 6, 6, 10, 0);
      emitBox(quads, getCapSprite(), 5, 11, 0, 1, 5, 11, -1);
    }

    return quads;
  }

  private static boolean isUnconnectedCap(int mask, Direction side) {
    return mask == 0 || (1 << side.getOpposite().ordinal() & mask) == mask;
  }

  private static void emitBox(List<BakedQuad> quads, TextureAtlasSprite sprite,
                              double x0, double x1, double y0, double y1, double z0, double z1, int tintIndex) {
    Vector3f from = new Vector3f((float) x0, (float) y0, (float) z0);
    Vector3f to = new Vector3f((float) x1, (float) y1, (float) z1);
    for (Direction face : Direction.values()) {
      float[] uv = boxUV(x0, y0, z0, x1, y1, z1, face);
      BlockElementFace faceDef = new BlockElementFace(null, tintIndex, "", new BlockFaceUV(uv, 0));
      quads.add(BAKERY.bakeQuad(from, to, faceDef, sprite, face, BlockModelRotation.X0_Y0, null, true));
    }
  }

  private static float[] boxUV(double x0, double y0, double z0, double x1, double y1, double z1, Direction dir) {
    return switch (dir) {
      case DOWN, UP -> new float[]{(float) x0, (float) z0, (float) x1, (float) z1};
      case NORTH, SOUTH -> new float[]{(float) x0, (float) (16 - y1), (float) x1, (float) (16 - y0)};
      case WEST, EAST -> new float[]{(float) z0, (float) (16 - y1), (float) z1, (float) (16 - y0)};
    };
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

  public static int computeConnections(BlockAndTintGetter blockView, BlockPos pos) {
    int selfColor = Color.LightGray;
    var selfTe = blockView.getBlockEntity(pos);
    if (selfTe instanceof Cable selfCable) {
      selfColor = selfCable.color();
    }
    int connections = 0;
    for (Direction side : Direction.values()) {
      BlockPos neighbor = pos.relative(side);
      BlockEntity neighborTE = blockView.getBlockEntity(neighbor);
      if (isOCNeighbor(neighborTE, side.getOpposite())) {
        int neighborColor = Color.LightGray;
        if (neighborTE instanceof Cable nc) {
          neighborColor = nc.color();
        }
        if (selfColor == neighborColor || selfColor == Color.LightGray || neighborColor == Color.LightGray) {
          connections |= 1 << side.ordinal();
        }
      }
    }
    return connections;
  }

  public static Set<Direction> computeCableNeighbors(BlockAndTintGetter blockView, BlockPos pos) {
    var neighbors = new java.util.HashSet<Direction>();
    for (Direction side : Direction.values()) {
      BlockEntity te = blockView.getBlockEntity(pos.relative(side));
      if (te instanceof Cable) {
        neighbors.add(side);
      }
    }
    return neighbors;
  }

  private static boolean isOCNeighbor(BlockEntity te, Direction side) {
    if (te instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy) return false;
    if (te instanceof li.cil.oc.api.network.Environment || te instanceof SidedEnvironment) {
      if (te instanceof SidedEnvironment sideEnv) return sideEnv.canConnect(side);
      return true;
    }
    return false;
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
