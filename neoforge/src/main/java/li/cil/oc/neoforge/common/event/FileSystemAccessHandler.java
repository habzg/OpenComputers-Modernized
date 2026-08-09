package li.cil.oc.neoforge.common.event;

import java.util.function.Predicate;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.blockentity.Case;
import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import li.cil.oc.core.impl.common.blockentity.Raid;
import li.cil.oc.core.impl.server.component.Server;
import li.cil.oc.neoforge.server.component.DiskDriveMountable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;


public final class FileSystemAccessHandler {
    private static Predicate<Level> clientLevelChecker = level -> false;

    public static void setClientLevelChecker(Predicate<Level> checker) {
        clientLevelChecker = checker;
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileSystemAccess(FileSystemAccessEvent.Server e) {
        if (e.getBlockEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                var mountable = t.getMountable(slot);
                if (mountable instanceof Server server) {
                    if (server.componentSlot(e.getNode().address()) >= 0) {
                        server.lastFileSystemAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                } else if (mountable instanceof DiskDriveMountable diskDrive) {
                    diskDrive.filesystemNode();
                    if (e.getNode().canBeReachedFrom(diskDrive.filesystemNode())) {
                        diskDrive.lastAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileSystemAccess(FileSystemAccessEvent.Client e) {
        float volume = OCSettings.get().soundVolume;
        if (clientLevelChecker.test(e.getWorld())) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(e.getSound()));
            e.getWorld().playLocalSound(e.getX(), e.getY(), e.getZ(), soundEvent, SoundSource.BLOCKS, volume, 1, false);
        }
        if (e.getBlockEntity() instanceof DiskDrive t) {
            t.lastAccess = System.currentTimeMillis();
        } else if (e.getBlockEntity() instanceof Case t) {
            t.lastFileSystemAccess = System.currentTimeMillis();
        } else if (e.getBlockEntity() instanceof Raid t) {
            t.lastAccess = System.currentTimeMillis();
        } else if (e.getBlockEntity() instanceof li.cil.oc.core.impl.common.blockentity.Rack t) {
            long now = System.currentTimeMillis();
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                var data = t.lastData[slot];
                if (data != null) {
                    var mountable = t.getMountable(slot);
                    if (mountable instanceof Server) {
                        data.putLong("lastFileSystemAccess", now);
                    } else if (mountable instanceof DiskDriveMountable) {
                        data.putLong("lastAccess", now);
                    }
                }
            }
        }
    }
}
