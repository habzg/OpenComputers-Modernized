package li.cil.oc.neoforge.integration.rftools;

import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModRFTools implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.RFTools;
    }

    @Override
    public void initialize() {
        WirelessRedstone.register(new WirelessRedstoneRFTools());

        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.rftools.EventHandlerRFTools.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.rftools.EventHandlerRFTools.isWrench");
    }
}
