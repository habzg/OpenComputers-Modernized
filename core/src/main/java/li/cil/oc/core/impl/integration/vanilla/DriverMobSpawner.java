package li.cil.oc.core.impl.integration.vanilla;

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
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;

@SuppressWarnings("unused")
public final class DriverMobSpawner extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return SpawnerBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((SpawnerBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<SpawnerBlockEntity> implements NamedBlock {
        public Environment(SpawnerBlockEntity BlockEntity) {
          super(BlockEntity, "mob_spawner");
        }

        @Override
        public String preferredName() {
            return "mob_spawner";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():string -- Get the name of the entity that is being spawned by this spawner.")
        public Object[] getSpawningMobName(Context context, Arguments args) {
            var spawner = BlockEntity.getSpawner();
            var level = BlockEntity.getLevel();
            if (level != null) {
                var dispEntity = spawner.getOrCreateDisplayEntity(level, BlockEntity.getBlockPos());
                if (dispEntity != null) {
                    return ResultWrapper.result(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(dispEntity.getType()).toString());
                }
            }
            return ResultWrapper.result("");
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && Block.byItem(stack.getItem()) == Blocks.SPAWNER) {
                return Environment.class;
            }
            return null;
        }
    }
}
