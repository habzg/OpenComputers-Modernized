package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

public class UpgradeGenerator extends AbstractManagedEnvironment implements DeviceInfo {
    public interface IFuelProvider {
        boolean isFuel(ItemStack stack);
        int getBurnTime(ItemStack stack);
    }

    private static IFuelProvider fuelProvider = new IFuelProvider() {
        @Override
        public boolean isFuel(ItemStack stack) {
            return FurnaceBlockEntity.isFuel(stack);
        }

        @Override
        public int getBurnTime(ItemStack stack) {
            return net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.getFuel().getOrDefault(stack.getItem(), 0);
        }
    };

    public static void setFuelProvider(IFuelProvider provider) {
        fuelProvider = provider;
    }
    public final li.cil.oc.api.internal.Agent host;

    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("generator", Visibility.Neighbors)
            .withConnector()
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Power);
        put(DeviceAttribute.Description, "Generator");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "Portagen 2.0 (Rev. 3)");
        put(DeviceAttribute.Capacity, "1");
    }};
    private ItemStack inventory = null;

    private int remainingTicks = 0;

    public UpgradeGenerator(li.cil.oc.api.internal.Agent host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to insert fuel from the selected slot into the generator's queue.")
    public Object[] insert(Context context, Arguments args) {
        int count = args.optInteger(0, 64);
        ItemStack stack = host.mainInventory().getItem(host.selectedSlot());
        if (stack.isEmpty()) return ResultWrapper.result(null, "selected slot is empty");
        if (!fuelProvider.isFuel(stack)) {
            return ResultWrapper.result(null, "selected slot does not contain fuel");
        }
        Item containerItem = stack.getItem().getCraftingRemainingItem();
        ItemStack container = containerItem != null ? containerItem.getDefaultInstance() : ItemStack.EMPTY;
        ItemStack inQueue = inventory;
        if (inQueue != null && !inQueue.isEmpty()) {
            if (!ItemStack.isSameItem(inQueue, stack) || !ItemStack.isSameItemSameComponents(inQueue, stack)) {
                return ResultWrapper.result(null, "different fuel type already queued");
            }
        } else {
            inQueue = null;
        }
        int space = inQueue != null ? inQueue.getMaxStackSize() - inQueue.getCount() : stack.getMaxStackSize();
        if (space == 0) {
            return ResultWrapper.result(null, "queue is full");
        }
        ItemStack previousSelectedFuel = stack.copy();
        int insertLimit = Math.min(stack.getCount(), Math.min(space, count));
        ItemStack fuelToInsert = stack.split(insertLimit);

        if (stack.isEmpty()) {
            host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
        } else {
            host.mainInventory().setItem(host.selectedSlot(), stack);
        }

        if (!container.isEmpty()) {
            container.grow(fuelToInsert.getCount() - 1);
            if (!host.player().getInventory().add(container)) {
                host.mainInventory().setItem(host.selectedSlot(), previousSelectedFuel);
                return ResultWrapper.result(false, "no space in inventory for fuel containers");
            } else if (container.getCount() > 0) {
                var player = host.player();
                var itemEntity = new ItemEntity(host.level(), player.getX(), player.getY() - 0.75, player.getZ(), container.copy());
                host.level().addFreshEntity(itemEntity);
            }
        }

        if (inQueue != null) {
            fuelToInsert.grow(inQueue.getCount());
        }

        inventory = fuelToInsert;

        return ResultWrapper.result(true, insertLimit);
    }

    @Callback(doc = "function():number -- Get the size of the item stack in the generator's queue.")
    public Object[] count(Context context, Arguments args) {
        if (inventory != null && inventory.getCount() > 0) {
            return ResultWrapper.result(inventory.getCount(), inventory.getHoverName().getString());
        }
        return ResultWrapper.result(0);
    }

    @Callback(doc = "function([count:number]):boolean -- Tries to remove items from the generator's queue.")
    public Object[] remove(Context context, Arguments args) {
        int count = args.optInteger(0, Integer.MAX_VALUE);
        if (count <= 0) {
            return ResultWrapper.result(true);
        }
        ItemStack inQueue = inventory;
        if (inQueue == null || inQueue.isEmpty()) {
            return ResultWrapper.result(false, "queue is empty");
        }
        ItemStack previousSelectedItem = host.mainInventory().getItem(host.selectedSlot());
        previousSelectedItem = previousSelectedItem.copy();

        Item requiredContainerItem = inQueue.getItem().getCraftingRemainingItem();
        ItemStack requiredContainer = requiredContainerItem != null ? requiredContainerItem.getDefaultInstance() : ItemStack.EMPTY;
        ItemStack selectedEmptyContainer = null;
        if (!requiredContainer.isEmpty() && requiredContainer.getCount() > 0) {
            if (previousSelectedItem.getCount() > 0 && previousSelectedItem.getItem() == requiredContainer.getItem() && ItemStack.isSameItemSameComponents(previousSelectedItem, requiredContainer)) {
                selectedEmptyContainer = previousSelectedItem.copy();
            } else {
                return ResultWrapper.result(false, "removing this fuel requires the appropriate container in the selected slot");
            }
        }

        int removeLimit = Math.min(inQueue.getCount(), selectedEmptyContainer != null ? selectedEmptyContainer.getCount() : count);

        ItemStack previousQueue = inQueue.copy();
        ItemStack forUser = inQueue.split(removeLimit);

        if (selectedEmptyContainer != null) {
            selectedEmptyContainer.shrink(removeLimit);
            if (selectedEmptyContainer.isEmpty()) {
                host.mainInventory().setItem(host.selectedSlot(), ItemStack.EMPTY);
            } else {
                host.mainInventory().removeItem(host.selectedSlot(), removeLimit);
            }
        }

        if (!host.player().getInventory().add(forUser)) {
            host.mainInventory().setItem(host.selectedSlot(), previousSelectedItem);
            inventory = previousQueue;
            return ResultWrapper.result(false, "no inventory space available for fuel");
        } else {
            previousQueue.setCount(inQueue.getCount() + forUser.getCount());
            inventory = previousQueue.isEmpty() ? null : previousQueue;
            return ResultWrapper.result(true, removeLimit - forUser.getCount());
        }
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        super.update();
        if (remainingTicks <= 0 && inventory != null) {
            ItemStack stack = inventory;
            remainingTicks = fuelProvider.getBurnTime(stack);
            if (remainingTicks > 0) {
                updateClient();
                stack.shrink(1);
                if (stack.isEmpty()) {
                    inventory = null;
                }
            }
        }
        if (remainingTicks > 0) {
            remainingTicks -= 1;
            if (node != null) {
                if (remainingTicks == 0 && inventory == null) {
                    updateClient();
                }
                ((Connector) node).changeBuffer(OCSettings.get().generatorEfficiency);
            }
        }
    }

    private void updateClient() {
        if (host instanceof li.cil.oc.api.internal.Robot) {
            ((li.cil.oc.api.internal.Robot) host).synchronizeSlot(host.componentSlot(node.address()));
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            if (inventory != null) {
                var world = host.level();
                var entity = new ItemEntity(world, host.xPosition(), host.yPosition(), host.zPosition(), inventory.copy());
                entity.setDeltaMovement(0, 0.04, 0);
                entity.setPickUpDelay(5);
                world.addFreshEntity(entity);
                inventory = null;
            }
            remainingTicks = 0;
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        if (nbt.contains("inventory")) {
            inventory = ItemStack.parseOptional(host.level().registryAccess(), nbt.getCompound("inventory"));
        }
        remainingTicks = nbt.getInt("remainingTicks");
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (inventory != null && !inventory.isEmpty()) {
            nbt.put("inventory", inventory.save(host.level().registryAccess(), new CompoundTag()));
        }
        if (remainingTicks > 0) {
            nbt.putInt("remainingTicks", remainingTicks);
        }
    }
}
