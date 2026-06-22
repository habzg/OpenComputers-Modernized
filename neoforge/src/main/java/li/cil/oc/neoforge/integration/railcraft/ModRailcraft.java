package li.cil.oc.neoforge.integration.railcraft;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModRailcraft implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.Railcraft;
    }

    @Override
    public void initialize() {
        li.cil.oc.api.API.driver.add(new DriverBoilerFirebox());
        li.cil.oc.api.API.driver.add(new DriverSteamTurbine());
        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.railcraft.EventHandlerRailcraft.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.railcraft.EventHandlerRailcraft.isWrench");
    }
}

