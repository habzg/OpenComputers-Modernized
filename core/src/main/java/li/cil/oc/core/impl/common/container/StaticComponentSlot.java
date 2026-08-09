package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.impl.client.gui.Icons;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class StaticComponentSlot extends ComponentSlot {
    private final String slotType;
    private final int tierType;
    private final ResourceLocation tierIcon;

    public StaticComponentSlot(AbstractContainerMenu container, Container inventory, int index, int x, int y,
                                String slot, int tier) {
        super(container, inventory, index, x, y);
        this.slotType = slot;
        this.tierType = tier;
        this.tierIcon = Icons.get(tier);
    }

    @Override
    public String slot() {
        return slotType;
    }

    @Override
    public int tier() {
        return tierType;
    }

    @Override
    public ResourceLocation tierIcon() {
        return tierIcon;
    }

    @Override
    public int getMaxStackSize() {
        if (slotType.equals(li.cil.oc.core.common.Slot.Tool) || slotType.equals(li.cil.oc.core.common.Slot.Any) || slotType.equals(li.cil.oc.core.common.Slot.Filtered)) {
            return super.getMaxStackSize();
        }
        if (slotType.equals(li.cil.oc.core.common.Slot.None)) return 0;
        return 1;
    }
}
