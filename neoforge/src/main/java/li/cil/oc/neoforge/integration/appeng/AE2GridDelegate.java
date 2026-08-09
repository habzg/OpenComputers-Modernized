package li.cil.oc.neoforge.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNodeListener;
import appeng.api.networking.IManagedGridNode;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.traits.power.AE2PowerDelegate;
import li.cil.oc.core.impl.common.blockentity.traits.power.Common;
import li.cil.oc.core.impl.integration.util.Power;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public final class AE2GridDelegate implements AE2PowerDelegate {
    private static final IGridNodeListener<BlockEntity> LISTENER = (nodeOwner, node) -> nodeOwner.setChanged();

    private final Map<Common, IManagedGridNode> nodes = new IdentityHashMap<>();

    private static BlockEntity asBE(Common tile) {
        return (BlockEntity) tile;
    }

    private IManagedGridNode getOrCreateNode(Common tile) {
        var existing = nodes.get(tile);
        if (existing != null) return existing;
        var be = asBE(tile);
        var node = GridHelper.createManagedNode(be, LISTENER)
                .setIdlePowerUsage(0.0)
                .setInWorldNode(true)
                .setTagName("ae2power");
        nodes.put(tile, node);
        return node;
    }

    private EnumSet<Direction> computeExposedSides(Common tile) {
        var sides = EnumSet.noneOf(Direction.class);
        for (var side : Direction.values()) {
            if (tile.canConnectPower(side)) {
                sides.add(side);
            }
        }
        return sides;
    }

    @Override
    public void onUpdateEntity(Common block) {
        if (block.isClient()) return;
        var node = nodes.get(block);
        if (node == null) return;
        var be = asBE(block);
        var level = be.getLevel();
        if (level == null) return;
        if (level.getGameTime() % (long) OCSettings.get().tickFrequency != 0) return;
        updateEnergy(block, node);
    }

    private void updateEnergy(Common tile, IManagedGridNode node) {
        var grid = node.getGrid();
        if (grid == null) return;
        var energy = AEUtil.getGridEnergy(grid);
        if (energy == null) return;
        var budget = tile.energyThroughput() * OCSettings.get().tickFrequency;
        for (var side : Direction.values()) {
            var demandAE = Power.toAE(Math.min(budget, tile.globalDemand(side)));
            if (demandAE > 1) {
                var extractedAE = energy.extractAEPower(demandAE, Actionable.MODULATE, PowerMultiplier.CONFIG);
                var extractedOC = Power.fromAE(extractedAE);
                if (extractedOC > 0) {
                    budget -= tile.tryChangeBuffer(side, extractedOC);
                }
            }
        }
    }

    @Override
    public void onValidate(Common block) {
        if (block.isClient()) return;
        var be = asBE(block);
        var level = be.getLevel();
        if (level == null) return;
        getOrCreateNode(block);
        scheduleCreate(block);
    }

    private void scheduleCreate(Common tile) {
        var scheduler = EventHandlerDelegate.get();
        if (scheduler == null) {
            doCreate(tile);
            return;
        }
        scheduler.scheduleServer(() -> {
            var node = nodes.get(tile);
            if (node == null) return;
            doCreate(tile);
        });
    }

    private void doCreate(Common tile) {
        var node = nodes.get(tile);
        if (node == null) return;
        if (node.getNode() != null) return;
        var be = asBE(tile);
        var level = be.getLevel();
        if (level == null) return;
        node.setExposedOnSides(computeExposedSides(tile));
        node.create(level, be.getBlockPos());
    }

    @Override
    public void onInvalidate(Common block) {
        if (block.isClient()) return;
        var node = nodes.remove(block);
        if (node != null) node.destroy();
    }

    @Override
    public void onNeighborChanged(Common block) {
        if (block.isClient()) return;
        var node = nodes.get(block);
        if (node == null) return;
        if (node.isReady() && node.getNode() != null) {
            node.setExposedOnSides(EnumSet.noneOf(Direction.class));
            node.setExposedOnSides(computeExposedSides(block));
        } else {
            doCreate(block);
        }
    }

    @Override
    public void readFromNBT(Common block, CompoundTag nbt) {
        if (block.isClient()) return;
        var node = getOrCreateNode(block);
        node.loadFromNBT(nbt);
    }

    @Override
    public void writeToNBT(Common block, CompoundTag nbt) {
        if (block.isClient()) return;
        var node = nodes.get(block);
        if (node != null) node.saveToNBT(nbt);
    }

    @Override
    public Object getGridNode(Common block, Direction side) {
        var node = nodes.get(block);
        return node != null ? node.getNode() : null;
    }
}
