package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class HoverBootsData extends ItemData {
    public double charge = 0.0;

    public HoverBootsData() {
        super(Constants.ItemName.HoverBoots);
    }

    public HoverBootsData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        charge = nbt.getDouble(Settings.namespace + "charge");
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        nbt.putDouble(Settings.namespace + "charge", charge);
    }
}
