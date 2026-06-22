package li.cil.oc.neoforge.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.networking.security.IActionSource;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.core.definitions.AEItems;
import appeng.helpers.IConfigInvHost;
import appeng.parts.automation.ExportBusPart;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

@SuppressWarnings("unused")
public class DriverExportBus extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return IPartHost.class;
    }

    @Override
    public boolean worksWith(Level world, int x, int y, int z, Direction side) {
        BlockEntity tile = world.getBlockEntity(new BlockPos(x, y, z));
        if (tile instanceof IPartHost host) {
            for (Direction dir : Direction.values()) {
                if (host.getPart(dir) instanceof ExportBusPart) return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<IPartHost> implements NamedBlock, PartEnvironmentBase {
        public Environment(IPartHost host) {
            super(host, "me_exportbus");
        }

        @Override
        public IPartHost partHost() {
            return getTileEntity();
        }

        @Override
        public String preferredName() {
            return "me_exportbus";
        }

        @Override
        public int priority() {
            return 2;
        }

        @Callback(doc = "function(side:number[, slot:number]):boolean -- Get the configuration of the export bus pointing in the specified direction.")
        public Object[] getExportConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the export bus pointing in the specified direction.")
        public Object[] setExportConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args);
        }

        private long doExport(MEStorage storage, AEItemKey key, long amount, IItemHandler inventory, int targetSlot, boolean hasTargetSlot, IActionSource source, boolean simulate) {
            long limit = Math.min(amount, Integer.MAX_VALUE);
            var stack = key.toStack((int) limit);
            int original = stack.getCount();
            if (hasTargetSlot && targetSlot >= 0 && targetSlot < inventory.getSlots()) {
                stack = inventory.insertItem(targetSlot, stack, simulate);
            } else {
                for (int i = 0; i < inventory.getSlots() && !stack.isEmpty(); i++) {
                    stack = inventory.insertItem(i, stack, simulate);
                }
            }
            long canInsert = original - stack.getCount();
            if (canInsert <= 0) return 0;
            long extracted = storage.extract(key, canInsert, simulate ? Actionable.SIMULATE : Actionable.MODULATE, source);
            if (extracted <= 0) return 0;
            return canInsert;
        }

        @Callback(doc = "function(side:number, [slot:number]):boolean -- Make the export bus facing the specified direction perform a single export operation into the specified slot.")
        public Object[] exportIntoSlot(Context context, Arguments args) {
            Direction side = ExtendedArguments.checkSideAny(args, 0);
            IPart part = getTileEntity().getPart(side);
            if (!(part instanceof ExportBusPart exportBus)) {
                return ResultWrapper.result(null, "no export bus");
            }

            var host = getTileEntity();
            if (!(host instanceof BlockEntity be)) return ResultWrapper.result(null, "no block entity");
            var pos = be.getBlockPos();
            var level = be.getLevel();
            if (level == null) return ResultWrapper.result(null, "no level");

            var targetPos = pos.relative(side);
            var targetInv = level.getCapability(Capabilities.ItemHandler.BLOCK, targetPos, side.getOpposite());
            if (targetInv == null) return ResultWrapper.result(null, "no inventory");

            int targetSlot = args.optInteger(1, -1) - 1;
            boolean hasTargetSlot = args.isInteger(1) && targetSlot >= 0;

            var node = exportBus.getActionableNode();
            if (node == null) return ResultWrapper.result(null, "no grid");
            var grid = node.getGrid();
            if (grid == null) return ResultWrapper.result(null, "no grid");
            var storage = AEUtil.getGridStorage(grid);
            var source = new MachineSource(exportBus);

            int speedCount = exportBus.getInstalledUpgrades(AEItems.SPEED_CARD);
            int count = switch (speedCount) {
                case 1 -> 8;
                case 2 -> 32;
                case 3 -> 64;
                case 4 -> 96;
                default -> 1;
            };
            boolean hasFuzzy = exportBus.getInstalledUpgrades(AEItems.FUZZY_CARD) > 0;
            FuzzyMode fuzzyMode = exportBus.getConfigManager().getSetting(Settings.FUZZY_MODE);
            int potentialWork = count;

            var config = ((IConfigInvHost) exportBus).getConfig();
            var all = new KeyCounter();
            storage.getAvailableStacks(all);

            for (int slot = 0; slot < config.size() && count > 0; slot++) {
                var filterStack = config.getStack(slot);
                if (filterStack == null) continue;
                AEKey filterKey = filterStack.what();
                if (!(filterKey instanceof AEItemKey filterItemKey)) continue;

                if (hasFuzzy) {
                    var matches = all.findFuzzy(filterKey, fuzzyMode);
                    for (var entry : matches) {
                        if (count <= 0) break;
                        if (!(entry.getKey() instanceof AEItemKey itemKey)) continue;
                        long available = entry.getLongValue();
                        if (available <= 0) continue;
                        long toExport = Math.min(available, count);
                        long simulated = doExport(storage, itemKey, toExport, targetInv, targetSlot, hasTargetSlot, source, true);
                        if (simulated > 0) {
                            long exported = doExport(storage, itemKey, toExport, targetInv, targetSlot, hasTargetSlot, source, false);
                            if (exported > 0) {
                                count -= (int) exported;
                                context.pause(0.25);
                            }
                        }
                    }
                } else {
                    long available = all.get(filterKey);
                    if (available <= 0) continue;
                    long toExport = Math.min(available, count);
                    long simulated = doExport(storage, filterItemKey, toExport, targetInv, targetSlot, hasTargetSlot, source, true);
                    if (simulated > 0) {
                        long exported = doExport(storage, filterItemKey, toExport, targetInv, targetSlot, hasTargetSlot, source, false);
                        if (exported > 0) {
                            count -= (int) exported;
                            context.pause(0.25);
                        }
                    }
                }
            }

            if (potentialWork == count) {
                return ResultWrapper.result(null, "no items moved");
            }
            return ResultWrapper.result(potentialWork - count);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (AEUtil.isExportBus(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
