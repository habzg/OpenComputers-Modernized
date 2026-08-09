package li.cil.oc.neoforge.client;

import io.netty.buffer.ByteBuf;
import java.io.IOException;
import java.util.Objects;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.client.ClientComponentTracker;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.common.component.TerminalServer;
import li.cil.oc.core.impl.common.nanomachines.ControllerImpl;
import li.cil.oc.core.impl.common.blockentity.Adapter;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Charger;
import li.cil.oc.core.impl.common.blockentity.Disassembler;
import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import li.cil.oc.core.impl.common.blockentity.Hologram;
import li.cil.oc.core.impl.common.blockentity.NetSplitter;
import li.cil.oc.core.impl.common.blockentity.Printer;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Raid;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.common.blockentity.traits.Colored;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.common.blockentity.traits.PowerInformation;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.common.blockentity.traits.SwitchLike;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.client.renderer.PetRenderer;
import li.cil.oc.neoforge.common.PacketHandler;
import li.cil.oc.neoforge.common.blockentity.Transposer;
import li.cil.oc.api.event.FileSystemAccessEvent;
import li.cil.oc.api.event.NetworkActivityEvent;
import li.cil.oc.neoforge.util.Audio;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;


public final class ClientPacketHandler extends PacketHandler {
    public static final ClientPacketHandler INSTANCE = new ClientPacketHandler();
    private static final int MAX_DROP_FILE_SIZE = 1024 * 1024;
    private final java.util.Map<net.minecraft.world.entity.player.Player, Long> lastDropTimes = new java.util.HashMap<>();

    public void onPacket(ByteBuf payload) {
        onPacketData(payload, Minecraft.getInstance().player);
    }

    @Override
    public Level world(Player player, int ignoredDimension) {
        return player.level();
    }

    @Override
    public void dispatch(PacketParser p) {
        switch (p.packetType) {
            case AdapterState -> onAdapterState(p);
            case Analyze -> onAnalyze(p);
            case ChargerState -> onChargerState(p);
            case ClientLog -> onClientLog(p);
            case Clipboard -> onClipboard(p);
            case DropFile -> onDropFile(p);
            case ColorChange -> onColorChange(p);
            case ComputerState -> onComputerState(p);
            case ComputerUserList -> onComputerUserList(p);
            case ContainerUpdate -> onContainerUpdate(p);
            case DisassemblerActiveChange -> onDisassemblerActiveChange(p);
            case FileSystemActivity -> onFileSystemActivity(p);
            case FloppyChange -> onFloppyChange(p);
            case HologramArea -> onHologramArea(p);
            case HologramClear -> onHologramClear(p);
            case HologramColor -> onHologramColor(p);
            case HologramPowerChange -> onHologramPowerChange(p);
            case HologramRotation -> onHologramRotation(p);
            case HologramRotationSpeed -> onHologramRotationSpeed(p);
            case HologramScale -> onHologramScale(p);
            case HologramTranslation -> onHologramPositionOffsetY(p);
            case HologramValues -> onHologramValues(p);
            case LootDisk -> onLootDisk(p);
            case CyclingDisk -> onCyclingDisk(p);
            case NanomachinesConfiguration -> onNanomachinesConfiguration(p);
            case NanomachinesInputs -> onNanomachinesInputs(p);
            case NanomachinesPower -> onNanomachinesPower(p);
            case NetSplitterState -> onNetSplitterState(p);
            case NetworkActivity -> onNetworkActivity(p);
            case ParticleEffect -> onParticleEffect(p);
            case PetVisibility -> onPetVisibility(p);
            case PowerState -> onPowerState(p);
            case PrinterState -> onPrinterState(p);
            case RackInventory -> onRackInventory(p);
            case RackMountableData -> onRackMountableData(p);
            case RaidStateChange -> onRaidStateChange(p);
            case RedstoneState -> onRedstoneState(p);
            case RobotAnimateSwing -> onRobotAnimateSwing(p);
            case RobotAnimateTurn -> onRobotAnimateTurn(p);
            case RobotAssemblingState -> onRobotAssemblingState(p);
            case RobotInventoryChange -> onRobotInventoryChange(p);
            case RobotLightChange -> onRobotLightChange(p);
            case RobotMove -> onRobotMove(p);
            case RobotNameChange -> onRobotNameChange(p);
            case RobotSelectedSlotChange -> onRobotSelectedSlotChange(p);
            case RotatableState -> onRotatableState(p);
            case SwitchActivity -> onSwitchActivity(p);
            case OpenTabletTerminal -> onOpenTabletTerminal(p);
            case MachineItemStateResponse -> onMachineItemStateResponse(p);
            case TextBufferInit -> onTextBufferInit(p);
            case TextBufferPowerChange -> onTextBufferPowerChange(p);
            case TextBufferMulti -> onTextBufferMulti(p);
            case ScreenTouchMode -> onScreenTouchMode(p);
            case Sound -> onSound(p);
            case SoundPattern -> onSoundPattern(p);
            case TransposerActivity -> onTransposerActivity(p);
            case WaypointLabel -> onWaypointLabel(p);
            default -> {
            }
        }
    }

    private void onAdapterState(PacketParser p) {
        try {
            Adapter t = p.readBlockEntity(Adapter.class);
            if (t != null) {
                t.uncompressSides(p.readByte());
                var level = t.getLevel();
                if (level != null) {
                    level.markAndNotifyBlock(t.getBlockPos(), level.getChunkAt(t.getBlockPos()), t.getBlockState(), t.getBlockState(), 3, 512);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onAnalyze(PacketParser p) {
        try {
            String address = p.readUTF();
            if (GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                    GLFW.glfwGetKey(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS) {
                Minecraft.getInstance().keyboardHandler.setClipboard(address);
                p.player.sendSystemMessage(Component.translatable("gui.opencomputers.analyzer.addresscopied"));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onChargerState(PacketParser p) {
        try {
            Charger t = p.readBlockEntity(Charger.class);
            if (t != null) {
                t.chargeSpeed = p.readDouble();
                t.hasPower = p.readBoolean();
                var level = t.getLevel();
                if (level != null) {
                    level.markAndNotifyBlock(t.getBlockPos(), level.getChunkAt(t.getBlockPos()), t.getBlockState(), t.getBlockState(), 3, 512);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onClientLog(PacketParser p) {
        try {
            OpenComputers.log().info(p.readUTF());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onClipboard(PacketParser p) {
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(p.readUTF());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onDropFile(PacketParser p) {
        try {
            String fileName = p.readUTF();
            if (fileName.isEmpty() || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                OpenComputers.log().warn("Invalid drop file name: {}", fileName);
                return;
            }
            long now = System.currentTimeMillis();
            Long lastDrop = lastDropTimes.get(p.player);
            if (lastDrop != null && now - lastDrop < 1000) {
                OpenComputers.log().warn("Drop file rate limit exceeded for player {}", p.player.getName().getString());
                return;
            }
            lastDropTimes.put(p.player, now);
            int fileSize = p.readInt();
            if (fileSize <= 0 || fileSize > MAX_DROP_FILE_SIZE) {
                OpenComputers.log().warn("Invalid drop file size: {}", fileSize);
                return;
            }
            byte[] data = new byte[fileSize];
            p.readFully(data);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onColorChange(PacketParser p) {
        try {
            Colored t = p.readBlockEntity(Colored.class);
            if (t != null) {
                t.color(p.readInt());
                if (t instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                    var level = be.getLevel();
                    if (level != null) {
                        level.markAndNotifyBlock(be.getBlockPos(), level.getChunkAt(be.getBlockPos()), be.getBlockState(), be.getBlockState(), 3, 512);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onComputerState(PacketParser p) {
        try {
            Computer t = p.readBlockEntity(Computer.class);
            if (t != null) {
                boolean running = p.readBoolean();
                t.setRunning(running);
                t.hasErrored(p.readBoolean());
                if (t instanceof net.minecraft.world.level.block.entity.BlockEntity be) {
                    if (running) {
                        var level = be.getLevel();
                        var sound = t.runSound();
                        if (level != null && sound != null) {
                            Sound.startLoop(be, sound, 0.5f, 50 + level.random.nextInt(50));
                        }
                    } else {
                        Sound.stopLoop(be);
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onComputerUserList(PacketParser p) {
        try {
            Computer t = p.readBlockEntity(Computer.class);
            if (t != null) {
                int count = p.readInt();
                if (count < 0 || count > 1024) {
                    OpenComputers.log().warn("Received invalid computer user list count: {}", count);
                    return;
                }
                java.util.List<String> users = new java.util.ArrayList<>();
                for (int i = 0; i < count; i++) users.add(p.readUTF());
                t.setUsers(users);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onContainerUpdate(PacketParser p) {
        try {
            int windowId = p.readUnsignedByte();
            if (p.player.containerMenu.containerId == windowId) {
                if (p.player.containerMenu instanceof li.cil.oc.core.impl.common.container.Player) {
                    ((li.cil.oc.core.impl.common.container.Player) p.player.containerMenu).updateCustomData(p.readNBT());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onDisassemblerActiveChange(PacketParser p) {
        try {
            Disassembler t = p.readBlockEntity(Disassembler.class);
            if (t != null) {
                t.isActive = p.readBoolean();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onFileSystemActivity(PacketParser p) {
        try {
            String sound = p.readUTF();
            var data = NbtIo.read(p, NbtAccounter.create(0x200000L));
            if (p.readBoolean()) {
                net.minecraft.world.level.block.entity.BlockEntity t = p.readBlockEntity(net.minecraft.world.level.block.entity.BlockEntity.class);
                if (t != null) {
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, t, data));
                }
            } else {
                Level w = world(p.player, p.readInt());
                double x = p.readDouble();
                double y = p.readDouble();
                double z = p.readDouble();
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new FileSystemAccessEvent.Client(sound, w, x, y, z, data));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onNetworkActivity(PacketParser p) {
        try {
            var data = NbtIo.read(p, NbtAccounter.create(0x200000L));
            if (p.readBoolean()) {
                net.minecraft.world.level.block.entity.BlockEntity t = p.readBlockEntity(net.minecraft.world.level.block.entity.BlockEntity.class);
                if (t != null) {
                    net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new NetworkActivityEvent.Client(t, data));
                }
            } else {
                Level w = world(p.player, p.readInt());
                double x = p.readDouble();
                double y = p.readDouble();
                double z = p.readDouble();
                net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new NetworkActivityEvent.Client(w, x, y, z, data));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onFloppyChange(PacketParser p) {
        DiskDrive t = p.readBlockEntity(DiskDrive.class);
        if (t != null) {
            t.setItem(0, p.readItemStack());
        }
    }

    private void onHologramClear(PacketParser p) {
        Hologram t = p.readBlockEntity(Hologram.class);
        if (t != null) {
            java.util.Arrays.fill(t.volume, 0);
            t.needsRendering = true;
        }
    }

    private void onHologramColor(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                int index = p.readInt();
                int value = p.readInt();
                if (index < 0 || index >= t.colors.length) {
                    OpenComputers.log().warn("Received HologramColor with out-of-range index {}", index);
                    return;
                }
                t.colors[index] = value & 0xFFFFFF;
                t.needsRendering = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramPowerChange(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                t.hasPower = p.readBoolean();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramScale(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                t.scale = p.readDouble();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramArea(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                int fromX = Math.max(0, p.readByte());
                int untilX = Math.min(Hologram.WIDTH, p.readByte());
                int fromZ = Math.max(0, p.readByte());
                int untilZ = Math.min(Hologram.WIDTH, p.readByte());
                if (untilX < fromX || untilZ < fromZ) return;
                for (int x = fromX; x < untilX; x++) {
                    for (int z = fromZ; z < untilZ; z++) {
                        t.volume[x + z * Hologram.WIDTH] = p.readInt();
                        t.volume[x + z * Hologram.WIDTH + Hologram.WIDTH * Hologram.WIDTH] = p.readInt();
                    }
                }
                t.needsRendering = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramValues(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                int count = p.readInt();
                for (int i = 0; i < count; i++) {
                    int xz = p.readShort();
                    int x = (xz >> 8) & 0xFF;
                    int z = xz & 0xFF;
                    if (x >= Hologram.WIDTH || z >= Hologram.WIDTH) continue;
                    t.volume[x + z * Hologram.WIDTH] = p.readInt();
                    t.volume[x + z * Hologram.WIDTH + Hologram.WIDTH * Hologram.WIDTH] = p.readInt();
                }
                t.needsRendering = true;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramPositionOffsetY(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                t.translationX = p.readDouble();
                t.translationY = p.readDouble();
                t.translationZ = p.readDouble();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramRotation(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                t.rotationAngle = p.readFloat();
                t.rotationX = p.readFloat();
                t.rotationY = p.readFloat();
                t.rotationZ = p.readFloat();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onHologramRotationSpeed(PacketParser p) {
        try {
            Hologram t = p.readBlockEntity(Hologram.class);
            if (t != null) {
                t.rotationSpeed = p.readFloat();
                t.rotationSpeedX = p.readFloat();
                t.rotationSpeedY = p.readFloat();
                t.rotationSpeedZ = p.readFloat();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onLootDisk(PacketParser p) {
        var stack = p.readItemStack();
        if (stack != null) {
            if (LootManager.pendingDiskSync) {
                LootManager.pendingDiskSync = false;
                LootManager.disksForClient.clear();
            }
            LootManager.disksForClient.add(stack);
        }
    }

    private void onCyclingDisk(PacketParser p) {
        var stack = p.readItemStack();
        if (stack != null) {
            LootManager.disksForCyclingClient.add(stack);
        }
    }

    private void onNanomachinesConfiguration(PacketParser p) {
        try {
            Player player = p.readEntity(Player.class);
            if (player != null) {
                boolean hasController = p.readBoolean();
                if (hasController) {
                    Object controller = li.cil.oc.api.Nanomachines.installController(player);
                    if (controller instanceof ControllerImpl) {
                        ((ControllerImpl) controller).load(p.readNBT(), p.player.level().registryAccess());
                    }
                } else {
                    li.cil.oc.api.Nanomachines.uninstallController(player);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onNanomachinesInputs(PacketParser p) {
        try {
            Player player = p.readEntity(Player.class);
            if (player != null) {
                Object controller = li.cil.oc.api.Nanomachines.getController(player);
                if (controller instanceof ControllerImpl ctrl) {
                    int count = p.readInt();
                    if (count < 0 || count > 256) {
                        OpenComputers.log().warn("Received invalid input count from nanomachines: {}", count);
                        return;
                    }
                    byte[] inputs = new byte[count];
                    p.readFully(inputs);
                    synchronized (ctrl.configuration) {
                        for (int i = 0; i < inputs.length && i < ctrl.configuration.triggers.size(); i++) {
                            ctrl.configuration.triggers.get(i).isActive = inputs[i] == 1;
                        }
                        ctrl.activeBehaviorsDirty = true;
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onNanomachinesPower(PacketParser p) {
        try {
            Player player = p.readEntity(Player.class);
            if (player != null) {
                Object controller = li.cil.oc.api.Nanomachines.getController(player);
                if (controller instanceof ControllerImpl) {
                    ((ControllerImpl) controller).storedEnergy = p.readDouble();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onNetSplitterState(PacketParser p) {
        try {
            NetSplitter t = p.readBlockEntity(NetSplitter.class);
            if (t != null) {
                t.isInverted = p.readBoolean();
                t.uncompressSides(p.readByte());
                var level = t.getLevel();
                if (level != null) {
                    level.markAndNotifyBlock(t.getBlockPos(), level.getChunkAt(t.getBlockPos()), t.getBlockState(), t.getBlockState(), 3, 512);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onParticleEffect(PacketParser p) {
        try {
            int dimension = p.readInt();
            Level w = world(p.player, dimension);
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            double velocity = p.readDouble();
            Direction direction = p.readDirection();
            String particleName = p.readUTF();
            int count = p.readUnsignedByte() / (1 << Minecraft.getInstance().options.particles().get().getId());

            net.minecraft.core.particles.ParticleOptions particle = null;
            var key = net.minecraft.resources.ResourceLocation.tryParse(particleName);
            if (key != null) {
                var type = net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.get(key);
                if (type instanceof net.minecraft.core.particles.ParticleOptions opts) {
                    particle = opts;
                }
            }

            for (int i = 0; i < count; i++) {
                double vx, vy, vz;
                if (direction != null) {
                    vx = w.random.nextFloat() - 0.5 + direction.getStepX() * 0.5;
                    vy = w.random.nextFloat() - 0.5 + direction.getStepY() * 0.5;
                    vz = w.random.nextFloat() - 0.5 + direction.getStepZ() * 0.5;
                } else {
                    vx = w.random.nextFloat() * 2.0 - 1;
                    vy = w.random.nextFloat() * 2.0 - 1;
                    vz = w.random.nextFloat() * 2.0 - 1;
                }
                if (vx * vx + vy * vy + vz * vz < 1) {
                    double px, py, pz;
                    if (direction != null) {
                        px = x + 0.5 + vx * velocity * 0.5 + direction.getStepX() * velocity;
                        py = y + 0.5 + vy * velocity * 0.5 + direction.getStepY() * velocity;
                        pz = z + 0.5 + vz * velocity * 0.5 + direction.getStepZ() * velocity;
                    } else {
                        px = x + 0.5 + vx * velocity;
                        py = y + 0.5 + vy * velocity;
                        pz = z + 0.5 + vz * velocity;
                    }
                    w.addParticle(Objects.requireNonNullElseGet(particle, () -> new DustParticleOptions(new Vector3f(1.0f, 0.0f, 0.0f), 1.0f)), px, py, pz, vx, vy + velocity * 0.25, vz);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onPetVisibility(PacketParser p) {
        try {
            if (!PetRenderer.isInitialized) {
                PetRenderer.isInitialized = true;
                if (OCSettings.get().hideOwnPet) {
                    var player = Minecraft.getInstance().player;
                    if (player != null) PetRenderer.hidden.add(player.getScoreboardName());
                }
                PacketSender.sendPetVisibility();
            }

            int count = p.readInt();
            if (count < 0 || count > 1024) {
                OpenComputers.log().warn("Received invalid pet visibility count: {}", count);
                return;
            }
            for (int i = 0; i < count; i++) {
                String name = p.readUTF();
                if (p.readBoolean()) {
                    PetRenderer.hidden.remove(name);
                } else {
                    PetRenderer.hidden.add(name);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onPowerState(PacketParser p) {
        try {
            PowerInformation t = p.readBlockEntity(PowerInformation.class);
            if (t != null) {
                t.globalBuffer(p.readDouble());
                t.globalBufferSize(p.readDouble());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onPrinterState(PacketParser p) {
        try {
            Printer t = p.readBlockEntity(Printer.class);
            if (t != null) {
                if (p.readBoolean()) t.requiredEnergy = 9001;
                else t.requiredEnergy = 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRackInventory(PacketParser p) {
        try {
            Rack t = p.readBlockEntity(Rack.class);
            if (t != null) {
                int count = Math.min(p.readInt(), t.getContainerSize());
                if (count < 0) count = 0;
                for (int i = 0; i < count; i++) {
                    int slot = p.readInt();
                    if (slot < 0 || slot >= t.getContainerSize()) continue;
                    var stack = p.readItemStack();
                    t.updateItems(slot, stack);
                }
                t.connectComponents();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRackMountableData(PacketParser p) {
        try {
            Rack t = p.readBlockEntity(Rack.class);
            if (t != null) {
                int mountableIndex = p.readInt();
                if (mountableIndex < 0 || mountableIndex >= t.lastData.length) {
                    OpenComputers.log().warn("Received invalid mountable index: {}", mountableIndex);
                    return;
                }
                var data = p.readNBT();
                t.lastData[mountableIndex] = data;
                TerminalServer.TerminalServerCache.loaded.completePending();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRaidStateChange(PacketParser p) {
        try {
            Raid t = p.readBlockEntity(Raid.class);
            if (t != null) {
                for (int slot = 0; slot < t.getContainerSize(); slot++) {
                    t.presence[slot] = p.readBoolean();
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRedstoneState(PacketParser p) {
        try {
            RedstoneAware t = p.readBlockEntity(RedstoneAware.class);
            if (t != null) {
                t.setOutputEnabled(p.readBoolean());
                for (Direction d : Direction.values()) {
                    t.setOutput(d, p.readByte());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotAnimateSwing(PacketParser p) {
        try {
            var t = p.readBlockEntity(li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                t.setAnimateSwing(p.readInt());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotAnimateTurn(PacketParser p) {
        try {
            var t = p.readBlockEntity(li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                t.setAnimateTurn(p.readByte(), p.readInt());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotAssemblingState(PacketParser p) {
        try {
            Assembler t = p.readBlockEntity(Assembler.class);
            if (t != null) {
                if (p.readBoolean()) t.requiredEnergy = 9001;
                else t.requiredEnergy = 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotInventoryChange(PacketParser p) {
        try {
            var t = p.readBlockEntity(li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                int slot = p.readInt();
                var stack = p.readItemStack();
                if (slot < 0 || slot >= t.getContainerSize()) {
                    OpenComputers.log().warn("Received robot inventory change with out-of-bounds slot: {}", slot);
                    return;
                }
                if (slot >= t.getContainerSize() - t.componentCount()) {
                    int compIdx = slot - (t.getContainerSize() - t.componentCount());
                    if (compIdx >= 0 && compIdx < t.info.components.size()) {
                        t.info.components.set(compIdx, stack);
                    }
                    var comps = t.robot._components();
                    if (comps != null && slot < comps.length && comps[slot] != null && !stack.isEmpty()) {
                        var driver = li.cil.oc.api.API.driver.driverFor(stack);
                        if (driver != null) {
                            try {
                                var data = driver.dataTag(stack);
                                if (data == null) {
                                    data = li.cil.oc.core.impl.integration.opencomputers.Item.getDataTag(stack);
                                }
                                comps[slot].load(data, t.level().registryAccess());
                            } catch (Throwable e) {
                                OpenComputers.log().warn("Failed to reload component in slot {} from sync data", slot, e);
                            }
                        }
                    }
                } else {
                    t.setItem(slot, stack);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotLightChange(PacketParser p) {
        try {
            int dimension = p.readInt();
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            int lightColor = p.readInt();
            var t = p.getBlockEntity(dimension, x, y, z, li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                t.info.lightColor = lightColor;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotNameChange(PacketParser p) {
        try {
            int dimension = p.readInt();
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            int len = p.readShort();
            if (len < 0 || len > 1024) {
                OpenComputers.log().warn("Received robot name change with invalid length: {}", len);
                return;
            }
            StringBuilder name = new StringBuilder(len);
            for (int i = 0; i < len; i++) {
                name.append(p.readChar());
            }
            var t = p.getBlockEntity(dimension, x, y, z, li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                t.setName(name.toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotMove(PacketParser p) {
        try {
            int dimension = p.readInt();
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            Direction direction = p.readDirection();
            var t = p.getBlockEntity(dimension, x, y, z, li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null && direction != null) {
                t.move(direction);
            } else if (direction != null) {
                var oldPos = new net.minecraft.core.BlockPos(x, y, z);
                var newPos = oldPos.relative(direction);
                var newProxy = p.getBlockEntity(dimension, newPos.getX(), newPos.getY(), newPos.getZ(), li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
                if (newProxy != null) {
                    var moveTicks = Math.max((int) (li.cil.oc.core.impl.OCSettings.get().moveDelay * 20), 1);
                    newProxy.setAnimateMove(li.cil.oc.core.impl.util.BlockPosition.apply(x, y, z, newProxy.getLevel()), moveTicks);
                } else {
                    PacketSender.sendRobotStateRequest(dimension, newPos.getX(), newPos.getY(), newPos.getZ());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRobotSelectedSlotChange(PacketParser p) {
        try {
            int dimension = p.readInt();
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            int value = p.readInt();
            var t = p.getBlockEntity(dimension, x, y, z, li.cil.oc.neoforge.common.blockentity.RobotProxy.class);
            if (t != null) {
                t.robot.selectedSlot = value;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onRotatableState(PacketParser p) {
        Rotatable t = p.readBlockEntity(Rotatable.class);
        if (t != null) {
            Direction pitch = p.readDirection();
            Direction yaw = p.readDirection();
            if (pitch != null && yaw != null && t instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
                screen.trySetPitchYaw(pitch, yaw);
            }
        }
    }

    private void onSwitchActivity(PacketParser p) {
        SwitchLike t = p.readBlockEntity(SwitchLike.class);
        if (t != null) {
            t.lastMessage(System.currentTimeMillis());
        }
    }

    private void onTextBufferPowerChange(PacketParser p) {
        try {
            li.cil.oc.api.network.ManagedEnvironment env = ClientComponentTracker.INSTANCE.get(p.player.level(), p.readUTF());
            if (env instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                buffer.setRenderingEnabled(p.readBoolean());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onOpenTabletTerminal(PacketParser p) {
        String address;
        try {
            address = p.readUTF();
            p.readNBT();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var mc = net.minecraft.client.Minecraft.getInstance();
        var player = mc.player;
        if (player == null) return;
        var stack = player.getMainHandItem();
        var cache = li.cil.oc.core.impl.util.TabletCache.forSide(true);
        if (cache != null) {
            cache.invalidate(stack);
        }
        var host = li.cil.oc.core.impl.common.item.Tablet.get(stack, player);
        host.update();
        li.cil.oc.neoforge.client.PacketSender.sendMachineItemStateRequest(stack);
        for (ManagedEnvironment env : host.componentEnvironments()) {
            if (env instanceof li.cil.oc.core.impl.common.component.TextBuffer buffer) {
                if (buffer.proxy.nodeAddress.isEmpty() && !address.isEmpty()) {
                    buffer.proxy.nodeAddress = address;
                    var level = buffer.host().level();
                    if (level != null) {
                        li.cil.oc.core.impl.client.ClientComponentTracker.INSTANCE.add(level, address, buffer);
                    }
                    if (!li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.contains(buffer)) {
                        li.cil.oc.core.impl.common.component.TextBuffer.clientBuffers.add(buffer);
                    }
                    li.cil.oc.neoforge.client.PacketSender.sendTextBufferInit(address);
                }
                break;
            }
        }
        for (ManagedEnvironment env : host.componentEnvironments()) {
            if (env instanceof TextBuffer buffer) {
                var inner = new li.cil.oc.core.impl.client.gui.Screen(buffer, true, () -> true, buffer::isRenderingEnabled);
                Minecraft.getInstance().setScreen(new net.minecraft.client.gui.screens.Screen(net.minecraft.network.chat.Component.literal("tablet")) {
                    @Override
                    public boolean isPauseScreen() {
                        return false;
                    }

                    @Override
                    protected void init() {
                        super.init();
                        li.cil.oc.core.impl.client.renderer.gui.BufferRenderer.init(Minecraft.getInstance().getTextureManager());
                        inner.setGuiSize(this.width, this.height);
                    }

                    @Override
                    public void render(net.minecraft.client.gui.@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float dt) {
                        renderBackground(guiGraphics, mouseX, mouseY, dt);
                        inner.render(guiGraphics, mouseX, mouseY, dt);
                    }

                    @Override
                    public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        return inner.mouseClicked(mouseX, mouseY, button);
                    }

                    @Override
                    public boolean mouseReleased(double mouseX, double mouseY, int button) {
                        return inner.mouseReleased(mouseX, mouseY, button);
                    }

                    @Override
                    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
                        return inner.mouseDragged(mouseX, mouseY, button, dragX, dragY);
                    }

                    @Override
                    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDeltaX, double scrollDeltaY) {
                        return inner.mouseScrolled(mouseX, mouseY, scrollDeltaY);
                    }

                    @Override
                    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                        return inner.handleKeyPress(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
                    }

                    @Override
                    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
                        return inner.handleKeyRelease(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
                    }

                    @Override
                    public boolean charTyped(char codePoint, int modifiers) {
                        return inner.handleCharTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
                    }
                });
                return;
            }
        }
    }

    private void onMachineItemStateResponse(PacketParser p) {
        try {
            var packetStack = p.readItemStack();
            var running = p.readBoolean();
            if (packetStack != null) {
                var player = Minecraft.getInstance().player;
                if (player != null) {
                    var packetId = li.cil.oc.core.impl.util.TabletCache.getOrCreateId(packetStack);
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        var invStack = player.getInventory().getItem(i);
                        if (!invStack.isEmpty()) {
                            var invId = li.cil.oc.core.impl.util.TabletCache.getOrCreateId(invStack);
                            if (invId.equals(packetId)) {
                                var data = new li.cil.oc.core.impl.common.item.data.TabletData(packetStack);
                                data.isRunning = running;
                                data.save(invStack);
                                break;
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferInit(PacketParser p) {
        try {
            String addr = p.readUTF();
            li.cil.oc.api.network.ManagedEnvironment bufEnv = ClientComponentTracker.INSTANCE.get(p.player.level(), addr);
            if (bufEnv instanceof li.cil.oc.core.impl.common.component.TextBuffer buffer) {
                var nbt = p.readNBT();
                if (nbt.contains("maxWidth")) {
                    int maxWidth = nbt.getInt("maxWidth");
                    int maxHeight = nbt.getInt("maxHeight");
                    buffer.setMaximumResolution(maxWidth, maxHeight);
                }
                buffer.data.load(nbt, p.player.level().registryAccess());
                if (nbt.contains("viewportWidth")) {
                    int viewportWidth = nbt.getInt("viewportWidth");
                    int viewportHeight = nbt.getInt("viewportHeight");
                    buffer.setViewport(viewportWidth, viewportHeight);
                }
                buffer.proxy.markDirty();
                buffer.markInitialized();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMulti(PacketParser p) {
        try {
            String addr = p.readUTF();
            li.cil.oc.api.network.ManagedEnvironment bufEnv2 = ClientComponentTracker.INSTANCE.get(p.player.level(), addr);
            if (bufEnv2 instanceof li.cil.oc.api.internal.TextBuffer buffer) {
                multiLoop:
                while (true) {
                    PacketType type = p.readPacketType();
                    if (type == null) break;
                    switch (type) {
                        case TextBufferMultiColorChange -> onTextBufferMultiColorChange(p, buffer);
                        case TextBufferMultiCopy -> onTextBufferMultiCopy(p, buffer);
                        case TextBufferMultiDepthChange -> onTextBufferMultiDepthChange(p, buffer);
                        case TextBufferMultiFill -> onTextBufferMultiFill(p, buffer);
                        case TextBufferMultiPaletteChange -> onTextBufferMultiPaletteChange(p, buffer);
                        case TextBufferMultiResolutionChange -> onTextBufferMultiResolutionChange(p, buffer);
                        case TextBufferMultiViewportResolutionChange ->
                                onTextBufferMultiViewportResolutionChange(p, buffer);
                        case TextBufferMultiMaxResolutionChange -> onTextBufferMultiMaxResolutionChange(p, buffer);
                        case TextBufferMultiSet -> onTextBufferMultiSet(p, buffer);
                        case TextBufferRamInit -> onTextBufferRamInit(p, buffer);
                        case TextBufferBitBlt -> onTextBufferBitBlt(p, buffer);
                        case TextBufferRamDestroy -> onTextBufferRamDestroy(p, buffer);
                        case TextBufferMultiRawSetText -> onTextBufferMultiRawSetText(p, buffer);
                        case TextBufferMultiRawSetBackground -> onTextBufferMultiRawSetBackground(p, buffer);
                        case TextBufferMultiRawSetForeground -> onTextBufferMultiRawSetForeground(p, buffer);
                        default -> {
                            break multiLoop;
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiColorChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int foreground = p.readInt();
            boolean foregroundIsPalette = p.readBoolean();
            buffer.setForegroundColor(foreground, foregroundIsPalette);
            int background = p.readInt();
            boolean backgroundIsPalette = p.readBoolean();
            buffer.setBackgroundColor(background, backgroundIsPalette);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiCopy(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int w = p.readInt();
            int h = p.readInt();
            int tx = p.readInt();
            int ty = p.readInt();
            buffer.copy(col, row, w, h, tx, ty);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiDepthChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            buffer.setColorDepth(li.cil.oc.api.internal.TextBuffer.ColorDepth.values()[p.readInt()]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiFill(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int w = p.readInt();
            int h = p.readInt();
            int c = p.readMedium();
            buffer.fill(col, row, w, h, c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiPaletteChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int index = p.readInt();
            int color = p.readInt();
            buffer.setPaletteColor(index, color);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int w = p.readInt();
            int h = p.readInt();
            buffer.setResolution(w, h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiViewportResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int w = p.readInt();
            int h = p.readInt();
            buffer.setViewport(w, h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiMaxResolutionChange(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int w = p.readInt();
            int h = p.readInt();
            buffer.setMaximumResolution(w, h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiSet(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            String s = p.readUTF();
            boolean vertical = p.readBoolean();
            buffer.set(col, row, s, vertical);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferRamInit(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            String owner = p.readUTF();
            int id = p.readInt();
            var nbt = p.readNBT();
            li.cil.oc.core.impl.common.component.GpuTextBuffer.ClientGpuTextBufferHandler.loadBuffer(buffer, owner, id, nbt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferBitBlt(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int w = p.readInt();
            int h = p.readInt();
            String owner = p.readUTF();
            int id = p.readInt();
            int fromCol = p.readInt();
            int fromRow = p.readInt();
            li.cil.oc.core.impl.common.component.GpuTextBuffer.ClientGpuTextBufferHandler.bitblt(buffer, col, row, w, h, owner, id, fromCol, fromRow);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferRamDestroy(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            String owner = p.readUTF();
            int id = p.readInt();
            li.cil.oc.core.impl.common.component.GpuTextBuffer.ClientGpuTextBufferHandler.removeBuffer(buffer, owner, id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiRawSetText(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int maxRows = buffer.getMaximumHeight();
            int maxCols = buffer.getMaximumWidth();
            int rows = Math.min(p.readShort(), maxRows);
            if (rows < 0) rows = 0;
            int[][] text = new int[rows][];
            for (int y = 0; y < rows; y++) {
                int cols = Math.min(p.readShort(), maxCols);
                if (cols < 0) cols = 0;
                int[] line = new int[cols];
                for (int x = 0; x < cols; x++) {
                    line[x] = p.readMedium();
                }
                text[y] = line;
            }
            buffer.rawSetText(col, row, text);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiRawSetBackground(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int maxRows = buffer.getMaximumHeight();
            int maxCols = buffer.getMaximumWidth();
            int rows = Math.min(p.readShort(), maxRows);
            if (rows < 0) rows = 0;
            int[][] color = new int[rows][];
            for (int y = 0; y < rows; y++) {
                int cols = Math.min(p.readShort(), maxCols);
                if (cols < 0) cols = 0;
                int[] line = new int[cols];
                for (int x = 0; x < cols; x++) {
                    line[x] = p.readInt();
                }
                color[y] = line;
            }
            buffer.rawSetBackground(col, row, color);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTextBufferMultiRawSetForeground(PacketParser p, li.cil.oc.api.internal.TextBuffer buffer) {
        try {
            int col = p.readInt();
            int row = p.readInt();
            int maxRows = buffer.getMaximumHeight();
            int maxCols = buffer.getMaximumWidth();
            int rows = Math.min(p.readShort(), maxRows);
            if (rows < 0) rows = 0;
            int[][] color = new int[rows][];
            for (int y = 0; y < rows; y++) {
                int cols = Math.min(p.readShort(), maxCols);
                if (cols < 0) cols = 0;
                int[] line = new int[cols];
                for (int x = 0; x < cols; x++) {
                    line[x] = p.readInt();
                }
                color[y] = line;
            }
            buffer.rawSetForeground(col, row, color);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onScreenTouchMode(PacketParser p) {
        try {
            Screen t = p.readBlockEntity(Screen.class);
            if (t != null) {
                t.invertTouchMode = p.readBoolean();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onSound(PacketParser p) {
        try {
            int dimension = p.readInt();
            world(p.player, dimension);
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            short frequency = p.readShort();
            short duration = p.readShort();
            Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, frequency, duration);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onSoundPattern(PacketParser p) {
        try {
            int dimension = p.readInt();
            world(p.player, dimension);
            int x = p.readInt();
            int y = p.readInt();
            int z = p.readInt();
            String pattern = p.readUTF();
            Audio.play(x + 0.5f, y + 0.5f, z + 0.5f, pattern);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void onTransposerActivity(PacketParser p) {
        Transposer t = p.readBlockEntity(Transposer.class);
        if (t != null) {
            t.lastOperation = System.currentTimeMillis();
        }
    }

    private void onWaypointLabel(PacketParser p) {
        try {
            Waypoint t = p.readBlockEntity(Waypoint.class);
            if (t != null) {
                t.label = p.readUTF();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
