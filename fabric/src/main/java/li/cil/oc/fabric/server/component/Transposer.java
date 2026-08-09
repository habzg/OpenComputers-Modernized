package li.cil.oc.fabric.server.component;

import li.cil.oc.api.Items;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.item.data.TransposerData;
import li.cil.oc.core.impl.server.component.TransposerBase;
import li.cil.oc.core.impl.server.component.traits.InventoryTransfer;
import li.cil.oc.core.impl.server.component.traits.WorldFluidContainerAnalytics;
import li.cil.oc.core.impl.server.component.traits.WorldInventoryAnalytics;
import li.cil.oc.core.impl.server.component.traits.WorldTankAnalytics;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.fabric.server.component.traits.FluidContainerTransfer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;


@SuppressWarnings("unused")
public final class Transposer {
    private Transposer() {
    }

    @SuppressWarnings("unused")
    public abstract static class Common extends TransposerBase implements
            WorldInventoryAnalytics, WorldTankAnalytics, WorldFluidContainerAnalytics,
            InventoryTransfer, FluidContainerTransfer {
    }

    public static class Block extends Common {
        private final li.cil.oc.fabric.common.blockentity.Transposer host;

        @SuppressWarnings("unused")
        public Block(li.cil.oc.fabric.common.blockentity.Transposer host) {
            this.host = host;
        }

        @SuppressWarnings("unused")
        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @Override
        @SuppressWarnings({"DataFlowIssue", "unused"})
        public @NotNull String onTransferContents() {
            String result = super.onTransferContents();
            if (result == null) {
                PacketSender.sendTransposerActivity(host);
            }
            return result;
        }

        @SuppressWarnings("unused")
        @Override
        public int fluidTransferRate() {
            return host.info.fluidTransferRate;
        }
    }

    public static class Upgrade extends Common {
        private final EnvironmentHost host;

        @SuppressWarnings("unused")
        public Upgrade(EnvironmentHost host) {
            this.host = host;
            ((Component) node).setVisibility(Visibility.Neighbors);
        }

        @SuppressWarnings("unused")
        @Override
        public BlockPosition position() {
            return BlockPosition.apply(host);
        }

        @SuppressWarnings("unused")
        @Override
        public int fluidTransferRate() {
            if (host instanceof li.cil.oc.core.impl.common.blockentity.Microcontroller mc) {
                for (int i = 0; i < mc.info.components.size(); i++) {
                    ItemStack stack = mc.info.components.get(i);
                    if (stack != null && ItemStack.isSameItemSameComponents(stack,
                            Items.get(Constants.BlockName.Transposer).createItemStack(1))) {
                        var customData = stack.get(DataComponents.CUSTOM_DATA);
                        if (customData != null && !customData.isEmpty()) {
                            CompoundTag _tag = customData.copyTag();
                            if (_tag.contains(TransposerData.FLUID_TRANSFER_RATE)) {
                                return _tag.getInt(TransposerData.FLUID_TRANSFER_RATE);
                            }
                        }
                        return OCSettings.get().transposerFluidTransferRate;
                    }
                }
            }
            return 0;
        }
    }
}
