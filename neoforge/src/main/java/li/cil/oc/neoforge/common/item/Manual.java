package li.cil.oc.neoforge.common.item;

import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class Manual extends DelegateItem {
    public Manual(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        tooltip.add(Component.literal(ChatFormatting.DARK_GRAY + "v" + OpenComputers.Version));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        if (level.isClientSide) {
            if (player.isShiftKeyDown()) {
                li.cil.oc.api.Manual.reset();
            }
            li.cil.oc.api.Manual.openFor(player);
        }
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        var level = context.getLevel();
        var player = context.getPlayer();
        var pos = context.getClickedPos();
        String path = li.cil.oc.api.Manual.pathFor(level, pos);
        if (path != null) {
            if (level.isClientSide) {
                li.cil.oc.api.Manual.openFor(player);
                li.cil.oc.api.Manual.reset();
                li.cil.oc.api.Manual.navigate(path);
            }
            return InteractionResult.SUCCESS;
        }
        return super.useOn(context);
    }
}
