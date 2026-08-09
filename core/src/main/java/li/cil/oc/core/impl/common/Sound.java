package li.cil.oc.core.impl.common;

import java.util.Map;
import java.util.WeakHashMap;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

public final class Sound {
    private static final Map<EnvironmentHost, Map<String, Long>> globalTimeouts = new WeakHashMap<>();
    private static final long COOLDOWN = 250;

    private Sound() {
    }

    public static synchronized void play(EnvironmentHost host, String name) {
        Map<String, Long> hostTimeouts = globalTimeouts.get(host);
        long now = System.currentTimeMillis();
        if (hostTimeouts != null && hostTimeouts.getOrDefault(name, 0L) > now) {
            return;
        }
        var location = ResourceLocation.parse(OCSettings.resourceDomain + ":" + name);
        var soundEvent = SoundEvent.createVariableRangeEvent(location);
        host.level().playSound(null, host.xPosition(), host.yPosition(), host.zPosition(), soundEvent, SoundSource.BLOCKS, OCSettings.get().soundVolume, 1.0f);
        globalTimeouts.computeIfAbsent(host, k -> new java.util.HashMap<>()).put(name, now + COOLDOWN);
    }

    public static void playDiskInsert(EnvironmentHost host) {
        play(host, "floppy_insert");
    }

    public static void playDiskEject(EnvironmentHost host) {
        play(host, "floppy_eject");
    }

}
