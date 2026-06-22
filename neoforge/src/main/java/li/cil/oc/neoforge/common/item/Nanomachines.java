package li.cil.oc.neoforge.common.item;

import li.cil.oc.neoforge.common.item.data.NanomachineData;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import li.cil.oc.neoforge.common.nanomachines.ControllerImpl;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Nanomachines extends DelegateItem {
    public Nanomachines(Item.Properties properties) {
        super(properties);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return Rarity.UNCOMMON;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null && !customData.isEmpty()) {
            var data = new NanomachineData(stack);
            if (data.uuid != null && !data.uuid.isEmpty()) {
                tooltip.add(Component.literal("§8" + data.uuid.substring(0, Math.min(13, data.uuid.length())) + "...§7"));
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 32;
    }

    @Override
    public @NotNull ItemStack finishUsingItem(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity) {
        if (entity instanceof Player player) {
            if (!level.isClientSide) {
                var data = new NanomachineData(stack);
                li.cil.oc.api.Nanomachines.uninstallController(player);
                var controller = li.cil.oc.api.Nanomachines.installController(player);
                if (controller instanceof ControllerImpl ctrl) {
                    if (data.uuid != null && !data.uuid.isEmpty()) {
                        ctrl.uuid = data.uuid;
                    }
                    if (data.configuration != null) {
                        ctrl.configuration.load(data.configuration, level.registryAccess());
                    } else {
                        ctrl.reconfigure();
                    }
                } else if (controller != null) {
                    controller.reconfigure();
                }
            }
            stack.shrink(1);
        }
        if (stack.getCount() > 0) return stack;
        return ItemStack.EMPTY;
    }
}
