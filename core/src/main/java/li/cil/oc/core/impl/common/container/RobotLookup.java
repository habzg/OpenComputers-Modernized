package li.cil.oc.core.impl.common.container;

import li.cil.oc.core.impl.client.ClientRobotTracker;
import li.cil.oc.core.impl.common.blockentity.Robot;
import li.cil.oc.core.impl.server.ServerRobotRegistry;
import net.minecraft.world.level.Level;

public final class RobotLookup {
    private RobotLookup() {
    }

    public static Robot get(Level level, String address) {
        if (level == null || address == null || address.isEmpty()) return null;
        if (level.isClientSide) {
            return ClientRobotTracker.INSTANCE.get(level, address);
        } else {
            var proxy = ServerRobotRegistry.INSTANCE.get(level, address);
            return proxy != null ? proxy.robot : null;
        }
    }
}
