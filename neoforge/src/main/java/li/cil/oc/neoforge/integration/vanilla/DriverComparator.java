package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;

@SuppressWarnings("unused")
public final class DriverComparator extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return ComparatorBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((ComparatorBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<ComparatorBlockEntity> implements NamedBlock {
        public Environment(ComparatorBlockEntity BlockEntity) {
            super(BlockEntity, "comparator");
        }

        @Override
        public String preferredName() {
            return "comparator";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the strength of the comparators output signal.")
        public Object[] getOutputSignal(Context context, Arguments args) {
            return ResultWrapper.result(BlockEntity.getOutputSignal());
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && stack.getItem() == Items.COMPARATOR) {
                return Environment.class;
            }
            return null;
        }
    }
}
