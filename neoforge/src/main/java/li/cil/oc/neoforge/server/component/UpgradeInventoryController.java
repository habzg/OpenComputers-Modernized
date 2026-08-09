package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.server.component.traits.InventoryAnalytics;
import li.cil.oc.core.impl.server.component.traits.InventoryWorldControlMk2;
import li.cil.oc.core.impl.server.component.traits.ItemInventoryControl;
import li.cil.oc.core.impl.server.component.traits.WorldInventoryAnalytics;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.server.component.UpgradeInventoryControllerBase;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;


public class UpgradeInventoryController {
    public static class Robot extends AbstractManagedEnvironment implements InventoryAnalytics, InventoryWorldControlMk2, WorldInventoryAnalytics, ItemInventoryControl, UpgradeInventoryControllerBase.Common {
        public final li.cil.oc.neoforge.common.blockentity.Robot host;

        @SuppressWarnings("unused")
        public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
                .withComponent("inventory_controller", Visibility.Neighbors)
                .create();

        public Robot(li.cil.oc.neoforge.common.blockentity.Robot host) {
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
            return host.toGlobal(ExtendedArguments.checkSideForAction(args, n));
        }

        @Callback(doc = "function():boolean -- Swaps the equipped tool with the content of the currently selected inventory slot.")
        public Object[] equip(Context context, Arguments args) {
            if (inventory().getContainerSize() > 0) {
                net.minecraft.world.item.ItemStack equipped = host.getItem(0);
                net.minecraft.world.item.ItemStack selected = inventory().getItem(selectedSlot());
                host.setItem(0, selected);
                inventory().setItem(selectedSlot(), equipped);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(false);
        }

        @Callback(doc = "function([slot:number]):boolean -- Swaps the installed upgrade in the slot (1 by default) with the content of the currently selected inventory slot.")
        public Object[] installUpgrade(Context context, Arguments args) {
            if (inventory().getContainerSize() > 0) {
                int slot = args.optInteger(0, 1);
                if (!host.isContainerSlot(slot))
                    return ResultWrapper.result(false, "not a container slot");
                net.minecraft.world.item.ItemStack selected = inventory().getItem(selectedSlot());
                if (!selected.isEmpty() && !host.canPlaceItem(slot, selected))
                    return ResultWrapper.result(false, "Invalid upgrade");
                net.minecraft.world.item.ItemStack equipped = host.getItem(slot);
                host.setItem(slot, selected);
                inventory().setItem(selectedSlot(), equipped);
                return ResultWrapper.result(true);
            }
            return ResultWrapper.result(false);
        }

        @Callback(doc = "function(slot:number):string -- get upgrade container type at the given slot.")
        public Object[] getUpgradeContainerType(Context context, Arguments args) {
            int slot = args.checkInteger(0);
            if (!host.isContainerSlot(slot))
                return ResultWrapper.result("None");
            return ResultWrapper.result(host.containerSlotType(slot));
        }

        @Callback(doc = "function(slot:number):number -- get upgrade container tier at the given slot.")
        public Object[] getUpgradeContainerTier(Context context, Arguments args) {
            int slot = args.checkInteger(0);
            if (!host.isContainerSlot(slot))
                return ResultWrapper.result(0);
            return ResultWrapper.result(host.containerSlotTier(slot));
        }
    }
}
