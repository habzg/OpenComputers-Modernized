package li.cil.oc.fabric.common.capability;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.impl.OCSettings;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;
import team.reborn.energy.api.EnergyStorage;

public final class ChargeableEnergyStorage implements EnergyStorage {
    private final ContainerItemContext context;
    private final Chargeable item;

    public ChargeableEnergyStorage(ContainerItemContext context, Chargeable item) {
        this.context = context;
        this.item = item;
    }

    private static double ratio() {
        return OCSettings.get().ratioRedstoneFlux();
    }

    @Override
    public long getAmount() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return (long) (coreChargeable.getCharge(context.getItemVariant().toStack()) * ratio());
        }
        return 0;
    }

    @Override
    public long getCapacity() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return (long) (coreChargeable.maxCharge(context.getItemVariant().toStack()) * ratio());
        }
        return 0;
    }

    @Override
    public boolean supportsInsertion() {
        return item.canCharge(context.getItemVariant().toStack());
    }

    @Override
    public long insert(long maxAmount, TransactionContext transaction) {
        ItemStack current = context.getItemVariant().toStack();
        if (current.isEmpty()) return 0;
        double ocAmount = maxAmount / ratio();
        double unused = item.charge(current.copy(), ocAmount, true);
        double usedOc = ocAmount - unused;
        if (usedOc <= 0) return 0;
        ItemStack charged = current.copy();
        item.charge(charged, usedOc, false);
        ItemVariant oldVariant = context.getItemVariant();
        ItemVariant newVariant = ItemVariant.of(charged);
        if (oldVariant.equals(newVariant)) return 0;
        if (context.extract(oldVariant, 1, transaction) != 1) return 0;
        if (context.insert(newVariant, 1, transaction) != 1) return 0;
        return (long) (usedOc * ratio());
    }

    @Override
    public boolean supportsExtraction() {
        if (item instanceof li.cil.oc.core.impl.common.item.traits.Chargeable coreChargeable) {
            return coreChargeable.canExtract(context.getItemVariant().toStack());
        }
        return item.getClass().getName().contains("UpgradeBattery");
    }

    @Override
    public long extract(long maxAmount, TransactionContext transaction) {
        if (!supportsExtraction()) return 0;
        ItemStack current = context.getItemVariant().toStack();
        if (current.isEmpty()) return 0;
        double ocAmount = -maxAmount / ratio();
        double unused = item.charge(current.copy(), ocAmount, true);
        double extractedOc = Math.abs(ocAmount - unused);
        if (extractedOc <= 0) return 0;
        ItemStack discharged = current.copy();
        item.charge(discharged, -extractedOc, false);
        ItemVariant oldVariant = context.getItemVariant();
        ItemVariant newVariant = ItemVariant.of(discharged);
        if (oldVariant.equals(newVariant)) return 0;
        if (context.extract(oldVariant, 1, transaction) != 1) return 0;
        if (context.insert(newVariant, 1, transaction) != 1) return 0;
        return (long) (extractedOc * ratio());
    }
}
