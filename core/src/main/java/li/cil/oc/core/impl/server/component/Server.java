package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.server.network.Connector;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;


public class Server extends ServerBase {
    public Server(Rack rack, int slot) {
        super(rack, slot);
    }

    @Override
    public int tier() {
        var item = rack.getItem(slot);
        if (item.getItem() instanceof li.cil.oc.core.impl.common.item.Server serverItem) {
            return serverItem.tier;
        }
        return 0;
    }

    @Override
    public boolean onActivate(@NotNull Player player, InteractionHand hand, ItemStack heldItem, float hitX, float hitY) {
        if (!rack.level().isClientSide) {
            if (player.isShiftKeyDown()) {
                if (!machine.isRunning() && stillValid(player)) {
                    wasRunning = false;
                    hadErrored = false;
                    machine.start();
                }
            } else {
                var position = BlockPosition.apply(rack);
                ContainerProviderDelegate.get().openMenu(player, GuiType.ServerInRack, rack.level(), position.x(), GuiType.embedSlot(position.y(), this.slot), position.z());
            }
        }
        return true;
    }

    @Override
    protected void setInfiniteBuffer() {
        var n = node();
        if (n instanceof Connector c) {
            c.changeBuffer(Double.POSITIVE_INFINITY);
        }
    }

    @Override
    public void onItemRemoved(int slot, @NotNull ItemStack stack) {
        super.onItemRemoved(slot, stack);
        if (!rack.level().isClientSide) {
            var slotType = InventorySlots.server[tier()][slot].slot();
            if (slotType.equals(Slot.CPU)) {
                machine.stop();
            }
        }
    }
}
