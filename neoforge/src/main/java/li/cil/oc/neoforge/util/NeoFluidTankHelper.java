package li.cil.oc.neoforge.util;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.util.FluidTankHelper;
import net.neoforged.neoforge.fluids.IFluidTank;

public class NeoFluidTankHelper extends FluidTankHelper {
    @Override
    public Object createMultiTank(Object inv) {
        ComponentInventory inventory = (ComponentInventory) inv;
        return new MultiTank() {
            @Override
            public int tankCount() {
                int count = 0;
                for (var comp : inventory.componentEnvironments()) {
                    if (comp instanceof IFluidTank) count++;
                }
                return count;
            }

            @Override
            public IFluidTank getFluidTank(int index) {
                int count = 0;
                for (var comp : inventory.componentEnvironments()) {
                    if (comp instanceof IFluidTank tank) {
                        if (count == index) return tank;
                        count++;
                    }
                }
                return null;
            }
        };
    }

}
