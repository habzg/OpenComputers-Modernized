package li.cil.oc.fabric.common;

import li.cil.oc.core.impl.util.StateSaveManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.world.level.storage.LevelResource;

public final class SaveHandler {
    public static void init() {
        ServerWorldEvents.LOAD.register((server, world) -> {
            StateSaveManager.setSaveRoot(server.getWorldPath(LevelResource.ROOT).toFile());
            StateSaveManager.touchStateFiles();
        });

        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> StateSaveManager.stateSaveHandler.withPool(pool -> {
            pool.submit(StateSaveManager::cleanSaveData);
            return null;
        }));
    }
}
