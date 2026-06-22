package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;

public class Printer extends Player {
    public final li.cil.oc.core.impl.common.tileentity.Printer printer;

    public Printer(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Printer printer, net.minecraft.world.entity.player.Player player) {
        super(Menus.PRINTER.get(), containerId, playerInventory, printer, player);
        this.printer = printer;
        addSlot(18, 19, Slot.Filtered, li.cil.oc.core.common.Tier.Any);
        addSlot(18, 51, Slot.Filtered, li.cil.oc.core.common.Tier.Any);
        addSlot(152, 35);
        addPlayerInventorySlots(8, 84);
    }

    public double progress() {
        return synchronizedData.getDouble("progress");
    }

    public int amountMaterial() {
        return synchronizedData.getInt("amountMaterial");
    }

    public int amountInk() {
        return synchronizedData.getInt("amountInk");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        synchronizedData.putDouble("progress", printer.isPrinting() ? printer.progress() / 100.0 : 0);
        synchronizedData.putInt("amountMaterial", printer.amountMaterial);
        synchronizedData.putInt("amountInk", printer.amountInk);
        super.detectCustomDataChanges(nbt);
    }
}
