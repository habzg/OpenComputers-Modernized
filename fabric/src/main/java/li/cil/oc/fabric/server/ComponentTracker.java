package li.cil.oc.fabric.server;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.Level;

public final class ComponentTracker extends li.cil.oc.core.impl.common.ComponentTracker {
    public static final ComponentTracker INSTANCE = new ComponentTracker();

    private ComponentTracker() {
    }

    public static void init() {
        ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (!world.isClientSide()) {
                INSTANCE.clear(world);
            }
        });
    }

    @Override
    public void clear(Level world) {
        if (!world.isClientSide) super.clear(world);
    }
}
