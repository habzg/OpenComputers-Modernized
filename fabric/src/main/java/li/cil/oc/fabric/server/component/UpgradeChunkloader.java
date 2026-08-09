package li.cil.oc.fabric.server.component;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeChunkloaderBase;
import li.cil.oc.fabric.common.event.ChunkloaderUpgradeHandler;

public class UpgradeChunkloader extends UpgradeChunkloaderBase {
    public UpgradeChunkloader(EnvironmentHost host) {
        super(host);
    }

    @Override
    protected boolean consumeEnergy(double amount) {
        return ((Connector) node).tryChangeBuffer(-amount);
    }

    @Override
    protected void onChunkTicketActive() {
        ChunkloaderUpgradeHandler.updateLoadedChunk(this);
    }

    @Override
    protected void onChunkTicketInactive() {
        ChunkloaderUpgradeHandler.releaseLoadedChunk(this);
    }
}
