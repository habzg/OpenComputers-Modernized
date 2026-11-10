package li.cil.oc.neoforge.model;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.List;
import java.util.function.Function;
import li.cil.oc.core.impl.client.renderer.item.ItemState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StateModel implements IDynamicBakedModel {
  private final BakedModel defaultModel;
  private final BakedModel onModel;
  private final BakedModel offModel;
  private final ItemOverrides overrides;

  public StateModel(BakedModel defaultModel, BakedModel onModel, BakedModel offModel,
      Function<ItemStack, ItemState> stateResolver) {
    this.defaultModel = defaultModel;
    this.onModel = onModel;
    this.offModel = offModel;
    this.overrides = new ItemOverrides() {
      @Override
      public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        BakedModel result = modelFor(stateResolver.apply(stack));
        return result != null ? result : original;
      }
    };
  }

  private BakedModel modelFor(ItemState state) {
    BakedModel model = switch (state) {
      case ON -> onModel;
      case OFF -> offModel;
      default -> defaultModel;
    };
    return model != null ? model : fallback();
  }

  private BakedModel fallback() {
    if (offModel != null) return offModel;
    if (onModel != null) return onModel;
    return defaultModel;
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData data, @Nullable RenderType renderType) {
    BakedModel model = fallback();
    return model != null ? model.getQuads(state, side, rand, data, renderType) : List.of();
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

  @SuppressWarnings("deprecation")
  @Override
  public @NotNull TextureAtlasSprite getParticleIcon() {
    BakedModel model = fallback();
    return model != null ? model.getParticleIcon(ModelData.EMPTY) : Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
  }

  @Override
  public @NotNull TextureAtlasSprite getParticleIcon(@NotNull ModelData data) {
    return getParticleIcon();
  }

  @Override
  public @NotNull BakedModel applyTransform(@NotNull ItemDisplayContext transformType, @NotNull PoseStack poseStack, boolean applyLeftHandTransform) {
    BakedModel model = fallback();
    if (model != null) model.applyTransform(transformType, poseStack, applyLeftHandTransform);
    return this;
  }
}
