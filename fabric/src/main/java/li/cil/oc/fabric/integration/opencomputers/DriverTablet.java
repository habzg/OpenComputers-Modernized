package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;


@SuppressWarnings("unused")
public final class DriverTablet extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return isOneOf(stack, li.cil.oc.api.Items.get(Constants.ItemName.Tablet));
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() != null && host.level().isClientSide()) return null;
        TabletData data = new TabletData(stack);
        for (ItemStack fs : data.items) {
            if (fs != null && !fs.isEmpty()) {
                if (new DriverFileSystem().worksWith(fs)) {
                    li.cil.oc.api.network.ManagedEnvironment environment = new DriverFileSystem().createEnvironment(fs, host);
                    if (environment != null && environment.node() instanceof Component component) {
                        component.setVisibility(Visibility.Network);
                        environment.save(dataTag(stack), host.level().registryAccess());
                        return environment;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Tablet;
    }

    public CompoundTag dataTag(ItemStack stack) {
        TabletData data = new TabletData(stack);
        int slot = -1;
        for (int i = 0; i < data.items.size(); i++) {
            ItemStack item = data.items.get(i);
            if (item != null && !item.isEmpty()) {
                if (new DriverFileSystem().worksWith(item)) {
                    slot = i;
                    break;
                }
            }
        }
        if (slot >= 0) {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null && !cd.isEmpty()) {
                CompoundTag rootTag = cd.copyTag();
                if (rootTag.contains(OCSettings.namespace + "items")) {
                    net.minecraft.nbt.ListTag baseTag = rootTag.getList(OCSettings.namespace + "items", Tag.TAG_COMPOUND);
                    for (int i = 0; i < baseTag.size(); i++) {
                        CompoundTag entryTag = baseTag.getCompound(i);
                        if (entryTag.getByte("slot") == (byte) slot) {
                            if (!entryTag.contains("item")) {
                                entryTag.put("item", new CompoundTag());
                            }
                            CompoundTag itemTag = entryTag.getCompound("item");
                            if (!itemTag.contains("tag")) {
                                itemTag.put("tag", new CompoundTag());
                            }
                            CompoundTag stackTag = itemTag.getCompound("tag");
                            if (!stackTag.contains(OCSettings.namespace + "data")) {
                                stackTag.put(OCSettings.namespace + "data", new CompoundTag());
                            }
                            return stackTag.getCompound(OCSettings.namespace + "data");
                        }
                    }
                }
            }
        }
        return new CompoundTag();
    }
}
