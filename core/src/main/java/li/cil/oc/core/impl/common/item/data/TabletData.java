package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class TabletData extends ItemData {
    public List<ItemStack> items = new ArrayList<>();
    public boolean isRunning = false;
    public double energy = 0.0;
    public double maxEnergy = 0.0;
    public int tier = Tier.One;
    public ItemStack container;

    public TabletData() {
        super(Constants.ItemName.Tablet);
    }

    public TabletData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        items.clear();
        for (int i = 0; i < 32; i++) items.add(null);
        var itemList = nbt.getList(Settings.namespace + "items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            var slotNbt = itemList.getCompound(i);
            int slot = slotNbt.getByte("slot");
            if (slot >= 0 && slot < 32) {
                items.set(slot, ItemStack.parseOptional(provider, slotNbt.getCompound("item")));
            }
        }
        isRunning = nbt.getBoolean(Settings.namespace + "isRunning");
        energy = nbt.getDouble(Settings.namespace + "energy");
        maxEnergy = nbt.getDouble(Settings.namespace + "maxEnergy");
        tier = nbt.getInt(Settings.namespace + "tier");
        if (nbt.contains(Settings.namespace + "container")) {
            container = ItemStack.parseOptional(provider, nbt.getCompound(Settings.namespace + "container"));
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (int i = 0; i < items.size(); i++) {
            var opt = items.get(i);
            if (opt != null) {
                var slotNbt = new CompoundTag();
                slotNbt.putByte("slot", (byte) i);
                slotNbt.put("item", opt.save(provider, new CompoundTag()));
                list.add(slotNbt);
            }
        }
        nbt.put(Settings.namespace + "items", list);
        nbt.putBoolean(Settings.namespace + "isRunning", isRunning);
        nbt.putDouble(Settings.namespace + "energy", energy);
        nbt.putDouble(Settings.namespace + "maxEnergy", maxEnergy);
        nbt.putInt(Settings.namespace + "tier", tier);
        if (container != null && !container.isEmpty()) {
            nbt.put(Settings.namespace + "container", container.save(provider, new CompoundTag()));
        }
    }
}
