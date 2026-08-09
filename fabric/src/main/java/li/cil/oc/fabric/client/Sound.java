package li.cil.oc.fabric.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;

@SuppressWarnings("unused")
public final class Sound {
    private static final Map<BlockEntity, PseudoLoopingStream> sources = new HashMap<>();
    private static final PriorityQueue<Command> commandQueue = new PriorityQueue<>();
    private static float lastVolume = -1f;
    private static int tickCount = 0;

    public static void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            manager();
            if (client.level != null && OCSettings.get().soundVolume > 0) {
                tickCount++;
                if (tickCount % 10 == 0) {
                    synchronized (sources) {
                        sources.entrySet().removeIf(entry -> entry.getKey().isRemoved());
                        updateVolume();
                        processQueue();
                    }
                }
            }
        });

        ClientChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world == Minecraft.getInstance().level) {
                handleChunkLoad(world, chunk);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
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
        });
    }

    private static void handleChunkLoad(ClientLevel level, LevelChunk chunk) {
        if (OCSettings.get().soundVolume <= 0) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            if (be instanceof Computer computer && computer.isRunning() && computer.runSound() != null) {
                if (ClientDistanceHelper.distanceSquared(be.getLevel(),
                        be.getBlockPos().getX() + 0.5, be.getBlockPos().getY() + 0.5, be.getBlockPos().getZ() + 0.5, player) <= 32 * 32) {
                    startLoop(be, computer.runSound(), 0.5f, 50 + level.random.nextInt(50));
                }
            }
        }
    }

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
                        li.cil.oc.fabric.OpenComputers.log().warn("Error processing sound command.", t);
                    }
                }
            }
        }
    }

    public static void startLoop(BlockEntity blockEntity, String name, float volume, long delay) {
        if (OCSettings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new StartCommand(System.currentTimeMillis() + delay, blockEntity, name, volume));
            }
        }
    }

    public static void stopLoop(BlockEntity blockEntity) {
        if (OCSettings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new StopCommand(blockEntity));
            }
        }
    }

    public static void updatePosition(BlockEntity blockEntity) {
        if (OCSettings.get().soundVolume > 0 && Minecraft.getInstance().level != null) {
            synchronized (commandQueue) {
                commandQueue.offer(new UpdatePositionCommand(blockEntity));
            }
        }
    }

    private abstract static class Command implements Comparable<Command> {
        final long when;
        final BlockEntity blockEntity;

        @SuppressWarnings("unused")
        Command(long when, BlockEntity blockEntity) {
            this.when = when;
            this.blockEntity = blockEntity;
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

        @SuppressWarnings("unused")
        StartCommand(long when, BlockEntity blockEntity, String name, float volume) {
            super(when, blockEntity);
            this.name = name;
            this.volume = volume;
        }

        @Override
        void run() {
            synchronized (sources) {
                sources.computeIfAbsent(blockEntity, k -> new PseudoLoopingStream(blockEntity, volume)).play(name);
            }
        }
    }

    private static class StopCommand extends Command {
        @SuppressWarnings("unused")
        StopCommand(BlockEntity blockEntity) {
            super(System.currentTimeMillis() + 1, blockEntity);
        }

        @Override
        void run() {
            synchronized (sources) {
                PseudoLoopingStream sound = sources.remove(blockEntity);
                if (sound != null) sound.stop();
            }
            synchronized (commandQueue) {
                List<Command> remaining = new ArrayList<>();
                for (Command cmd : commandQueue) {
                    if (cmd.blockEntity != blockEntity) remaining.add(cmd);
                }
                commandQueue.clear();
                commandQueue.addAll(remaining);
            }
        }
    }

    private static class UpdatePositionCommand extends Command {
        @SuppressWarnings("unused")
        UpdatePositionCommand(BlockEntity blockEntity) {
            super(System.currentTimeMillis(), blockEntity);
        }

        @Override
        void run() {
            synchronized (sources) {
                PseudoLoopingStream sound = sources.get(blockEntity);
                if (sound != null) sound.updatePosition();
            }
        }
    }

    private static class PseudoLoopingStream {
        final BlockEntity blockEntity;
        final float volume;
        SoundInstance current;

        @SuppressWarnings("unused")
        PseudoLoopingStream(BlockEntity blockEntity, float volume) {
            this.blockEntity = blockEntity;
            this.volume = volume;
        }

        void updateVolume() {
            if (current instanceof ComputerRunningSound s) {
                float baseVolume = volume * OCSettings.get().soundVolume;
                float userVolume = Minecraft.getInstance().options.getSoundSourceVolume(SoundSource.BLOCKS);
                s.setVolume(baseVolume * (isGamePaused() ? 0f : userVolume));
            }
        }

        void updatePosition() {
            if (current instanceof ComputerRunningSound s) {
                if (blockEntity != null) {
                    BlockPos pos = blockEntity.getBlockPos();
                    s.setPosition(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                }
            }
        }

        void play(String name) {
            String resourceName = OCSettings.resourceDomain + ":" + name;
            var location = ResourceLocation.parse(resourceName);
            var soundEvent = SoundEvent.createFixedRangeEvent(location, 16.0F);
            if (!initialized) {
                initialized = true;
                current = new ComputerRunningSound(soundEvent, blockEntity, volume);
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
        private final BlockEntity blockEntity;

        protected ComputerRunningSound(SoundEvent soundEvent, BlockEntity blockEntity, float volume) {
            super(soundEvent, SoundSource.BLOCKS, SoundInstance.createUnseededRandom());
            this.blockEntity = blockEntity;
            this.looping = true;
            this.delay = 0;
            this.volume = volume * OCSettings.get().soundVolume;
            this.relative = false;
            this.attenuation = Attenuation.LINEAR;
            if (blockEntity != null) {
                BlockPos pos = blockEntity.getBlockPos();
                this.x = pos.getX() + 0.5;
                this.y = pos.getY() + 0.5;
                this.z = pos.getZ() + 0.5;
            }
        }

        @Override
        public void tick() {
            if (blockEntity == null || blockEntity.isRemoved()) {
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
