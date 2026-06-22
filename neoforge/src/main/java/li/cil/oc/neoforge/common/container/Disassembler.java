package li.cil.oc.neoforge.common.container;

import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;

public class Disassembler extends Player {
    public final li.cil.oc.core.impl.common.tileentity.Disassembler disassembler;

    public Disassembler(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Disassembler disassembler, net.minecraft.world.entity.player.Player player) {
        super(Menus.DISASSEMBLER.get(), containerId, playerInventory, disassembler, player);
        this.disassembler = disassembler;
        addSlot(80, 35, "ocitem", li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 84);
    }

    public double disassemblyProgress() {
        return synchronizedData.getDouble("disassemblyProgress");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        synchronizedData.putDouble("disassemblyProgress", disassembler.progress());
        super.detectCustomDataChanges(nbt);
    }
}
