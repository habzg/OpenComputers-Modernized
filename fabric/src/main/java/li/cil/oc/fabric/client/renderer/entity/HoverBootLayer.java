package li.cil.oc.fabric.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ItemColorizer;
import li.cil.oc.fabric.common.init.Items;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

public class HoverBootLayer extends RenderLayer<Player, PlayerModel<Player>> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/drone.png");

    private final ModelPart light0;
    private final ModelPart light1;
    private final ModelPart light2;
    private final ModelPart light3;

    @SuppressWarnings("unused")
    public HoverBootLayer(RenderLayerParent<Player, PlayerModel<Player>> renderer) {
        super(renderer);
        ModelPart root = createLightLayerDefinition().bakeRoot();
        this.light0 = root.getChild("light0");
        this.light1 = root.getChild("light1");
        this.light2 = root.getChild("light2");
        this.light3 = root.getChild("light3");
    }

    private static LayerDefinition createLightLayerDefinition() {
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
    public void render(@NotNull PoseStack poseStack, @NotNull MultiBufferSource buffer, int packedLight,
                       @NotNull Player player, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        var boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!boots.is(Items.HOVER_BOOTS)) return;

        int lightARGB = 0xFF000000 | (ItemColorizer.hasColor(boots) ? ItemColorizer.getColor(boots) : 0x66DD55);
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));

        poseStack.pushPose();
        this.getParentModel().leftLeg.translateAndRotate(poseStack);
        poseStack.translate(0, 10f / 16f, 0);
        this.light0.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightARGB);
        this.light1.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightARGB);
        poseStack.popPose();

        poseStack.pushPose();
        this.getParentModel().rightLeg.translateAndRotate(poseStack);
        poseStack.translate(0, 10f / 16f, 0);
        this.light2.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightARGB);
        this.light3.render(poseStack, consumer, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, lightARGB);
        poseStack.popPose();
    }
}
