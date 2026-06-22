package li.cil.oc.neoforge.integration.enderstorage;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModEnderStorage implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.EnderStorage;
    }

    @Override
    public void initialize() {
        li.cil.oc.api.API.driver.add(new DriverFrequencyOwner());
    }
}
