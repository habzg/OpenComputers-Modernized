package li.cil.oc.fabric.util;

import java.nio.ByteBuffer;
import java.util.Iterator;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Audio {
    private static final Logger LOGGER = LoggerFactory.getLogger(Audio.class);
    private static final java.util.Set<Source> sources = new java.util.HashSet<>();
    private static boolean disableAudio = false;

    private static float sampleRate() {
        return OCSettings.get().beepSampleRate;
    }

    private static int amplitude() {
        return OCSettings.get().beepAmplitude;
    }

    private static int maxDistance() {
        return (int) OCSettings.get().beepRadius;
    }

    private static float volume() {
        return Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
    }

    public static void stopBeep(double x, double y, double z) {
        synchronized (sources) {
            Iterator<Source> it = sources.iterator();
            while (it.hasNext()) {
                Source s = it.next();
                if (Math.abs(s.x - x) < 0.5 && Math.abs(s.y - y) < 0.5 && Math.abs(s.z - z) < 0.5) {
                    s.stop();
                    it.remove();
                }
            }
        }
    }

    public static void play(double x, double y, double z, short frequency, short duration) {
        play(x, y, z, ".", frequency, duration);
    }

    public static void play(double x, double y, double z, String pattern) {
        play(x, y, z, pattern, 1000, 200);
    }

    public static void play(double x, double y, double z, String pattern, int frequencyInHz, int durationInMilliseconds) {
        if (disableAudio) return;
        stopBeep(x, y, z);
        try {
            var mc = Minecraft.getInstance();
            if (mc.level == null || mc.player == null) return;
            float dist = (float) Math.sqrt(mc.player.distanceToSqr(x, y, z));
            float distanceBasedGain = Math.max(0, 1 - dist / maxDistance());
            float gain = distanceBasedGain * volume();
            if (gain <= 0 || amplitude() <= 0) return;

            if (disableAudio) {
                float clampedFrequency = Math.clamp(frequencyInHz - 20, 0, 1980) / 1980f + 0.5f;
                int delay = 0;
                for (int i = 0; i < pattern.length(); i++) {
                    char ch = pattern.charAt(i);
                    int finalDelay = delay;
                    var record = new net.minecraft.client.resources.sounds.SimpleSoundInstance(
                            SoundEvents.NOTE_BLOCK_HARP.value(), SoundSource.BLOCKS, gain, clampedFrequency,
                            mc.level.getRandom(), new net.minecraft.core.BlockPos((int) Math.round(x), (int) Math.round(y), (int) Math.round(z)));
                    if (finalDelay == 0) mc.getSoundManager().play(record);
                    else mc.getSoundManager().playDelayed(record, finalDelay);
                    delay += Math.max(1, (ch == '.' ? durationInMilliseconds : 2 * durationInMilliseconds) * 20 / 1000);
                }
            } else {
                int[] sampleCounts = new int[pattern.length()];
                int totalSamples = 0;
                for (int i = 0; i < pattern.length(); i++) {
                    sampleCounts[i] = (pattern.charAt(i) == '.' ? durationInMilliseconds : 2 * durationInMilliseconds) * (int) sampleRate() / 1000;
                    totalSamples += sampleCounts[i];
                }
                int pauseSampleCount = 50 * (int) sampleRate() / 1000;
                totalSamples += (sampleCounts.length - 1) * pauseSampleCount;

                ByteBuffer data = BufferUtils.createByteBuffer(totalSamples);
                float step = frequencyInHz / sampleRate();
                float offset = 0;
                for (int i = 0; i < sampleCounts.length; i++) {
                    for (int sample = 0; sample < sampleCounts[i]; sample++) {
                        double angle = 2 * Math.PI * offset;
                        byte value = (byte) ((int) (Math.signum(Math.sin(angle)) * amplitude()) ^ 0x80);
                        offset += step;
                        if (offset > 1) offset -= 1;
                        data.put(value);
                    }
                    if (i < sampleCounts.length - 1) {
                        for (int sample = 0; sample < pauseSampleCount; sample++) {
                            data.put((byte) 127);
                        }
                    }
                }
                data.rewind();

                synchronized (sources) {
                    try {
                        sources.add(new Source(x, y, z, data, gain));
                    } catch (Exception e) {
                        if (e.getMessage() != null && e.getMessage().contains("Out of memory")) {
                            LOGGER.info("Couldn't play computer speaker sound because your sound card ran out of memory. Disabling computer speakers.");
                            disableAudio = true;
                        } else {
                            LOGGER.warn("Error playing computer speaker sound.", e);
                        }
                    }
                }
            }
        } catch (Throwable t) {
            disableAudio = true;
        }
    }

    @SuppressWarnings("unused")
    public static void onClientTick() {
        if (disableAudio) return;
        synchronized (sources) {
            sources.removeIf(Source::checkFinished);
        }
    }

    private static class Source {
        final double x, y, z;
        final int source;
        final int buffer;
        @SuppressWarnings("unused")
        final float gain;

        @SuppressWarnings("unused")
        Source(double x, double y, double z, ByteBuffer data, float gain) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.gain = gain;

            AL10.alGetError();

            buffer = AL10.alGenBuffers();
            checkALError();

            try {
                AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO8, data, (int) sampleRate());
                checkALError();

                source = AL10.alGenSources();
                checkALError();

                try {
                    AL10.alSourceQueueBuffers(source, buffer);
                    checkALError();

                    AL10.alSource3f(source, AL10.AL_POSITION, (float) x, (float) y, (float) z);
                    AL10.alSourcef(source, AL10.AL_REFERENCE_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_MAX_DISTANCE, maxDistance());
                    AL10.alSourcef(source, AL10.AL_GAIN, gain * 0.3f);
                    checkALError();

                    AL10.alSourcePlay(source);
                    checkALError();
                } catch (Throwable t) {
                    AL10.alDeleteSources(source);
                    throw t;
                }
            } catch (Throwable t) {
                AL10.alDeleteBuffers(buffer);
                throw t;
            }
        }

        boolean checkFinished() {
            if (AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                AL10.alDeleteSources(source);
                AL10.alDeleteBuffers(buffer);
                return true;
            }
            return false;
        }

        void stop() {
            try {
                AL10.alSourceStop(source);
            } catch (Throwable ignored) {
            }
            try {
                AL10.alDeleteSources(source);
            } catch (Throwable ignored) {
            }
            try {
                AL10.alDeleteBuffers(buffer);
            } catch (Throwable ignored) {
            }
        }
    }

    private static void checkALError() {
        int errorCode = AL10.alGetError();
        if (errorCode != AL10.AL_NO_ERROR) {
            throw new RuntimeException("OpenAL error: " + errorCode);
        }
    }
}
