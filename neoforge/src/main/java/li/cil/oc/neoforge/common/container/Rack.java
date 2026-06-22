package li.cil.oc.neoforge.common.container;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.core.common.Slot;
import li.cil.oc.neoforge.common.init.Menus;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;

public class Rack extends li.cil.oc.neoforge.common.container.Player {
    public final li.cil.oc.core.impl.common.tileentity.Rack rack;

    public static final int MaxConnections = 4;

    public final boolean[][] nodePresence = new boolean[4][4];

    public Rack(int containerId, Inventory playerInventory, li.cil.oc.core.impl.common.tileentity.Rack rack, net.minecraft.world.entity.player.Player player) {
        super(Menus.RACK.get(), containerId, playerInventory, rack, player);
        this.rack = rack;

        addSlot(20, 23, Slot.RackMountable, li.cil.oc.core.common.Tier.Any);
        addSlot(20, 43, Slot.RackMountable, li.cil.oc.core.common.Tier.Any);
        addSlot(20, 63, Slot.RackMountable, li.cil.oc.core.common.Tier.Any);
        addSlot(20, 83, Slot.RackMountable, li.cil.oc.core.common.Tier.Any);
        addPlayerInventorySlots(8, 128);
    }

    @Override
    public void updateCustomData(CompoundTag nbt) {
        super.updateCustomData(nbt);
        if (nbt.contains("nodeMapping")) {
            var mappingList = nbt.getList("nodeMapping", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < mappingList.size() && i < rack.nodeMapping.length; i++) {
                var sides = mappingList.getIntArray(i);
                for (int j = 0; j < sides.length && j < rack.nodeMapping[i].length; j++) {
                    int id = sides[j];
                    rack.nodeMapping[i][j] = id >= 0 ? Direction.from3DDataValue(id) : null;
                }
            }
        }
        if (nbt.contains("nodePresence")) {
            byte[] presence = nbt.getByteArray("nodePresence");
            for (int i = 0; i < nodePresence.length && i * MaxConnections < presence.length; i++) {
                for (int j = 0; j < MaxConnections; j++) {
                    int idx = i * MaxConnections + j;
                    nodePresence[i][j] = idx < presence.length && presence[idx] != 0;
                }
            }
        }
        rack.isRelayEnabled = nbt.getBoolean("isRelayEnabled");
    }

    @Override
    protected void detectCustomDataChanges(CompoundTag nbt) {
        super.detectCustomDataChanges(nbt);

        var mappingList = new ListTag();
        for (var buses : rack.nodeMapping) {
            var arr = new int[buses.length];
            for (int j = 0; j < buses.length; j++) {
                arr[j] = buses[j] != null ? buses[j].ordinal() : -1;
            }
            mappingList.add(new IntArrayTag(arr));
        }
        nbt.put("nodeMapping", mappingList);

        var presenceBytes = new byte[rack.getContainerSize() * MaxConnections];
        for (int slot = 0; slot < rack.getContainerSize(); slot++) {
            var mountable = rack.getMountable(slot);
            if (mountable instanceof RackMountable rm) {
                presenceBytes[slot * MaxConnections] = 1;
                int count = Math.min(MaxConnections - 1, rm.getConnectableCount());
                for (int ci = 0; ci < count; ci++) {
                    presenceBytes[slot * MaxConnections + 1 + ci] = rm.getConnectableAt(ci) != null ? (byte) 1 : 0;
                }
            } else {
                for (int ci = 0; ci < MaxConnections; ci++) {
                    presenceBytes[slot * MaxConnections + ci] = 0;
                }
            }
        }
        nbt.putByteArray("nodePresence", presenceBytes);

        nbt.putBoolean("isRelayEnabled", rack.isRelayEnabled);
    }
}
