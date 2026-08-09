package li.cil.oc.fabric.util;

import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.core.impl.common.inventory.ComponentInventory;
import li.cil.oc.core.util.FluidTank;

public class FluidTankHelper extends li.cil.oc.core.util.FluidTankHelper {
    @Override
    public Object createMultiTank(Object inv) {
        ComponentInventory inventory = (ComponentInventory) inv;
        return new MultiTank() {
            @Override
            public int tankCount() {
                int count = 0;
                for (var comp : inventory.componentEnvironments()) {
                    if (comp instanceof FluidTank) count++;
                }
                return count;
            }

            @Override
            public Object getFluidTank(int index) {
                int count = 0;
                for (var comp : inventory.componentEnvironments()) {
                    if (comp instanceof FluidTank tank) {
                        if (count == index) return tank;
                        count++;
                    }
                }
                return null;
            }
        };
    }
}
