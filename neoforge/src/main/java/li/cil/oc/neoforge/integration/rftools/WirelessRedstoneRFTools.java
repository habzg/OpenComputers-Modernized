package li.cil.oc.neoforge.integration.rftools;

import li.cil.oc.core.impl.integration.util.WirelessRedstone.WirelessRedstoneSystem;
import li.cil.oc.core.impl.server.component.RedstoneWireless;
import mcjty.rftoolsutility.modules.logic.tools.RedstoneChannels;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public final class WirelessRedstoneRFTools implements WirelessRedstoneSystem {
    @Override
    public String name() {
        return "rftools";
    }

    @Override
    public void addReceiver(RedstoneWireless rs) {
    }

    @Override
    public void removeReceiver(RedstoneWireless rs) {
    }

    @Override
    public void updateOutput(RedstoneWireless rs) {
        Level level = rs.redstone().level();
        if (level == null || level.isClientSide()) return;
        RedstoneChannels channels = RedstoneChannels.getChannels(level);
        RedstoneChannels.RedstoneChannel ch = channels.getOrCreateChannel(rs.getFreq());
        ch.setValue(rs.getWirelessOutputValue() ? 15 : 0);
        channels.save();
    }

    @Override
    public void removeTransmitter(RedstoneWireless rs) {
        Level level = rs.redstone().level();
        if (level == null || level.isClientSide()) return;
        RedstoneChannels channels = RedstoneChannels.getChannels(level);
        RedstoneChannels.RedstoneChannel ch = channels.getChannel(rs.getFreq());
        if (ch != null) {
            ch.setValue(0);
            channels.save();
        }
    }

    @Override
    public boolean getInput(RedstoneWireless rs) {
        Level level = rs.redstone().level();
        if (level == null || level.isClientSide()) return false;
        RedstoneChannels channels = RedstoneChannels.getChannels(level);
        RedstoneChannels.RedstoneChannel ch = channels.getChannel(rs.getFreq());
        return ch != null && ch.getValue() > 0;
    }

    @Override
    public void resetRedstone(RedstoneWireless rs) {
    }

    @Override
    public boolean canHandleFrequency(int frequency) {
        return true;
    }
}
