package li.cil.oc.neoforge.common;

import li.cil.oc.core.impl.util.StateSaveManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

public final class SaveHandler {
    private SaveHandler() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    @SuppressWarnings("unused")
    public static void onWorldLoad(LevelEvent.Load e) {
        if (e.getLevel() instanceof ServerLevel serverLevel) {
            StateSaveManager.setSaveRoot(serverLevel.getServer().getWorldPath(LevelResource.ROOT).toFile());
        }
        StateSaveManager.touchStateFiles();
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public static void onWorldSave(LevelEvent.Save e) {
        StateSaveManager.stateSaveHandler.withPool(pool -> {
            pool.submit(StateSaveManager::cleanSaveData);
            return null;
        });
    }
}
