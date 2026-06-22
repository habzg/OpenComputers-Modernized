package li.cil.oc.neoforge.common.event;

import li.cil.oc.api.internal.Rack;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.tileentity.Case;
import li.cil.oc.core.impl.common.tileentity.DiskDrive;
import li.cil.oc.core.impl.common.tileentity.Raid;
import li.cil.oc.neoforge.event.FileSystemAccessEventImpl;
import li.cil.oc.neoforge.server.component.DiskDriveMountable;
import li.cil.oc.neoforge.server.component.Server;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;

import java.util.function.Predicate;


public final class FileSystemAccessHandler {
    private static Predicate<Level> clientLevelChecker = level -> false;

    public static void setClientLevelChecker(Predicate<Level> checker) {
        clientLevelChecker = checker;
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileSystemAccess(FileSystemAccessEventImpl.Server e) {
        if (e.tileEntity() instanceof Rack t) {
            for (int slot = 0; slot < t.getContainerSize(); slot++) {
                var mountable = t.getMountable(slot);
                if (mountable instanceof Server server) {
                    if (server.componentSlot(e.node().address()) >= 0) {
                        server.lastFileSystemAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                } else if (mountable instanceof DiskDriveMountable diskDrive) {
                    diskDrive.filesystemNode();
                    if (e.node().canBeReachedFrom(diskDrive.filesystemNode())) {
                        diskDrive.lastAccess = System.currentTimeMillis();
                        t.markChanged(slot);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onFileSystemAccess(FileSystemAccessEventImpl.Client e) {
        float volume = Settings.get().soundVolume;
        if (clientLevelChecker.test(e.level())) {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(ResourceLocation.parse(e.sound()));
            e.level().playLocalSound(e.x(), e.y(), e.z(), soundEvent, SoundSource.BLOCKS, volume, 1, false);
        }
        if (e.tileEntity() instanceof DiskDrive t) {
            t.lastAccess = System.currentTimeMillis();
        } else if (e.tileEntity() instanceof Case t) {
            t.lastFileSystemAccess = System.currentTimeMillis();
        } else if (e.tileEntity() instanceof Raid t) {
            t.lastAccess = System.currentTimeMillis();
        } else if (e.tileEntity() instanceof li.cil.oc.core.impl.common.tileentity.Rack t) {
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
