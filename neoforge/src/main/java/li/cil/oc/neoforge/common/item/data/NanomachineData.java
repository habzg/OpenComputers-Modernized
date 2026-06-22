package li.cil.oc.neoforge.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.ItemData;
import li.cil.oc.neoforge.common.nanomachines.ControllerImpl;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class NanomachineData extends ItemData {
    public String uuid = "";
    public CompoundTag configuration;

    public NanomachineData() {
        super(Constants.ItemName.Nanomachines);
    }

    public NanomachineData(ItemStack stack) {
        this();
        load(stack);
    }

    public NanomachineData(ControllerImpl controller) {
        this();
        uuid = controller.uuid;
        var nbt = new CompoundTag();
        controller.configuration.save(nbt, true);
        configuration = nbt;
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        uuid = nbt.getString(Settings.namespace + "uuid");
        if (nbt.contains(Settings.namespace + "configuration")) {
            configuration = nbt.getCompound(Settings.namespace + "configuration");
        } else {
            configuration = null;
        }
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        nbt.putString(Settings.namespace + "uuid", uuid);
        if (configuration != null) {
            nbt.put(Settings.namespace + "configuration", configuration);
        }
    }
}
