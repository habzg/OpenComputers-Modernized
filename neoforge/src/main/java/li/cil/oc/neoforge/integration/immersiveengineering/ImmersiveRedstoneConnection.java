package li.cil.oc.neoforge.integration.immersiveengineering;

import blusunrize.immersiveengineering.api.wires.redstone.CapabilityRedstoneNetwork.RedstoneBundleConnection;
import li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.ICapabilityProvider;

import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;
import org.jetbrains.annotations.NotNull;

public final class ImmersiveRedstoneConnection extends RedstoneBundleConnection {
  private final WeakReference<BundledRedstoneAware> redstone;
  private final Direction side;
  private final int[] emitted = new int[16];

  ImmersiveRedstoneConnection(BundledRedstoneAware redstone, Direction side) {
    this.redstone = new WeakReference<>(redstone);
    this.side = side;
  }

  @Override
  public void updateInput(byte[] signals, Direction side) {
    var redstone = this.redstone.get();
    if (redstone == null) return;
    boolean enabled = redstone.isOutputEnabled();
    for (int color = 0; color < 16; color++) {
      int value = enabled ? ModImmersiveEngineering.toImmersive(redstone.getBundledOutput(this.side, color)) : 0;
      emitted[color] = value;
      if (value > signals[color]) signals[color] = (byte) value;
    }
  }

  @Override
  public void onChange(byte[] externalInputs, Direction side) {
    var redstone = this.redstone.get();
    if (redstone == null) return;
    redstone.checkRedstoneInputChanged();
  }

  @Override
  public boolean pollDirty() {
    if (super.pollDirty()) return true;
    var redstone = this.redstone.get();
    if (redstone == null) return false;
    boolean enabled = redstone.isOutputEnabled();
    for (int color = 0; color < 16; color++) {
      int value = enabled ? ModImmersiveEngineering.toImmersive(redstone.getBundledOutput(this.side, color)) : 0;
      if (value != emitted[color]) return true;
    }
    return false;
  }

  public static final class Provider implements ICapabilityProvider<BlockEntity, Direction, RedstoneBundleConnection> {
    private final Map<BlockEntity, EnumMap<Direction, ImmersiveRedstoneConnection>> connections = new WeakHashMap<>();

    @Override
    public RedstoneBundleConnection getCapability(@NotNull BlockEntity blockEntity, Direction side) {
      if (side == null) return null;
      if (!(blockEntity instanceof BundledRedstoneAware redstone)) return null;
      synchronized (connections) {
        var perSide = connections.computeIfAbsent(blockEntity, key -> new EnumMap<>(Direction.class));
        return perSide.computeIfAbsent(side, key -> new ImmersiveRedstoneConnection(redstone, key));
      }
    }
  }
}
