package li.cil.oc.neoforge.integration.projectred;

import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import mrtjp.projectred.api.ProjectRedAPI;
import net.minecraft.core.Direction;

@SuppressWarnings("unused")
public final class ModProjectRed implements ModProxy, BundledRedstone.RedstoneProvider {
    @Override
    public Mods.ModBase getMod() {
        return Mods.ProjectRedTransmission;
    }

    @Override
    public void initialize() {
        BundledRedstone.addProvider(this);

        var api = ProjectRedAPI.transmissionAPI;
        if (api != null) {
            api.registerBundledTileInteraction(new BundledTileInteraction());
        }

        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.projectred.EventHandlerProjectRed.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.projectred.EventHandlerProjectRed.isWrench");
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        return 0;
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        var api = ProjectRedAPI.transmissionAPI;
        if (api == null) return null;
        var level = pos.level();
        if (level == null) return null;
        var blockPos = new net.minecraft.core.BlockPos(pos.x(), pos.y(), pos.z());
        var signal = api.getBundledInput(level, blockPos, side);
        if (signal == null) return null;
        var result = new int[signal.length];
        for (int i = 0; i < signal.length; i++) {
            result[i] = signal[i] & 0xFF;
        }
        return result;
    }
}
