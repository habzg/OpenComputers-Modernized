package li.cil.oc.neoforge.integration.enderio;

import com.enderio.enderio.api.conduits.Conduit;
import com.enderio.enderio.api.conduits.ConduitRedstoneSignalAware;
import com.enderio.enderio.api.conduits.connection.ConnectionStatus;
import com.enderio.enderio.content.conduits.bundle.ConduitBundleBlockEntity;
import com.enderio.enderio.content.conduits.type.redstone.RedstoneConduitConnectionConfig;
import com.enderio.enderio.content.conduits.type.redstone.RedstoneConduitNetworkContext;
import com.enderio.enderio.init.EIOConduitTypes;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public final class ModEnderIO implements ModProxy, BundledRedstone.RedstoneProvider {
    @Override
    public Mods.ModBase getMod() {
        return Mods.EnderIO;
    }

    @Override
    public void initialize() {
        BundledRedstone.addProvider(this);

        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.enderio.EventHandlerEnderIO.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.enderio.EventHandlerEnderIO.isWrench");
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        return 0;
    }

    @Override
    @SuppressWarnings("UnstableApiUsage")
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        var level = pos.level();
        if (level == null) return null;
        var conduitPos = pos.offset(side).toBlockPos();
        BlockEntity be = level.getBlockEntity(conduitPos);
        if (!(be instanceof ConduitBundleBlockEntity conduit)) return null;

        Holder<Conduit<?, ?>> redstoneConduit =
                conduit.getConduitByType(EIOConduitTypes.REDSTONE.get());
        if (redstoneConduit == null) return null;

        var conduitSide = side.getOpposite();
        ConnectionStatus status = conduit.getConnectionStatus(redstoneConduit, conduitSide);
        if (!status.isEndpoint()) return null;

        var config = conduit.getConnectionConfig(redstoneConduit, conduitSide, RedstoneConduitConnectionConfig.TYPE);
        if (!config.canInsert(ConduitRedstoneSignalAware.NONE)) return null;

        var node = conduit.getConduitNode(redstoneConduit);
        var network = node.getNetwork();
        var context = network.getContext(RedstoneConduitNetworkContext.TYPE);
        if (context == null) return null;

        int[] result = new int[16];
        for (DyeColor color : DyeColor.values()) {
            result[color.getId()] = context.isActive(color) ? 255 : 0;
        }
        return result;
    }
}
