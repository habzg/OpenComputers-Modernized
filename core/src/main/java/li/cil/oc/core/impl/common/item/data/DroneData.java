package li.cil.oc.core.impl.common.item.data;

import com.google.common.base.Strings;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.item.data.NameProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DroneData extends MicrocontrollerData {
    public String name = "";

    public DroneData() {
        super(Constants.ItemName.Drone);
    }

    public DroneData(ItemStack stack) {
        this();
        load(stack, li.cil.oc.core.impl.util.SideTracker.getCurrentServer().registryAccess());
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("display") && nbt.getCompound("display").contains("Name")) {
            name = nbt.getCompound("display").getString("Name");
        }
        if (Strings.isNullOrEmpty(name)) {
            name = NameProvider.randomName();
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (!Strings.isNullOrEmpty(name)) {
            if (!nbt.contains("display")) {
                nbt.put("display", new CompoundTag());
            }
            nbt.getCompound("display").putString("Name", name);
        }
    }
}
