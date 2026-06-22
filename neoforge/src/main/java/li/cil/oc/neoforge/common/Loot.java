package li.cil.oc.neoforge.common;

import li.cil.oc.api.API;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.init.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

public class Loot {
    public static final Map<String, Callable<FileSystem>> factories = new HashMap<>();
    public static final List<ItemStack> disksForCyclingServer = new ArrayList<>();
    public static final List<ItemStack> disksForCyclingClient = new ArrayList<>();
    public static final List<ItemStack> disksForSampling = new ArrayList<>();
    public static final List<ItemStack> disksForClient = new ArrayList<>();
    private static final List<ItemStack[]> worldDisks = new ArrayList<>();
    public static final List<ItemStack[]> globalDisks = new ArrayList<>();
    public static java.util.function.Consumer<ItemStack> jeiDiskAdder = null;

    private Loot() {
    }

    public static List<ItemStack> disksForCycling() {
        return !disksForCyclingClient.isEmpty() ? disksForCyclingClient : disksForCyclingServer;
    }

    public static boolean isLootDisk(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Floppy) &&
                customData != null && !customData.isEmpty() && customData.copyTag().contains(Settings.namespace + "lootFactory");
    }

    public static ItemStack registerLootDisk(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        return registerLootDisk(name, color, factory, doRecipeCycling, "unknown");
    }

    public static ItemStack registerLootDisk(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling, String mod) {
        OpenComputers.log().info("Registering loot disk '{}' from mod {}.", name, mod);

        String modSpecificName = mod + ":" + name;

        CompoundTag data = new CompoundTag();
        data.putString(Settings.namespace + "fs.label", name);

        CompoundTag nbt = new CompoundTag();
        nbt.put(Settings.namespace + "data", data);
        nbt.putString(Settings.namespace + "lootFactory", modSpecificName);
        nbt.putInt(Settings.namespace + "color", Math.clamp(color, 0, 15));

        ItemStack stack = API.items.get(Constants.ItemName.Floppy).createItemStack(1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        factories.put(modSpecificName, factory);

        if (doRecipeCycling) {
            disksForCyclingServer.add(stack);
        }

        if (jeiDiskAdder != null) {
            jeiDiskAdder.accept(stack);
        }

        return stack.copy();
    }

    public static void init() {
        try (InputStream listStream = Loot.class.getResourceAsStream("/assets/" + Settings.resourceDomain + "/loot/loot.properties")) {
            if (listStream != null) {
                parseLootDisks(loadProperties(listStream), globalDisks, false);
            }
        } catch (IOException e) {
            OpenComputers.log().warn("Failed loading loot properties.", e);
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void initForWorld(LevelEvent.Load e) {
        if (!e.getLevel().isClientSide() && ((net.minecraft.world.level.Level) e.getLevel()).dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            worldDisks.clear();
            disksForSampling.clear();
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            File path = new File(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), Settings.savePath + "loot/");
            if (path.exists() && path.isDirectory()) {
                File listFile = new File(path, "loot.properties");
                if (listFile.exists() && listFile.isFile()) {
                    try (FileInputStream listStream = new FileInputStream(listFile)) {
                        parseLootDisks(loadProperties(listStream), worldDisks, true);
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Failed opening loot descriptor file in saves folder.");
                    }
                }
            }
            for (ItemStack[] entry : globalDisks) {
                if (!worldDisks.contains(entry)) {
                    worldDisks.add(entry);
                }
            }
            for (ItemStack[] entry : worldDisks) {
                for (int i = 0; i < entry[1].getCount(); i++) {
                    disksForSampling.add(entry[0]);
                }
            }
        }
    }

    private static Map<String, String> loadProperties(InputStream stream) {
        Map<String, String> result = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                int eq = line.indexOf('=');
                if (eq >= 0) {
                    result.put(line.substring(0, eq).trim(), line.substring(eq + 1).trim());
                }
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static void parseLootDisks(Map<String, String> entries, List<ItemStack[]> acc, boolean external) {
        for (var entry : entries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            try {
                String[] parts = value.split(":");
                ItemStack stack;
                int count;
                if (parts.length >= 3) {
                    stack = createLootDisk(parts[0], key, external, java.util.Arrays.asList(Color.dyes).indexOf(parts[2]));
                    count = Integer.parseInt(parts[1]);
                } else if (parts.length == 2) {
                    stack = createLootDisk(parts[0], key, external);
                    count = Integer.parseInt(parts[1]);
                } else {
                    stack = createLootDisk(value, key, external);
                    count = 1;
                }
                acc.add(new ItemStack[]{stack, li.cil.oc.api.Items.get(Constants.ItemName.Floppy).createItemStack(1).copyWithCount(count)});
            } catch (Throwable t) {
                OpenComputers.log().warn("Bad loot descriptor: {}", value, t);
            }
        }
    }

    public static ItemStack createLootDisk(String name, String path, boolean external) {
        return createLootDisk(name, path, external, 8);
    }

    public static ItemStack createLootDisk(String name, String path, boolean external, int color) {
        Callable<FileSystem> callable;
        if (external) {
            callable = () -> li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromSaveDirectory("loot/" + path, 0, false));
        } else {
            callable = () -> li.cil.oc.api.FileSystem.fromClass(OpenComputers.class, Settings.resourceDomain, "loot/" + path);
        }
        ItemStack stack = registerLootDisk(path, color, callable, true, Settings.resourceDomain);
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
        if (!external) {
            Items.registerStack(stack, path);
        }
        return stack;
    }

}
