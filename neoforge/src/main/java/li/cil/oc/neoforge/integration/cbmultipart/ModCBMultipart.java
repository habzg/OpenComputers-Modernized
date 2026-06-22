package li.cil.oc.neoforge.integration.cbmultipart;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;

@SuppressWarnings("unused")
public final class ModCBMultipart implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.CBMultipart;
    }

    @Override
    public void initialize() {
        MultipartNetworkBridge.install();
    }
}
