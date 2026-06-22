package li.cil.oc.neoforge.integration.computercraft;

import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.server.fs.FileSystem;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModComputerCraft implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.ComputerCraft;
    }

    @Override
    public void initialize() {
        BundledRedstone.addProvider(BundledRedstoneProvider.INSTANCE);

        FileSystem.setComputerCraftMountConverter(mount -> {
            if (mount instanceof dan200.computercraft.api.filesystem.WritableMount writable) {
                return new ComputerCraftWritableFileSystem(writable);
            }
            if (mount instanceof dan200.computercraft.api.filesystem.Mount readOnly) {
                return new ComputerCraftFileSystem(readOnly);
            }
            return null;
        });

        li.cil.oc.api.API.driver.add(new DriverComputerCraftMedia());
        li.cil.oc.api.API.driver.add(new DriverPeripheral());
        li.cil.oc.api.API.driver.add(new ConverterLuaObject());
    }
}
