package li.cil.oc.core.impl.common.item.data;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

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
        var diskList = nbt.getList(OCSettings.namespace + "disks", Tag.TAG_COMPOUND);
        disks.clear();
        for (int i = 0; i < diskList.size(); i++) {
            disks.add(ItemStack.parseOptional(provider, diskList.getCompound(i)));
        }
        filesystem = nbt.getCompound(OCSettings.namespace + "filesystem");
        if (nbt.contains(OCSettings.namespace + "label")) {
            label = nbt.getString(OCSettings.namespace + "label");
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
        nbt.put(OCSettings.namespace + "disks", list);
        nbt.put(OCSettings.namespace + "filesystem", filesystem);
        if (label != null) {
            nbt.putString(OCSettings.namespace + "label", label);
        }
    }
}
