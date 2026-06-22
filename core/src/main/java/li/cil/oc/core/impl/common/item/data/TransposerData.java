package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class TransposerData extends ItemData {
    public static final String FLUID_TRANSFER_RATE = Settings.namespace + "fluidTransferRate";
    public int fluidTransferRate = Settings.get().transposerFluidTransferRate;

    public TransposerData() {
        super(Constants.BlockName.Transposer);
    }

    @SuppressWarnings("unused")
    public TransposerData(String itemName) {
        super(itemName);
    }

    @SuppressWarnings("unused")
    public TransposerData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        if (nbt.contains(FLUID_TRANSFER_RATE)) {
            fluidTransferRate = nbt.getInt(FLUID_TRANSFER_RATE);
        }
    }

    @Override
    public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
        nbt.putInt(FLUID_TRANSFER_RATE, fluidTransferRate);
    }
}
