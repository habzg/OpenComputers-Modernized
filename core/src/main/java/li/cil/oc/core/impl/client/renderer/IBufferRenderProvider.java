package li.cil.oc.core.impl.client.renderer;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public interface IBufferRenderProvider {
    RenderType borderRenderType();

    RenderType backgroundRenderType();

    RenderType textRenderType(ResourceLocation texture);
}
