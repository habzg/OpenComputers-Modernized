package li.cil.oc.neoforge.integration.jade;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModJade implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.Jade;
    }

    @Override
    public void initialize() {
    }
}
