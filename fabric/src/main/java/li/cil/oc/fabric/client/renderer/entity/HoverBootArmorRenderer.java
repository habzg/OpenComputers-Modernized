package li.cil.oc.fabric.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.core.impl.client.renderer.item.HoverBootRenderer;
import li.cil.oc.core.impl.util.ItemColorizer;
import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public class HoverBootArmorRenderer implements ArmorRenderer {
    public static final HoverBootArmorRenderer INSTANCE = new HoverBootArmorRenderer();

    private HoverBootArmorRenderer() {
    }

    @Override
    public void render(PoseStack matrices, MultiBufferSource vertexConsumers, ItemStack stack, LivingEntity entity, EquipmentSlot slot, int light, HumanoidModel<LivingEntity> contextModel) {
        HoverBootRenderer.INSTANCE.leftLeg.copyFrom(contextModel.leftLeg);
        HoverBootRenderer.INSTANCE.rightLeg.copyFrom(contextModel.rightLeg);
        HoverBootRenderer.INSTANCE.lightColor = ItemColorizer.hasColor(stack) ? ItemColorizer.getColor(stack) : 0x66DD55;
        ArmorRenderer.renderPart(matrices, vertexConsumers, light, stack, HoverBootRenderer.INSTANCE,
                HoverBootLayer.TEXTURE);
    }
}
