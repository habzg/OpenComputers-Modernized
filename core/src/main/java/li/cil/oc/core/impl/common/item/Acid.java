package li.cil.oc.core.impl.common.item;

import li.cil.oc.api.Nanomachines;
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

public class Acid extends DelegateItem {

    public Acid(Properties properties) {
        super(properties);
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 32;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, Level world, @NotNull LivingEntity entity) {
        if (!world.isClientSide && entity instanceof Player player) {
            player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 200));
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 600));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 1200));
            player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 2000));
            Nanomachines.uninstallController(player);
        }
        stack.shrink(1);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }
}
