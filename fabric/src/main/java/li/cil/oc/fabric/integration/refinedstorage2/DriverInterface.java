package li.cil.oc.fabric.integration.refinedstorage2;

import com.refinedmods.refinedstorage.common.iface.InterfaceBlockEntity;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class DriverInterface extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return RS2Util.interfaceClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<InterfaceBlockEntity>
            implements NamedBlock, NetworkControl {
        public Environment(BlockEntity tile) {
            super((InterfaceBlockEntity) tile, "rs_interface");
        }

        @Override
        public String preferredName() {
            return "rs_interface";
        }

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public BlockEntity tile() {
            return getBlockEntity();
        }

        @Override
        public Node node() {
            return super.node();
        }

        @Callback(doc = "function([slot:number]):table -- Get the configuration of the interface.")
        public Object[] getInterfaceConfiguration(Context context, Arguments args) {
            return ConfigHelper.getInterfaceConfiguration(getBlockEntity(), args);
        }

        @Callback(doc = "function([slot:number][, database:address, entry:number]):boolean -- Configure the interface.")
        public Object[] setInterfaceConfiguration(Context context, Arguments args) {
            return ConfigHelper.setInterfaceConfiguration(getBlockEntity(), args, node());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (RS2Util.isInterface(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}