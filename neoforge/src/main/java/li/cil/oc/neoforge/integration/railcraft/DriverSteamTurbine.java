package li.cil.oc.neoforge.integration.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import mods.railcraft.world.level.block.entity.SteamTurbineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

@SuppressWarnings("unused")
public final class DriverSteamTurbine extends DriverSidedTileEntity {
    @Override
    public Class<?> getTileEntityClass() {
        return SteamTurbineBlockEntity.class;
    }

    @Override
    public ManagedEnvironment createEnvironment(
            final Level world, final int x, final int y, final int z, final Direction side) {
        return new Environment((SteamTurbineBlockEntity) world.getBlockEntity(new BlockPos(x, y, z)));
    }

    public static final class Environment extends ManagedTileEntityEnvironment<SteamTurbineBlockEntity> implements NamedBlock {
        public Environment(final SteamTurbineBlockEntity BlockEntity) {
            super(BlockEntity, "steam_turbine");
        }

        @Override
        public String preferredName() {
            return "steam_turbine";
        }

        @Override
        public int priority() {
            return 0;
        }

        @Callback(doc = "function():number -- Returns the output ratio of the steam turbine (0.0 to 1.0)")
        public Object[] getTurbineOutput(final Context context, final Arguments args) {
            return new Object[]{BlockEntity.getSteamTurbineModule().getOperatingRatio()};
        }

        @Callback(doc = "function():number -- Returns the durability of the rotor in percent.")
        public Object[] getTurbineRotorStatus(final Context context, final Arguments args) {
            final var rotorContainer = BlockEntity.getSteamTurbineModule().getRotorContainer();
            if (rotorContainer.getContainerSize() > 0) {
                final var itemStack = rotorContainer.getItem(0);
                if (!itemStack.isEmpty()) {
                    return new Object[]{100 - (int) (itemStack.getDamageValue() * 100.0 / itemStack.getMaxDamage())};
                }
            }
            return new Object[]{0};
        }
    }
}

