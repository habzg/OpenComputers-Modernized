package li.cil.oc.core.impl.common.item.data;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

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
    public void save(ItemStack stack, HolderLookup.Provider provider) {
        super.save(stack, provider);
        stack.set(DataComponents.RARITY, li.cil.oc.core.impl.util.Rarity.byTier(tier));
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        items.clear();
        for (int i = 0; i < 32; i++) items.add(null);
        var itemList = nbt.getList(OCSettings.namespace + "items", Tag.TAG_COMPOUND);
        for (int i = 0; i < itemList.size(); i++) {
            var slotNbt = itemList.getCompound(i);
            int slot = slotNbt.getByte("slot");
            if (slot >= 0 && slot < 32) {
                items.set(slot, ItemStack.parseOptional(provider, slotNbt.getCompound("item")));
            }
        }
        isRunning = nbt.getBoolean(OCSettings.namespace + "isRunning");
        energy = nbt.getDouble(OCSettings.namespace + "energy");
        maxEnergy = nbt.getDouble(OCSettings.namespace + "maxEnergy");
        tier = nbt.getInt(OCSettings.namespace + "tier");
        if (nbt.contains(OCSettings.namespace + "container")) {
            container = ItemStack.parseOptional(provider, nbt.getCompound(OCSettings.namespace + "container"));
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
        nbt.put(OCSettings.namespace + "items", list);
        nbt.putBoolean(OCSettings.namespace + "isRunning", isRunning);
        nbt.putDouble(OCSettings.namespace + "energy", energy);
        nbt.putDouble(OCSettings.namespace + "maxEnergy", maxEnergy);
        nbt.putInt(OCSettings.namespace + "tier", tier);
        if (container != null && !container.isEmpty()) {
            nbt.put(OCSettings.namespace + "container", container.save(provider, new CompoundTag()));
        }
    }
}
