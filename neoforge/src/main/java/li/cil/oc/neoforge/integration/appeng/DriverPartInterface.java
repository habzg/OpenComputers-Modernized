package li.cil.oc.neoforge.integration.appeng;

import appeng.api.networking.security.IActionHost;
import appeng.api.parts.IPartHost;
import appeng.parts.misc.InterfacePart;
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
public class DriverPartInterface extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return IPartHost.class;
    }

    @Override
    public boolean worksWith(Level world, BlockPos pos, Direction side) {
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IPartHost host) {
            for (Direction dir : Direction.values()) {
                if (host.getPart(dir) instanceof InterfacePart) return true;
            }
        }
        return false;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((IPartHost) world.getBlockEntity(pos), side);
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<IPartHost>
            implements NamedBlock, NetworkControl<IActionHost>, PartEnvironmentBase {
        private final Direction aeSide;

        public Environment(IPartHost host, Direction side) {
            super(host, "me_interface");
            this.aeSide = side.getOpposite();
        }

        @Override
        public IPartHost partHost() {
            return getBlockEntity();
        }

        @Override
        public String preferredName() {
            return "me_interface";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Override
        public IActionHost tile() {
            var host = getBlockEntity();
            var part = host.getPart(aeSide);
            if (part instanceof IActionHost actionHost) {
                return actionHost;
            }
            return null;
        }

        @Override
        public Node node() {
            return super.node();
        }

        @Callback(doc = "function(side:number[, slot:number]):table -- Get the configuration of the interface pointing in the specified direction.")
        public Object[] getInterfaceConfiguration(Context context, Arguments args) {
            return getPartConfig(context, args);
        }

        @Callback(doc = "function(side:number[, slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface pointing in the specified direction.")
        public Object[] setInterfaceConfiguration(Context context, Arguments args) {
            return setPartConfig(context, args);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (AEUtil.isPartInterface(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
