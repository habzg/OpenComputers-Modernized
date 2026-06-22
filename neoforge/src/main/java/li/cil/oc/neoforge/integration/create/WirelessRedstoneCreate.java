package li.cil.oc.neoforge.integration.create;

import com.simibubi.create.Create;
import com.simibubi.create.content.redstone.link.IRedstoneLinkable;
import com.simibubi.create.content.redstone.link.RedstoneLinkNetworkHandler.Frequency;
import li.cil.oc.core.impl.integration.util.WirelessRedstone.WirelessRedstoneSystem;
import li.cil.oc.core.impl.server.component.RedstoneWireless;
import li.cil.oc.neoforge.common.block.ChameliumBlock;
import li.cil.oc.neoforge.common.init.Items;
import net.createmod.catnip.data.Couple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.LevelAccessor;

import java.util.IdentityHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public final class WirelessRedstoneCreate implements WirelessRedstoneSystem {
    private final Map<RedstoneWireless, CreateLinkable> linkables = new IdentityHashMap<>();

    @Override
    public String name() {
        return "create";
    }

    @Override
    public void addReceiver(RedstoneWireless rs) {
        CreateLinkable linkable = linkables.get(rs);
        if (linkable != null) return;
        linkable = new CreateLinkable(rs);
        linkables.put(rs, linkable);
        LevelAccessor level = rs.redstone().level();
        if (level != null && !level.isClientSide()) {
            Create.REDSTONE_LINK_NETWORK_HANDLER.addToNetwork(level, linkable);
        }
    }

    @Override
    public void removeReceiver(RedstoneWireless rs) {
        CreateLinkable linkable = linkables.remove(rs);
        if (linkable != null) {
            LevelAccessor level = rs.redstone().level();
            if (level != null && !level.isClientSide()) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.removeFromNetwork(level, linkable);
            }
        }
    }

    @Override
    public void updateOutput(RedstoneWireless rs) {
        CreateLinkable linkable = linkables.get(rs);
        if (linkable != null) {
            LevelAccessor level = rs.redstone().level();
            if (level != null && !level.isClientSide()) {
                Create.REDSTONE_LINK_NETWORK_HANDLER.updateNetworkOf(level, linkable);
            }
        }
    }

    @Override
    public void removeTransmitter(RedstoneWireless rs) {
        removeReceiver(rs);
    }

    @Override
    public boolean getInput(RedstoneWireless rs) {
        CreateLinkable linkable = linkables.get(rs);
        return linkable != null && linkable.receivedStrength > 0;
    }

    @Override
    public void resetRedstone(RedstoneWireless rs) {
        CreateLinkable linkable = linkables.get(rs);
        if (linkable != null) {
            linkable.receivedStrength = 0;
        }
    }

    @Override
    public boolean canHandleFrequency(int frequency) {
        return (frequency & 0xFF) == frequency;
    }

    private static final class CreateLinkable implements IRedstoneLinkable {
        private final RedstoneWireless rs;
        private volatile int receivedStrength = 0;

        CreateLinkable(RedstoneWireless rs) {
            this.rs = rs;
        }

        @Override
        public int getTransmittedStrength() {
            return rs.getWirelessOutputValue() ? 15 : 0;
        }

        @Override
        public void setReceivedStrength(int power) {
            this.receivedStrength = Math.clamp(power, 0, 15);
        }

        @Override
        public boolean isListening() {
            return true;
        }

        @Override
        public boolean isAlive() {
            return rs.redstone().level() != null;
        }

        @Override
        public Couple<Frequency> getNetworkKey() {
            int freq = rs.getFreq();
            DyeColor slot1Color = ChameliumBlock.dyeColorFromFrequency(freq);
            ItemStack first = new ItemStack(Items.CHAMELIUM_BLOCK.get());
            first.set(DataComponents.DYED_COLOR, new DyedItemColor(slot1Color.getTextColor(), false));
            Frequency slot1 = Frequency.of(first);
            Frequency slot2 = Frequency.EMPTY;
            if (freq >= 16) {
                DyeColor slot2Color = ChameliumBlock.dyeColorFromFrequency(freq >> 4);
                ItemStack second = new ItemStack(Items.CHAMELIUM_BLOCK.get());
                second.set(DataComponents.DYED_COLOR, new DyedItemColor(slot2Color.getTextColor(), false));
                slot2 = Frequency.of(second);
            }
            return Couple.create(slot1, slot2);
        }

        @Override
        public BlockPos getLocation() {
            return BlockPos.containing(
                    rs.redstone().xPosition(),
                    rs.redstone().yPosition(),
                    rs.redstone().zPosition()
            );
        }
    }
}
