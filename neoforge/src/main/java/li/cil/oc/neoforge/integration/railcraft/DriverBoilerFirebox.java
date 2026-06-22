package li.cil.oc.neoforge.integration.railcraft;

import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedTileEntity;
import li.cil.oc.neoforge.integration.ManagedTileEntityEnvironment;
import mods.railcraft.world.level.block.entity.steamboiler.SteamBoilerBlockEntity;
import mods.railcraft.world.level.material.steam.SteamBoiler;
import mods.railcraft.world.module.SteamBoilerModule;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.Optional;

@SuppressWarnings("unused")
public class DriverBoilerFirebox extends DriverSidedTileEntity {
    @Override
    public ManagedEnvironment createEnvironment(
            final Level world, final int x, final int y, final int z, final Direction side) {
        var be = world.getBlockEntity(new BlockPos(x, y, z));
        if (be instanceof SteamBoilerBlockEntity boiler) {
            return new Environment(boiler);
        }
        return null;
    }

    @Override
    public Class<?> getTileEntityClass() {
        return SteamBoilerBlockEntity.class;
    }

    public static final class Environment extends ManagedTileEntityEnvironment<SteamBoilerBlockEntity>
            implements NamedBlock {
        public Environment(final SteamBoilerBlockEntity BlockEntity) {
            super(BlockEntity, "boiler_firebox");
        }

        @Override
        public String preferredName() {
            return "boiler_firebox";
        }

        @Override
        public int priority() {
            return 0;
        }

        private Optional<SteamBoiler> getBoiler() {
            return BlockEntity.getModule(mods.railcraft.world.module.SteamBoilerModule.class)
                    .map(SteamBoilerModule::getBoiler)
                    .or(() -> BlockEntity.getMasterBlockEntity()
                            .flatMap(master -> master.getModule(mods.railcraft.world.module.SteamBoilerModule.class))
                            .map(SteamBoilerModule::getBoiler));
        }

        @Callback(doc = "function():boolean -- Get whether the boiler is active or not.")
        public Object[] isBurning(final Context context, final Arguments args) {
            return new Object[]{getBoiler().map(SteamBoiler::isBurning).orElse(false)};
        }

        @Callback(doc = "function():number -- Get the temperature of the boiler.")
        public Object[] getTemperature(final Context context, final Arguments args) {
            return new Object[]{getBoiler().map(SteamBoiler::getTemperature).orElse(0f)};
        }

        @Callback(doc = "function():number -- Get the maximum temperature of the boiler.")
        public Object[] getMaxHeat(final Context context, final Arguments args) {
            return new Object[]{getBoiler().map(SteamBoiler::getMaxTemperature).orElse(0f)};
        }
    }
}

