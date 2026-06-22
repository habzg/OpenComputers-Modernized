package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class UpgradeTractorBeam {
    static boolean hasPickupDelay(ItemEntity entity) {
        return entity.hasPickUpDelay();
    }

    public static abstract class Common extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
        private static final int pickupRadius = 3;
        @SuppressWarnings("unused")
        public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
                .withComponent("tractor_beam")
                .create();
        private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
            put(DeviceAttribute.Class, DeviceClass.Generic);
            put(DeviceAttribute.Description, "Tractor beam");
            put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
            put(DeviceAttribute.Product, "T313-K1N.3515");
        }};

        @Override
        public Map<String, String> getDeviceInfo() {
            return deviceInfo;
        }

        protected abstract BlockPosition position();

        protected abstract void collectItem(ItemEntity item);

        private Level level() {
            return position().level();
        }

        @Callback(doc = "function():boolean -- Tries to pick up a random item in the robots' vicinity.")
        public Object[] suck(Context context, Arguments args) {
            List<ItemEntity> items = level().getEntitiesOfClass(ItemEntity.class, position().bounds().inflate(pickupRadius, pickupRadius, pickupRadius));
            items.removeIf(item -> !item.isAlive() || hasPickupDelay(item));
            if (!items.isEmpty()) {
                ItemEntity item = items.get(level().random.nextInt(items.size()));
                ItemStack stack = item.getItem();
                int size = stack.getCount();
                collectItem(item);
                if (stack.getCount() < size || !item.isAlive()) {
                    context.pause(Settings.get().suckDelay);
                    level().levelEvent(2003, net.minecraft.core.BlockPos.containing(item.getX(), item.getY(), item.getZ()), 0);
                    return ResultWrapper.result(true);
                }
            }
            return ResultWrapper.result(false);
        }
    }

    public static class Player extends Common {
        public final EnvironmentHost owner;
        private final java.util.function.Supplier<net.minecraft.world.entity.player.Player> player;

        public Player(EnvironmentHost owner, java.util.function.Supplier<net.minecraft.world.entity.player.Player> player) {
            this.owner = owner;
            this.player = player;
        }

        @Override
        protected BlockPosition position() {
            return BlockPosition.apply(owner);
        }

        @Override
        protected void collectItem(ItemEntity item) {
            item.playerTouch(player.get());
        }
    }

    public static class Drone extends Common {
        public final li.cil.oc.api.internal.Agent owner;

        public Drone(li.cil.oc.api.internal.Agent owner) {
            this.owner = owner;
        }

        @Override
        protected BlockPosition position() {
            return BlockPosition.apply(owner);
        }

        @Override
        protected void collectItem(ItemEntity item) {
            InventoryUtils.insertIntoInventory(item.getItem(), owner.mainInventory(), null, 64, false, Arrays.stream(insertionSlots()).boxed().collect(Collectors.toList()));
        }

        private int[] insertionSlots() {
            int size = owner.mainInventory().getContainerSize();
            int sel = owner.selectedSlot();
            int[] result = new int[size];
            int idx = 0;
            for (int i = sel; i < size; i++) result[idx++] = i;
            for (int i = 0; i < sel; i++) result[idx++] = i;
            return result;
        }
    }
}
