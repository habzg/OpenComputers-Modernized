package li.cil.oc.neoforge.integration.refinedstorage2;

import com.refinedmods.refinedstorage.api.network.impl.node.exporter.ExporterNetworkNode;
import com.refinedmods.refinedstorage.common.exporter.AbstractExporterBlockEntity;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class DriverExporter extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return RS2Util.exporterClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<AbstractExporterBlockEntity>
            implements NamedBlock {
        public Environment(BlockEntity tile) {
            super((AbstractExporterBlockEntity) tile, "rs_exporter");
        }

        @Override
        public String preferredName() {
            return "rs_exporter";
        }

        @Override
        public int priority() {
            return 2;
        }

        @Override
        public Node node() {
            return super.node();
        }

        @Callback(doc = "function([slot:number]):boolean -- Make the exporter perform a single export operation immediately.")
        public Object[] exportIntoSlot(Context context, Arguments args) {
            for (var nodeContainer : getBlockEntity().getContainerProvider().getContainers()) {
                if (nodeContainer.getNode() instanceof ExporterNetworkNode node) {
                    node.doWork();
                    return ResultWrapper.result(true);
                }
            }
            return ResultWrapper.result(false, "no exporter node");
        }

        @Callback(doc = "function([slot:number]):table -- Get the configuration of the exporter.")
        public Object[] getExportConfiguration(Context context, Arguments args) {
            return ConfigHelper.getExportConfiguration(getBlockEntity(), args);
        }

        @Callback(doc = "function([slot:number][, database:address, entry:number]):boolean -- Configure the exporter.")
        public Object[] setExportConfiguration(Context context, Arguments args) {
            return ConfigHelper.setExportConfiguration(getBlockEntity(), args, node());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (RS2Util.isExporter(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
