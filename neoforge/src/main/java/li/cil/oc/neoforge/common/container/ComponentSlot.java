package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.impl.common.tileentity.traits.PlayerInputAware;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public abstract class ComponentSlot extends Slot {
    public final AbstractContainerMenu containerMenu;
    public Consumer<Slot> changeListener = null;

    public ComponentSlot(AbstractContainerMenu containerMenu, net.minecraft.world.Container inventory, int index, int x, int y) {
        super(inventory, index, x, y);
        this.containerMenu = containerMenu;
    }

    public abstract String slot();

    public abstract int tier();

    public abstract ResourceLocation tierIcon();

    @Override
    public boolean isActive() {
        return !slot().equals(li.cil.oc.core.common.Slot.None) && tier() != li.cil.oc.core.common.Tier.None && super.isActive();
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return container.canPlaceItem(getSlotIndex(), stack);
    }

    @Override
    public void onTake(@NotNull Player player, @NotNull ItemStack stack) {
        super.onTake(player, stack);
        for (Slot slotObj : containerMenu.slots) {
            if (slotObj instanceof ComponentSlot) {
                ((ComponentSlot) slotObj).clearIfInvalid(player);
            }
        }
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        super.set(stack);
        if (container instanceof PlayerInputAware) {
            Player player = getPlayerFromContainer();
            if (player != null) {
                ((PlayerInputAware) container).onSetInventorySlotContents(player, getSlotIndex(), stack);
            }
        }
    }

    @Override
    public void setChanged() {
        super.setChanged();
        Player player = getPlayerFromContainer();
        for (Slot slotObj : containerMenu.slots) {
            if (slotObj instanceof ComponentSlot) {
                ((ComponentSlot) slotObj).clearIfInvalid(player);
            }
        }
        if (changeListener != null) {
            changeListener.accept(this);
        }
    }

    private Player getPlayerFromContainer() {
        if (containerMenu instanceof li.cil.oc.neoforge.common.container.Player ocPlayer) {
            return ocPlayer.playerEntity;
        }
        return null;
    }

    protected void clearIfInvalid(Player player) {
    }
}
