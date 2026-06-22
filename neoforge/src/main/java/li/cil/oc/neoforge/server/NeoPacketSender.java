package li.cil.oc.neoforge.server;

import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.server.PetVisibility;
import li.cil.oc.neoforge.common.Loot;
import li.cil.oc.neoforge.common.SimplePacketBuilder;
import li.cil.oc.neoforge.common.nanomachines.ControllerImpl;
import li.cil.oc.neoforge.common.nanomachines.NeuralNetwork;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;


public final class NeoPacketSender extends PacketSender {
    @Override
    protected PacketBuilderBase<?> createBuilder(PacketType type) {
        return new SimplePacketBuilder(type);
    }

    @Override
    protected void sendClientLogImpl(String line, ServerPlayer player) {
        try (CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.ClientLog)) {
            pb.writeUTF(line);
            pb.sendToPlayer(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendHologramAreaImpl(BlockEntity t, byte dirtyFromX, byte dirtyUntilX, byte dirtyFromZ, byte dirtyUntilZ, int[] volume, int width) {
        try (CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.HologramArea)) {
            pb.writeTileEntity(t);
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
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendHologramValuesImpl(BlockEntity t, java.util.BitSet dirty, int[] volume, int width) {
        try (CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.HologramValues)) {
            pb.writeTileEntity(t);
            pb.writeInt(dirty.cardinality());
            for (int xz = dirty.nextSetBit(0); xz >= 0; xz = dirty.nextSetBit(xz + 1)) {
                byte x = (byte) (xz >> 8);
                byte z = (byte) xz;
                pb.writeShort(xz);
                int rangeStart = x + z * width;
                int rangeFinal = x + z * width + width * width;
                pb.writeInt(volume[Math.clamp(rangeStart, 0, volume.length - 1)]);
                pb.writeInt(volume[Math.clamp(rangeFinal, 0, volume.length - 1)]);
            }
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendLootDisksImpl(ServerPlayer p) {
        for (ItemStack[] entry : Loot.globalDisks) {
            try (PacketBuilderBase<?> pb = createBuilder(PacketType.LootDisk)) {
                pb.writeItemStack(entry[0], p.level().registryAccess());
                pb.sendToPlayer(p);
            }
        }
        for (ItemStack stack : Loot.disksForCyclingServer) {
            try (PacketBuilderBase<?> pb = createBuilder(PacketType.CyclingDisk)) {
                pb.writeItemStack(stack, p.level().registryAccess());
                pb.sendToPlayer(p);
            }
        }
    }

    @Override
    protected void sendFileSystemActivityImpl(Node node, EnvironmentHost host, String name) {
        com.google.common.cache.Cache<String, Long> hostTimeouts;
        synchronized (fileSystemAccessTimeouts) {
            hostTimeouts = fileSystemAccessTimeouts.get(node);
            if (hostTimeouts == null) {
                hostTimeouts = com.google.common.cache.CacheBuilder.newBuilder()
                        .concurrencyLevel(li.cil.oc.core.impl.Settings.get().threads)
                        .maximumSize(250)
                        .expireAfterWrite(li.cil.oc.core.impl.Settings.get().diskActivitySoundDelay, java.util.concurrent.TimeUnit.MILLISECONDS)
                        .build();
                fileSystemAccessTimeouts.put(node, hostTimeouts);
            }
        }
        Long lastHostTimeout = hostTimeouts.getIfPresent(name);
        if (lastHostTimeout != null && lastHostTimeout > System.currentTimeMillis()) return;
        if (host == null) return;

        FileSystemAccessEvent.Server event;
        if (host instanceof BlockEntity) {
            event = li.cil.oc.api.event.OCEventFactory.get().createFileSystemAccessEventServer(name, (BlockEntity) host, node);
        } else {
            event = li.cil.oc.api.event.OCEventFactory.get().createFileSystemAccessEventServer(name, host.level(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        }
        li.cil.oc.api.event.OCEventBus.post(event);
        if (event.isCanceled()) return;

        hostTimeouts.put(name, System.currentTimeMillis() + li.cil.oc.core.impl.Settings.get().diskActivitySoundDelay);

        try (PacketBuilderBase<?> pb = createBuilder(PacketType.FileSystemActivity)) {
            pb.writeUTF(event.sound());
            NbtIo.write(event.data(), pb);
            if (event.tileEntity() != null) {
                pb.writeBoolean(true);
                pb.writeTileEntity(event.tileEntity());
            } else {
                pb.writeBoolean(false);
                pb.writeInt(event.level().dimension().location().hashCode());
                pb.writeDouble(event.x());
                pb.writeDouble(event.y());
                pb.writeDouble(event.z());
            }
            pb.sendToPlayersNearHost(host, li.cil.oc.core.impl.Settings.get().maxNetworkClientSoundPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendNetworkActivityImpl(Node node, EnvironmentHost host) {
        NetworkActivityEvent.Server event;
        if (host instanceof BlockEntity) {
            event = li.cil.oc.api.event.OCEventFactory.get().createNetworkActivityEventServer((BlockEntity) host, node);
        } else {
            event = li.cil.oc.api.event.OCEventFactory.get().createNetworkActivityEventServer(host.level(), host.xPosition(), host.yPosition(), host.zPosition(), node);
        }
        li.cil.oc.api.event.OCEventBus.post(event);
        if (event.isCanceled()) return;

        try (PacketBuilderBase<?> pb = createBuilder(PacketType.NetworkActivity)) {
            NbtIo.write(event.data(), pb);
            if (event.tileEntity() != null) {
                pb.writeBoolean(true);
                pb.writeTileEntity(event.tileEntity());
            } else {
                pb.writeBoolean(false);
                pb.writeInt(event.level().dimension().location().hashCode());
                pb.writeDouble(event.x());
                pb.writeDouble(event.y());
                pb.writeDouble(event.z());
            }
            pb.sendToPlayersNearHost(host, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendNanomachineConfigurationImpl(Player player) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.NanomachinesConfiguration)) {
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
            try (PacketBuilderBase<?> pb = createBuilder(PacketType.NanomachinesInputs)) {
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
            try (PacketBuilderBase<?> pb = createBuilder(PacketType.NanomachinesPower)) {
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
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.PetVisibility)) {
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
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RackInventory)) {
            pb.writeTileEntity(t);
            pb.writeInt(items.length);
            for (int slot = 0; slot < items.length; slot++) {
                pb.writeInt(slot);
                var level = t.getLevel();
                pb.writeItemStack(items[slot], level != null ? level.registryAccess() : null);
            }
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRackMountableDataImpl(BlockEntity t, int mountable, CompoundTag data) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RackMountableData)) {
            pb.writeTileEntity(t);
            pb.writeInt(mountable);
            pb.writeNBT(data);
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRaidChangeImpl(BlockEntity t, boolean[] slots) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RaidStateChange)) {
            pb.writeTileEntity(t);
            for (boolean slot : slots) pb.writeBoolean(slot);
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotMoveImpl(BlockEntity t, BlockPosition position, Direction direction) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotMove)) {
            var level = t.getLevel();
            pb.writeInt(level != null ? level.dimension().location().hashCode() : 0);
            pb.writeInt(position.x());
            pb.writeInt(position.y());
            pb.writeInt(position.z());
            pb.writeDirection(direction);
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotAnimateSwingImpl(BlockEntity t, int animationTicksTotal) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotAnimateSwing)) {
            pb.writeTileEntity(t);
            pb.writeInt(animationTicksTotal);
            pb.sendToPlayersNearTileEntity(t, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotAnimateTurnImpl(BlockEntity t, byte turnAxis, int animationTicksTotal) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotAnimateTurn)) {
            pb.writeTileEntity(t);
            pb.writeByte(turnAxis);
            pb.writeInt(animationTicksTotal);
            pb.sendToPlayersNearTileEntity(t, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotInventoryImpl(BlockEntity t, int slot, ItemStack stack) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotInventoryChange)) {
            pb.writeTileEntity(t);
            pb.writeInt(slot);
            var robotLevel = t.getLevel();
            pb.writeItemStack(stack, robotLevel != null ? robotLevel.registryAccess() : null);
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotLightChangeImpl(BlockEntity t, int lightColor) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotLightChange)) {
            pb.writeTileEntity(t);
            pb.writeInt(lightColor);
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotNameChangeImpl(BlockEntity t, String name) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotNameChange)) {
            pb.writeTileEntity(t);
            short len = (short) name.length();
            pb.writeShort(len);
            for (int x = 0; x < len; x++) pb.writeChar(name.charAt(x));
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendRobotSelectedSlotChangeImpl(BlockEntity t, int selectedSlot) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotSelectedSlotChange)) {
            pb.writeTileEntity(t);
            pb.writeInt(selectedSlot);
            pb.sendToPlayersNearTileEntity(t, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance / 4.0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void sendTextBufferInitImpl(String address, CompoundTag value, ServerPlayer player) {
        try (CompressedPacketBuilder pb = new CompressedPacketBuilder(PacketType.TextBufferInit)) {
            pb.writeUTF(address);
            pb.writeNBT(value);
            pb.sendToPlayer(player);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final java.util.WeakHashMap<Node, com.google.common.cache.Cache<String, Long>> fileSystemAccessTimeouts = new java.util.WeakHashMap<>();

    @Override
    protected void sendRedstoneStateImpl(BlockEntity t, boolean outputEnabled, int[] output) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RedstoneState)) {
            pb.writeTileEntity(t);
            pb.writeBoolean(outputEnabled);
            for (int i = 0; i < 6; i++) {
                pb.writeByte((byte) (i < output.length ? output[i] : 0));
            }
            pb.sendToPlayersNearTileEntity(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
