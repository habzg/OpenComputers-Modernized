package li.cil.oc.neoforge.integration.refinedstorage_mekanism;

import li.cil.oc.neoforge.integration.Mod;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModRefinedStorageMekanism implements ModProxy {
    @Override
    public Mod getMod() {
        return Mods.RefinedStorageMekanism;
    }

    @Override
    public void initialize() {
        li.cil.oc.api.API.driver.add(new DriverController());
        li.cil.oc.api.API.driver.add(new DriverInterface());
    }
}