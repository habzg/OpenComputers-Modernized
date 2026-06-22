package li.cil.oc.core.impl.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableMap;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.server.component.TabletHostBase;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

public abstract class TabletCache {
    private static TabletCache serverInstance;
    private static TabletCache clientInstance;

    public static void setInstance(TabletCache inst) {
        serverInstance = inst;
    }

    public static TabletCache get() {
        return serverInstance;
    }

    public static void setClientInstance(TabletCache inst) {
        clientInstance = inst;
    }

    public static TabletCache forSide(boolean isClientSide) {
        return isClientSide ? clientInstance : serverInstance;
    }

    private static final String ID_TAG = Settings.namespace + "tablet";

    public static String getOrCreateId(ItemStack stack) {
        var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        CompoundTag data;
        if (tag != null && !tag.isEmpty()) {
            data = tag.copyTag();
        } else {
            data = new CompoundTag();
        }
        if (!data.contains(ID_TAG)) {
            data.putString(ID_TAG, UUID.randomUUID().toString());
            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(data));
        }
        return data.getString(ID_TAG);
    }

    private final Cache<String, TabletHostBase> cache;

    protected TabletCache(long timeoutSeconds) {
        this.cache = CacheBuilder.newBuilder()
                .expireAfterAccess(timeoutSeconds, TimeUnit.SECONDS)
                .removalListener((com.google.common.cache.RemovalNotification<String, TabletHostBase> notification) -> {
                    var wrapper = notification.getValue();
                    if (wrapper != null) {
                        var level = wrapper.level();
                        if (!level.isClientSide()) {
                            var provider = level.registryAccess();
                            wrapper.saveComponents(provider);
                            wrapper.persistMachineState();
                        }
                        wrapper.stopMachine();
                        if (wrapper.node() != null && wrapper.node().network() != null) {
                            wrapper.node().remove();
                        }
                        if (!level.isClientSide()) {
                            var provider = level.registryAccess();
                            wrapper.saveComponents(provider);
                            wrapper.persistMachineState();
                        }
                    }
                })
                .build();
    }

    protected abstract TabletHostBase createHost(ItemStack stack, Player player);

    public TabletHostBase get(ItemStack stack, Player player) {
        var id = getOrCreateId(stack);
        try {
            var host = cache.get(id, () -> createHost(stack, player));
            if (host.creationLevel != player.level()) {
                host.creationLevel = player.level();

                if (!player.level().isClientSide) {
                    host.machine();
                    if (host.machine().node() != null && host.machine().node().network() != null) {
                        host.persistMachineState();
                    }
                }
                cache.invalidate(id);
                cache.cleanUp();
                host = cache.get(id, () -> createHost(stack, player));
            }
            return host;
        } catch (Exception e) {
            return createHost(stack, player);
        }
    }

    public void saveAll(Level level) {
        for (var entry : cache.asMap().entrySet()) {
            var wrapper = entry.getValue();
            if (wrapper.level() == level && !level.isClientSide()) {
                var provider = level.registryAccess();
                wrapper.saveComponents(provider);
                wrapper.persistMachineState();
            }
        }
    }

    public void clear(Level level) {
        synchronized (cache) {
            var tabletsInWorld = cache.asMap().entrySet().stream()
                    .filter(e -> e.getValue().level() == level)
                    .map(java.util.Map.Entry::getKey)
                    .toList();
            cache.invalidateAll(tabletsInWorld);
            cache.cleanUp();
        }
    }

    public void cleanUp() {
        synchronized (cache) {
            cache.cleanUp();
        }
    }

    @SuppressWarnings("UnusedReturnValue")
    public ImmutableMap<String, TabletHostBase> keepAlive() {
        return cache.getAllPresent(cache.asMap().keySet());
    }

    public void invalidate(ItemStack stack) {
        var id = getId(stack);
        if (id != null) {
            cache.invalidate(id);
        }
    }

    private static String getId(ItemStack stack) {
        var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (tag != null && !tag.isEmpty()) {
            var data = tag.copyTag();
            if (data.contains(ID_TAG)) {
                return data.getString(ID_TAG);
            }
        }
        return null;
    }
}
