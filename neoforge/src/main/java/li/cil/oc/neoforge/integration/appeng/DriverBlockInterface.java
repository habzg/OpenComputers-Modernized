package li.cil.oc.neoforge.integration.appeng;

import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.misc.InterfaceBlockEntity;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public class DriverBlockInterface extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return AEUtil.interfaceClass();
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((InterfaceBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<InterfaceBlockEntity>
            implements NamedBlock, NetworkControl<InterfaceBlockEntity> {
        public Environment(InterfaceBlockEntity tile) {
            super(tile, "me_interface");
        }

        @Override
        public String preferredName() {
            return "me_interface";
        }

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public InterfaceBlockEntity tile() {
            return getTileEntity();
        }

        @Override
        public Node node() {
            return super.node();
        }

        @Callback(doc = "function([slot:number]):table -- Get the configuration of the interface.")
        public Object[] getInterfaceConfiguration(Context context, Arguments args) {
            var config = tile().getConfig();
            int slot = Math.max(0, args.optInteger(0, 1) - 1);
            if (slot >= config.size()) return ResultWrapper.result(ItemStack.EMPTY);
            var stack = config.getStack(slot);
            if (stack != null && stack.what() instanceof AEItemKey itemKey) {
                return ResultWrapper.result(itemKey.toStack((int) stack.amount()));
            }
            return ResultWrapper.result(ItemStack.EMPTY);
        }

        @Callback(doc = "function([slot:number][, database:address, entry:number[, size:number]]):boolean -- Configure the interface.")
        public Object[] setInterfaceConfiguration(Context context, Arguments args) {
            var config = tile().getConfig();
            int slot;
            int valOffset;
            if (args.isInteger(0)) {
                slot = args.checkInteger(0) - 1;
                valOffset = 1;
            } else {
                slot = 0;
                valOffset = 0;
            }
            if (slot < 0 || slot >= config.size()) {
                throw new IllegalArgumentException("invalid slot");
            }
            ItemStack stack;
            if (args.count() > 1) {
                stack = DatabaseAccess.getStackFromDatabase(node(), args, valOffset);
            } else {
                stack = ItemStack.EMPTY;
            }
            if (stack != null && !stack.isEmpty()) {
                config.setStack(slot, GenericStack.fromItemStack(stack));
            } else {
                config.setStack(slot, null);
            }
            context.pause(0.5);
            return ResultWrapper.result(true);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (AEUtil.isBlockInterface(stack)) {
                return Environment.class;
            }
            return null;
        }
    }
}
