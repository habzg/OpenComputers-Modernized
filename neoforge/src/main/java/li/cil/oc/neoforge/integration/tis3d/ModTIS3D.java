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
        // TIS-3D registers its serial interface provider registry in its own
        // mod constructor, which may run after ours, so the provider cannot be
        // registered there. Instead, register it when the RegisterEvent for the
        // registry fires during mod loading, at which point the registry exists
        // and is not yet frozen. This must be called from the mod constructor
        // (see OpenComputers), as common setup runs after the RegisterEvents.
        OpenComputers.getModEventBus().addListener(RegisterEvent.class, event -> {
            if (event.getRegistryKey().equals(SerialInterfaceProvider.REGISTRY)) {
                event.register(SerialInterfaceProvider.REGISTRY, helper ->
                        helper.register(ResourceLocation.fromNamespaceAndPath(ModIDs.TIS3D, "opencomputers_adapter"),
                                new SerialInterfaceProviderAdapter()));
            }
        });
    }
}