package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MicrocontrollerData extends ItemData {
    public int tier = Tier.One;
    public List<ItemStack> components = new ArrayList<>();
    public int storedEnergy = 0;

    public MicrocontrollerData() {
        super(Constants.BlockName.Microcontroller);
    }

    public MicrocontrollerData(String itemName) {
        super(itemName);
    }

    public MicrocontrollerData(ItemStack stack) {
        this();
        load(stack, SideTracker.getCurrentServer().registryAccess());
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        tier = nbt.getByte(Settings.namespace + "tier");
        var componentList = nbt.getList(Settings.namespace + "components", Tag.TAG_COMPOUND);
        components.clear();
        for (int i = 0; i < componentList.size(); i++) {
            var stack = ItemStack.parseOptional(provider, componentList.getCompound(i));
            if (!stack.isEmpty()) {
                components.add(stack);
            }
        }
        storedEnergy = nbt.getInt(Settings.namespace + "storedEnergy");
        boolean hasEeprom = false;
        for (var stack : components) {
            if (stack != null && li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.EEPROM)) {
                hasEeprom = true;
                break;
            }
        }
        if (!hasEeprom) {
            components.add(null);
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        nbt.putByte(Settings.namespace + "tier", (byte) tier);
        ListTag list = new ListTag();
        for (var stack : components) {
            if (stack != null && !stack.isEmpty()) {
                list.add(stack.save(provider, new CompoundTag()));
            }
        }
        nbt.put(Settings.namespace + "components", list);
        nbt.putInt(Settings.namespace + "storedEnergy", storedEnergy);
    }
}
