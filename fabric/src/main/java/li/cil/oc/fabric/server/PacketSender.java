package li.cil.oc.fabric.server;

import java.io.IOException;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.core.impl.common.nanomachines.ControllerImpl;
import li.cil.oc.core.impl.common.nanomachines.NeuralNetwork;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.server.PetVisibility;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PacketSender extends li.cil.oc.core.impl.common.PacketSender {
    private static final java.util.WeakHashMap<Node, com.google.common.cache.Cache<String, Long>> fileSystemAccessTimeouts = new java.util.WeakHashMap<>();

    @Override
    protected PacketBuilderBase<?> createBuilder(PacketType type) {
        return new PacketBuilder(type);
    }

    @Override
    protected void sendClientLogImpl(String line, ServerPlayer player) {
        try (var pb = new PacketBuilder.Compressed(PacketType.ClientLog)) {
            pb.writeUTF(line);
            pb.sendToPlayer(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendHologramAreaImpl(BlockEntity t, byte dirtyFromX, byte dirtyUntilX, byte dirtyFromZ, byte dirtyUntilZ, int[] volume, int width) {
        try (var pb = new PacketBuilder.Compressed(PacketType.HologramArea)) {
            pb.writeBlockEntity(t);
            pb.writeByte(dirtyFromX);
            pb.writeByte(dirtyUntilX);
            pb.writeByte(dirtyFromZ);
            pb.writeByte(dirtyUntilZ);
            for (int x = dirtyFromX; x < dirtyUntilX; x++) {
                for (int z = dirtyFromZ; z < dirtyUntilZ; z++) {
                    pb.writeInt(volume[x + z * width]);
                    pb.writeInt(volume[x + z * width + width * width]);
                }
            }
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendHologramValuesImpl(BlockEntity t, java.util.BitSet dirty, int[] volume, int width) {
        try (var pb = new PacketBuilder.Compressed(PacketType.HologramValues)) {
            pb.writeBlockEntity(t);
            pb.writeInt(dirty.cardinality());
            for (int xz = dirty.nextSetBit(0); xz >= 0; xz = dirty.nextSetBit(xz + 1)) {
                pb.writeShort(xz);
                int rangeStart = (xz >> 8) + (xz & 0xFF) * width;
                int rangeFinal = (xz >> 8) + (xz & 0xFF) * width + width * width;
                pb.writeInt(volume[Math.clamp(rangeStart, 0, volume.length - 1)]);
                pb.writeInt(volume[Math.clamp(rangeFinal, 0, volume.length - 1)]);
            }
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendLootDisksImpl(ServerPlayer p) {
        for (ItemStack[] entry : LootManager.globalDisks) {
            try (var pb = createBuilder(PacketType.LootDisk)) {
                pb.writeItemStack(entry[0], p.level().registryAccess());
                pb.sendToPlayer(p);
            }
        }
        for (ItemStack stack : LootManager.disksForCyclingServer) {
            try (var pb = createBuilder(PacketType.CyclingDisk)) {
                pb.writeItemStack(stack, p.level().registryAccess());
                pb.sendToPlayer(p);
            }
        }
    }

    @Override
    protected void sendTextBufferInitImpl(String address, CompoundTag value, ServerPlayer player) {
        try (var pb = new PacketBuilder.Compressed(PacketType.TextBufferInit)) {
            pb.writeUTF(address);
            pb.writeNBT(value);
            pb.sendToPlayer(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotMoveImpl(BlockEntity t, BlockPosition position, Direction direction) {
        try (var pb = new PacketBuilder(PacketType.RobotMove)) {
            var level = t.getLevel();
            pb.writeInt(level != null ? level.dimension().location().hashCode() : 0);
            pb.writeInt(position.x());
            pb.writeInt(position.y());
            pb.writeInt(position.z());
            pb.writeDirection(direction);
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotAnimateSwingImpl(BlockEntity t, int animationTicksTotal) {
        try (var pb = new PacketBuilder(PacketType.RobotAnimateSwing)) {
            pb.writeBlockEntity(t);
            pb.writeInt(animationTicksTotal);
            pb.sendToPlayersNearBlockEntity(t, OCSettings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotAnimateTurnImpl(BlockEntity t, byte turnAxis, int animationTicksTotal) {
        try (var pb = new PacketBuilder(PacketType.RobotAnimateTurn)) {
            pb.writeBlockEntity(t);
            pb.writeByte(turnAxis);
            pb.writeInt(animationTicksTotal);
            pb.sendToPlayersNearBlockEntity(t, OCSettings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotInventoryImpl(BlockEntity t, int slot, ItemStack stack) {
        try (var pb = new PacketBuilder(PacketType.RobotInventoryChange)) {
            pb.writeBlockEntity(t);
            pb.writeInt(slot);
            var robotLevel = t.getLevel();
            pb.writeItemStack(stack, robotLevel != null ? robotLevel.registryAccess() : null);
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotLightChangeImpl(BlockEntity t, int lightColor) {
        try (var pb = new PacketBuilder(PacketType.RobotLightChange)) {
            pb.writeBlockEntity(t);
            pb.writeInt(lightColor);
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotNameChangeImpl(BlockEntity t, String name) {
        try (var pb = new PacketBuilder(PacketType.RobotNameChange)) {
            pb.writeBlockEntity(t);
            short len = (short) name.length();
            pb.writeShort(len);
            for (int x = 0; x < len; x++) pb.writeChar(name.charAt(x));
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotSelectedSlotChangeImpl(BlockEntity t, int selectedSlot) {
        try (var pb = new PacketBuilder(PacketType.RobotSelectedSlotChange)) {
            pb.writeBlockEntity(t);
            pb.writeInt(selectedSlot);
            pb.sendToPlayersNearBlockEntity(t, OCSettings.get().maxNetworkClientEffectPacketDistance / 4.0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendFileSystemActivityImpl(Node node, EnvironmentHost host, String name) {
        com.google.common.cache.Cache<String, Long> hostTimeouts;
        synchronized (fileSystemAccessTimeouts) {
            hostTimeouts = fileSystemAccessTimeouts.get(node);
            if (hostTimeouts == null) {
                hostTimeouts = com.google.common.cache.CacheBuilder.newBuilder()
                        .concurrencyLevel(OCSettings.get().threads)
                        .maximumSize(250)
                        .expireAfterWrite(OCSettings.get().diskActivitySoundDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build();
                fileSystemAccessTimeouts.put(node, hostTimeouts);
            }
        }
        Long lastHostTimeout = hostTimeouts.getIfPresent(name);
        if (lastHostTimeout != null && lastHostTimeout > System.currentTimeMillis()) return;
        if (host == null) return;

        FileSystemAccessEvent.Server event;
        if (host instanceof BlockEntity) {
            event = new FileSystemAccessEvent.Server(name, (BlockEntity) host, node);
        } else {
            event = new FileSystemAccessEvent.Server(name, host.level(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        }
        FileSystemAccessEvent.Server.EVENT.invoker().onFileSystemAccessServer(event);
        if (event.isCanceled()) return;

        hostTimeouts.put(name, System.currentTimeMillis() + OCSettings.get().diskActivitySoundDelay);

        try (var pb = createBuilder(PacketType.FileSystemActivity)) {
            pb.writeUTF(event.getSound());
            NbtIo.write(event.getData(), pb);
            if (event.getBlockEntity() != null) {
                pb.writeBoolean(true);
                pb.writeBlockEntity(event.getBlockEntity());
            } else {
                pb.writeBoolean(false);
                pb.writeInt(event.getWorld().dimension().location().hashCode());
                pb.writeDouble(event.getX());
                pb.writeDouble(event.getY());
                pb.writeDouble(event.getZ());
            }
            pb.sendToPlayersNearHost(host, OCSettings.get().maxNetworkClientSoundPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendNetworkActivityImpl(Node node, EnvironmentHost host) {
        NetworkActivityEvent.Server event;
        if (host instanceof BlockEntity) {
            event = new NetworkActivityEvent.Server((BlockEntity) host, node);
        } else {
            event = new NetworkActivityEvent.Server(host.level(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        }
        NetworkActivityEvent.Server.EVENT.invoker().onNetworkActivityServer(event);
        if (event.isCanceled()) return;

        try (var pb = createBuilder(PacketType.NetworkActivity)) {
            NbtIo.write(event.getData(), pb);
            if (event.getBlockEntity() != null) {
                pb.writeBoolean(true);
                pb.writeBlockEntity(event.getBlockEntity());
            } else {
                pb.writeBoolean(false);
                pb.writeInt(event.getWorld().dimension().location().hashCode());
                pb.writeDouble(event.getX());
                pb.writeDouble(event.getY());
                pb.writeDouble(event.getZ());
            }
            pb.sendToPlayersNearHost(host, OCSettings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendNanomachineConfigurationImpl(Player player) {
        try (var pb = createBuilder(PacketType.NanomachinesConfiguration)) {
            pb.writeEntity(player);
            Object controller = li.cil.oc.api.Nanomachines.getController(player);
            if (controller instanceof ControllerImpl ci) {
                pb.writeBoolean(true);
                CompoundTag nbt = new CompoundTag();
                ci.save(nbt, player.level().registryAccess());
                pb.writeNBT(nbt);
            } else {
                pb.writeBoolean(false);
            }
            pb.sendToPlayersNearEntity(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendNanomachineInputsImpl(Player player) {
        Object controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl ci) {
            try (var pb = createBuilder(PacketType.NanomachinesInputs)) {
                pb.writeEntity(player);
                byte[] inputs = new byte[ci.configuration.triggers.size()];
                int i = 0;
                for (NeuralNetwork.TriggerNeuron trigger : ci.configuration.triggers) {
                    inputs[i++] = (byte) (trigger.isActive() ? 1 : 0);
                }
                pb.writeInt(inputs.length);
                pb.write(inputs);
                pb.sendToPlayersNearEntity(player);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void sendNanomachinePowerImpl(Player player) {
        Object controller = li.cil.oc.api.Nanomachines.getController(player);
        if (controller instanceof ControllerImpl ci) {
            try (var pb = createBuilder(PacketType.NanomachinesPower)) {
                pb.writeEntity(player);
                pb.writeDouble(ci.getLocalBuffer());
                pb.sendToPlayersNearEntity(player);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void sendPetVisibilityImpl(String name, ServerPlayer player) {
        try (var pb = createBuilder(PacketType.PetVisibility)) {
            if (name != null) {
                pb.writeInt(1);
                pb.writeUTF(name);
                pb.writeBoolean(!PetVisibility.hidden.contains(name));
            } else {
                pb.writeInt(PetVisibility.hidden.size());
                for (String n : PetVisibility.hidden) {
                    pb.writeUTF(n);
                    pb.writeBoolean(false);
                }
            }
            if (player != null) pb.sendToPlayer(player);
            else pb.sendToAllPlayers();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRackInventoryImpl(BlockEntity t, ItemStack[] items) {
        try (var pb = createBuilder(PacketType.RackInventory)) {
            pb.writeBlockEntity(t);
            pb.writeInt(items.length);
            for (int slot = 0; slot < items.length; slot++) {
                pb.writeInt(slot);
                var level = t.getLevel();
                pb.writeItemStack(items[slot], level != null ? level.registryAccess() : null);
            }
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRackMountableDataImpl(BlockEntity t, int mountable, CompoundTag data) {
        try (var pb = createBuilder(PacketType.RackMountableData)) {
            pb.writeBlockEntity(t);
            pb.writeInt(mountable);
            pb.writeNBT(data);
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRaidChangeImpl(BlockEntity t, boolean[] slots) {
        try (var pb = createBuilder(PacketType.RaidStateChange)) {
            pb.writeBlockEntity(t);
            for (boolean slot : slots) pb.writeBoolean(slot);
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRedstoneStateImpl(BlockEntity t, boolean outputEnabled, int[] output) {
        try (var pb = createBuilder(PacketType.RedstoneState)) {
            pb.writeBlockEntity(t);
            pb.writeBoolean(outputEnabled);
            for (int i = 0; i < 6; i++) {
                pb.writeByte((byte) (i < output.length ? output[i] : 0));
            }
            pb.sendToPlayersNearBlockEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
