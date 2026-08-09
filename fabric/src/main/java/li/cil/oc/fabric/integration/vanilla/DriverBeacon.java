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
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

@SuppressWarnings("unused")
public final class DriverBeacon extends DriverSidedBlockEntity {
    @Override
    public Class<?> getBlockEntityClass() {
        return BeaconBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, BlockPos pos, Direction side) {
        return new Environment((BeaconBlockEntity) world.getBlockEntity(pos));
    }

    public static final class Environment extends ManagedBlockEntityEnvironment<BeaconBlockEntity> implements NamedBlock {
        public Environment(BeaconBlockEntity BlockEntity) {
          super(BlockEntity, "beacon");
        }

        private static String getEffectName(Holder<MobEffect> effect) {
            if (effect != null) {
                var key = effect.unwrapKey();
                return key.map(mobEffectResourceKey -> mobEffectResourceKey.location().toString()).orElseGet(() -> effect.value().getDescriptionId());
            }
            return null;
        }

        @Override
        public String preferredName() {
            return "beacon";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Get the number of levels for this beacon.")
        public Object[] getLevels(Context context, Arguments args) {
            return ResultWrapper.result(this.getBlockEntity().levels);
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(getEffectName(this.getBlockEntity().primaryPower));
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(getEffectName(this.getBlockEntity().secondaryPower));
        }
    }

    public static final class Provider implements EnvironmentProvider {
        @Override
        public Class<?> getEnvironment(ItemStack stack) {
            if (stack != null && Block.byItem(stack.getItem()) == Blocks.BEACON) {
                return Environment.class;
            }
            return null;
        }
    }
}
