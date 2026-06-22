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
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;

@SuppressWarnings("unused")
public final class DriverBeacon extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return BeaconBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(Level world, int x, int y, int z, Direction side) {
        return new Environment((BeaconBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<BeaconBlockEntity> implements NamedBlock {
        public Environment(BeaconBlockEntity BlockEntity) {
            super(BlockEntity, "beacon");
        }

        private static String getEffectName(Holder<MobEffect> effect) {
            if (effect != null) {
                var key = effect.getKey();
                if (key != null) {
                    return key.location().toString();
                }
                return effect.value().getDescriptionId();
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
            return ResultWrapper.result(getTileEntity().levels);
        }

        @Callback(doc = "function():string -- Get the name of the active primary effect.")
        public Object[] getPrimaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(getEffectName(getTileEntity().primaryPower));
        }

        @Callback(doc = "function():string -- Get the name of the active secondary effect.")
        public Object[] getSecondaryEffect(Context context, Arguments args) {
            return ResultWrapper.result(getEffectName(getTileEntity().secondaryPower));
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
