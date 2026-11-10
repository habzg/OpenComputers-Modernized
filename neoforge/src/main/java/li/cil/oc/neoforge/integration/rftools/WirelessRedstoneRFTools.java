package li.cil.oc.neoforge.integration.rftools;

import li.cil.oc.core.impl.integration.util.WirelessRedstone;
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
    rs.setWirelessInputValue(WirelessRedstone.getInput(rs));
  }

  @Override
  public void removeReceiver(RedstoneWireless rs) {
    rs.setWirelessInputValue(false);
  }

  @Override
  public void updateOutput(RedstoneWireless rs) {
    int frequency = rs.getFreq();
    if (frequency <= 0) return;
    Level level = rs.redstone().level();
    if (level == null || level.isClientSide()) return;
    RedstoneChannels channels = RedstoneChannels.getChannels(level);
    RedstoneChannels.RedstoneChannel ch = channels.getOrCreateChannel(frequency);
    ch.setValue(rs.getWirelessOutputValue() ? 15 : 0);
    channels.save();
  }

  @Override
  public void removeTransmitter(RedstoneWireless rs) {
    if (!rs.getWirelessOutputValue()) return;
    int frequency = rs.getFreq();
    if (frequency <= 0) return;
    Level level = rs.redstone().level();
    if (level == null || level.isClientSide()) return;
    RedstoneChannels channels = RedstoneChannels.getChannels(level);
    RedstoneChannels.RedstoneChannel ch = channels.getChannel(frequency);
    if (ch != null) {
      ch.setValue(0);
      channels.save();
    }
  }

  @Override
  public boolean getInput(RedstoneWireless rs) {
    int frequency = rs.getFreq();
    if (frequency <= 0) return false;
    Level level = rs.redstone().level();
    if (level == null || level.isClientSide()) return false;
    RedstoneChannels channels = RedstoneChannels.getChannels(level);
    RedstoneChannels.RedstoneChannel ch = channels.getChannel(frequency);
    return ch != null && ch.getValue() > 0;
  }

  @Override
  public boolean canHandleFrequency(int frequency) {
    return true;
  }
}
