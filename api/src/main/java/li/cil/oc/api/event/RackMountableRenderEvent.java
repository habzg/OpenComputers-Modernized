package li.cil.oc.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Fired to allow rendering a custom overlay for {@link RackMountable}s.
 * <br>
 * When this event is fired, the GL state is set up such that the origin is
 * the top left corner of the mountable the event was fired for. It's the
 * event handler's responsibility to not render outside the are of the
 * mountable (unless that's explicitly what they're going for, of course).
 */
public interface RackMountableRenderEvent extends Event {
    /**
     * The rack that house the mountable this event is fired for.
     */
    @SuppressWarnings("unused")
    Rack rack();

    /**
     * The index of the mountable in the rack the event is fired for.
     */
    @SuppressWarnings("unused")
    int mountable();

    /**
     * Some additional data made available by the mountable. May be <code>null</code>.
     *
     * @see RackMountable#getData()
     */
    @SuppressWarnings("unused")
    CompoundTag data();

    /**
     * Fired when the static rack model is rendered.
     * <br>
     * Code here runs inside a <code>ISimpleBlockRenderingHandler</code>, so functionality
     * is limited to what's possible in there. This is primarily meant to allow setting
     * a custom override texture (<code>renderer.setOverrideBlockTexture</code>) for the
     * mountables front.
     * <br>
     * The bounds will be set up before this call, so you may adjust those, if you wish.
     */
    interface Block extends RackMountableRenderEvent {
        /**
         * The front-facing side, i.e. where the mountable is visible on the rack.
         */
        @SuppressWarnings("unused")
        Direction side();

        /**
         * The renderer used for rendering the block.
         */
        @SuppressWarnings("unused")
        Object renderer();

        /**
         * The texture currently set to use for the front of the mountable, or <code>null</code>.
         */
        @SuppressWarnings("unused")
        TextureAtlasSprite getFrontTextureOverride();

        /**
         * Set the texture to use for the front of the mountable.
         *
         * @param texture the texture to use.
         */
        @SuppressWarnings("unused")
        void setFrontTextureOverride(TextureAtlasSprite texture);
    }

    /**
     * Fired when the dynamic rack model is rendered.
     * <br>
     * Code here runs inside a <code>BlockEntityRenderer</code>, so go nuts. This is
     * primarily meant to allow rendering custom overlays, such as LEDs. The GL state
     * will have been adjusted such that rendering a one by one quad starting at the
     * origin will fill the full front face of the rack (i.e. rotation and translation
     * have already been applied).
     * <br>
     * If you wish to have something glowing (like LEDs), you'll have to disable
     * lighting yourself (and enable it again afterwards!).
     * <br>
     * Use the {@link #renderOverlay(ResourceLocation)} to render a slice from a
     * texture in the vertical area occupied by the mountable.
     */
    interface BlockEntity extends RackMountableRenderEvent {
        /**
         * The vertical low texture coordinate for the mountable's slot.
         */
        float v0();

        /**
         * The vertical high texture coordinate for the mountable's slot.
         */
        float v1();

        @SuppressWarnings("unused")
        void setPoseStack(PoseStack poseStack);

        @SuppressWarnings("unused")
        void setBufferSource(MultiBufferSource bufferSource);

        @SuppressWarnings("unused")
        void setPackedLight(int packedLight);

        @SuppressWarnings("unused")
        void setPackedOverlay(int packedOverlay);

        PoseStack getPoseStack();

        MultiBufferSource getBufferSource();

        @SuppressWarnings("unused")
        int getPackedLight();

        int getPackedOverlay();

        /**
         * Utility method for rendering a texture as the front-side overlay.
         *
         * @param texture the texture to use to render the overlay.
         */
        default void renderOverlay(final ResourceLocation texture) {
            renderOverlay(texture, 0, 1);
        }

        /**
         * Utility method for rendering a texture as the front-side overlay
         * over a specified horizontal area.
         *
         * @param texture the texture to use to render the overlay.
         * @param u0      the lower end of the vertical area to render at.
         * @param u1      the upper end of the vertical area to render at.
         */
        default void renderOverlay(final ResourceLocation texture, final float u0, final float u1) {
            PoseStack poseStack = getPoseStack();
            MultiBufferSource bufferSource = getBufferSource();
            if (poseStack == null || bufferSource == null) return;
            int packedOverlay = getPackedOverlay();
            var consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
            var m = poseStack.last().pose();
            int fullBright = 0xF000F0;
            float v0 = v0();
            float v1 = v1();
            consumer.addVertex(m, u0, v1, 0).setColor(255, 255, 255, 255).setUv(u0, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(m, u1, v1, 0).setColor(255, 255, 255, 255).setUv(u1, v1).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(m, u1, v0, 0).setColor(255, 255, 255, 255).setUv(u1, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            consumer.addVertex(m, u0, v0, 0).setColor(255, 255, 255, 255).setUv(u0, v0).setOverlay(packedOverlay).setLight(fullBright).setNormal(0, 1, 0);
            if (bufferSource instanceof MultiBufferSource.BufferSource bs) {
                bs.endBatch();
            }
        }
    }
}
