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
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

@SuppressWarnings("unused")
public final class DriverFurnace extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return FurnaceBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((FurnaceBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<FurnaceBlockEntity> implements NamedBlock {
        public Environment(FurnaceBlockEntity BlockEntity) {
            super(BlockEntity, "furnace");
        }

        @Override
        public String preferredName() {
            return "furnace";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- The number of ticks that the furnace will keep burning from the last consumed fuel.")
        public Object[] getBurnTime(Context context, Arguments args) {
            return ResultWrapper.result(getTileEntity().litTime);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(Context context, Arguments args) {
            return ResultWrapper.result(getTileEntity().cookingProgress);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
        public Object[] getTotalCookTime(Context context, Arguments args) {
            return ResultWrapper.result(getTileEntity().cookingTotalTime);
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(Context context, Arguments args) {
            return ResultWrapper.result(getTileEntity().litDuration);
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(Context context, Arguments args) {
            return ResultWrapper.result(getTileEntity().litTime > 0);
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && Block.byItem(stack.getItem()) == Blocks.FURNACE) {
                return Environment.class;
            }
            return null;
        }
    }
}
