package li.cil.oc.fabric.integration.opencomputers;

import java.util.UUID;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.common.item.FloppyDisk;
import li.cil.oc.core.impl.common.item.HardDiskDrive;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import li.cil.oc.core.impl.server.component.Drive;
import li.cil.oc.core.impl.server.fs.FileSystem.ItemLabel;
import li.cil.oc.core.impl.server.fs.FileSystem.ReadOnlyLabel;
import li.cil.oc.fabric.OpenComputers;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

@SuppressWarnings("unused")
public final class DriverFileSystem extends Item {
    private static boolean isValidUUID(String s) {
        if (s == null) return false;
        try {
            UUID.fromString(s);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack,
                li.cil.oc.api.Items.get(Constants.ItemName.HDDTier1),
                li.cil.oc.api.Items.get(Constants.ItemName.HDDTier2),
                li.cil.oc.api.Items.get(Constants.ItemName.HDDTier3),
                li.cil.oc.api.Items.get(Constants.ItemName.Floppy));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        var subItem = stack.getItem();
        if (subItem instanceof HardDiskDrive hdd) {
            return createEnvironment(stack, hdd.kiloBytes() * 1024, hdd.platterCount(), host, hdd.tier() + 2);
        } else if (subItem instanceof FloppyDisk disk) {
            return createEnvironment(stack, OCSettings.get().floppySize * 1024, 1, host, 1);
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof HardDiskDrive) {
            return Slot.HDD;
        } else if (subItem instanceof FloppyDisk) {
            return Slot.Floppy;
        }
        throw new IllegalArgumentException();
    }

    @Override
    public int tier(ItemStack stack) {
        var subItem = stack.getItem();
        if (subItem instanceof HardDiskDrive hdd) {
            return hdd.tier();
        }
        return 0;
    }

    private li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, int capacity, int platterCount, EnvironmentHost host, int speed) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty() && cd.copyTag().contains(OCSettings.namespace + "lootFactory")) {
            String factoryKey = cd.copyTag().getString(OCSettings.namespace + "lootFactory");
            java.util.concurrent.Callable<li.cil.oc.api.fs.FileSystem> factory = LootManager.factories.get(factoryKey);
            if (factory != null) {
                String label = getTag(stack).contains(OCSettings.namespace + "fs.label")
                        ? getTag(stack).getString(OCSettings.namespace + "fs.label") : null;
                try {
                    var fs = factory.call();
                    if (fs == null) {
                        OpenComputers.log().warn("Loot factory '{}' returned null filesystem.", factoryKey);
                    }
                    return li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, OCSettings.resourceDomain + ":floppy_access");
                } catch (Exception e) {
                    OpenComputers.log().warn("Loot factory '{}' threw an exception.", factoryKey, e);
                    return null;
                }
            }
            return null;
        } else {
            String address = addressFromTag(getTag(stack));
            li.cil.oc.api.fs.Label label = new ReadWriteItemLabel();
            boolean isFloppy = li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Floppy);
            String sound = OCSettings.resourceDomain + ":" + (isFloppy ? "floppy_access" : "hdd_access");
            DriveData drive = new DriveData(stack);
            li.cil.oc.api.network.ManagedEnvironment environment;
            if (drive.isUnmanaged) {
                environment = new Drive(Math.max(capacity, 0), platterCount, label, host, sound, speed, drive.isLocked());
            } else {
                li.cil.oc.api.fs.FileSystem fs = li.cil.oc.api.FileSystem.fromSaveDirectory(address, Math.max(capacity, 0), OCSettings.get().bufferChanges);
                if (drive.isLocked()) {
                    fs = li.cil.oc.api.FileSystem.asReadOnly(fs);
                    label = new ReadOnlyLabel(label.getLabel());
                }
                environment = li.cil.oc.api.FileSystem.asManagedEnvironment(fs, label, host, sound, speed);
            }
            if (environment != null && environment.node() != null) {
                ((li.cil.oc.core.impl.server.network.Node) environment.node()).address_$eq(address);
            }
            return environment;
        }
    }

    private String addressFromTag(CompoundTag tag) {
        if (tag.contains("node") && tag.getCompound("node").contains("address")) {
            String address = tag.getCompound("node").getString("address");
            if (isValidUUID(address)) {
                return address;
            } else {
                String newAddress = java.util.UUID.randomUUID().toString();
                tag.getCompound("node").putString("address", newAddress);
                OpenComputers.log().warn("Generated new address for disk '{}'.", newAddress);
                return newAddress;
            }
        }
        return java.util.UUID.randomUUID().toString();
    }

    private CompoundTag getTag(ItemStack stack) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag nbt = cd.copyTag();
            if (nbt.contains(OCSettings.namespace + "data")) {
                return nbt.getCompound(OCSettings.namespace + "data");
            }
            return nbt;
        }
        return new CompoundTag();
    }

    private static class ReadWriteItemLabel extends ItemLabel {
        private String label;

        @Override
        public String getLabel() {
            return label;
        }

        public void setLabel(String value) {
            if (value != null && value.length() > 16) {
                label = value.substring(0, 16);
            } else {
                label = value;
            }
        }

        @Override
        public void load(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
            if (nbt.contains(OCSettings.namespace + "fs.label")) {
                label = nbt.getString(OCSettings.namespace + "fs.label");
            }
        }

        @Override
        public void save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider provider) {
            if (label != null) nbt.putString(OCSettings.namespace + "fs.label", label);
        }
    }
}
