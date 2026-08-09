package li.cil.oc.fabric.model;

import java.util.List;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class FloppyModel implements BakedModel {
    private final ResourceLocation[] dyeModelIds;
    private BakedModel[] dyeModels;
    private final ItemOverrides overrides;

    @SuppressWarnings("unused")
    public FloppyModel(ResourceLocation[] dyeModelIds) {
        this.dyeModelIds = dyeModelIds;
        this.dyeModels = null;
        this.overrides = new ItemOverrides(null, null, List.of()) {
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
                BakedModel result = resolveDyeModel(color);
                return result != null ? result : original;
            }
        };
    }

    private BakedModel resolveDyeModel(int index) {
        if (dyeModels == null) {
            dyeModels = new BakedModel[dyeModelIds.length];
        }
        if (dyeModels[index] == null) {
            var modelManager = Minecraft.getInstance().getModelManager();
            var id = dyeModelIds[index];
            dyeModels[index] = modelManager.getModel(id);
        }
        return dyeModels[index];
    }

    private BakedModel defaultModel() {
        return resolveDyeModel(8);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
        var m = defaultModel();
        return m != null ? m.getQuads(state, side, rand) : List.of();
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
        var m = defaultModel();
        return m != null ? m.getParticleIcon() : Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
    }

    @Override
    public @NotNull ItemTransforms getTransforms() {
        var m = defaultModel();
        return m != null ? m.getTransforms() : ItemTransforms.NO_TRANSFORMS;
    }
}
