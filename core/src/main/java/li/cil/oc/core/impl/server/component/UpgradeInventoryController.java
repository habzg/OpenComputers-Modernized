package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.server.component.traits.InventoryAnalytics;
import li.cil.oc.core.impl.server.component.traits.InventoryWorldControlMk2;
import li.cil.oc.core.impl.server.component.traits.ItemInventoryControl;
import li.cil.oc.core.impl.server.component.traits.WorldInventoryAnalytics;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.server.component.UpgradeInventoryControllerBase;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;

public abstract class UpgradeInventoryController {

    public static class Adapter extends AbstractManagedEnvironment implements WorldInventoryAnalytics, UpgradeInventoryControllerBase.Common {
        public final EnvironmentHost host;

        @SuppressWarnings("unused")
        public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
                .withComponent("inventory_controller", Visibility.Network)
                .create();

        public Adapter(EnvironmentHost host) {
            this.host = host;
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }
    }

    public static class Drone extends AbstractManagedEnvironment implements InventoryAnalytics, InventoryWorldControlMk2, WorldInventoryAnalytics, ItemInventoryControl, UpgradeInventoryControllerBase.Common {
        public final li.cil.oc.api.internal.Agent host;

        @SuppressWarnings("unused")
        public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
                .withComponent("inventory_controller", Visibility.Neighbors)
                .create();

        public Drone(li.cil.oc.api.internal.Agent host) {
            this.host = host;
        }

        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        public Container inventory() {
            return host.mainInventory();
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
        public net.minecraft.world.entity.player.Player fakePlayer() {
            return host.player();
        }

        @Override
        public Direction checkSideForAction(Arguments args, int n) {
            return ExtendedArguments.checkSideAny(args, n);
        }
    }
}
