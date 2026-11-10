package li.cil.oc.fabric.integration.top;

import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.TheOneProbePlugin;

@SuppressWarnings("unused")
public final class OCTheOneProbePlugin implements TheOneProbePlugin {
  @Override
  public void register(ITheOneProbe probe) {
    var provider = new OCProbeProvider();
    var entityProvider = new OCProbeEntityProvider();
    probe.registerProbeConfigProvider(new OCProbeConfigProvider());
    probe.registerProvider(provider);
    probe.registerBlockDisplayOverride(new OCProbeBlockOverride());
    probe.registerEntityProvider(entityProvider);
    probe.registerEntityDisplayOverride(entityProvider);
  }
}
