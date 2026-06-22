package li.cil.oc.core.impl.server.driver;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.driver.item.HostAware;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Registry implements li.cil.oc.api.detail.DriverAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(Registry.class);
    public static final Registry INSTANCE = new Registry();

    private final List<li.cil.oc.api.driver.SidedBlock> blocks = new ArrayList<>();
    private final List<li.cil.oc.api.driver.Item> items = new ArrayList<>();
    private final List<Converter> converters = new ArrayList<>();
    private final List<EnvironmentProvider> environmentProviders = new ArrayList<>();
    private final List<InventoryProvider> inventoryProviders = new ArrayList<>();
    private final List<Map.Entry<ItemStack, Set<Class<?>>>> blacklist = new ArrayList<>();
    private boolean locked = false;

    private Registry() {
    }

    public void setLocked(boolean value) {
        locked = value;
    }

    @Override
    public void add(li.cil.oc.api.driver.SidedBlock driver) {
        if (locked) throw new IllegalStateException("Please register all drivers in the init phase.");
        if (!blocks.contains(driver)) {
            LOGGER.debug("Registering block driver {}.", driver.getClass().getName());
            blocks.add(driver);
        }
    }

    @Override
    public void add(li.cil.oc.api.driver.Item driver) {
        if (locked) throw new IllegalStateException("Please register all drivers in the init phase.");
        if (!items.contains(driver)) {
            LOGGER.debug("Registering item driver {}.", driver.getClass().getName());
            items.add(driver);
        }
    }

    @Override
    public void add(Converter converter) {
        if (locked) throw new IllegalStateException("Please register all converters in the init phase.");
        if (!converters.contains(converter)) {
            LOGGER.debug("Registering converter {}.", converter.getClass().getName());
            converters.add(converter);
        }
    }

    @Override
    public void add(EnvironmentProvider provider) {
        if (locked) throw new IllegalStateException("Please register all environment providers in the init phase.");
        if (!environmentProviders.contains(provider)) {
            LOGGER.debug("Registering environment provider {}.", provider.getClass().getName());
            environmentProviders.add(provider);
        }
    }

    @Override
    public void add(InventoryProvider provider) {
        if (locked) throw new IllegalStateException("Please register all inventory providers in the init phase.");
        if (!inventoryProviders.contains(provider)) {
            LOGGER.debug("Registering inventory provider {}.", provider.getClass().getName());
            inventoryProviders.add(provider);
        }
    }

    @Override
    public li.cil.oc.api.driver.SidedBlock driverFor(Level world, BlockPos pos, Direction side) {
        List<li.cil.oc.api.driver.SidedBlock> sidedDrivers = new ArrayList<>();
        for (li.cil.oc.api.driver.SidedBlock d : blocks) {
            if (d.worksWith(world, pos.getX(), pos.getY(), pos.getZ(), side)) sidedDrivers.add(d);
        }
        if (!sidedDrivers.isEmpty()) {
            return new CompoundBlockDriver(sidedDrivers.toArray(new li.cil.oc.api.driver.SidedBlock[0]));
        }
        return null;
    }

    @Override
    public li.cil.oc.api.driver.Item driverFor(ItemStack stack, Class<? extends EnvironmentHost> host) {
        if (stack == null) return null;
        List<li.cil.oc.api.driver.Item> hostAware = new ArrayList<>();
        for (li.cil.oc.api.driver.Item driver : items) {
            if (driver instanceof HostAware && driver.worksWith(stack)) {
                hostAware.add(driver);
            }
        }
        if (!hostAware.isEmpty()) {
            for (li.cil.oc.api.driver.Item driver : hostAware) {
                if (((HostAware) driver).worksWith(stack, host)) return driver;
            }
            return null;
        }
        return driverFor(stack);
    }

    @Override
    public li.cil.oc.api.driver.Item driverFor(ItemStack stack) {
        if (stack == null) return null;
        for (li.cil.oc.api.driver.Item driver : items) {
            if (driver.worksWith(stack)) return driver;
        }
        return null;
    }

    @Override
    public Set<Class<?>> environmentsFor(ItemStack stack) {
        Set<Class<?>> result = new HashSet<>();
        for (EnvironmentProvider provider : environmentProviders) {
            Class<?> clazz = provider.getEnvironment(stack);
            if (clazz != null) result.add(clazz);
        }
        return result;
    }

    @Override
    public Container inventoryFor(ItemStack stack, Player player) {
        for (InventoryProvider provider : inventoryProviders) {
            if (provider.worksWith(stack, player)) {
                return provider.getInventory(stack, player);
            }
        }
        return null;
    }

    @Override
    public List<li.cil.oc.api.driver.SidedBlock> blockDrivers() {
        return Collections.unmodifiableList(blocks);
    }

    @Override
    public List<li.cil.oc.api.driver.Item> itemDrivers() {
        return Collections.unmodifiableList(items);
    }

    public boolean isBlacklisted(ItemStack stack, Class<?> host) {
        return blacklist.stream().anyMatch(entry ->
                ItemStack.isSameItem(entry.getKey(), stack) &&
                        entry.getValue().stream().anyMatch(h -> h.isAssignableFrom(host)));
    }

    public void blacklistHost(ItemStack stack, Class<?> host) {
        for (Map.Entry<ItemStack, Set<Class<?>>> entry : blacklist) {
            if (ItemStack.isSameItem(entry.getKey(), stack)) {
                entry.getValue().add(host);
                return;
            }
        }
        Set<Class<?>> hosts = new HashSet<>();
        hosts.add(host);
        blacklist.add(new AbstractMap.SimpleEntry<>(stack, hosts));
    }

    public Object[] convert(Object[] value) {
        if (value == null) return null;
        Object[] result = new Object[value.length];
        for (int i = 0; i < value.length; i++) {
            result[i] = convertRecursively(value[i], new IdentityHashMap<>());
        }
        return result;
    }

    public Object convertRecursively(Object value, IdentityHashMap<Object, Object> memo) {
        return convertRecursively(value, memo, false);
    }

    public Object convertRecursively(Object valueRef, IdentityHashMap<Object, Object> memo, boolean force) {
        if (!force && memo.containsKey(valueRef)) {
            return memo.get(valueRef);
        }
        switch (valueRef) {
            case null -> {
                return null;
            }
            case Boolean ignored -> {
                return valueRef;
            }
            case Byte ignored -> {
                return valueRef;
            }
            case Character ignored -> {
                return valueRef;
            }
            case Short ignored -> {
                return valueRef;
            }
            case Integer ignored -> {
                return valueRef;
            }
            case Long ignored -> {
                return valueRef;
            }
            case Float ignored -> {
                return valueRef;
            }
            case Double ignored -> {
                return valueRef;
            }
            case Number number -> {
                return number.doubleValue();
            }
            case String ignored -> {
                return valueRef;
            }
            case boolean[] ignored -> {
                return valueRef;
            }
            case byte[] ignored -> {
                return valueRef;
            }
            case char[] ignored -> {
                return valueRef;
            }
            case short[] ignored -> {
                return valueRef;
            }
            case int[] ignored -> {
                return valueRef;
            }
            case long[] ignored -> {
                return valueRef;
            }
            case float[] ignored -> {
                return valueRef;
            }
            case double[] ignored -> {
                return valueRef;
            }
            case String[] ignored -> {
                return valueRef;
            }
            case Value ignored -> {
                return valueRef;
            }
            case Object[] objects -> {
                return convertList(valueRef, Arrays.asList(objects).iterator(), memo);
            }
            case Map<?, ?> map -> {
                return convertMap(valueRef, map, memo);
            }
            case Iterable<?> iterable -> {
                List<Object> list = new ArrayList<>();
                for (Object o : iterable) list.add(o);
                return convertList(valueRef, list.iterator(), memo);
            }
            default -> {
            }
        }
        java.util.HashMap<Object, Object> converted = new java.util.HashMap<>();
        memo.put(valueRef, converted);
        for (Converter converter : converters) {
            try {
                converter.convert(valueRef, converted);
            } catch (Throwable t) {
                LOGGER.warn("Type converter threw an exception.", t);
            }
        }
        if (converted.isEmpty()) {
            String str = valueRef.toString();
            memo.put(valueRef, str);
            return str;
        } else {
            memo.put(converted, converted);
            convertRecursively(converted, memo, true);
            memo.remove(converted);
            if (converted.size() == 1 && converted.containsKey("oc:flatten")) {
                Object flatValue = converted.get("oc:flatten");
                memo.put(valueRef, flatValue);
                return flatValue;
            }
            return converted;
        }
    }

    private Object convertList(Object obj, Iterator<Object> iterator, IdentityHashMap<Object, Object> memo) {
        List<Object> converted = new ArrayList<>();
        memo.put(obj, converted);
        while (iterator.hasNext()) {
            converted.add(convertRecursively(iterator.next(), memo));
        }
        return converted.toArray();
    }

    @SuppressWarnings("unchecked")
    private Object convertMap(Object obj, java.util.Map<?, ?> map, IdentityHashMap<Object, Object> memo) {
        Map<Object, Object> converted = (Map<Object, Object>) memo.computeIfAbsent(obj, k -> new HashMap<>());
        Map<Object, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object key = convertRecursively(entry.getKey(), memo);
            Object value = convertRecursively(entry.getValue(), memo);
            result.put(key, value);
        }
        converted.putAll(result);
        return memo.get(obj);
    }
}
