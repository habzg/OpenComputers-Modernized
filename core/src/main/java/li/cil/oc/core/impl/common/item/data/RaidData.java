package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RaidData extends ItemData {
    public final List<ItemStack> disks = new ArrayList<>();
    public CompoundTag filesystem = new CompoundTag();
    public String label;

    public RaidData() {
        super(Constants.BlockName.Raid);
    }

    public RaidData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        var diskList = nbt.getList(Settings.namespace + "disks", Tag.TAG_COMPOUND);
        disks.clear();
        for (int i = 0; i < diskList.size(); i++) {
            disks.add(ItemStack.parseOptional(provider, diskList.getCompound(i)));
        }
        filesystem = nbt.getCompound(Settings.namespace + "filesystem");
        if (nbt.contains(Settings.namespace + "label")) {
            label = nbt.getString(Settings.namespace + "label");
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        ListTag list = new ListTag();
        for (var stack : disks) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(Settings.namespace + "disks", list);
        nbt.put(Settings.namespace + "filesystem", filesystem);
        if (label != null) {
            nbt.putString(Settings.namespace + "label", label);
        }
    }
}
