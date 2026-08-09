package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.media.IMedia;
import dan200.computercraft.api.media.MediaCapability;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

@SuppressWarnings("unused")
public final class DriverComputerCraftMedia extends Item {
    @Override
    public boolean worksWith(ItemStack stack) {
        return stack.getCapability(MediaCapability.get()) != null;
    }

    @Override
    public li.cil.oc.api.network.ManagedEnvironment createEnvironment(ItemStack stack, EnvironmentHost host) {
        if (host.level() instanceof ServerLevel serverLevel) {
            var media = stack.getCapability(MediaCapability.get());
            if (media != null) {
                var mount = media.createDataMount(stack, serverLevel);
                if (mount != null) {
                    var ocFs = li.cil.oc.api.FileSystem.fromComputerCraft(mount);
                    if (ocFs != null) {
                        var label = new ComputerCraftLabel(stack, media);
                        var environment = li.cil.oc.api.FileSystem.asManagedEnvironment(ocFs, label, host, OCSettings.resourceDomain + ":floppy_access");
                        if (environment != null && environment.node() != null) {
                            var address = li.cil.oc.core.impl.integration.opencomputers.Item.address(stack);
                            if (address != null) {
                                ((li.cil.oc.core.impl.server.network.Node) environment.node()).address_$eq(address);
                            }
                        }
                        return environment;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public String slot(ItemStack stack) {
        return Slot.Floppy;
    }

    private record ComputerCraftLabel(ItemStack stack, IMedia media) implements Label {
        @Override
        public String getLabel() {
            return media.getLabel(null, stack);
        }

        @Override
        public void setLabel(String value) {
            media.setLabel(stack, value);
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        }
    }
}
