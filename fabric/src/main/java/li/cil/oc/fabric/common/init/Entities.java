package li.cil.oc.fabric.common.init;

import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.fabric.OpenComputers;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class Entities {
    private Entities() {
    }

    public static final EntityType<Drone> DRONE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ResourceLocation.fromNamespaceAndPath(OpenComputers.ID, "drone"),
            EntityType.Builder.of(Drone::new, MobCategory.MISC)
                    .sized(12 / 16f, 6 / 16f)
                    .clientTrackingRange(80)
                    .updateInterval(1)
                    .build("drone")
    );

    public static void init() {
    }
}
