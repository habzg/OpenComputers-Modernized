package li.cil.oc.neoforge.integration.morered;

import commoble.morered.api.ChanneledPowerSupplier;
import li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public final class ChanneledPower implements ChanneledPowerSupplier {
    private final BundledRedstoneAware redstone;
    private final Direction side;

    public ChanneledPower(final BundledRedstoneAware redstone, final Direction side) {
        this.redstone = redstone;
        this.side = side;
    }

    @Override
    public int getPowerOnChannel(@NotNull Level world, @NotNull BlockPos wirePos, @NotNull BlockState wireState, Direction wireFace, int channel) {
        if (side == null) {
            return 0;
        }

        return Math.min(31, (int) Math.round(redstone.getBundledOutput(side, channel) * 2 / 17.0));
    }
}