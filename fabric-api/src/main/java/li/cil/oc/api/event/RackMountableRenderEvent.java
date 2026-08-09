package li.cil.oc.api.event;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.internal.Rack;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

/**
 * Fired to allow rendering a custom overlay for {@link RackMountable}s.
 * <br>
 * When this event is fired, the GL state is set up such that the origin is
 * the top left corner of the mountable the event was fired for. It's the
 * event handler's responsibility to not render outside the area of the
 * mountable (unless that's explicitly what they're going for, of course).
 */
@SuppressWarnings("unused")
public abstract class RackMountableRenderEvent implements Cancelled {
    private boolean canceled;

    /**
     * The rack that houses the mountable this event is fired for.
     */
    public final Rack rack;

    /**
     * The index of the mountable in the rack the event is fired for.
     */
    public final int mountable;

    /**
     * Some additional data made available by the mountable. May be {@code null}.
     *
     * @see RackMountable#getData
     */
    public final CompoundTag data;

    public RackMountableRenderEvent(Rack rack, int mountable, CompoundTag data) {
        this.rack = rack;
        this.mountable = mountable;
        this.data = data;
    }

    @Override
    public boolean isCanceled() {
        return canceled;
    }

    @Override
    public void setCanceled(boolean canceled) {
        this.canceled = canceled;
    }

    /**
     * Fired when the static rack model is rendered.
     * <br>
     * Code here runs as a part of model baking, so functionality is
     * limited to what's possible with models. This is meant to allow
     * setting a custom front texture for the mountables front.
     * <br>
     * The bounds will be set up before this call, so you may adjust those, if you wish.
     */
    public static class Block extends RackMountableRenderEvent {
        /**
         * The front-facing side, i.e. where the mountable is visible on the rack.
         */
        public final Direction side;

        /**
         * The renderer used for the rack's base model, may be {@code null}.
         */
        public final Object renderer;

        private TextureAtlasSprite frontTextureOverride;

        public Block(final Rack rack, final int mountable, final CompoundTag data, final Direction side, final Object renderer) {
            super(rack, mountable, data);
            this.side = side;
            this.renderer = renderer;
        }

        /**
         * The texture currently set to use for the front of the mountable, or {@code null}.
         */
        public TextureAtlasSprite getFrontTextureOverride() {
            return frontTextureOverride;
        }

        /**
         * Set the texture to use for the front of the mountable.
         *
         * @param texture the texture to use.
         */
        public void setFrontTextureOverride(final TextureAtlasSprite texture) {
            frontTextureOverride = texture;
        }

        @FunctionalInterface
        public interface Listener {
            void onRackMountableRender(Block event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRackMountableRender(event);
                if (event.isCanceled()) break;
            }
        });
    }

    /**
     * Fired when the dynamic rack model is rendered.
     * <br>
     * Code here runs inside a {@link BlockEntityRenderer}, so go nuts. This is
     * primarily meant to allow rendering custom overlays, such as LEDs. The GL state
     * will have been adjusted such that rendering a one by one quad starting at the
     * origin will fill the full front face of the rack (i.e. rotation and translation
     * have already been applied).
     */
    public static class BlockEntity extends RackMountableRenderEvent {
        /**
         * The vertical low texture coordinate for the mountable's slot.
         * <br>
         * This is purely for convenience; they're computed as {@code (2/16)+i*(3/16)}.
         */
        public final float v0;

        /**
         * The vertical high texture coordinate for the mountable's slot.
         */
        public final float v1;

        private PoseStack poseStack;
        private MultiBufferSource bufferSource;
        private int packedLight;
        private int packedOverlay;

        public BlockEntity(final Rack rack, final int mountable, final CompoundTag data, final float v0, final float v1) {
            super(rack, mountable, data);
            this.v0 = v0;
            this.v1 = v1;
        }

        public void setPoseStack(PoseStack poseStack) {
            this.poseStack = poseStack;
        }

        public void setBufferSource(MultiBufferSource bufferSource) {
            this.bufferSource = bufferSource;
        }

        public void setPackedLight(int packedLight) {
            this.packedLight = packedLight;
        }

        public void setPackedOverlay(int packedOverlay) {
            this.packedOverlay = packedOverlay;
        }

        /**
         * The transformation used by the rendering engine.
         */
        public PoseStack getPoseStack() {
            return poseStack;
        }

        /**
         * An accessor to the renderer's buffer context.
         */
        public MultiBufferSource getBufferSource() {
            return bufferSource;
        }

        /**
         * Packed block light texture coordinates.
         */
        public int getPackedLight() {
            return packedLight;
        }

        /**
         * Packed overlay texture coordinates.
         */
        public int getPackedOverlay() {
            return packedOverlay;
        }

        @FunctionalInterface
        public interface Listener {
            void onRackMountableRender(BlockEntity event);
        }

        public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, (listeners) -> (event) -> {
            for (Listener listener : listeners) {
                listener.onRackMountableRender(event);
            }
        });
    }
}
