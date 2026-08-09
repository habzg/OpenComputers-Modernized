package li.cil.oc.fabric.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.MultiTank;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.server.component.traits.TankInventoryControl;
import li.cil.oc.core.impl.server.component.traits.WorldTankAnalytics;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.server.component.UpgradeTankControllerBase;
import net.minecraft.core.Direction;

public class UpgradeTankController {
    public static class Robot extends AbstractManagedEnvironment implements TankInventoryControl, WorldTankAnalytics, UpgradeTankControllerBase.Common {
        public final li.cil.oc.fabric.common.blockentity.Robot host;

        @SuppressWarnings("unused")
        public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
                .withComponent("tank_controller", Visibility.Neighbors)
                .create();

        public Robot(li.cil.oc.fabric.common.blockentity.Robot host) {
            this.host = host;
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public net.minecraft.world.Container inventory() {
            return host.mainInventory;
        }

        @Override
        public int selectedSlot() {
            return host.selectedSlot();
        }

        @Override
        public void selectedSlot_$eq(int value) {
            host.setSelectedSlot(value);
        }

        @Override
        public MultiTank tank() {
            return host.tank;
        }

        @Override
        public int selectedTank() {
            return host.selectedTank();
        }

        @Override
        public void selectedTank_$eq(int value) {
            host.setSelectedTank(value);
        }

        @Override
        public net.minecraft.world.entity.player.Player fakePlayer() {
            return host.player();
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return host.toGlobal(ExtendedArguments.checkSideForAction(args, n));
        }
    }
}
