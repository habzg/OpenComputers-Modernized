package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.server.command.DebugWhitelistCommand;
import li.cil.oc.neoforge.common.item.data.DebugCardData;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import li.cil.oc.neoforge.server.component.DebugCard;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TpsCard extends DelegateItem {

    @SuppressWarnings("unused")
    public TpsCard(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        super.tooltipExtended(stack, tooltip);
        String accessPlayer = getAccessPlayerFromStack(stack);
        if (!accessPlayer.isEmpty()) {
            tooltip.add(Component.literal("§8" + accessPlayer + "§r"));
        }
    }

    private static String getAccessPlayerFromStack(ItemStack stack) {
        return DebugCardData.getAccessPlayer(stack);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player.isShiftKeyDown() && DebugWhitelistCommand.INSTANCE.isOp(player.createCommandSourceStack())) {
            var data = new DebugCardData(stack);
            String name = player.getName().getString();
            String accessPlayer = getAccessPlayerFromStack(stack);
            if (!accessPlayer.isEmpty() && accessPlayer.equals(name)) {
                data.access = null;
            } else {
                data.access = new DebugCard.AccessContext(name, "");
            }
            data.save(stack);
            player.swing(hand);
        }
        return InteractionResultHolder.pass(stack);
    }
}
