package li.cil.oc.neoforge.client.renderer.item;

import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class UpgradeRenderers {
    private static final Map<ResourceLocation, UpgradeRenderer> renderers = new HashMap<>();

    private UpgradeRenderers() {
    }

    public static void register(ResourceLocation itemId, UpgradeRenderer renderer) {
        renderers.put(itemId, renderer);
    }

    public static UpgradeRenderer get(ResourceLocation itemId) {
        return renderers.get(itemId);
    }

    public static void register() {
        register(ResourceLocation.parse("opencomputers:craftingupgrade"), new UpgradeRenderer() {
            private static final ResourceLocation TEXTURE = ResourceLocation.parse("opencomputers:textures/model/upgradecrafting.png");

            @Override
            @SuppressWarnings("unused")
            public String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints) {
                return MountPointName.Any;
            }

            @Override
            @SuppressWarnings("unused")
            public void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot, float pt, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
                li.cil.oc.core.impl.client.renderer.item.UpgradeRenderer.drawSimpleBlock(poseStack, buffer, packedLight, packedOverlay, mountPoint, TEXTURE, 0);
            }
        });

        register(ResourceLocation.parse("opencomputers:generatorupgrade"), new UpgradeRenderer() {
            private static final ResourceLocation TEXTURE = ResourceLocation.parse("opencomputers:textures/model/upgradegenerator.png");

            @Override
            @SuppressWarnings("unused")
            public String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints) {
                return MountPointName.Any;
            }

            @Override
            @SuppressWarnings("unused")
            public void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot, float pt, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
                float offset = 0;
                var cd = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (cd != null) {
                    var tag = cd.copyTag();
                    if (tag.contains("remainingTicks") && tag.getInt("remainingTicks") > 0) offset = 0.5f;
                    else if (tag.contains("oc:data")) {
                        var data = tag.getCompound("oc:data");
                        if (data.contains("remainingTicks") && data.getInt("remainingTicks") > 0) offset = 0.5f;
                    }
                }
                li.cil.oc.core.impl.client.renderer.item.UpgradeRenderer.drawSimpleBlock(poseStack, buffer, packedLight, packedOverlay, mountPoint, TEXTURE, offset);
            }
        });

        register(ResourceLocation.parse("opencomputers:inventoryupgrade"), new UpgradeRenderer() {
            private static final ResourceLocation TEXTURE = ResourceLocation.parse("opencomputers:textures/model/upgradeinventory.png");

            @Override
            @SuppressWarnings("unused")
            public String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints) {
                return MountPointName.Any;
            }

            @Override
            @SuppressWarnings("unused")
            public void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot, float pt, com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.MultiBufferSource buffer, int packedLight, int packedOverlay) {
                li.cil.oc.core.impl.client.renderer.item.UpgradeRenderer.drawSimpleBlock(poseStack, buffer, packedLight, packedOverlay, mountPoint, TEXTURE, 0);
            }
        });
    }
}
