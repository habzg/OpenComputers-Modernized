package li.cil.oc.neoforge.integration.opencomputers;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.Settings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.io.File;

@SuppressWarnings("unused")
public final class DriverLootDisk extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.LootDisk));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (!host.level().isClientSide()) {
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.overworld();
                CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
                if (cd != null && !cd.isEmpty()) {
                    String lootPath = "loot/" + cd.copyTag().getString(Settings.namespace + "lootPath");
                    File saveDir = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).toFile();
                    File savePath = new File(saveDir, Settings.savePath + lootPath);
                    li.cil.oc.api.fs.FileSystem fs;
                    if (savePath.exists() && savePath.isDirectory()) {
                        fs = li.cil.oc.api.FileSystem.fromSaveDirectory(lootPath, 0, false);
                    } else {
                        fs = li.cil.oc.api.FileSystem.fromClass(Settings.class, Settings.resourceDomain, lootPath);
                    }
                    String label = null;
                    if (Item.getDataTag(stack).contains(Settings.namespace + "fs.label")) {
                        label = dataTag(stack).getString(Settings.namespace + "fs.label");
                    }
                    return li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, Settings.resourceDomain + ":floppy_access");
                }
            }
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Floppy;
    }
}
