package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.InventorySlots.InventorySlot;
import li.cil.oc.core.impl.client.gui.Icons;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;
import java.util.function.Supplier;

public class DynamicComponentSlot extends ComponentSlot {
    private final Function<DynamicComponentSlot, InventorySlot> info;
    private final Supplier<Integer> containerTierGetter;

    public DynamicComponentSlot(AbstractContainerMenu container, Container inventory, int index, int x, int y,
                                Function<DynamicComponentSlot, InventorySlot> info,
                                Supplier<Integer> containerTierGetter) {
        super(container, inventory, index, x, y);
        this.info = info;
        this.containerTierGetter = containerTierGetter;
    }

    @Override
    public int tier() {
        int mainTier = containerTierGetter.get();
        if (mainTier >= 0) return info.apply(this).tier();
        return mainTier;
    }

    @Override
    public ResourceLocation tierIcon() {
        return Icons.get(tier());
    }

    @Override
    public String slot() {
        int mainTier = containerTierGetter.get();
        if (mainTier >= 0) return info.apply(this).slot();
        return li.cil.oc.core.common.Slot.None;
    }

    @Override
    public int getMaxStackSize() {
        String s = slot();
        if (s.equals(li.cil.oc.core.common.Slot.Tool) || s.equals(li.cil.oc.core.common.Slot.Any) || s.equals(li.cil.oc.core.common.Slot.Filtered)) {
            return super.getMaxStackSize();
        }
        if (s.equals(li.cil.oc.core.common.Slot.None)) return 0;
        return 1;
    }

    @Override
    protected void clearIfInvalid(Player player) {
        if (SideTracker.isServer() && hasItem() && !mayPlace(getItem())) {
            ItemStack stack = getItem();
            set(ItemStack.EMPTY);
            InventoryUtils.addToPlayerInventory(stack, player);
        }
    }
}
