package li.cil.oc.core.impl.common.inventory;

import li.cil.oc.api.driver.Item;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.util.Lifecycle;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.ClientTickScheduler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;

public interface ComponentInventory extends Inventory, Environment {
    Logger LOGGER = LoggerFactory.getLogger(ComponentInventory.class);

    static void applyLifecycleState(Object component, Lifecycle.LifecycleState state) {
        if (component instanceof Lifecycle) {
            ((Lifecycle) component).onLifecycleStateChange(state);
        }
    }

    ManagedEnvironment[] _components();

    void _components(ManagedEnvironment[] value);

    boolean isSizeInventoryReady();

    default ManagedEnvironment[] componentEnvironments() {
        if (_components() == null && isSizeInventoryReady()) {
            int size = getContainerSize();
            ManagedEnvironment[] comps = new ManagedEnvironment[size];
            _components(comps);
        }
        if (_components() == null) return new ManagedEnvironment[0];
        return _components();
    }

    ArrayList<ManagedEnvironment> updatingComponents();

    EnvironmentHost host();

    default ItemStack[] pendingRemovals() {
        return null;
    }

    default ItemStack[] pendingAdds() {
        return null;
    }

    default void updateComponents() {
        ArrayList<ManagedEnvironment> comps = updatingComponents();
        if (!comps.isEmpty()) {
            for (ManagedEnvironment comp : comps) {
                comp.update();
            }
        }
    }

    static void connectComponents(ComponentInventory ci) {
        ManagedEnvironment[] comps = ci.componentEnvironments();
        for (int slot = 0; slot < ci.getContainerSize() && slot < comps.length; slot++) {
            ItemStack stack = ci.getItem(slot);
            if (!stack.isEmpty() && (comps[slot] == null) && ci.isComponentSlot(slot, stack)) {
                Item driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver != null) {
                    ManagedEnvironment component = driver.createEnvironment(stack, ci.host());
                    if (component != null) {
                        applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
                        var level = ci.host().level();
                        HolderLookup.Provider provider = level != null ? level.registryAccess() : null;
                        if (provider == null && ci.host() instanceof li.cil.oc.core.impl.common.tileentity.traits.TileEntity te) {
                            provider = te.getEffectiveProvider();
                        }
                        if (provider != null) {
                            try {
                                component.load(ci.dataTag(driver, stack), provider);
                            } catch (Throwable e) {
                                LOGGER.warn("An item component of type '{}' (provided by driver '{}') threw an error while loading.", component.getClass().getName(), driver.getClass().getName(), e);
                            }
                        }
                        if (component.canUpdate()) {
                            ci.updatingComponents().add(component);
                        }
                        comps[slot] = component;
                    }
                }
            }
        }
        li.cil.oc.api.Network.joinNewNetwork(ci.node());
        for (ManagedEnvironment component : comps) {
            if (component != null) {
                applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
                ci.connectItemNode(component.node());
                applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
            }
        }
    }

    static void disconnectComponents(ComponentInventory ci) {
        for (ManagedEnvironment component : ci.componentEnvironments()) {
            if (component != null) {
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
                if (component.node() != null) component.node().remove();
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
            }
        }
    }

    static void saveComponents(ComponentInventory ci, HolderLookup.Provider provider) {
        ManagedEnvironment[] comps = ci.componentEnvironments();
        for (int slot = 0; slot < ci.getContainerSize(); slot++) {
            ItemStack stack = ci.getItem(slot);
            if (!stack.isEmpty()) {
                if (slot >= comps.length) {
                    LOGGER.error("ComponentInventory components length {} does not accommodate inventory size {}", comps.length, ci.getContainerSize());
                    return;
                } else if (comps[slot] != null) {
                    Item driver = li.cil.oc.api.API.driver.driverFor(stack);
                    if (driver != null) {
                        ci.save(comps[slot], driver, stack, provider);
                    }
                }
            }
        }
    }

    default void connectComponents() {
        connectComponents(this);
    }

    default void disconnectComponents() {
        disconnectComponents(this);
    }

    @Override
    default void load(CompoundTag nbt, HolderLookup.Provider provider) {
        Inventory.super.load(nbt, provider);
        var level = host().level();
        if (level != null && level.isClientSide()) {
            connectComponents();
        }
    }

    @Override
    default void save(CompoundTag nbt, HolderLookup.Provider provider) {
        saveComponents(provider);
        Inventory.super.save(nbt, provider);
    }

    default void saveComponents(HolderLookup.Provider provider) {
        saveComponents(this, provider);
    }

    @Override
    default int getMaxStackSize() {
        return 1;
    }

    @Override
    default void onItemAdded(int slot, ItemStack stack) {
        var level = host().level();
        if (level == null || !level.isClientSide()) {
            processItemAdded(slot, stack);
        } else {
            var adds = pendingAdds();
            if (adds != null && slot >= 0 && slot < adds.length) {
                var removals = pendingRemovals();
                if (removals != null && slot < removals.length &&
                        removals[slot] != null && !removals[slot].isEmpty() &&
                        ItemStack.isSameItemSameComponents(stack, removals[slot])) {
                    removals[slot] = ItemStack.EMPTY;
                } else {
                    adds[slot] = stack.copy();
                }
                scheduleInventoryChange();
            }
        }
    }

    @Override
    default void onItemRemoved(int slot, ItemStack stack) {
        var level = host().level();
        if (level == null || !level.isClientSide()) {
            processItemRemoved(slot, stack);
        } else {
            var adds = pendingAdds();
            if (adds != null && slot >= 0 && slot < adds.length) {
                var addsStack = adds[slot];
                if (addsStack != null && !addsStack.isEmpty() &&
                        ItemStack.isSameItemSameComponents(stack, addsStack)) {
                    adds[slot] = ItemStack.EMPTY;
                } else {
                    var removals = pendingRemovals();
                    if (removals != null && slot < removals.length && removals[slot] == null) {
                        removals[slot] = stack.copy();
                    }
                }
                scheduleInventoryChange();
            }
        }
    }

    default void processItemAdded(int slot, ItemStack stack) {
        ManagedEnvironment[] comps = componentEnvironments();
        if (slot >= 0 && slot < comps.length && isComponentSlot(slot, stack)) {
            Item driver = li.cil.oc.api.API.driver.driverFor(stack);
            if (driver != null) {
                ManagedEnvironment component = driver.createEnvironment(stack, host());
                if (component != null) {
                    synchronized (this) {
                        comps[slot] = component;
                        applyLifecycleState(component, Lifecycle.LifecycleState.Constructing);
                        var level = host().level();
                        if (level != null) {
                            try {
                                component.load(dataTag(driver, stack), level.registryAccess());
                            } catch (Throwable e) {
                                LOGGER.warn("An item component of type '{}' (provided by driver '{}') threw an error while loading.", component.getClass().getName(), driver.getClass().getName(), e);
                            }
                        }
                        if (component.canUpdate()) {
                            updatingComponents().add(component);
                        }
                        applyLifecycleState(component, Lifecycle.LifecycleState.Initializing);
                        if (component.node() != null) {
                            connectItemNode(component.node());
                        }
                        applyLifecycleState(component, Lifecycle.LifecycleState.Initialized);
                        if (level != null && !level.isClientSide()) {
                            save(component, driver, stack, level.registryAccess());
                        }
                    }
                }
            }
        }
    }

    default void processItemRemoved(int slot, ItemStack stack) {
        ManagedEnvironment[] comps = componentEnvironments();
        if (slot >= 0 && slot < comps.length && comps[slot] != null) {
            synchronized (this) {
                ManagedEnvironment component = comps[slot];
                comps[slot] = null;
                updatingComponents().remove(component);
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposing);
                if (component.node() != null) component.node().remove();
                Item driver = li.cil.oc.api.API.driver.driverFor(stack);
                if (driver != null) {
                    var level = host().level();
                    if (level != null && !level.isClientSide()) {
                        save(component, driver, stack, level.registryAccess());
                    }
                }
                if (component.node() != null) component.node().remove();
                applyLifecycleState(component, Lifecycle.LifecycleState.Disposed);
            }
        }
    }

    default void applyInventoryChanges() {
        var removals = pendingRemovals();
        var adds = pendingAdds();
        if (removals == null || adds == null) {
            return;
        }
        int limit = Math.min(Math.min(removals.length, adds.length), getContainerSize());
        for (int slot = 0; slot < limit; slot++) {
            var removed = removals[slot];
            var added = adds[slot];
            if (removed != null && !removed.isEmpty() && added != null && !added.isEmpty()) {
                if (!ItemStack.isSameItemSameComponents(removed, added)) {
                    processItemRemoved(slot, removed);
                    processItemAdded(slot, added);
                    setChanged();
                }
            } else if (removed != null && !removed.isEmpty()) {
                processItemRemoved(slot, removed);
                setChanged();
            } else if (added != null && !added.isEmpty()) {
                processItemAdded(slot, added);
                setChanged();
            }
            removals[slot] = ItemStack.EMPTY;
            adds[slot] = ItemStack.EMPTY;
        }
    }

    default void scheduleInventoryChange() {
        ClientTickScheduler.schedule(this::applyInventoryChanges);
    }

    default boolean isComponentSlot(int slot, ItemStack stack) {
        return true;
    }

    default void connectItemNode(Node node) {
        if (node() != null && node != null) {
            if (node().network() == null) {
                li.cil.oc.api.Network.joinNewNetwork(node());
            }
            node().connect(node);
        }
    }

    default CompoundTag dataTag(li.cil.oc.api.driver.Item driver, ItemStack stack) {
        if (driver != null) {
            try {
                CompoundTag driverTag = driver.dataTag(stack);
                if (driverTag != null) return driverTag;
            } catch (Throwable ignored) {}
        }
        var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag nbt;
        if (customData != null && !customData.isEmpty()) {
            nbt = customData.copyTag();
        } else {
            nbt = new CompoundTag();
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
        }
        if (!nbt.contains(Settings.namespace + "data")) {
            nbt.put(Settings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(Settings.namespace + "data");
    }

    default void save(ManagedEnvironment component, li.cil.oc.api.driver.Item driver, ItemStack stack, HolderLookup.Provider provider) {
        try {
            var customData = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
            CompoundTag nbt;
            if (customData != null && !customData.isEmpty()) {
                nbt = customData.copyTag();
            } else {
                nbt = new CompoundTag();
            }
            CompoundTag data = nbt.contains(Settings.namespace + "data") ?
                    nbt.getCompound(Settings.namespace + "data") : new CompoundTag();
            for (String key : List.copyOf(data.getAllKeys())) {
                data.remove(key);
            }
            component.save(data, provider);
            nbt.put(Settings.namespace + "data", data);
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
        } catch (Throwable e) {
            LOGGER.warn("An item component of type '{}' (provided by driver '{}') threw an error while saving.", component.getClass().getName(), driver.getClass().getName(), e);
        }
    }
}
