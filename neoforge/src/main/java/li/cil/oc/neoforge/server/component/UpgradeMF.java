package li.cil.oc.neoforge.server.component;

import java.util.function.Consumer;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.impl.server.component.UpgradeMFBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.common.event.BlockChangeHandler;
import li.cil.oc.neoforge.common.event.BlockChangeHandler.ChangeListener;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class UpgradeMF extends UpgradeMFBase implements ChangeListener {
    public UpgradeMF(EnvironmentHost host, BlockPosition coord, Direction dir) {
        super(host, coord, dir);
    }

    @Override
    protected void connectToTileNode(@NotNull BlockEntity tile, @NotNull Consumer<Node> consumer) {
        Node otherNode = li.cil.oc.core.impl.server.network.Network.getNetworkNode(tile, dir);
        if (otherNode != null) {
            consumer.accept(otherNode);
        }
    }

    @Override
    protected void registerBlockChangeListener() {
        BlockChangeHandler.addListener(this, coord);
    }

    @Override
    protected void unregisterBlockChangeListener() {
        BlockChangeHandler.removeListener(this);
    }

    @Override
    protected boolean consumeEnergy(double amount) {
        return ((Connector) node).tryChangeBuffer(-amount);
    }

    @Override
    public void onBlockChanged() {
        updateBoundState();
    }
}
