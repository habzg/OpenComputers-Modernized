package li.cil.oc.neoforge.integration.appmek;

import appeng.api.networking.security.IActionHost;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import li.cil.oc.neoforge.integration.appeng.AEUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class DriverController extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return AEUtil.controllerClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment(world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BlockEntity>
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
            return (IActionHost) getTileEntity();
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
