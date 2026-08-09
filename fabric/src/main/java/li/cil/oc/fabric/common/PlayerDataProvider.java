package li.cil.oc.fabric.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import li.cil.oc.core.impl.IPlayerDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;

public final class PlayerDataProvider implements IPlayerDataProvider {
    public static final IPlayerDataProvider INSTANCE = new PlayerDataProvider();
    private static final Map<UUID, CompoundTag> clientData = new HashMap<>();

    private PlayerDataProvider() {}

    @Override
    public CompoundTag getPersistentData(Player player) {
        var server = player.getServer();
        if (server == null) {
            return clientData.computeIfAbsent(player.getUUID(), k -> new CompoundTag());
        }
        var storage = server.overworld().getDataStorage();
        var savedData = storage.computeIfAbsent(
                new SavedData.Factory<>(
                        PlayerSavedData::new,
                        PlayerSavedData::load,
                        DataFixTypes.LEVEL
                ),
                "opencomputers_playerdata"
        );
        return savedData.getData(player.getUUID());
    }

    private static class PlayerSavedData extends SavedData {
        private final Map<UUID, CompoundTag> data = new HashMap<>();

        public CompoundTag getData(UUID uuid) {
            setDirty();
            return data.computeIfAbsent(uuid, k -> new CompoundTag());
        }

        @Override
        public @NotNull CompoundTag save(@NotNull CompoundTag tag, HolderLookup.@NotNull Provider registries) {
            var map = new CompoundTag();
            for (var entry : data.entrySet()) {
                map.put(entry.getKey().toString(), entry.getValue());
            }
            tag.put("playerData", map);
            return tag;
        }

        public static PlayerSavedData load(CompoundTag tag, HolderLookup.Provider ignoredRegistries) {
            var saved = new PlayerSavedData();
            var map = tag.getCompound("playerData");
            for (var key : map.getAllKeys()) {
                try {
                    saved.data.put(UUID.fromString(key), map.getCompound(key));
                } catch (IllegalArgumentException ignored) {
                }
            }
            return saved;
        }
    }
}
