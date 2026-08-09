package li.cil.oc.fabric.util;

import li.cil.oc.core.impl.common.blockentity.Charger;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.fabric.common.blockentity.RobotProxy;
import net.minecraft.world.phys.Vec3;

public class RobotChargeableFactory extends li.cil.oc.core.util.RobotChargeableFactory {
    @Override
    public Object tryCreate(Object te) {
        if (te instanceof RobotProxy robot) {
            var connector = robot.node();
            var pos = BlockPosition.apply(robot.getBlockPos().getX(), robot.getBlockPos().getY(), robot.getBlockPos().getZ(), robot.getLevel()).toVec3();
            return new RobotChargeableImpl((li.cil.oc.api.network.Connector) connector, pos, robot);
        }
        return null;
    }

    private record RobotChargeableImpl(li.cil.oc.api.network.Connector connector, Vec3 pos,
                                        Object robot) implements Charger.Chargeable {
        @Override
        public Vec3 pos() {
            return pos;
        }

        @Override
        public double changeBuffer(double delta) {
            return connector.changeBuffer(delta);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof RobotChargeableImpl rci) return rci.robot == robot;
            return false;
        }

        @Override
        public int hashCode() {
            return robot.hashCode();
        }
    }
}
