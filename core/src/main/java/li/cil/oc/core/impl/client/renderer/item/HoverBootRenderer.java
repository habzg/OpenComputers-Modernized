package li.cil.oc.core.impl.client.renderer.item;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class HoverBootRenderer extends HumanoidModel<LivingEntity> {
    public static final HoverBootRenderer INSTANCE = new HoverBootRenderer();

    public int lightColor = 0x66DD55;

    private final ModelPart light0;
    private final ModelPart light1;
    private final ModelPart light2;
    private final ModelPart light3;

    @SuppressWarnings("unused")
    public HoverBootRenderer() {
        super(createLayerDefinition().bakeRoot());
        var lightRoot = createLightLayerDefinition().bakeRoot();
        this.light0 = lightRoot.getChild("light0");
        this.light1 = lightRoot.getChild("light1");
        this.light2 = lightRoot.getChild("light2");
        this.light3 = lightRoot.getChild("light3");
    }

    public static LayerDefinition createLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0), PartPose.ZERO);
        root.addOrReplaceChild("hat", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0), PartPose.ZERO);
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(0, 0).addBox(0, 0, 0, 0, 0, 0), PartPose.ZERO);

        float bootOffset = 10.0f;
        float rot = (float) Math.toRadians(45);

        PartDefinition leftLeg = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12.0F, 0.0F));
        PartDefinition bootLeft = leftLeg.addOrReplaceChild("bootLeft", CubeListBuilder.create(), PartPose.offset(0, bootOffset, 0));

        CubeListBuilder bodyCubes = CubeListBuilder.create()
                .texOffs(0, 23).addBox("middle", -1, 0, -1, 2, 1, 2)
                .texOffs(0, 1).addBox("top", -3, 1, -3, 6, 1, 6)
                .texOffs(0, 17).addBox("bottom", -2, -1, -2, 4, 1, 4);
        bootLeft.addOrReplaceChild("body", bodyCubes, PartPose.offsetAndRotation(0, 0, 0, 0, rot, 0));

        bootLeft.addOrReplaceChild("wing0", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap0", -1, 0, -7, 6, 1, 6)
                .texOffs(0, 27).addBox("pin0", 0, -1, -3, 1, 3, 1), PartPose.ZERO);
        bootLeft.addOrReplaceChild("wing1", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap1", -1, 0, 1, 6, 1, 6)
                .texOffs(0, 27).addBox("pin1", 0, -1, 2, 1, 3, 1), PartPose.ZERO);

        PartDefinition rightLeg = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12.0F, 0.0F));
        PartDefinition bootRight = rightLeg.addOrReplaceChild("bootRight", CubeListBuilder.create(), PartPose.offset(0, bootOffset, 0));

        bootRight.addOrReplaceChild("body", CubeListBuilder.create()
                        .texOffs(0, 23).addBox("middle", -1, 0, -1, 2, 1, 2)
                        .texOffs(0, 1).addBox("top", -3, 1, -3, 6, 1, 6)
                        .texOffs(0, 17).addBox("bottom", -2, -1, -2, 4, 1, 4),
                PartPose.offsetAndRotation(0, 0, 0, 0, rot, 0));

        bootRight.addOrReplaceChild("wing2", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap2", -5, 0, 1, 6, 1, 6)
                .texOffs(0, 27).addBox("pin2", -1, -1, 2, 1, 3, 1), PartPose.ZERO);
        bootRight.addOrReplaceChild("wing3", CubeListBuilder.create()
                .texOffs(0, 9).addBox("flap3", -5, 0, -7, 6, 1, 6)
                .texOffs(0, 27).addBox("pin3", -1, -1, -3, 1, 3, 1), PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createLightLayerDefinition() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("light0", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1, 0, -7, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light1", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-1, 0, 1, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light2", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-5, 0, 1, 6, 1, 6), PartPose.ZERO);
        root.addOrReplaceChild("light3", CubeListBuilder.create()
                .texOffs(24, 0).addBox(-5, 0, -7, 6, 1, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.leftLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        this.rightLeg.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
        renderLights(poseStack, vertexConsumer, packedLight, packedOverlay);
    }

    @SuppressWarnings("unused")
    public void renderLights(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay) {
        int lightARGB = 0xFF000000 | lightColor;

        poseStack.pushPose();
        this.leftLeg.translateAndRotate(poseStack);
        poseStack.translate(0, 10f / 16f, 0);
        this.light0.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, lightARGB);
        this.light1.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, lightARGB);
        poseStack.popPose();

        poseStack.pushPose();
        this.rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0, 10f / 16f, 0);
        this.light2.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, lightARGB);
        this.light3.render(poseStack, vertexConsumer, LightTexture.FULL_BRIGHT, packedOverlay, lightARGB);
        poseStack.popPose();
    }

    @Override
    public void setupAnim(@NotNull LivingEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.young = false;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
    }
}
