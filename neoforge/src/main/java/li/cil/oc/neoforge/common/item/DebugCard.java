package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.data.DebugCardData;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class DebugCard extends DelegateItem {
    public DebugCard(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        super.tooltipExtended(stack, tooltip);
        var data = new DebugCardData(stack);
        if (data.access != null) {
            tooltip.add(Component.literal("§8" + DebugCardData.getAccessPlayer(stack) + "§r"));
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide && player.isShiftKeyDown()) {
            var data = new DebugCardData(stack);
            String name = player.getName().getString();
            String accessPlayer = DebugCardData.getAccessPlayer(stack);
            if (!accessPlayer.isEmpty() && accessPlayer.equals(name)) {
                data.access = null;
            } else {
                String nonce = "";
                if (Settings.get().debugCardAccess instanceof Settings.DebugCardAccess.Whitelist wl) {
                    var n = wl.nonce(name);
                    if (n != null) {
                        nonce = n;
                    } else {
                        player.sendSystemMessage(Component.literal("§cYou are not whitelisted to use debug card"));
                        player.swing(hand);
                        return InteractionResultHolder.pass(stack);
                    }
                }
                data.access = new li.cil.oc.neoforge.server.component.DebugCard.AccessContext(name, nonce);
            }
            data.save(stack);
            player.swing(hand);
        }
        return super.use(world, player, hand);
    }
}
