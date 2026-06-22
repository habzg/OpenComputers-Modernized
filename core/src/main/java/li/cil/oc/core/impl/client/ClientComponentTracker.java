package li.cil.oc.core.impl.client;

import li.cil.oc.core.impl.common.ComponentTracker;
import net.minecraft.world.level.Level;

public final class ClientComponentTracker extends ComponentTracker {
    public static final ClientComponentTracker INSTANCE = new ClientComponentTracker();

    @Override
    public void clear(Level world) {
        if (world.isClientSide) super.clear(world);
    }
}
