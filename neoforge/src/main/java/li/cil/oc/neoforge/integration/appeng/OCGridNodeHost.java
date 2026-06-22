package li.cil.oc.neoforge.integration.appeng;

import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.util.AECableType;
import li.cil.oc.core.impl.common.tileentity.traits.power.AppliedEnergistics2;

public final class OCGridNodeHost implements IInWorldGridNodeHost {
    private final AppliedEnergistics2 tile;

    public OCGridNodeHost(AppliedEnergistics2 tile) {
        this.tile = tile;
    }

    @Override
    public IGridNode getGridNode(net.minecraft.core.Direction side) {
        var node = tile.getGridNode(side);
        return node instanceof IGridNode gridNode ? gridNode : null;
    }

    @Override
    public AECableType getCableConnectionType(net.minecraft.core.Direction side) {
        return AECableType.SMART;
    }
}
