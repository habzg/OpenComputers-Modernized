package li.cil.oc.fabric.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.container.Player;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;

public class Relay extends Player {
    public final li.cil.oc.core.impl.common.blockentity.Relay relay;

    public Relay(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.blockentity.Relay relay, net.minecraft.world.entity.player.Player player) {
        super(Menus.RELAY, containerId, playerInventory, relay, player);
        this.relay = relay;
        addSlot(151, 15, Slot.CPU, Tier.Any);
        addSlot(151, 34, Slot.Memory, Tier.Any);
        addSlot(151, 53, Slot.HDD, Tier.Any);
        addSlot(178, 15, Slot.Card, Tier.Any);
        addPlayerInventorySlots(8, 84);
    }

    public int relayDelay() {
        return synchronizedData.getInt("relayDelay");
    }

    public int relayAmount() {
        return synchronizedData.getInt("relayAmount");
    }

    public int maxQueueSize() {
        return synchronizedData.getInt("maxQueueSize");
    }

    public int packetsPerCycleAvg() {
        return synchronizedData.getInt("packetsPerCycleAvg");
    }

    public int queueSize() {
        return synchronizedData.getInt("queueSize");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        synchronizedData.putInt("relayDelay", relay.relayDelay);
        synchronizedData.putInt("relayAmount", relay.relayAmount);
        synchronizedData.putInt("maxQueueSize", relay.maxQueueSize);
        synchronizedData.putInt("packetsPerCycleAvg", relay.packetsPerCycleAvg());
        synchronizedData.putInt("queueSize", relay.packetQueue.size());
        super.detectCustomDataChanges(nbt);
    }
}
