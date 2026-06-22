package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.GeolyzerHostHelper;
import li.cil.oc.neoforge.common.tileentity.RobotProxy;
import net.minecraft.core.Direction;

public class NeoGeolyzerHostHelper extends GeolyzerHostHelper {
    @Override
    public boolean isRobot(Object host) {
        return host instanceof RobotProxy;
    }

    @Override
    public BlockPosition robotPosition(Object host) {
        return ((RobotProxy) host).position();
    }

    @Override
    public Direction robotToGlobal(Object host, Direction side) {
        return ((RobotProxy) host).toGlobal(side);
    }
}
