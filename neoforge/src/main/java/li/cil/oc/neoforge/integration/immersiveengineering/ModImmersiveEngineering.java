package li.cil.oc.neoforge.integration.immersiveengineering;

import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.redstone.CapabilityRedstoneNetwork;
import blusunrize.immersiveengineering.api.wires.redstone.RedstoneNetworkHandler;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.common.init.BlockEntities;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@SuppressWarnings("unused")
public final class ModImmersiveEngineering implements ModProxy, BundledRedstone.RedstoneProvider {
  @Override
  public Mods.ModBase getMod() {
    return Mods.ImmersiveEngineering;
  }

  @Override
  public void initialize() {
    BundledRedstone.addProvider(this);
  }

  public static void registerCapabilities(RegisterCapabilitiesEvent event) {
    var provider = new ImmersiveRedstoneConnection.Provider();
    event.registerBlockEntity(CapabilityRedstoneNetwork.REDSTONE_BUNDLE_CONNECTION, BlockEntities.CASE.get(), provider);
    event.registerBlockEntity(CapabilityRedstoneNetwork.REDSTONE_BUNDLE_CONNECTION, BlockEntities.RACK.get(), provider);
    event.registerBlockEntity(CapabilityRedstoneNetwork.REDSTONE_BUNDLE_CONNECTION, BlockEntities.REDSTONE.get(), provider);
    event.registerBlockEntity(CapabilityRedstoneNetwork.REDSTONE_BUNDLE_CONNECTION, BlockEntities.ROBOT.get(), provider);
    event.registerBlockEntity(CapabilityRedstoneNetwork.REDSTONE_BUNDLE_CONNECTION, BlockEntities.MICROCONTROLLER.get(), provider);
  }

  static int toImmersive(int value) {
    return Math.min(15, (int) Math.round(value / 17.0));
  }

  static int toOpenComputers(int value) {
    return Math.min(255, (int) Math.round(value * 17.0));
  }

  @Override
  public int computeInput(BlockPosition pos, Direction side) {
    return 0;
  }

  @Override
  public int[] computeBundledInput(BlockPosition pos, Direction side) {
    var level = pos.level();
    if (level == null || level.isClientSide) return null;
    var neighbour = pos.offset(side).toBlockPos();
    var net = GlobalWireNetwork.getNetwork(level).getNullableLocalNet(neighbour);
    if (net == null) return null;
    var handler = net.getHandler(RedstoneNetworkHandler.ID, RedstoneNetworkHandler.class);
    if (handler == null) return null;
    var values = handler.getValuesExcluding(new ConnectionPoint(neighbour, 0));
    var result = new int[16];
    for (int color = 0; color < 16; color++) {
      result[color] = toOpenComputers(values[color]);
    }
    return result;
  }
}
