package li.cil.oc.neoforge.integration.appeng;

import appeng.api.parts.IPartHost;
import appeng.parts.automation.ImportBusPart;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class DriverImportBus extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return IPartHost.class;
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IPartHost host) {
            for (Direction dir : Direction.values()) {
                if (host.getPart(dir) instanceof ImportBusPart) return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IPartHost> implements NamedBlock, PartEnvironmentBase {
        public Environment(IPartHost host) {
            super(host, "me_importbus");
        }

        @Override
        public IPartHost partHost() {
            return getBlockEntity();
        }

        @Override
        public String preferredName() {
            return "me_importbus";
        }

        @Override
        public int priority() {
            return 1;
        }

        @Callback(doc = "function(side:number[, slot:number]):boolean -- Get the configuration of the import bus pointing in the specified direction.")
        public Object[] getImportConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number]):boolean -- Configure the import bus pointing in the specified direction to import item stacks matching the specified descriptor.")
        public Object[] setImportConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (AEUtil.isImportBus(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
