package li.cil.oc.neoforge.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.Random;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.common.blockentity.Cable;
import li.cil.oc.core.impl.common.blockentity.Print;
import li.cil.oc.core.impl.util.ExtendedAABB;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RenderHighlightEvent;

public final class HighlightRenderer {
    private static final Random random = new Random();
    private static final ItemInfo tablet = li.cil.oc.api.Items.get(Constants.ItemName.Tablet);
    private static final RenderType HOLOGRAM_RENDER_TYPE = RenderType.create(
            "hologram_overlay",
            DefaultVertexFormat.BLOCK,
            VertexFormat.Mode.QUADS,
            RenderType.TRANSIENT_BUFFER_SIZE,
            false,
            true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderType.TextureStateShard(Textures.blockHologram, false, false))
                    .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderType.LIGHTMAP)
                    .createCompositeState(false)
    );

    @SubscribeEvent(priority = EventPriority.HIGH)
    @SuppressWarnings("unused")
    public static void onDrawBlockHighlight(RenderHighlightEvent.Block e) {
        var hitInfo = e.getTarget();
        if (!(e.getCamera().getEntity() instanceof  Player player)) return;

        var level = player.level();
        var blockPos = hitInfo.getBlockPos();

        if (hitInfo.getType() == HitResult.Type.BLOCK && li.cil.oc.api.Items.get(player.getItemInHand(InteractionHand.MAIN_HAND)) == tablet) {
            if (!level.isEmptyBlock(blockPos)) {
                var state = level.getBlockState(blockPos);
                var shape = state.getShape(level, blockPos);
                var bounds = shape.bounds();
                var sideHit = hitInfo.getDirection();

                var camera = e.getCamera();
                var camPos = camera.getPosition();

                var pose = e.getPoseStack();
                pose.pushPose();
                pose.translate(blockPos.getX() - camPos.x, blockPos.getY() - camPos.y, blockPos.getZ() - camPos.z);
                pose.scale(1.002f, 1.002f, 1.002f);

                if (OCSettings.get().hologramFlickerFrequency > 0 && random.nextDouble() < OCSettings.get().hologramFlickerFrequency) {
                    double sx = 1 - Math.abs(sideHit.getStepX());
                    double sy = 1 - Math.abs(sideHit.getStepY());
                    double sz = 1 - Math.abs(sideHit.getStepZ());
                    pose.scale(1 + (float) (random.nextGaussian() * 0.01), 1 + (float) (random.nextGaussian() * 0.001), 1 + (float) (random.nextGaussian() * 0.01));
                    pose.translate(random.nextGaussian() * 0.01 * sx, random.nextGaussian() * 0.01 * sy, random.nextGaussian() * 0.01 * sz);
                }

                var bufferSource = e.getMultiBufferSource();
                VertexConsumer b = bufferSource.getBuffer(HOLOGRAM_RENDER_TYPE);

                var minX = (float) bounds.minX;
                var minY = (float) bounds.minY;
                var minZ = (float) bounds.minZ;
                var maxX = (float) bounds.maxX;
                var maxY = (float) bounds.maxY;
                var maxZ = (float) bounds.maxZ;

                int r = 0, g = 255, bCol = 0, a = 102;
                int fullLight = 15728880;

                switch (sideHit) {
                    case UP -> {
                        b.addVertex(pose.last(), maxX, maxY + 0.002f, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, maxX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
                        b.addVertex(pose.last(), maxX, maxY + 0.002f, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, maxX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
                        b.addVertex(pose.last(), minX, maxY + 0.002f, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, minX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
                        b.addVertex(pose.last(), minX, maxY + 0.002f, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, minX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 1.0F, 0.0F);
                    }
                    case DOWN -> {
                        b.addVertex(pose.last(), maxX, minY - 0.002f, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, maxX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, -1.0F, 0.0F);
                        b.addVertex(pose.last(), maxX, minY - 0.002f, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, maxX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, -1.0F, 0.0F);
                        b.addVertex(pose.last(), minX, minY - 0.002f, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, minX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, -1.0F, 0.0F);
                        b.addVertex(pose.last(), minX, minY - 0.002f, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, minX * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, -1.0F, 0.0F);
                    }
                    case EAST -> {
                        b.addVertex(pose.last(), maxX + 0.002f, maxY, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), maxX + 0.002f, maxY, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), maxX + 0.002f, minY, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), maxX + 0.002f, minY, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 1.0F, 0.0F, 0.0F);
                    }
                    case WEST -> {
                        b.addVertex(pose.last(), minX - 0.002f, maxY, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), -1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), minX - 0.002f, maxY, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), -1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), minX - 0.002f, minY, minZ).setColor(r, g, bCol, a).setUv(minZ * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), -1.0F, 0.0F, 0.0F);
                        b.addVertex(pose.last(), minX - 0.002f, minY, maxZ).setColor(r, g, bCol, a).setUv(maxZ * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), -1.0F, 0.0F, 0.0F);
                    }
                    case SOUTH -> {
                        b.addVertex(pose.last(), maxX, maxY, maxZ + 0.002f).setColor(r, g, bCol, a).setUv(maxX * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                        b.addVertex(pose.last(), minX, maxY, maxZ + 0.002f).setColor(r, g, bCol, a).setUv(minX * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                        b.addVertex(pose.last(), minX, minY, maxZ + 0.002f).setColor(r, g, bCol, a).setUv(minX * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                        b.addVertex(pose.last(), maxX, minY, maxZ + 0.002f).setColor(r, g, bCol, a).setUv(maxX * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, 1.0F);
                    }
                    default -> {
                        b.addVertex(pose.last(), minX, maxY, minZ - 0.002f).setColor(r, g, bCol, a).setUv(minX * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, -1.0F);
                        b.addVertex(pose.last(), maxX, maxY, minZ - 0.002f).setColor(r, g, bCol, a).setUv(maxX * 16, maxY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, -1.0F);
                        b.addVertex(pose.last(), maxX, minY, minZ - 0.002f).setColor(r, g, bCol, a).setUv(maxX * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, -1.0F);
                        b.addVertex(pose.last(), minX, minY, minZ - 0.002f).setColor(r, g, bCol, a).setUv(minX * 16, minY * 16).setLight(fullLight).setNormal(pose.last(), 0.0F, 0.0F, -1.0F);
                    }
                }
                pose.popPose();
            }
        }

        if (hitInfo.getType() == HitResult.Type.BLOCK) {
            var te = level.getBlockEntity(blockPos);
            if (te instanceof Print print) {
                var state = level.getBlockState(blockPos);
                var facing = print.facing();
                var pose = e.getPoseStack();
                var camera = e.getCamera();
                var camPos = camera.getPosition();
                float expansion = 0.002f;

                var bufferSource = e.getMultiBufferSource();
                VertexConsumer lineBuilder = bufferSource.getBuffer(RenderType.lines());

                var shapes = print.state ? print.data.stateOn : print.data.stateOff;

                pose.pushPose();
                pose.translate(blockPos.getX() - camPos.x, blockPos.getY() - camPos.y, blockPos.getZ() - camPos.z);

                for (var shape : shapes) {
                    var aabb = ExtendedAABB.rotateTowards(shape.bounds(), facing).inflate(expansion, expansion, expansion);
                    LevelRenderer.renderLineBox(pose, lineBuilder, aabb, 0.0f, 0.0f, 0.0f, 0.4f);
                }
                pose.popPose();

                e.setCanceled(true);
            }
        }

        if (hitInfo.getType() == HitResult.Type.BLOCK) {
            var te = level.getBlockEntity(blockPos);
            boolean isCable = te instanceof Cable;
            boolean isMultipartCable = !isCable && li.cil.oc.neoforge.common.MultipartHooks.isCableHit(te, hitInfo);
            if (isCable || isMultipartCable) {
                var pose = e.getPoseStack();
                var camera = e.getCamera();
                var camPos = camera.getPosition();

                pose.pushPose();
                pose.translate(blockPos.getX() - camPos.x, blockPos.getY() - camPos.y, blockPos.getZ() - camPos.z);

                var bufferSource = e.getMultiBufferSource();
                VertexConsumer lineBuilder = bufferSource.getBuffer(RenderType.lines());

                int mask = getCableConnections(level, blockPos);
                drawCableOverlay(lineBuilder, pose, mask);

                pose.popPose();

                if (!isMultipartCable) {
                    e.setCanceled(true);
                }
            }
        }
    }

    private static final double CABLE_MIN = 0.375;
    private static final double CABLE_MAX = 0.625;
    private static final double CABLE_EXPAND = 0.002;

    @SuppressWarnings("unused")
    private static int getCableConnections(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos) {
        int mask = 0;
        var te = level.getBlockEntity(pos);
        for (Direction side : Direction.values()) {
            var neighborPos = pos.relative(side);
            var neighbor = level.getBlockEntity(neighborPos);
            if (neighbor != null && !(neighbor instanceof li.cil.oc.neoforge.common.blockentity.RobotProxy)) {
                boolean hasNode;
                switch (neighbor) {
                    case SidedEnvironment sided -> hasNode = sided.canConnect(side.getOpposite());
                    case li.cil.oc.api.network.Environment environment -> hasNode = true;
                    default -> hasNode = li.cil.oc.neoforge.common.MultipartHooks.hasOCPart(neighbor);
                }
                if (hasNode) {
                    mask |= (1 << side.get3DDataValue());
                }
            }
        }
        return mask;
    }

    @SuppressWarnings("unused")
    private static void drawCableOverlay(VertexConsumer consumer, com.mojang.blaze3d.vertex.PoseStack pose, int mask) {
        var matrix = pose.last().pose();
        float r = 0, g = 0, b = 0, a = 0.4f;

        for (Direction side : Direction.values()) {
            if ((mask & (1 << side.get3DDataValue())) != 0) {
                double offset = side.getAxisDirection() == net.minecraft.core.Direction.AxisDirection.NEGATIVE ? -CABLE_EXPAND : 1 + CABLE_EXPAND;
                double centre = side.getAxisDirection() == net.minecraft.core.Direction.AxisDirection.NEGATIVE ? CABLE_MIN : CABLE_MAX;

                drawLineAdjacent(consumer, matrix, side.getAxis(), offset, CABLE_MIN, CABLE_MIN, CABLE_MIN, CABLE_MAX);
                drawLineAdjacent(consumer, matrix, side.getAxis(), offset, CABLE_MIN, CABLE_MAX, CABLE_MAX, CABLE_MAX);
                drawLineAdjacent(consumer, matrix, side.getAxis(), offset, CABLE_MAX, CABLE_MAX, CABLE_MAX, CABLE_MIN);
                drawLineAdjacent(consumer, matrix, side.getAxis(), offset, CABLE_MAX, CABLE_MIN, CABLE_MIN, CABLE_MIN);

                drawLineAlong(consumer, matrix, side.getAxis(), CABLE_MIN, CABLE_MIN, offset, centre);
                drawLineAlong(consumer, matrix, side.getAxis(), CABLE_MAX, CABLE_MIN, offset, centre);
                drawLineAlong(consumer, matrix, side.getAxis(), CABLE_MAX, CABLE_MAX, offset, centre);
                drawLineAlong(consumer, matrix, side.getAxis(), CABLE_MIN, CABLE_MAX, offset, centre);
            }
        }

        drawCore(consumer, matrix, mask, Direction.WEST, Direction.DOWN, Direction.Axis.Z);
        drawCore(consumer, matrix, mask, Direction.WEST, Direction.UP, Direction.Axis.Z);
        drawCore(consumer, matrix, mask, Direction.EAST, Direction.DOWN, Direction.Axis.Z);
        drawCore(consumer, matrix, mask, Direction.EAST, Direction.UP, Direction.Axis.Z);
        drawCore(consumer, matrix, mask, Direction.WEST, Direction.NORTH, Direction.Axis.Y);
        drawCore(consumer, matrix, mask, Direction.WEST, Direction.SOUTH, Direction.Axis.Y);
        drawCore(consumer, matrix, mask, Direction.EAST, Direction.NORTH, Direction.Axis.Y);
        drawCore(consumer, matrix, mask, Direction.EAST, Direction.SOUTH, Direction.Axis.Y);
        drawCore(consumer, matrix, mask, Direction.DOWN, Direction.NORTH, Direction.Axis.X);
        drawCore(consumer, matrix, mask, Direction.DOWN, Direction.SOUTH, Direction.Axis.X);
        drawCore(consumer, matrix, mask, Direction.UP, Direction.NORTH, Direction.Axis.X);
        drawCore(consumer, matrix, mask, Direction.UP, Direction.SOUTH, Direction.Axis.X);
    }

    private static void drawCore(VertexConsumer consumer, org.joml.Matrix4f matrix, int mask, Direction a, Direction b, Direction.Axis other) {
        if (((mask >> a.get3DDataValue()) & 1) != ((mask >> b.get3DDataValue()) & 1)) return;
        double offA = a.getAxisDirection() == net.minecraft.core.Direction.AxisDirection.NEGATIVE ? CABLE_MIN : CABLE_MAX;
        double offB = b.getAxisDirection() == net.minecraft.core.Direction.AxisDirection.NEGATIVE ? CABLE_MIN : CABLE_MAX;
        drawLineAlong(consumer, matrix, other, offA, offB, CABLE_MIN, CABLE_MAX);
    }

    private static void drawLineAlong(VertexConsumer consumer, org.joml.Matrix4f matrix, Direction.Axis axis, double offA, double offB, double start, double end) {
        float r = 0, g = 0, b = 0, a = 0.4f;
        switch (axis) {
            case X -> {
                consumer.addVertex(matrix, (float) start, (float) offA, (float) offB).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) end, (float) offA, (float) offB).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
            case Y -> {
                consumer.addVertex(matrix, (float) offA, (float) start, (float) offB).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) offA, (float) end, (float) offB).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
            case Z -> {
                consumer.addVertex(matrix, (float) offA, (float) offB, (float) start).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) offA, (float) offB, (float) end).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
        }
    }

    private static void drawLineAdjacent(VertexConsumer consumer, org.joml.Matrix4f matrix, Direction.Axis axis, double offset, double startA, double startB, double endA, double endB) {
        float r = 0, g = 0, b = 0, a = 0.4f;
        switch (axis) {
            case X -> {
                consumer.addVertex(matrix, (float) offset, (float) startA, (float) startB).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) offset, (float) endA, (float) endB).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
            case Y -> {
                consumer.addVertex(matrix, (float) startA, (float) offset, (float) startB).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) endA, (float) offset, (float) endB).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
            case Z -> {
                consumer.addVertex(matrix, (float) startA, (float) startB, (float) offset).setColor(r, g, b, a).setNormal(0, 1, 0);
                consumer.addVertex(matrix, (float) endA, (float) endB, (float) offset).setColor(r, g, b, a).setNormal(0, 1, 0);
            }
        }
    }
}
