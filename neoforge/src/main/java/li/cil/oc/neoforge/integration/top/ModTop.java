package li.cil.oc.neoforge.integration.top;

import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import mcjty.theoneprobe.api.ITheOneProbe;
import net.neoforged.fml.InterModComms;

import java.util.function.Function;

@SuppressWarnings("unused")
public final class ModTop implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.TheOneProbe;
    }

    @Override
    public void initialize() {
        InterModComms.sendTo("theoneprobe", "getTheOneProbe",
                () -> (Function<ITheOneProbe, Void>) probe -> {
                    var provider = new OCProbeProvider();
                    var entityProvider = new OCProbeEntityProvider();
                    probe.registerProbeConfigProvider(new OCProbeConfigProvider());
                    probe.registerProvider(provider);
                    probe.registerEntityProvider(entityProvider);
                    probe.registerEntityDisplayOverride(entityProvider);
                    return null;
                });
    }
}
