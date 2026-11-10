package li.cil.oc.neoforge.integration.tis3d;

import li.cil.oc.core.impl.integration.tis3d.SerialInterfaceProviderAdapter;
import li.cil.oc.core.integration.ModIDs;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.registries.RegisterEvent;

@SuppressWarnings("unused")
public final class ModTIS3D implements ModProxy {
  private static boolean registered;

  @Override
  public Mods.ModBase getMod() {
    return Mods.TIS3D;
  }

  @Override
  public void initialize() {
    if (registered) return;
    registered = true;
    OpenComputers.getModEventBus().addListener(RegisterEvent.class, event -> {
      if (event.getRegistryKey().equals(SerialInterfaceProvider.REGISTRY)) {
        event.register(SerialInterfaceProvider.REGISTRY, helper ->
          helper.register(ResourceLocation.fromNamespaceAndPath(ModIDs.TIS3D, "opencomputers_adapter"),
            new SerialInterfaceProviderAdapter()));
      }
    });
  }
}
