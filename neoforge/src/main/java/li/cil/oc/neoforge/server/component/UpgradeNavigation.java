package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeNavigationBase;
import li.cil.oc.core.impl.server.component.WaypointInfo;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.server.network.Waypoints;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

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
        Waypoints.findWaypoints(pos, range).forEach(wpd ->
                result.add(new WaypointInfo(wpd.position(), wpd.facing(), wpd.maxInput(), wpd.label(), wpd.node().address())));
        return result;
    }
}
