package li.cil.oc.core.impl.integration.tis3d;

import dev.architectury.registry.registries.RegistrarManager;
import li.cil.oc.core.integration.ModIDs;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import net.minecraft.resources.ResourceLocation;

public final class ModTIS3D {
    private static boolean registered;

    private ModTIS3D() {
    }

    public static void initialize() {
        if (registered) return;
        registered = true;
        RegistrarManager.get(ModIDs.TIS3D)
                .get(SerialInterfaceProvider.REGISTRY)
                .register(ResourceLocation.fromNamespaceAndPath(ModIDs.TIS3D, "opencomputers_adapter"), SerialInterfaceProviderAdapter::new);
    }
}
