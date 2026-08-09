package li.cil.oc.core.impl.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import li.cil.oc.api.API;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.Color;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;

public final class LootManager {
    public static final Map<String, Callable<FileSystem>> factories = new HashMap<>();
    public static final List<ItemStack> disksForCyclingServer = new ArrayList<>();
    public static final List<ItemStack> disksForCyclingClient = new ArrayList<>();
    public static final List<ItemStack> disksForSampling = new ArrayList<>();
    public static final List<ItemStack> disksForClient = new ArrayList<>();
    public static final List<ItemStack[]> worldDisks = new ArrayList<>();
    public static final List<ItemStack[]> globalDisks = new ArrayList<>();
    public static boolean pendingDiskSync = false;

    private LootManager() {
    }

    public static List<ItemStack> disksForCycling() {
        return !disksForCyclingClient.isEmpty() ? disksForCyclingClient : disksForCyclingServer;
    }

    public static boolean isLootDisk(ItemStack stack) {
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        return API.items.get(stack) == API.items.get(Constants.ItemName.Floppy) &&
                customData != null && !customData.isEmpty() && customData.copyTag().contains(OCSettings.namespace + "lootFactory");
    }

    @FunctionalInterface
    public interface DiskCreator {
        ItemStack create(String ignoredName, String ignoredPath, boolean ignoredExternal, int ignoredColor);
    }

    public static Map<String, String> loadProperties(InputStream stream) {
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

    public static void parseLootDisks(Map<String, String> entries, List<ItemStack[]> acc, boolean external, DiskCreator creator) {
        for (var entry : entries.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            try {
                String[] parts = value.split(":");
                ItemStack stack;
                int count;
                if (parts.length >= 3) {
                    stack = creator.create(parts[0], key, external, java.util.Arrays.asList(Color.dyes).indexOf(parts[2]));
                    count = Integer.parseInt(parts[1]);
                } else if (parts.length == 2) {
                    stack = creator.create(parts[0], key, external, 8);
                    count = Integer.parseInt(parts[1]);
                } else {
                    stack = creator.create(value, key, external, 8);
                    count = 1;
                }
                acc.add(new ItemStack[]{stack, API.items.get(Constants.ItemName.Floppy).createItemStack(1).copyWithCount(count)});
            } catch (Throwable t) {
                org.slf4j.LoggerFactory.getLogger(LootManager.class).warn("Bad loot descriptor: {}", value, t);
            }
        }
    }
}
