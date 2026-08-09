package li.cil.oc.core.impl.common.item;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Chamelium extends DelegateItem {
    public Chamelium(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 32;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, @NotNull Player player, @NotNull InteractionHand hand) {
        if (OCSettings.get().chameliumEdible) {
            player.startUsingItem(hand);
        }
        return super.use(world, player, hand);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, Level world, @NotNull LivingEntity entity) {
        if (!world.isClientSide && entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 100, 0));
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200, 0));
        }
        stack.shrink(1);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }
}
