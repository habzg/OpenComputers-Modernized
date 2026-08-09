package li.cil.oc.neoforge.integration.appmek;

import appeng.api.networking.security.IActionHost;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.neoforge.integration.appeng.AEUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class DriverController extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return AEUtil.controllerClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment(world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<BlockEntity>
            implements NamedBlock, ChemicalNetworkControl {
        public Environment(BlockEntity tile) {
            super(tile, "me_controller");
        }

        @Override
        public String preferredName() {
            return "me_controller";
        }

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public IActionHost tile() {
            return (IActionHost) getBlockEntity();
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (AEUtil.isController(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
