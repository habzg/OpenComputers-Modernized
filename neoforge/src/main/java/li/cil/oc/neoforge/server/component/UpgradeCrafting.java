package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.internal.Robot;
import li.cil.oc.core.impl.server.component.UpgradeCraftingBase;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class UpgradeCrafting extends UpgradeCraftingBase {
    public UpgradeCrafting(Robot host) {
        super(host);
    }

    @Override
    protected void postItemCraftedEvent(@NotNull Player player, @NotNull ItemStack result, @NotNull CraftingInventory inventory) {
        NeoForge.EVENT_BUS.post(new PlayerEvent.ItemCraftedEvent(player, result, inventory));
    }

    @Override
    protected void postPlayerDestroyItemEvent(@NotNull Player player, @NotNull ItemStack stack) {
        NeoForge.EVENT_BUS.post(new PlayerDestroyItemEvent(player, stack, InteractionHand.MAIN_HAND));
    }
}
