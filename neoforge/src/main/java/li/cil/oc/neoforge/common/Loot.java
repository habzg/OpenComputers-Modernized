package li.cil.oc.neoforge.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Callable;
import li.cil.oc.api.API;
import li.cil.oc.api.fs.FileSystem;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.init.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public class Loot {
    private Loot() {
    }

    public static ItemStack registerLootDisk(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling) {
        return registerLootDisk(name, color, factory, doRecipeCycling, "unknown");
    }

    public static ItemStack registerLootDisk(String name, int color, Callable<FileSystem> factory, boolean doRecipeCycling, String mod) {
        OpenComputers.log().info("Registering loot disk '{}' from mod {}.", name, mod);

        String modSpecificName = mod + ":" + name;

        CompoundTag data = new CompoundTag();
        data.putString(OCSettings.namespace + "fs.label", name);

        CompoundTag nbt = new CompoundTag();
        nbt.put(OCSettings.namespace + "data", data);
        nbt.putString(OCSettings.namespace + "lootFactory", modSpecificName);
        nbt.putInt(OCSettings.namespace + "color", Math.clamp(color, 0, 15));

        ItemStack stack = API.items.get(Constants.ItemName.Floppy).createItemStack(1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));

        LootManager.factories.put(modSpecificName, factory);

        if (doRecipeCycling) {
            LootManager.disksForCyclingServer.add(stack);
        }

        return stack.copy();
    }

    public static void init() {
        try (InputStream listStream = Loot.class.getResourceAsStream("/assets/" + OCSettings.resourceDomain + "/loot/loot.properties")) {
            if (listStream != null) {
                LootManager.parseLootDisks(LootManager.loadProperties(listStream), LootManager.globalDisks, false,
                        Loot::createLootDisk);
            }
        } catch (IOException e) {
            OpenComputers.log().warn("Failed loading loot properties.", e);
        }

        for (ItemStack[] entry : LootManager.globalDisks) {
            LootManager.disksForClient.add(entry[0].copy());
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void initForWorld(LevelEvent.Load e) {
        if (!e.getLevel().isClientSide() && ((net.minecraft.world.level.Level) e.getLevel()).dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            LootManager.worldDisks.clear();
            LootManager.disksForSampling.clear();
            var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            File path = new File(server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile(), OCSettings.savePath + "loot/");
            if (path.exists() && path.isDirectory()) {
                File listFile = new File(path, "loot.properties");
                if (listFile.exists() && listFile.isFile()) {
                    try (FileInputStream listStream = new FileInputStream(listFile)) {
                        LootManager.parseLootDisks(LootManager.loadProperties(listStream), LootManager.worldDisks, true,
                                Loot::createLootDisk);
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Failed opening loot descriptor file in saves folder.");
                    }
                }
            }
            for (ItemStack[] entry : LootManager.globalDisks) {
                if (!LootManager.worldDisks.contains(entry)) {
                    LootManager.worldDisks.add(entry);
                }
            }
            for (ItemStack[] entry : LootManager.worldDisks) {
                for (int i = 0; i < entry[1].getCount(); i++) {
                    LootManager.disksForSampling.add(entry[0]);
                }
            }
        }
    }

    public static ItemStack createLootDisk(String name, String path, boolean external, int color) {
        Callable<FileSystem> callable;
        if (external) {
            callable = () -> li.cil.oc.api.FileSystem.asReadOnly(li.cil.oc.api.FileSystem.fromSaveDirectory("loot/" + path, 0, false));
        } else {
            callable = () -> li.cil.oc.api.FileSystem.fromClass(OpenComputers.class, OCSettings.resourceDomain, "loot/" + path);
        }
        ItemStack stack = registerLootDisk(path, color, callable, true, OCSettings.resourceDomain);
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
        String modSpecificName = OCSettings.resourceDomain + ":" + path;
        for (var disk : LootManager.disksForCyclingServer) {
            var data = disk.get(DataComponents.CUSTOM_DATA);
            if (data != null && data.copyTag().getString(OCSettings.namespace + "lootFactory").equals(modSpecificName)) {
                disk.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal(name));
                break;
            }
        }
        if (!external) {
            Items.registerStack(stack, path);
        }
        return stack;
    }

}
