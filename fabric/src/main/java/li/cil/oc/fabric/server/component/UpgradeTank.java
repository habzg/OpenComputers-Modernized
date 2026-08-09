package li.cil.oc.fabric.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeTankBase;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.FluidTank;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class UpgradeTank extends UpgradeTankBase implements FluidTank {
    private FluidStack fluid = FluidStack.EMPTY;

    @SuppressWarnings("unused")
    public UpgradeTank(EnvironmentHost owner, int capacity) {
        super(owner, capacity);
    }

    @SuppressWarnings("unused")
    @Override
    protected void loadTankNbt(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        String fluidName = nbt.getString("id");
        int amount = nbt.getInt("Amount");
        fluid = !fluidName.isEmpty() ? new FluidStack(fluidName, amount) : FluidStack.EMPTY;
    }

    @SuppressWarnings("unused")
    @Override
    protected void saveTankNbt(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider) {
        if (!fluid.isEmpty()) {
            nbt.putString("id", fluid.fluidName());
            nbt.putInt("Amount", fluid.amount());
        }
    }

    @SuppressWarnings("unused")
    public @NotNull FluidStack getFluid() {
        return fluid;
    }

    @SuppressWarnings("unused")
    public int getFluidAmount() {
        return fluid.amount();
    }

    @SuppressWarnings("unused")
    public int getCapacity() {
        return capacity;
    }

    @SuppressWarnings("unused")
    @Override
    public int getSpace() {
        return capacity - fluid.amount();
    }

    @SuppressWarnings("SameReturnValue")
    public boolean isFluidValid(@NotNull FluidStack ignoredStack) {
        return true;
    }

    @SuppressWarnings("unused")
    @Override
    public int fill(FluidStack resource, boolean simulate) {
        if (resource.isEmpty()) {
            return 0;
        }
        if (!fluid.isEmpty() && !fluid.fluidName().equals(resource.fluidName())) {
            return 0;
        }
        int amount = Math.min(resource.amount(), capacity - fluid.amount());
        if (amount > 0 && !simulate) {
            if (fluid.isEmpty()) {
                fluid = resource.copyWithAmount(amount);
            } else {
                fluid = fluid.copyWithAmount(fluid.amount() + amount);
            }
            node.sendToVisible("computer.signal", "tank_changed", tankIndex(), amount);
        }
        return amount;
    }

    @SuppressWarnings("unused")
    @Override
    public @NotNull FluidStack drain(int maxDrain, boolean simulate) {
        int drained = Math.min(maxDrain, fluid.amount());
        FluidStack result = fluid.copyWithAmount(drained);
        if (drained > 0 && !simulate) {
            int newAmount = fluid.amount() - drained;
            fluid = newAmount > 0 ? fluid.copyWithAmount(newAmount) : FluidStack.EMPTY;
            node.sendToVisible("computer.signal", "tank_changed", tankIndex(), -drained);
        }
        return result;
    }
}
