package li.cil.oc.neoforge.integration.create;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModCreate implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.Create;
    }

    @Override
    public void initialize() {
        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.create.EventHandlerCreate.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.create.EventHandlerCreate.isWrench");
    }
}
