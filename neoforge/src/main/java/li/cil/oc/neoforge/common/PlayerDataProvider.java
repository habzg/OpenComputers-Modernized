package li.cil.oc.neoforge.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import li.cil.oc.core.impl.IPlayerDataProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public final class PlayerDataProvider implements IPlayerDataProvider {
    public static final IPlayerDataProvider INSTANCE = new PlayerDataProvider();
    private static final Map<UUID, CompoundTag> clientData = new HashMap<>();

    private PlayerDataProvider() {}

    @Override
    public CompoundTag getPersistentData(Player player) {
        if (player.level().isClientSide) {
            return clientData.computeIfAbsent(player.getUUID(), k -> new CompoundTag());
        }
        CompoundTag nbt = player.getPersistentData();
        if (!nbt.contains(Player.PERSISTED_NBT_TAG)) {
            nbt.put(Player.PERSISTED_NBT_TAG, new CompoundTag());
        }
        return nbt.getCompound(Player.PERSISTED_NBT_TAG);
    }
}
