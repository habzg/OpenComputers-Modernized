package li.cil.oc.fabric.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.util.function.Function;
import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.renderer.IBufferRenderProvider;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class BufferRenderProvider implements IBufferRenderProvider {
    public static final IBufferRenderProvider INSTANCE = new BufferRenderProvider();

    private final RenderType borderRenderType;
    private final RenderType backgroundRenderType;
    private final Function<ResourceLocation, RenderType> textRenderType;

    private BufferRenderProvider() {
        borderRenderType = RenderType.create(
                "oc_gui_border",
                DefaultVertexFormat.POSITION_TEX,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_TEX_SHADER)
                        .setTextureState(new RenderStateShard.TextureStateShard(Textures.guiBorders, false, false))
                        .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false)
        );

        backgroundRenderType = RenderType.create(
                "oc_gui_bg",
                DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS,
                256,
                false,
                false,
                RenderType.CompositeState.builder()
                        .setShaderState(RenderStateShard.POSITION_COLOR_SHADER)
                        .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                        .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                        .setCullState(RenderStateShard.NO_CULL)
                        .createCompositeState(false)
        );

        textRenderType = Util.memoize(texture ->
                RenderType.create(
                        "oc_gui_text",
                        DefaultVertexFormat.POSITION_COLOR_TEX_LIGHTMAP,
                        VertexFormat.Mode.QUADS,
                        786432,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(RenderStateShard.POSITION_COLOR_TEX_LIGHTMAP_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(texture, false, false))
                                .setTransparencyState(RenderStateShard.TRANSLUCENT_TRANSPARENCY)
                                .setLightmapState(RenderStateShard.LIGHTMAP)
                                .setDepthTestState(RenderStateShard.NO_DEPTH_TEST)
                                .setCullState(RenderStateShard.NO_CULL)
                                .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                                .createCompositeState(false)
                )
        );
    }

    @SuppressWarnings("unused")
    @Override
    public RenderType borderRenderType() {
        return borderRenderType;
    }

    @SuppressWarnings("unused")
    @Override
    public RenderType backgroundRenderType() {
        return backgroundRenderType;
    }

    @SuppressWarnings("unused")
    @Override
    public RenderType textRenderType(ResourceLocation texture) {
        return textRenderType.apply(texture);
    }
}
