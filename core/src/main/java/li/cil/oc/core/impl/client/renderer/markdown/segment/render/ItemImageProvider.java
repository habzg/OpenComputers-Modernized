package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ItemImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(String data) {
        int splitIndex = data.lastIndexOf('@');
        String name = splitIndex > 0 ? data.substring(0, splitIndex) : data;
        var location = ResourceLocation.parse(name.toLowerCase(java.util.Locale.ROOT));
        var item = BuiltInRegistries.ITEM.get(location);
        if (item != BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
            return new ItemStackImageRenderer(new ItemStack(item, 1));
        }
        return new InteractiveImageRenderer() {
            @Override
            public String getTooltip(String tooltip) {
                return "gui.opencomputers.manual.warning.itemmissing";
            }

            @Override
            public boolean onMouseClick(int mouseX, int mouseY) {
                return false;
            }

            @Override
            public int getWidth() {
                return 64;
            }

            @Override
            public int getHeight() {
                return 64;
            }

            @Override
            public void render(PoseStack poseStack, MultiBufferSource bufferSource, int mouseX, int mouseY) {
                new TextureImageRenderer(Textures.guiManualMissingItem).render(poseStack, bufferSource, mouseX, mouseY);
            }
        };
    }
}
