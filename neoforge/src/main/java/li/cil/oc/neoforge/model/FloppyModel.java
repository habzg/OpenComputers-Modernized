package li.cil.oc.neoforge.model;

import java.util.List;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloppyModel implements IDynamicBakedModel {
    private final BakedModel[] dyeModels;
    private final ItemOverrides overrides;

    @SuppressWarnings("unused")
    public FloppyModel(BakedModel[] dyeModels) {
        this.dyeModels = dyeModels;
        this.overrides = new ItemOverrides() {
            @Override
            public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
                int color = 8;
                var data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                if (data != null && !data.isEmpty()) {
                    var tag = data.copyTag();
                    if (tag.contains(OCSettings.namespace + "color")) {
                        color = tag.getInt(OCSettings.namespace + "color");
                    }
                }
                color = Math.clamp(color, 0, 15);
                BakedModel result = dyeModels[color];
                return result != null ? result : original;
            }
        };
    }

    @Override
    public @NotNull List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable net.minecraft.client.renderer.RenderType renderType) {
        return dyeModels[8].getQuads(state, side, rand, data, renderType);
    }

    @Override
    public @NotNull ItemOverrides getOverrides() {
        return overrides;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return true;
    }

    @Override
    public boolean isGui3d() {
        return false;
    }

    @Override
    public boolean usesBlockLight() {
        return false;
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon() {
        return dyeModels[8].getParticleIcon(net.neoforged.neoforge.client.model.data.ModelData.EMPTY);
    }

    @Override
    public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
        return dyeModels[8].getParticleIcon(data);
    }

    @Override
    public @NotNull BakedModel applyTransform(@NotNull net.minecraft.world.item.ItemDisplayContext transformType, @NotNull com.mojang.blaze3d.vertex.PoseStack poseStack, boolean applyLeftHandTransform) {
        dyeModels[8].applyTransform(transformType, poseStack, applyLeftHandTransform);
        return this;
    }
}
