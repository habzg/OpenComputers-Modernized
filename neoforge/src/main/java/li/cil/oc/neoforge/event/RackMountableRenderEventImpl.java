package li.cil.oc.neoforge.event;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.api.event.RackMountableRenderEvent;
import li.cil.oc.api.internal.Rack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RackMountableRenderEventImpl extends Event implements RackMountableRenderEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final Rack rack;
    protected final int mountable;
    protected final CompoundTag data;

    public RackMountableRenderEventImpl(Rack rack, int mountable, CompoundTag data) {
        this.rack = rack;
        this.mountable = mountable;
        this.data = data;
    }

    @Override
    public Rack rack() {
        return rack;
    }

    @Override
    public int mountable() {
        return mountable;
    }

    @Override
    public CompoundTag data() {
        return data;
    }

    public static class Block extends RackMountableRenderEventImpl implements RackMountableRenderEvent.Block {
        protected final Direction side;
        protected final Object renderer;
        private TextureAtlasSprite frontTextureOverride;

        public Block(Rack rack, int mountable, CompoundTag data, Direction side, Object renderer) {
            super(rack, mountable, data);
            this.side = side;
            this.renderer = renderer;
        }

        @Override
        public Direction side() {
            return side;
        }

        @Override
        public Object renderer() {
            return renderer;
        }

        @Override
        public TextureAtlasSprite getFrontTextureOverride() {
            return frontTextureOverride;
        }

        @Override
        public void setFrontTextureOverride(TextureAtlasSprite texture) {
            this.frontTextureOverride = texture;
        }
    }

    public static class BlockEntity extends RackMountableRenderEventImpl implements RackMountableRenderEvent.BlockEntity {
        protected final float v0;
        protected final float v1;
        private PoseStack poseStack;
        private MultiBufferSource bufferSource;
        private int packedLight;
        private int packedOverlay;

        public BlockEntity(Rack rack, int mountable, CompoundTag data, float v0, float v1) {
            super(rack, mountable, data);
            this.v0 = v0;
            this.v1 = v1;
        }

        @Override
        public float v0() {
            return v0;
        }

        @Override
        public float v1() {
            return v1;
        }

        @Override
        public void setPoseStack(PoseStack poseStack) {
            this.poseStack = poseStack;
        }

        @Override
        public void setBufferSource(MultiBufferSource bufferSource) {
            this.bufferSource = bufferSource;
        }

        @Override
        public void setPackedLight(int packedLight) {
            this.packedLight = packedLight;
        }

        @Override
        public void setPackedOverlay(int packedOverlay) {
            this.packedOverlay = packedOverlay;
        }

        @Override
        public PoseStack getPoseStack() {
            return poseStack;
        }

        @Override
        public MultiBufferSource getBufferSource() {
            return bufferSource;
        }

        @Override
        public int getPackedLight() {
            return packedLight;
        }

        @Override
        public int getPackedOverlay() {
            return packedOverlay;
        }
    }
}
