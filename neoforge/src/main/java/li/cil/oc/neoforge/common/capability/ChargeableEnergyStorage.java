package li.cil.oc.neoforge.common.capability;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.impl.Settings;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ChargeableEnergyStorage implements IEnergyStorage {
    private final ItemStack stack;
    private final Chargeable item;

    public ChargeableEnergyStorage(ItemStack stack, Chargeable item) {
        this.stack = stack;
        this.item = item;
    }

    private static double ratio() {
        return Settings.get().ratioRedstoneFlux();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        double ocAmount = maxReceive / ratio();
        double unused = item.charge(stack, ocAmount, simulate);
        double usedOc = ocAmount - unused;
        return (int) (usedOc * ratio());
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract()) return 0;
        double ocAmount = -maxExtract / ratio();
        double unused = item.charge(stack, ocAmount, simulate);
        double extractedOc = Math.abs(ocAmount - unused);
        return (int) (extractedOc * ratio());
    }

    @Override
    public int getEnergyStored() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return (int) (coreChargeable.getCharge(stack) * ratio());
        }
        return 0;
    }

    @Override
    public int getMaxEnergyStored() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return (int) (coreChargeable.maxCharge(stack) * ratio());
        }
        return 0;
    }

    @Override
    public boolean canExtract() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return coreChargeable.canExtract(stack);
        }
        return stack.getItem().getClass().getName().contains("UpgradeBattery");
    }

    @Override
    public boolean canReceive() {
        return item.canCharge(stack);
    }
}
