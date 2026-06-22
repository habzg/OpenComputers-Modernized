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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

@SuppressWarnings("unused")
public final class DriverCommandBlock extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return CommandBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((CommandBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<CommandBlockEntity> implements NamedBlock {
        public Environment(CommandBlockEntity BlockEntity) {
            super(BlockEntity, "command_block");
        }

        @Override
        public String preferredName() {
            return "command_block";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(direct = true, doc = "function():string -- Get the command currently set in this command block.")
        public Object[] getCommand(Context context, Arguments args) {
            return ResultWrapper.result(BlockEntity.getCommandBlock().getCommand());
        }

        @Callback(doc = "function(value:string) -- Set the specified command for the command block.")
        public Object[] setCommand(Context context, Arguments args) {
            BlockEntity.getCommandBlock().setCommand(args.checkString(0));
            BlockEntity.setChanged();
            return ResultWrapper.result(true);
        }

        @Callback(doc = "function():number -- Execute the currently set command. This has a slight delay to allow the command block to properly update.")
        public Object[] executeCommand(Context context, Arguments args) {
            context.pause(0.1);
            var server = ServerLifecycleHooks.getCurrentServer();
            if (server == null || !server.isCommandBlockEnabled()) {
                return ResultWrapper.result(null, "command blocks are disabled");
            } else {
                var sender = BlockEntity.getCommandBlock();
                var level = BlockEntity.getLevel();
                if (level == null) {
                    return ResultWrapper.result(null, "command block has no level");
                }
                sender.performCommand(level);
                return ResultWrapper.result(sender.getSuccessCount(), sender.getLastOutput().getString());
            }
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && Block.byItem(stack.getItem()) == Blocks.COMMAND_BLOCK) {
                return Environment.class;
            }
            return null;
        }
    }
}
