package li.cil.oc.core.impl.server.component;

import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;

public record WaypointInfo(BlockPosition position, Direction facing, int maxInput, String label, String address) {
}
