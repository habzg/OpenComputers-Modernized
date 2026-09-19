package li.cil.oc.fabric.integration.tis3d;

import li.cil.oc.fabric.integration.ModProxy;
import li.cil.oc.fabric.integration.Mods;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.core.registries.BuiltInRegistries;

@SuppressWarnings("unused")
public final class ModTIS3D implements ModProxy {
  @Override
  public Mods.ModBase getMod() {
    return Mods.TIS3D;
  }

  @Override
  public void initialize() {
    // If the registry already exists (TIS-3D loaded before us), register now.
    if (BuiltInRegistries.REGISTRY.containsKey(SerialInterfaceProvider.REGISTRY.location())) {
      li.cil.oc.core.impl.integration.tis3d.ModTIS3D.initialize();
    }
    // Also listen for late registration (TIS-3D loads after us).
    RegistryEntryAddedCallback.event(BuiltInRegistries.REGISTRY).register((rawId, id, registry) -> {
      if (id.equals(SerialInterfaceProvider.REGISTRY.location())) {
        li.cil.oc.core.impl.integration.tis3d.ModTIS3D.initialize();
      }
    });
  }
}
