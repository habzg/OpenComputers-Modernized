package li.cil.oc.neoforge.integration.computercraft;

import dan200.computercraft.api.ComputerCraftAPI;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.Direction;

@SuppressWarnings("unused")
public final class BundledRedstoneProvider implements BundledRedstone.RedstoneProvider {
    public static final BundledRedstoneProvider INSTANCE = new BundledRedstoneProvider();

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        return 0;
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        var level = pos.level();
        if (level == null) return null;
        var neighborPos = pos.offset(side).toBlockPos();
        var result = ComputerCraftAPI.getBundledRedstoneOutput(level, neighborPos, side.getOpposite());
        if (result <= 0) return null;
        int[] out = new int[16];
        for (int i = 0; i < 16; i++) {
            if ((result & (1 << i)) != 0) out[i] = 255;
        }
        return out;
    }
}
