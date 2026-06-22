package li.cil.oc.neoforge.integration.mekanism;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModMekanism implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.Mekanism;
    }

    @Override
    public void initialize() {
        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.mekanism.EventHandlerMekanism.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.mekanism.EventHandlerMekanism.isWrench");
        li.cil.oc.api.API.driver.add(new li.cil.oc.neoforge.integration.mekanism.gas.ConverterChemicalStack());
    }
}
