package li.cil.oc.neoforge.common.init;

import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Entities {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(Registries.ENTITY_TYPE, OpenComputers.ID);

    private Entities() {
    }

    public static final DeferredHolder<EntityType<?>, EntityType<Drone>> DRONE =
            ENTITY_TYPES.register("drone",
                    () -> EntityType.Builder.of(Drone::new, MobCategory.MISC)
                            .sized(12 / 16f, 6 / 16f)
                            .clientTrackingRange(80)
                            .updateInterval(1)
                            .build("drone"));
}
