package li.cil.oc.core.impl.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.entity.Drone;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class ModelQuadcopter extends Model {
    public final ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/drone.png");

    public final ModelPart body;
    public final ModelPart wing0, wing1, wing2, wing3;
    public final ModelPart light0, light1, light2, light3;

    @SuppressWarnings("unused")
    public ModelQuadcopter() {
        super(RenderType::entityCutoutNoCull);
        var root = createLayerDefinition().bakeRoot();
        this.body = root.getChild("body");
        this.wing0 = root.getChild("wing0");
        this.wing1 = root.getChild("wing1");
        this.wing2 = root.getChild("wing2");
        this.wing3 = root.getChild("wing3");
        this.light0 = root.getChild("light0");
        this.light1 = root.getChild("light1");
        this.light2 = root.getChild("light2");
        this.light3 = root.getChild("light3");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 23).addBox("middle", -1, 0, -1, 2, 1, 2)
                .texOffs(0, 1).addBox("top", -3, 1, -3, 6, 1, 6)
                .texOffs(0, 17).addBox("bottom", -2, -1, -2, 4, 1, 4), PartPose.offsetAndRotation(0, 0, 0, 0, (float) Math.toRadians(45), 0));
        root.addOrReplaceChild("wing0", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap0", 1, 0, -7, 6, 1, 6)
                .texOffs(0, 27).addBox("pin0", 2, -1, -3, 1, 3, 1), PartPose.ZERO);
        root.addOrReplaceChild("wing1", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap1", 1, 0, 1, 6, 1, 6)
                .texOffs(0, 27).addBox("pin1", 2, -1, 2, 1, 3, 1), PartPose.ZERO);
        root.addOrReplaceChild("wing2", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap2", -7, 0, 1, 6, 1, 6)
                .texOffs(0, 27).addBox("pin2", -3, -1, 2, 1, 3, 1), PartPose.ZERO);
        root.addOrReplaceChild("wing3", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap3", -7, 0, -7, 6, 1, 6)
                .texOffs(0, 27).addBox("pin3", -3, -1, -3, 1, 3, 1), PartPose.ZERO);
        root.addOrReplaceChild("light0", CubeListBuilder.create()
                .texOffs(24, 0).addBox("flap0", 1, 0, -7, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light1", CubeListBuilder.create()
                .texOffs(24, 0).addBox("flap1", 1, 0, 1, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light2", CubeListBuilder.create()
                .texOffs(24, 0).addBox("flap2", -7, 0, 1, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light3", CubeListBuilder.create()
                .texOffs(24, 0).addBox("flap3", -7, 0, -7, 6, 1, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    public void setupAnim(Drone drone, float ignoredPartialTick) {
        wing0.xRot = drone.flapAngles[0][0];
        wing0.zRot = drone.flapAngles[0][1];
        wing1.xRot = drone.flapAngles[1][0];
        wing1.zRot = drone.flapAngles[1][1];
        wing2.xRot = drone.flapAngles[2][0];
        wing2.zRot = drone.flapAngles[2][1];
        wing3.xRot = drone.flapAngles[3][0];
        wing3.zRot = drone.flapAngles[3][1];
        light0.xRot = wing0.xRot;
        light0.zRot = wing0.zRot;
        light1.xRot = wing1.xRot;
        light1.zRot = wing1.zRot;
        light2.xRot = wing2.xRot;
        light2.zRot = wing2.zRot;
        light3.xRot = wing3.xRot;
        light3.zRot = wing3.zRot;
    }

    public void applyHoverAndTilt(PoseStack poseStack, Drone drone, float partialTick) {
        int timeJitter = drone.hashCode() ^ 0xFF;
        if (drone.isRunning()) {
            float hover = (float) (Math.sin(timeJitter + (drone.level().getGameTime() + partialTick) / 20.0) * (1 / 16f));
            poseStack.translate(0, hover, 0);
        }
        Vec3 velocity = drone.getDeltaMovement();
        Vec3 direction = velocity.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (velocity.lengthSqr() > 1e-8 && Math.abs(direction.dot(up)) < 0.99) {
            Vec3 rotationAxis = direction.cross(up);
            float relativeSpeed = (float) (velocity.length() / drone.maxVelocity);
            float angle = (float) Math.toRadians(relativeSpeed * -20);
            poseStack.mulPose(new org.joml.Quaternionf().rotationAxis(
                    angle,
                    new org.joml.Vector3f((float) rotationAxis.x, (float) rotationAxis.y, (float) rotationAxis.z)
            ));
        }
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(drone.bodyAngle));
    }

    public void renderLights(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        light0.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        light1.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        light2.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        light3.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        wing0.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        wing1.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        wing2.render(poseStack, vertexConsumer, packedLight, packedOverlay);
        wing3.render(poseStack, vertexConsumer, packedLight, packedOverlay);
    }

    @SuppressWarnings({"EmptyMethod", "unused"})
    public void render(Entity ignoredEntity, float ignoredLimbSwing, float ignoredLimbSwingAmount, float ignoredAgeInTicks, float ignoredNetHeadYaw, float ignoredHeadPitch) {
    }
}
