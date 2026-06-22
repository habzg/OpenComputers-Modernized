package li.cil.oc.neoforge.common.container;

import li.cil.oc.core.common.InventorySlots.InventorySlot;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.template.AssemblerTemplates;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;

public class Assembler extends Player {
    public final li.cil.oc.core.impl.common.tileentity.Assembler assembler;

    public Assembler(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Assembler assembler, net.minecraft.world.entity.player.Player player) {
        super(Menus.ASSEMBLER.get(), containerId, playerInventory, assembler, player);
        this.assembler = assembler;

        addSlot(new StaticComponentSlot(this, otherInventory, slots.size(), 12, 12, "template", Tier.Any) {
            @Override
            public boolean isActive() {
                return !isAssembling() && super.isActive();
            }
        });

        int slotSize = 18;
        for (int i = 0; i < 3; i++) {
            addSlot(34 + i * slotSize, 70, this::slotInfo);
        }

        for (int i = 0; i < 9; i++) {
            addSlot(34 + (i % 3) * slotSize, 12 + (i / 3) * slotSize, this::slotInfo);
        }

        for (int i = 0; i < 3; i++) {
            addSlot(104, 12 + i * slotSize, this::slotInfo);
        }

        addSlot(126, 12, this::slotInfo);

        for (int i = 0; i < 2; i++) {
            addSlot(126, 30 + i * slotSize, this::slotInfo);
        }

        for (int i = 0; i < 3; i++) {
            addSlot(148, 12 + i * slotSize, this::slotInfo);
        }

        addPlayerInventorySlots(8, 110);
    }

    private InventorySlot slotInfo(DynamicComponentSlot slot) {
        var template = AssemblerTemplates.select(getSlot(0).getItem());
        if (template != null) {
            int index = slot.getSlotIndex();
            var tplSlot = AssemblerTemplates.NoSlot;
            if (index >= 1 && index < 4) tplSlot = template.containerSlots()[index - 1];
            else if (index >= 4 && index < 13) tplSlot = template.upgradeSlots()[index - 4];
            else if (index >= 13 && index < 21) tplSlot = template.componentSlots()[index - 13];
            return new InventorySlot(tplSlot.kind(), tplSlot.tier());
        }
        return new InventorySlot(Slot.None, Tier.None);
    }

    public boolean isAssembling() {
        return synchronizedData.getBoolean("isAssembling");
    }

    public double assemblyProgress() {
        return synchronizedData.getDouble("assemblyProgress");
    }

    public int assemblyRemainingTime() {
        return synchronizedData.getInt("assemblyRemainingTime");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        synchronizedData.putBoolean("isAssembling", assembler.isAssembling());
        synchronizedData.putDouble("assemblyProgress", assembler.progress());
        synchronizedData.putInt("assemblyRemainingTime", assembler.timeRemaining());
        super.detectCustomDataChanges(nbt);
    }
}
