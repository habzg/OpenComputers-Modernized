package li.cil.oc.neoforge.client.renderer.blockentity;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.UpgradeExperience;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.EventHandler;
import li.cil.oc.neoforge.common.blockentity.Robot;
import li.cil.oc.neoforge.common.blockentity.RobotProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class RobotRenderer implements BlockEntityRenderer<RobotProxy> {
    private static final RenderType LABEL_PLATE = RenderType.create(
            "oc_robot_label_plate",
            DefaultVertexFormat.POSITION_COLOR_LIGHTMAP,
            VertexFormat.Mode.QUADS,
            1536, false, true,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_TEXT_BACKGROUND_SHADER)
                    .setTextureState(RenderStateShard.NO_TEXTURE)
                    .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                    .setLightmapState(RenderStateShard.LIGHTMAP)
                    .setDepthTestState(RenderStateShard.LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .createCompositeState(false)
    );

    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/robot.png");

    private static float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;

    public static void setModelTint(float r, float g, float b) {
        tintR = r;
        tintG = g;
        tintB = b;
    }

    private static final RenderType LIGHT_RENDER_TYPE = RenderType.create(
            "robot_light_additive",
            DefaultVertexFormat.NEW_ENTITY,
            VertexFormat.Mode.QUADS,
            256, true, false,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderType.RENDERTYPE_ENTITY_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderType.TextureStateShard(TEXTURE, false, false))
                    .setTransparencyState(new RenderType.TransparencyStateShard("additive_transparency",
                            () -> {
                                RenderSystem.enableBlend();
                                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                            },
                            () -> {
                                RenderSystem.disableBlend();
                                RenderSystem.defaultBlendFunc();
                            }))
                    .setLightmapState(RenderType.LIGHTMAP)
                    .setOverlayState(RenderType.OVERLAY)
                    .setCullState(RenderType.NO_CULL)
                    .createCompositeState(true)
    );

    private static final float SIZE = 0.4f;
    private static final float L = 0.5f - SIZE;
    private static final float H = 0.5f + SIZE;
    private static final float LIGHT_SIZE = 0.3f;
    private static final float LIGHT_L = 0.5f - LIGHT_SIZE;
    private static final float LIGHT_H = 0.5f + LIGHT_SIZE;
    private static final float GAP = 1.0f / 28.0f;
    private static final float GT = 0.5f + GAP;
    private static final float GB = 0.5f - GAP;

    private static final RobotRenderEvent.MountPoint[] mountPoints = new RobotRenderEvent.MountPoint[7];

    private static final Map<String, Integer> slotNameMapping = new LinkedHashMap<>();

    static {
        slotNameMapping.put(UpgradeRenderer.MountPointName.TopLeft, 0);
        slotNameMapping.put(UpgradeRenderer.MountPointName.TopRight, 1);
        slotNameMapping.put(UpgradeRenderer.MountPointName.TopBack, 2);
        slotNameMapping.put(UpgradeRenderer.MountPointName.BottomLeft, 3);
        slotNameMapping.put(UpgradeRenderer.MountPointName.BottomRight, 4);
        slotNameMapping.put(UpgradeRenderer.MountPointName.BottomBack, 5);
        slotNameMapping.put(UpgradeRenderer.MountPointName.BottomFront, 6);

        for (Map.Entry<String, Integer> entry : slotNameMapping.entrySet()) {
            mountPoints[entry.getValue()] = new RobotRenderEvent.MountPoint(entry.getKey());
        }
    }

    @SuppressWarnings("unused")
    public RobotRenderer(BlockEntityRendererProvider.Context ignoredContext) {
    }

    private static void resetMountPoints(boolean running) {
        float offset = running ? 0 : -0.06f;

        mountPoints[0].offset.set(0, 0.2f, 0.24f);
        mountPoints[0].rotation.set(0, 1, 0, 90);

        mountPoints[1].offset.set(0, 0.2f, 0.24f);
        mountPoints[1].rotation.set(0, 1, 0, -90);

        mountPoints[2].offset.set(0, 0.2f, 0.24f);
        mountPoints[2].rotation.set(0, 1, 0, 180);

        mountPoints[3].offset.set(0, -0.2f - offset, 0.24f);
        mountPoints[3].rotation.set(0, 1, 0, 90);

        mountPoints[4].offset.set(0, -0.2f - offset, 0.24f);
        mountPoints[4].rotation.set(0, 1, 0, -90);

        mountPoints[5].offset.set(0, -0.2f - offset, 0.24f);
        mountPoints[5].rotation.set(0, 1, 0, 180);

        mountPoints[6].offset.set(0, -0.2f - offset, 0.24f);
        mountPoints[6].rotation.set(0, 1, 0, 0);
    }

    @Override
    public void render(@NotNull RobotProxy proxy, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight, int packedOverlay) {
        var robot = (li.cil.oc.neoforge.common.blockentity.Robot) proxy.robot;
        float worldTime = 0;
        if (robot.getLevel() != null) {
            worldTime = robot.getLevel().getGameTime() + partialTick;
        }

        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);

        if (robot.proxy != null && robot.proxy != proxy) {
            poseStack.translate(robot.proxy.worldPosition.getX() - proxy.worldPosition.getX(),
                robot.proxy.worldPosition.getY() - proxy.worldPosition.getY(),
                robot.proxy.worldPosition.getZ() - proxy.worldPosition.getZ());
        }

        if (robot.isAnimatingMove()) {
            double remaining = (robot.animationTicksLeft - partialTick) / (double) robot.animationTicksTotal;
            double dx = robot.moveFromX - robot.getBlockPos().getX();
            double dy = robot.moveFromY - robot.getBlockPos().getY();
            double dz = robot.moveFromZ - robot.getBlockPos().getZ();
            poseStack.translate(dx * remaining, dy * remaining, dz * remaining);
            if (robot.isRunning) {
                li.cil.oc.neoforge.client.Sound.updatePosition(robot);
            }
        }

        int timeJitter = robot.hashCode() ^ 0xFF;
        double hover;
        if (robot.isRunning) {
            hover = Math.sin(timeJitter + worldTime / 20.0) * 0.03;
        } else {
            hover = -0.03;
        }
        poseStack.translate(0, hover, 0);

        poseStack.pushPose();

        if (robot.isAnimatingTurn()) {
            double remaining = (robot.animationTicksLeft - partialTick) / (double) robot.animationTicksTotal;
            float angle = (float) (90 * remaining);
            poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(angle * robot.turnAxis)));
        }

        Direction yaw = robot.yaw();
        switch (yaw) {
            case SOUTH -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(180)));
            case WEST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
            case EAST -> poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-90)));
            default -> {}
        }

        poseStack.translate(-0.5, -0.5, -0.5);

        double offset = timeJitter + worldTime / 20.0;
        renderChassis(robot, robot, partialTick, offset, poseStack, buffer, packedLight, packedOverlay);

        poseStack.popPose();

        renderNameLabel(robot, robot, poseStack, buffer);

        poseStack.popPose();
    }

    private void renderChassis(Robot robot, Robot proxy, float partialTick, double offset, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        boolean isRunning = proxy.isRunning();

        resetMountPoints(robot != null && isRunning);
        var event = new RobotRenderEvent(robot, mountPoints);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event);

        int level = 0;
        if (robot != null) {
            try {
                for (var comp : robot._components()) {
                    if (comp instanceof UpgradeExperience xp) {
                        level += xp.level;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        if (level > 19) {
            tintR = 0.4f; tintG = 1.0f; tintB = 1.0f;
        } else if (level > 9) {
            tintR = 1.0f; tintG = 1.0f; tintB = 0.4f;
        } else {
            tintR = 0.5f; tintG = 0.5f; tintB = 0.5f;
        }

        float saveR = tintR, saveG = tintG, saveB = tintB;
        int lightColor = robot != null ? robot.info.lightColor : 0xF23030;
        renderChassis(poseStack, buffer, packedLight, packedOverlay, offset, isRunning, lightColor);

        if (robot != null && !robot.renderingErrored) {
            li.cil.oc.neoforge.client.renderer.item.UpgradeRenderer.setModelTint(saveR, saveG, saveB);
            renderTool(robot, proxy, partialTick, poseStack, buffer, packedLight, packedOverlay);

            if (Minecraft.getInstance().level != null) {
                renderUpgrades(robot, proxy, partialTick, poseStack, buffer, packedLight, packedOverlay);
            }
        }

        tintR = tintG = tintB = 1.0f;
    }

    private static void renderTop(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        var matrix = poseStack.last().pose();
        var pose = poseStack.last();

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                L, GT, H, 0, 0.5f, 0, 0.2f, 1,
                H, GT, H, 0.5f, 0.5f, 0, 0.2f, 1);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                H, GT, H, 0.5f, 0.5f, 0, 0.2f, 1,
                H, GT, L, 0.5f, 0, 1, 0.2f, 0);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                H, GT, L, 0.5f, 0, 0, 0.2f, -1,
                L, GT, L, 0, 0, 0, 0.2f, -1);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                0.5f, 1, 0.5f, 0.25f, 0.25f, 0, 0.2f, 1,
                L, GT, L, 0, 0, -1, 0.2f, 0,
                L, GT, H, 0, 0.5f, -1, 0.2f, 0);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                L, GT, H, 0, 1, 0, -1, 0,
                L, GT, L, 0, 0.5f, 0, -1, 0,
                H, GT, L, 0.5f, 0.5f, 0, -1, 0,
                H, GT, H, 0.5f, 1, 0, -1, 0);
    }

    private static void renderBottom(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(TEXTURE));
        var matrix = poseStack.last().pose();
        var pose = poseStack.last();

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                L, GB, L, 0.5f, 0, 0, -0.2f, 1,
                H, GB, L, 1, 0, 0, -0.2f, 1);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                H, GB, L, 1, 0, 1, -0.2f, 0,
                H, GB, H, 1, 0.5f, 1, -0.2f, 0);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                H, GB, H, 1, 0.5f, 0, -0.2f, -1,
                L, GB, H, 0.5f, 0.5f, 0, -0.2f, -1);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                0.5f, 0.03f, 0.5f, 0.75f, 0.25f, 0, -0.2f, 1,
                L, GB, H, 0.5f, 0.5f, -1, -0.2f, 0,
                L, GB, L, 0.5f, 0, -1, -0.2f, 0);

        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                L, GB, L, 0, 0.5f, 0, 1, 0,
                L, GB, H, 0, 1, 0, 1, 0,
                H, GB, H, 0.5f, 1, 0, 1, 0,
                H, GB, L, 0.5f, 0.5f, 0, 1, 0);
    }

    private static void addVertex(VertexConsumer consumer, org.joml.Matrix4f matrix, PoseStack.Pose pose, int packedLight, int packedOverlay, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            ny = 1.0f;
        }
        int c = 255;
        consumer.addVertex(matrix, x, y, z).setColor((int)(c * tintR), (int)(c * tintG), (int)(c * tintB), 255).setUv(u, v).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
    }

    @SuppressWarnings("SameParameterValue")
    private static void addQuad(VertexConsumer consumer, org.joml.Matrix4f matrix, PoseStack.Pose pose, int packedLight, int packedOverlay,
                                float x1, float y1, float z1, float u1, float v1, float nx1, float ny1, float nz1,
                                float x2, float y2, float z2, float u2, float v2, float nx2, float ny2, float nz2,
                                float x3, float y3, float z3, float u3, float v3, float nx3, float ny3, float nz3,
                                float x4, float y4, float z4, float u4, float v4, float nx4, float ny4, float nz4) {
        addVertex(consumer, matrix, pose, packedLight, packedOverlay, x1, y1, z1, u1, v1, nx1, ny1, nz1);
        addVertex(consumer, matrix, pose, packedLight, packedOverlay, x2, y2, z2, u2, v2, nx2, ny2, nz2);
        addVertex(consumer, matrix, pose, packedLight, packedOverlay, x3, y3, z3, u3, v3, nx3, ny3, nz3);
        addVertex(consumer, matrix, pose, packedLight, packedOverlay, x4, y4, z4, u4, v4, nx4, ny4, nz4);
    }

    private static void renderLightSides(org.joml.Matrix4f matrix, PoseStack.Pose pose, VertexConsumer consumer, float u0, float v0, float u1, float v1, float r, float g, float b) {
        // Neg X face (x = LIGHT_L)
        consumer.addVertex(matrix, LIGHT_L, GT, LIGHT_L).setColor(r, g, b, 1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, -1, 0, 0);
        consumer.addVertex(matrix, LIGHT_L, GB, LIGHT_L).setColor(r, g, b, 1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, -1, 0, 0);
        consumer.addVertex(matrix, LIGHT_L, GB, LIGHT_H).setColor(r, g, b, 1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, -1, 0, 0);
        consumer.addVertex(matrix, LIGHT_L, GT, LIGHT_H).setColor(r, g, b, 1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, -1, 0, 0);

        // Pos Z face (z = LIGHT_H)
        consumer.addVertex(matrix, LIGHT_L, GT, LIGHT_H).setColor(r, g, b, 1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(matrix, LIGHT_L, GB, LIGHT_H).setColor(r, g, b, 1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(matrix, LIGHT_H, GB, LIGHT_H).setColor(r, g, b, 1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);
        consumer.addVertex(matrix, LIGHT_H, GT, LIGHT_H).setColor(r, g, b, 1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, 1);

        // Pos X face (x = LIGHT_H)
        consumer.addVertex(matrix, LIGHT_H, GT, LIGHT_H).setColor(r, g, b, 1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 1, 0, 0);
        consumer.addVertex(matrix, LIGHT_H, GB, LIGHT_H).setColor(r, g, b, 1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 1, 0, 0);
        consumer.addVertex(matrix, LIGHT_H, GB, LIGHT_L).setColor(r, g, b, 1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 1, 0, 0);
        consumer.addVertex(matrix, LIGHT_H, GT, LIGHT_L).setColor(r, g, b, 1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 1, 0, 0);

        // Neg Z face (z = LIGHT_L)
        consumer.addVertex(matrix, LIGHT_H, GT, LIGHT_L).setColor(r, g, b, 1).setUv(u0, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
        consumer.addVertex(matrix, LIGHT_H, GB, LIGHT_L).setColor(r, g, b, 1).setUv(u0, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
        consumer.addVertex(matrix, LIGHT_L, GB, LIGHT_L).setColor(r, g, b, 1).setUv(u1, v1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
        consumer.addVertex(matrix, LIGHT_L, GT, LIGHT_L).setColor(r, g, b, 1).setUv(u1, v0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(pose, 0, 0, -1);
    }

    private void renderTool(Robot robot, Robot proxy, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack stack = proxy.getItem(0);
        if (stack.isEmpty()) return;
        try {
            poseStack.pushPose();

            poseStack.scale(1, -1, -1);
            poseStack.translate(0, -8 * 0.0625F - 0.0078125F, -0.5F);

            if (robot.isAnimatingSwing()) {
                int wantedTicksPerCycle = 10;
                int cycles = Math.max(robot.animationTicksTotal / wantedTicksPerCycle, 1);
                int ticksPerCycle = robot.animationTicksTotal / cycles;
                double remaining = (robot.animationTicksLeft - partialTick) / (double) ticksPerCycle;
                float swingAngle = (float) (Math.sin((remaining - (int) remaining) * Math.PI) * 45);
                poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(swingAngle)));
            }

            var item = stack.getItem();
            if (item instanceof BlockItem) {
                poseStack.mulPose(new Quaternionf().rotateX((float) Math.toRadians(180)));
                poseStack.mulPose(new Quaternionf().rotateY((float) Math.toRadians(90)));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            } else if (item == Items.BOW) {
                poseStack.translate(0, -3f / 16f, -0.125F);
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(170)));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            } else {
                poseStack.translate(1f / 16f, 1f / 16f, -2f / 16f);
                poseStack.scale(0.625F, 0.625F, 0.625F);
                poseStack.mulPose(new Quaternionf().rotateZ((float) Math.toRadians(180)));
            }

            var itemRenderer = Minecraft.getInstance().getItemRenderer();
            itemRenderer.renderStatic(stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, packedLight, packedOverlay, poseStack, buffer, Minecraft.getInstance().level, 0);

            poseStack.popPose();
        } catch (Exception e) {
            OpenComputers.log().warn("Failed rendering equipped item.", e);
            robot.renderingErrored = true;
        }
    }

    private void renderUpgrades(Robot robot, Robot proxy, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int ignoredPackedOverlay) {
        var availableSlots = new HashSet<>(slotNameMapping.keySet());
        var wildcardRenderers = new ArrayList<Map.Entry<ItemStack, UpgradeRenderer>>();
        var slotMapping = new ArrayList<Map.Entry<ItemStack, UpgradeRenderer>>(mountPoints.length);
        for (int i = 0; i < mountPoints.length; i++) slotMapping.add(null);

        var renderers = new ArrayList<Map.Entry<ItemStack, UpgradeRenderer>>();
        var slots = new HashSet<Integer>();
        slots.addAll(robot.containerSlots());
        slots.addAll(robot.componentSlots());
        for (int s : slots) {
            ItemStack stack = proxy.getItem(s);
            if (!stack.isEmpty()) {
                var ur = li.cil.oc.neoforge.client.renderer.item.UpgradeRenderers.get(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()));
                if (ur != null) {
                    renderers.add(new AbstractMap.SimpleEntry<>(stack, ur));
                }
            }
        }

        for (var entry : renderers) {
            var stack = entry.getKey();
            var renderer = entry.getValue();
            var preferredSlot = renderer.computePreferredMountPoint(stack, proxy, availableSlots);
            if (availableSlots.remove(preferredSlot)) {
                var idx = slotNameMapping.get(preferredSlot);
                if (idx != null) slotMapping.set(idx, entry);
            } else if (UpgradeRenderer.MountPointName.Any.equals(preferredSlot)) {
                wildcardRenderers.add(entry);
            }
        }

        for (var entry : wildcardRenderers) {
            for (int i = 0; i < slotMapping.size(); i++) {
                if (slotMapping.get(i) == null) {
                    slotMapping.set(i, entry);
                    break;
                }
            }
        }

        for (int i = 0; i < slotMapping.size(); i++) {
            var info = slotMapping.get(i);
            if (info != null) {
                try {
                    var stack = info.getKey();
                    var renderer = info.getValue();
                    var mountPoint = mountPoints[i];
                    poseStack.pushPose();
                    poseStack.translate(0.5f, 0.5f, 0.5f);
                    renderer.render(poseStack, buffer, packedLight, stack, mountPoint, proxy, partialTick);
                    poseStack.popPose();
                } catch (Exception e) {
                    OpenComputers.log().warn("Failed rendering equipped upgrade.", e);
                    robot.renderingErrored = true;
                }
            }
        }
    }

    private void renderNameLabel(Robot robot, Robot proxy, PoseStack poseStack, MultiBufferSource buffer) {
        String name = robot.info.name;
        if (!OCSettings.get().robotLabels) {
            return;
        }
        if (name == null || name.isEmpty()) {
            return;
        }

        boolean onContraption = proxy.getLevel() != Minecraft.getInstance().level;
        if (!onContraption) {
            double range = Minecraft.getInstance().getEntityRenderDispatcher().distanceToSqr(proxy.getBlockPos().getX() + 0.5, proxy.getBlockPos().getY() + 0.5, proxy.getBlockPos().getZ() + 0.5);
            if (range > 4096) {
                return;
            }
        }

        poseStack.pushPose();
        poseStack.translate(0, 0.8, 0);
        poseStack.mulPose(Minecraft.getInstance().getEntityRenderDispatcher().cameraOrientation());
        poseStack.scale(0.025f, -0.025f, 0.025f);

        Font font = Minecraft.getInstance().font;
        String displayName = (EventHandler.isItTime() ? "§k" : "") + name;
        float f2 = (float) (-font.width(displayName) / 2);
        var matrix = poseStack.last().pose();

        float f1 = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        int j = (int) (f1 * 255.0F) << 24;

        if (j != 0) {
            int width = font.width(displayName);
            float x0 = f2 - 1.0f;
            float x1 = f2 + width + 1.0f;
            int alpha = j >>> 24;
            var plateMatrix = poseStack.last().pose();
            VertexConsumer plate = buffer.getBuffer(LABEL_PLATE);
            plate.addVertex(plateMatrix, x0, -1.0f, -0.02f).setColor(0, 0, 0, alpha).setLight(LightTexture.FULL_BRIGHT);
            plate.addVertex(plateMatrix, x1, -1.0f, -0.02f).setColor(0, 0, 0, alpha).setLight(LightTexture.FULL_BRIGHT);
            plate.addVertex(plateMatrix, x1, 9.0f, -0.02f).setColor(0, 0, 0, alpha).setLight(LightTexture.FULL_BRIGHT);
            plate.addVertex(plateMatrix, x0, 9.0f, -0.02f).setColor(0, 0, 0, alpha).setLight(LightTexture.FULL_BRIGHT);
        }

        font.drawInBatch(displayName, f2, 0, -1, false, matrix, buffer, Font.DisplayMode.NORMAL, 0, LightTexture.FULL_BRIGHT);

        poseStack.popPose();
    }

    public static void renderChassis(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, double offset, boolean isRunning, int lightColor) {
        renderChassis(poseStack, buffer, packedLight, packedOverlay, offset, isRunning, lightColor, tintR, tintG, tintB, tintR, tintG, tintB);
    }

    public static void renderChassis(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, double offset, boolean isRunning, int lightColor,
                                      float topR, float topG, float topB, float bottomR, float bottomG, float bottomB) {
        if (!isRunning) {
            poseStack.translate(0, -2 * GAP, 0);
        }

        tintR = bottomR; tintG = bottomG; tintB = bottomB;
        renderBottom(poseStack, buffer, packedLight, packedOverlay);

        if (!isRunning) {
            poseStack.translate(0, -2 * GAP, 0);
        }

        tintR = topR; tintG = topG; tintB = topB;
        renderTop(poseStack, buffer, packedLight, packedOverlay);

        if (isRunning) {
            float vStep = 1.0f / 32.0f;
            int offsetVSteps = (int) ((offset - (int) offset) * 16);
            float offsetV = offsetVSteps * vStep;
            float u0 = 0.5f, u1 = 1f, v0 = 0.5f + offsetV, v1 = 0.5f + vStep + offsetV;

            VertexConsumer lightConsumer = buffer.getBuffer(LIGHT_RENDER_TYPE);
            float r = ((lightColor >> 16) & 0xFF) / 255f;
            float g = ((lightColor >> 8) & 0xFF) / 255f;
            float b = (lightColor & 0xFF) / 255f;

            var matrix = poseStack.last().pose();
            var pose = poseStack.last();

            renderLightSides(matrix, pose, lightConsumer, u0, v0, u1, v1, r, g, b);
        }

        tintR = tintG = tintB = 1.0f;
    }
}
