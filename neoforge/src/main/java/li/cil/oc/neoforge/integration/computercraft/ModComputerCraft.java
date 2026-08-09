package li.cil.oc.neoforge.integration.computercraft;

import li.cil.oc.core.impl.integration.computercraft.ConverterLuaObject;
import li.cil.oc.core.impl.integration.computercraft.DriverPeripheral;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

import dan200.computercraft.api.peripheral.PeripheralCapability;

@SuppressWarnings("unused")
public final class ModComputerCraft implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.ComputerCraft;
    }

    @Override
    public void initialize() {
        DriverPeripheral.addPeripheralFinder((world, pos, side) -> {
            try {
                return world.getCapability(PeripheralCapability.get(), pos, side);
            } catch (Throwable t) {
                return null;
            }
        });

        li.cil.oc.api.API.driver.add(new DriverComputerCraftMedia());
        li.cil.oc.api.API.driver.add(new DriverPeripheral());
        li.cil.oc.api.API.driver.add(new ConverterLuaObject());
    }
}
