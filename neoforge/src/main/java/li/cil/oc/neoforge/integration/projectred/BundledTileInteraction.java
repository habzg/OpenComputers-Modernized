package li.cil.oc.neoforge.integration.projectred;

import li.cil.oc.core.impl.common.tileentity.traits.BundledRedstoneAware;
import mrtjp.projectred.api.IBundledTileInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public final class BundledTileInteraction implements IBundledTileInteraction {
    @Override
    public boolean isValidInteractionFor(Level world, @NotNull BlockPos pos, @NotNull Direction side) {
        return world.getBlockEntity(pos) instanceof BundledRedstoneAware;
    }

    @Override
    public boolean canConnectBundled(Level world, @NotNull BlockPos pos, @NotNull Direction side) {
        var te = world.getBlockEntity(pos);
        if (te instanceof BundledRedstoneAware aware) {
            return aware.isOutputEnabled();
        }
        return false;
    }

    @Override
    public byte[] getBundledSignal(Level world, @NotNull BlockPos pos, @NotNull Direction side) {
        var te = world.getBlockEntity(pos);
        if (te instanceof BundledRedstoneAware aware) {
            var localSide = aware.toLocal(side);
            var output = aware.bundledOutput();
            if (output == null || localSide.ordinal() >= output.length) return null;
            var colors = output[localSide.ordinal()];
            if (colors == null) return null;
            var signal = new byte[16];
            for (int i = 0; i < 16 && i < colors.length; i++) {
                signal[i] = (byte) Math.clamp(colors[i], 0, 255);
            }
            return signal;
        }
        return null;
    }
}
