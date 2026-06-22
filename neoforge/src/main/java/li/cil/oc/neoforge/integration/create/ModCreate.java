package li.cil.oc.neoforge.integration.create;

import li.cil.oc.core.impl.integration.util.WirelessRedstone;
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
        WirelessRedstone.register(new WirelessRedstoneCreate());
    }
}
