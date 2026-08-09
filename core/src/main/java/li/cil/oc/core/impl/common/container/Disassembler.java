package li.cil.oc.core.impl.common.container;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

public class Disassembler extends Player {
    public final li.cil.oc.core.impl.common.blockentity.Disassembler disassembler;

    public Disassembler(MenuType<?> menuType, int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Disassembler disassembler, net.minecraft.world.entity.player.Player player) {
        super(menuType, containerId, playerInventory, disassembler, player);
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
