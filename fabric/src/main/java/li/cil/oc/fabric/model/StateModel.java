package li.cil.oc.fabric.model;

import java.util.List;
import java.util.function.Function;
import li.cil.oc.core.impl.client.renderer.item.ItemState;
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

public class StateModel implements BakedModel {
  private final BakedModel defaultModel;
  private final ResourceLocation onModelId;
  private final ResourceLocation offModelId;
  private final ItemOverrides overrides;
  private BakedModel onModel;
  private BakedModel offModel;

  public StateModel(BakedModel defaultModel, ResourceLocation onModelId, ResourceLocation offModelId,
      Function<ItemStack, ItemState> stateResolver) {
    this.defaultModel = defaultModel;
    this.onModelId = onModelId;
    this.offModelId = offModelId;
    //noinspection DataFlowIssue
    this.overrides = new ItemOverrides(null, null, List.of()) {
      @Override
      public @NotNull BakedModel resolve(@NotNull BakedModel original, @NotNull ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        BakedModel result = modelFor(stateResolver.apply(stack));
        return result != null ? result : original;
      }
    };
  }

  private BakedModel modelFor(ItemState state) {
    BakedModel model = switch (state) {
      case ON -> onModel();
      case OFF -> offModel();
      default -> defaultModel;
    };
    return model != null ? model : fallback();
  }

  private BakedModel fallback() {
    BakedModel model = offModel();
    if (model != null) return model;
    model = onModel();
    if (model != null) return model;
    return defaultModel;
  }

  private BakedModel onModel() {
    if (onModel == null && onModelId != null) {
      onModel = resolve(onModelId);
    }
    return onModel;
  }

  private BakedModel offModel() {
    if (offModel == null && offModelId != null) {
      offModel = resolve(offModelId);
    }
    return offModel;
  }

  private BakedModel resolve(ResourceLocation id) {
    if (id == null) return null;
    return Minecraft.getInstance().getModelManager().getModel(id);
  }

  @Override
  public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand) {
    BakedModel model = fallback();
    return model != null ? model.getQuads(state, side, rand) : List.of();
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
    BakedModel model = fallback();
    return model != null ? model.getParticleIcon() : Minecraft.getInstance().getModelManager().getMissingModel().getParticleIcon();
  }

  @Override
  public @NotNull ItemTransforms getTransforms() {
    BakedModel model = fallback();
    return model != null ? model.getTransforms() : ItemTransforms.NO_TRANSFORMS;
  }
}
