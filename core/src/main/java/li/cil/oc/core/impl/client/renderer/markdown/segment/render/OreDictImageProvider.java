package li.cil.oc.core.impl.client.renderer.markdown.segment.render;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.manual.ImageProvider;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.manual.InteractiveImageRenderer;
import li.cil.oc.core.impl.client.Textures;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

public class OreDictImageProvider implements ImageProvider {
    @Override
    public ImageRenderer getImage(String data) {
        var tagLocation = ResourceLocation.parse(data);
        var tagKey = TagKey.create(Registries.ITEM, tagLocation);
        var tag = BuiltInRegistries.ITEM.getTag(tagKey);
        if (tag.isPresent()) {
            var stacks = new ArrayList<ItemStack>();
            for (var holder : tag.get()) {
                var stack = new ItemStack(holder);
                if (!stack.isEmpty()) stacks.add(stack);
            }
            if (!stacks.isEmpty()) {
                return new ItemStackImageRenderer(stacks.toArray(new ItemStack[0]));
            }
        }

        return new InteractiveImageRenderer() {
            @Override
            public String getTooltip(String tooltip) {
                return "gui.opencomputers.manual.warning.oredictmissing";
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
