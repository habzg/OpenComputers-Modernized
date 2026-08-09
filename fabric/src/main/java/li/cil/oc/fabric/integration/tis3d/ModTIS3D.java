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
        // TIS-3D registers its serial interface provider registry in its own
        // mod initializer, which may run after ours, so the provider cannot be
        // registered there. Instead, register it when TIS-3D adds the registry
        // to the root registry, at which point it exists and is not frozen.
        RegistryEntryAddedCallback.event(BuiltInRegistries.REGISTRY).register((rawId, id, registry) -> {
            if (id.equals(SerialInterfaceProvider.REGISTRY.location())) {
                li.cil.oc.core.impl.integration.tis3d.ModTIS3D.initialize();
            }
        });
    }
}