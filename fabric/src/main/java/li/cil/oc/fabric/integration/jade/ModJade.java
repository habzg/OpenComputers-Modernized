package li.cil.oc.fabric.integration.jade;

import li.cil.oc.fabric.integration.ModProxy;
import li.cil.oc.fabric.integration.Mods;

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
