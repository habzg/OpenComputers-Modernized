package li.cil.oc.fabric.integration.vanilla;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlockEntity;
import li.cil.oc.core.impl.integration.ManagedBlockEntityEnvironment;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;

@SuppressWarnings("unused")
public final class DriverFurnace extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return FurnaceBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((FurnaceBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<FurnaceBlockEntity> implements NamedBlock {
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
            return ResultWrapper.result(this.getBlockEntity().litTime);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item has been cooking for.")
        public Object[] getCookTime(Context context, Arguments args) {
            return ResultWrapper.result(this.getBlockEntity().cookingProgress);
        }

        @Callback(doc = "function():number -- The number of ticks that the current item needs to cook.")
        public Object[] getTotalCookTime(Context context, Arguments args) {
            return ResultWrapper.result(this.getBlockEntity().cookingTotalTime);
        }

        @Callback(doc = "function():number -- The number of ticks that the currently burning fuel lasts in total.")
        public Object[] getCurrentItemBurnTime(Context context, Arguments args) {
            return ResultWrapper.result(this.getBlockEntity().litDuration);
        }

        @Callback(doc = "function():boolean -- Get whether the furnace is currently active.")
        public Object[] isBurning(Context context, Arguments args) {
            return ResultWrapper.result(this.getBlockEntity().litTime > 0);
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
