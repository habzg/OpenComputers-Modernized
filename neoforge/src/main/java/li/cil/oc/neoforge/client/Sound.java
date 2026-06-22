package li.cil.oc.neoforge.client;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.level.LevelEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public final class Sound {
    private static final Map<BlockEntity, PseudoLoopingStream> sources = new HashMap<>();
    private static final PriorityQueue<Command> commandQueue = new PriorityQueue<>();
    private static float lastVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
    private static int tickCount = 0;

    private static SoundManager manager() {
        return Minecraft.getInstance().getSoundManager();
    }

    private static void updateVolume() {
        float volume = isGamePaused() ? 0f : Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
        if (volume != lastVolume) {
            lastVolume = volume;
            synchronized (sources) {
                for (PseudoLoopingStream sound : sources.values()) {
                    sound.updateVolume();
                }
            }
        }
    }

    private static boolean isGamePaused() {
        return Minecraft.getInstance().isPaused();
    }

    private static void processQueue() {
        if (!commandQueue.isEmpty()) {
            synchronized (commandQueue) {
                while (!commandQueue.isEmpty() && commandQueue.peek().when < System.currentTimeMillis()) {
                    try {
                        Command cmd = commandQueue.poll();
                        if (cmd != null) cmd.run();
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Error processing sound command.", t);
                    }
                }
            }
        }
    }

    public static void startLoop(BlockEntity tileEntity, String name, float volume, long delay) {
        if (Settings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new StartCommand(System.currentTimeMillis() + delay, tileEntity, name, volume));
            }
        }
    }

    public static void stopLoop(BlockEntity tileEntity) {
        if (Settings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new StopCommand(tileEntity));
            }
        }
    }

    public static void updatePosition(BlockEntity tileEntity) {
        if (Settings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new UpdatePositionCommand(tileEntity));
            }
        }
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onTick(ClientTickEvent.Post e) {
        manager();
        if (Minecraft.getInstance().level != null && Settings.get().soundVolume > 0) {
            tickCount++;
            if (tickCount % 10 == 0) {
                synchronized (sources) {
                    updateVolume();
                    processQueue();
                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    @SuppressWarnings("unused")
    public static void onWorldUnload(LevelEvent.Unload event) {
        synchronized (sources) {
            try {
                for (PseudoLoopingStream sound : sources.values()) {
                    sound.stop();
                }
            } catch (Throwable ignored) {
            }
            sources.clear();
        }
        synchronized (commandQueue) {
            commandQueue.clear();
        }
    }

    private abstract static class Command implements Comparable<Command> {
        final long when;
        final BlockEntity tileEntity;

        Command(long when, BlockEntity tileEntity) {
            this.when = when;
            this.tileEntity = tileEntity;
        }

        abstract void run();

        @Override
        public int compareTo(Command that) {
            return Long.compare(this.when, that.when);
        }
    }

    private static class StartCommand extends Command {
        final String name;
        final float volume;

        StartCommand(long when, BlockEntity tileEntity, String name, float volume) {
            super(when, tileEntity);
            this.name = name;
            this.volume = volume;
        }

        @Override
        void run() {
            synchronized (sources) {
                sources.computeIfAbsent(tileEntity, k -> new PseudoLoopingStream(tileEntity, volume)).play(name);
            }
        }
    }

    private static class StopCommand extends Command {
        StopCommand(BlockEntity tileEntity) {
            super(System.currentTimeMillis() + 1, tileEntity);
        }

        @Override
        void run() {
            synchronized (sources) {
                PseudoLoopingStream sound = sources.remove(tileEntity);
                if (sound != null) sound.stop();
            }
            synchronized (commandQueue) {
                List<Command> remaining = new ArrayList<>();
                for (Command cmd : commandQueue) {
                    if (cmd.tileEntity != tileEntity) remaining.add(cmd);
                }
                commandQueue.clear();
                commandQueue.addAll(remaining);
            }
        }
    }

    private static class UpdatePositionCommand extends Command {
        UpdatePositionCommand(BlockEntity tileEntity) {
            super(System.currentTimeMillis(), tileEntity);
        }

        @Override
        void run() {
            synchronized (sources) {
                PseudoLoopingStream sound = sources.get(tileEntity);
                if (sound != null) sound.updatePosition();
            }
        }
    }

    private static class PseudoLoopingStream {
        final BlockEntity tileEntity;
        final float volume;
        SoundInstance current;

        PseudoLoopingStream(BlockEntity tileEntity, float volume) {
            this.tileEntity = tileEntity;
            this.volume = volume;
        }

        void updateVolume() {
            if (current instanceof ComputerRunningSound s) {
                float baseVolume = volume * Settings.get().soundVolume;
                float userVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
                s.setVolume(baseVolume * (isGamePaused() ? 0f : userVolume));
            }
        }

        void updatePosition() {
            if (current instanceof ComputerRunningSound s) {
                if (tileEntity != null) {
                    BlockPos pos = tileEntity.getBlockPos();
                    s.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }
            }
        }

        void play(String name) {
            String resourceName = Settings.resourceDomain + ":" + name;
            var location = ResourceLocation.parse(resourceName);
            var soundEvent = SoundEvent.createVariableRangeEvent(location);
            if (!initialized) {
                initialized = true;
                current = new ComputerRunningSound(soundEvent, tileEntity, volume);
                manager().play(current);
                updateVolume();
            }
        }

        boolean initialized = false;

        void stop() {
            if (current != null) {
                try {
                    manager().stop(current);
                } catch (Throwable ignored) {
                }
                current = null;
                initialized = false;
            }
        }
    }

    private static class ComputerRunningSound extends AbstractTickableSoundInstance {
        private final BlockEntity tileEntity;

        protected ComputerRunningSound(SoundEvent soundEvent, BlockEntity tileEntity, float volume) {
            super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.tileEntity = tileEntity;
            this.looping = true;
            this.delay = 0;
            this.volume = volume * Settings.get().soundVolume;
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
            if (tileEntity != null) {
                BlockPos pos = tileEntity.getBlockPos();
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
            }
        }

        @Override
        public void tick() {
            if (tileEntity == null || tileEntity.isRemoved()) {
                stop();
            }
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        public void setVolume(float vol) {
            this.volume = vol;
        }

        public void setPosition(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
