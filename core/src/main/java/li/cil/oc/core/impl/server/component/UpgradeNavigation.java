package li.cil.oc.core.impl.server.component;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.network.WaypointManager;
import li.cil.oc.core.impl.util.BlockPosition;
import org.jetbrains.annotations.NotNull;

public class UpgradeNavigation extends UpgradeNavigationBase {
    public UpgradeNavigation(EnvironmentHost host) {
        super(host);
    }

    @Override
    protected boolean consumeEnergy(double amount) {
        return ((Connector) node).tryChangeBuffer(-amount);
    }

    @Override
    protected @NotNull List<WaypointInfo> queryWaypoints(@NotNull BlockPosition pos, double range) {
        List<WaypointInfo> result = new ArrayList<>();
        WaypointManager.findWaypoints(pos, range).forEach(wpd ->
                result.add(new WaypointInfo(wpd.position(), wpd.facing(), wpd.maxInput(), wpd.label(), wpd.node().address())));
        return result;
    }
}
