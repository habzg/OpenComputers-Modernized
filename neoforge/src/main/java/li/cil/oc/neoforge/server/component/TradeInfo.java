package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.TradeInfoBase;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.trading.Merchant;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TradeInfo extends TradeInfoBase {
    public TradeInfo() {
        super(null, null, -1, -1);
    }

    public TradeInfo(EnvironmentHost host, Merchant merchant, int recipeID, int merchantID) {
        super(host, merchant, recipeID, merchantID);
    }

    @Override
    protected @Nullable Entity loadEntity(@NotNull CompoundTag nbt, @NotNull UUID uuid) {
        int dimension = nbt.getInt("dimensionID");
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel world : server.getAllLevels()) {
            if (world.dimension().location().hashCode() == dimension) {
                for (Entity e : world.getEntities().getAll()) {
                    if (e.getUUID().equals(uuid)) return e;
                }
            }
        }
        return null;
    }

    @Override
    protected @Nullable EnvironmentHost loadHostEntity(@NotNull CompoundTag nbt) {
        UUID uuid = new UUID(nbt.getLong("hostUUIDMost"), nbt.getLong("hostUUIDLeast"));
        Entity entity = loadEntity(nbt, uuid);
        if (entity instanceof EnvironmentHost) return (EnvironmentHost) entity;
        return null;
    }

    @Override
    protected @Nullable EnvironmentHost loadHostTileEntity(@NotNull CompoundTag nbt) {
        int dimension = nbt.getInt("dimensionID");
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return null;
        for (ServerLevel world : server.getAllLevels()) {
            if (world.dimension().location().hashCode() == dimension) {
                int x = nbt.getInt("hostX");
                int y = nbt.getInt("hostY");
                int z = nbt.getInt("hostZ");
                var te = world.getBlockEntity(new BlockPos(x, y, z));
                if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy) {
                    return proxy.robot;
                }
                if (te instanceof EnvironmentHost) return (EnvironmentHost) te;
            }
        }
        return null;
    }
}
