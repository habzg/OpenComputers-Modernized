package li.cil.oc.neoforge.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.api.event.RobotRenderEvent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;

@SuppressWarnings("unused")
public final class UpgradeRenderer {
    private static float tintR = 1.0f, tintG = 1.0f, tintB = 1.0f;

    public static void setModelTint(float r, float g, float b) {
        tintR = r;
        tintG = g;
        tintB = b;
    }

    private static final AABB bounds = new AABB(-0.1, -0.1, -0.1, 0.1, 0.1, 0.1);

    public static void drawSimpleBlock(PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay, RobotRenderEvent.MountPoint mountPoint, ResourceLocation texture, float frontOffset) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotateAxis((float) Math.toRadians(mountPoint.rotation.w), mountPoint.rotation.x, mountPoint.rotation.y, mountPoint.rotation.z));
        poseStack.translate(mountPoint.offset.x, mountPoint.offset.y, mountPoint.offset.z);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutout(texture));
        var matrix = poseStack.last().pose();
        var pose = poseStack.last();

        // Front
        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                (float) bounds.minX, (float) bounds.minY, (float) bounds.maxZ, frontOffset, 0.5f,
                (float) bounds.maxX, (float) bounds.minY, (float) bounds.maxZ, frontOffset + 0.5f, 0.5f,
                (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, frontOffset + 0.5f, 0,
                (float) bounds.minX, (float) bounds.maxY, (float) bounds.maxZ, frontOffset, 0,
                0, 0, 1);

        // Top
        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, 1, 0.5f,
                (float) bounds.maxX, (float) bounds.maxY, (float) bounds.minZ, 1, 1,
                (float) bounds.minX, (float) bounds.maxY, (float) bounds.minZ, 0.5f, 1,
                (float) bounds.minX, (float) bounds.maxY, (float) bounds.maxZ, 0.5f, 0.5f,
                0, 1, 0);

        // Bottom
        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                (float) bounds.minX, (float) bounds.minY, (float) bounds.maxZ, 0.5f, 0.5f,
                (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, 0.5f, 1,
                (float) bounds.maxX, (float) bounds.minY, (float) bounds.minZ, 1, 1,
                (float) bounds.maxX, (float) bounds.minY, (float) bounds.maxZ, 1, 0.5f,
                0, -1, 0);

        // Left (X+)
        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                (float) bounds.maxX, (float) bounds.maxY, (float) bounds.maxZ, 0, 0.5f,
                (float) bounds.maxX, (float) bounds.minY, (float) bounds.maxZ, 0, 1,
                (float) bounds.maxX, (float) bounds.minY, (float) bounds.minZ, 0.5f, 1,
                (float) bounds.maxX, (float) bounds.maxY, (float) bounds.minZ, 0.5f, 0.5f,
                1, 0, 0);

        // Right (X-)
        addQuad(consumer, matrix, pose, packedLight, packedOverlay,
                (float) bounds.minX, (float) bounds.minY, (float) bounds.maxZ, 0, 1,
                (float) bounds.minX, (float) bounds.maxY, (float) bounds.maxZ, 0, 0.5f,
                (float) bounds.minX, (float) bounds.maxY, (float) bounds.minZ, 0.5f, 0.5f,
                (float) bounds.minX, (float) bounds.minY, (float) bounds.minZ, 0.5f, 1,
                -1, 0, 0);

        poseStack.popPose();
    }

    private static void addQuad(VertexConsumer consumer, org.joml.Matrix4f matrix, PoseStack.Pose pose, int packedLight, int packedOverlay,
                                float x1, float y1, float z1, float u1, float v1,
                                float x2, float y2, float z2, float u2, float v2,
                                float x3, float y3, float z3, float u3, float v3,
                                float x4, float y4, float z4, float u4, float v4,
                                float nx, float ny, float nz) {
        int r = (int)(255 * tintR), g = (int)(255 * tintG), b = (int)(255 * tintB), a = 255;
        consumer.addVertex(matrix, x1, y1, z1).setColor(r, g, b, a).setUv(u1, v1).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x2, y2, z2).setColor(r, g, b, a).setUv(u2, v2).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x3, y3, z3).setColor(r, g, b, a).setUv(u3, v3).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
        consumer.addVertex(matrix, x4, y4, z4).setColor(r, g, b, a).setUv(u4, v4).setOverlay(packedOverlay).setLight(packedLight).setNormal(pose, nx, ny, nz);
    }
}
