package li.cil.oc.fabric.server;

import java.io.IOException;
import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.common.item.traits.FileSystemLike;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.Achievement;
import li.cil.oc.core.impl.common.component.TextBuffer;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.server.PetVisibility;
import li.cil.oc.fabric.common.network.CommonPacketHandler;
import li.cil.oc.fabric.common.blockentity.RobotProxy;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class ServerPacketHandler extends CommonPacketHandler {
    public static final ServerPacketHandler INSTANCE = new ServerPacketHandler();
    private static final Marker securityMarker = MarkerFactory.getMarker("SuspiciousPackets");

    private ServerPacketHandler() {}

    private static void logForgedPacket(ServerPlayer player) {
        li.cil.oc.fabric.OpenComputers.log().warn(securityMarker, "Player {} tried to send GUI packets without opening them", player.getGameProfile());
    }

    @Override
    protected Level world(Player player, int dimension) {
        return player instanceof ServerPlayer sp ? sp.serverLevel() : null;
    }

    @Override
    public void dispatch(PacketParser p) {
        try {
            switch (p.packetType) {
                case ComputerPower -> onComputerPower(p);
                case CopyToAnalyzer -> onCopyToAnalyzer(p);
                case DatabaseSetSlot -> onDatabaseSetSlot(p);
                case DriveLock -> onDriveLock(p);
                case DriveMode -> onDriveMode(p);
                case DronePower -> onDronePower(p);
                case KeyDown -> onKeyDown(p);
                case KeyUp -> onKeyUp(p);
                case Clipboard -> onClipboard(p);
                case DropFile -> onDropFile(p);
                case MouseClickOrDrag -> onMouseClick(p);
                case MouseScroll -> onMouseScroll(p);
                case MouseUp -> onMouseUp(p);
                case PetVisibility -> onPetVisibility(p);
                case RackMountableMapping -> onRackMountableMapping(p);
                case RackRelayState -> onRackRelayState(p);
                case RobotAssemblerStart -> onRobotAssemblerStart(p);
                case RobotStateRequest -> onRobotStateRequest(p);
                case ServerPower -> onServerPower(p);
                case TextBufferInit -> onTextBufferInit(p);
                case MachineItemStateRequest -> onMachineItemStateRequest(p);
                case WaypointLabel -> onWaypointLabel(p);
                default -> {}
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void trySetComputerPower(Machine computer, boolean value, ServerPlayer player) {
        if (computer.canInteract(player.getScoreboardName())) {
            if (value) {
                if (!computer.isPaused()) {
                    computer.start();
                    String lastError = computer.lastError();
                    if (lastError != null) {
                        player.sendSystemMessage(Component.translatable("gui.opencomputers.analyzer.lasterror", Component.translatable(lastError)));
                    }
                }
            } else {
                computer.stop();
            }
        }
    }

    private void onComputerPower(PacketParser p) throws IOException {
        byte mode = p.readByte();
        if (mode == 1) {
            String address = p.readUTF();
            boolean setPower = p.readBoolean();
            if (p.player instanceof ServerPlayer player) {
                if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Player container
                    && container instanceof li.cil.oc.fabric.common.container.Robot rc
                    && rc.address.equals(address)
                    && rc.current() instanceof Computer computer) {
                    trySetComputerPower(computer.machine(), setPower, player);
                    return;
                }
                logForgedPacket(player);
            }
            return;
        }
        Computer entity = p.readBlockEntity(Computer.class);
        boolean setPower = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Player container) {
                if (container.otherInventory instanceof Computer computer && entity != null) {
                    if (computer instanceof BlockEntity be1 && entity instanceof BlockEntity be2) {
                        if (be1.getBlockPos().equals(be2.getBlockPos())) {
                            trySetComputerPower(computer.machine(), setPower, player);
                            return;
                        }
                    }
                }
                logForgedPacket(player);
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onCopyToAnalyzer(PacketParser p) throws IOException {
        String text = p.readUTF();
        int line = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), text);
        if (opt instanceof TextBuffer buffer) {
            buffer.copyToAnalyzer(line, p.player);
        }
    }

    private void onDatabaseSetSlot(PacketParser p) throws IOException {
        byte slot = p.readByte();
        net.minecraft.world.item.ItemStack stack = p.readItemStack();
        if (p.player.containerMenu instanceof li.cil.oc.fabric.common.container.Database db) {
            int rows = (int) Math.ceil(Math.sqrt(db.otherInventory.getContainerSize()));
            if (slot >= 0 && slot < rows * rows) {
                db.setItem(slot, 0, stack);
            }
        }
    }

    private void onDriveLock(PacketParser p) {
        if (p.player instanceof ServerPlayer player) {
            var heldItem = player.getMainHandItem();
            var item = heldItem.getItem();
            if (item instanceof FileSystemLike) {
                DriveData.lock(heldItem, player);
            }
        }
    }

    private void onDriveMode(PacketParser p) throws IOException {
        boolean unmanaged = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            var heldItem = player.getMainHandItem();
            var item = heldItem.getItem();
            if (item instanceof FileSystemLike) {
                DriveData.setUnmanaged(heldItem, unmanaged);
            }
        }
    }

    private void onDronePower(PacketParser p) throws IOException {
        Drone entity = p.readEntity(Drone.class);
        boolean power = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Drone c && entity != null && c.drone == entity) {
                Drone drone = c.drone;
                if (power) drone.preparePowerUp();
                trySetComputerPower(drone.machine(), power, player);
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onKeyDown(PacketParser p) throws IOException {
        String address = p.readUTF();
        char key = p.readChar();
        int code = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.keyDown(key, code, p.player);
        }
    }

    private void onKeyUp(PacketParser p) throws IOException {
        String address = p.readUTF();
        char key = p.readChar();
        int code = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.keyUp(key, code, p.player);
        }
    }

    private void onClipboard(PacketParser p) throws IOException {
        String address = p.readUTF();
        String copy = p.readUTF();
        if (copy.length() > OCSettings.get().maxClipboard) return;
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.clipboard(copy, p.player);
        }
    }

    private void onDropFile(PacketParser p) throws IOException {
        String address = p.readUTF();
        String fileName = p.readUTF();
        String fileContent = p.readUTF();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.dropFile(fileName, fileContent, p.player);
        }
    }

    private void onMouseClick(PacketParser p) throws IOException {
        String address = p.readUTF();
        float x = p.readFloat();
        float y = p.readFloat();
        boolean dragging = p.readBoolean();
        byte button = p.readByte();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            Player player = p.player;
            if (dragging) buffer.mouseDrag(x, y, button, player);
            else buffer.mouseDown(x, y, button, player);
        }
    }

    private void onMouseUp(PacketParser p) throws IOException {
        String address = p.readUTF();
        float x = p.readFloat();
        float y = p.readFloat();
        byte button = p.readByte();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.mouseUp(x, y, button, p.player);
        }
    }

    private void onMouseScroll(PacketParser p) throws IOException {
        String address = p.readUTF();
        float x = p.readFloat();
        float y = p.readFloat();
        byte button = p.readByte();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.mouseScroll(x, y, button, p.player);
        }
    }

    private void onPetVisibility(PacketParser p) throws IOException {
        boolean value = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            String name = player.getScoreboardName();
            boolean changed;
            if (value) {
                changed = PetVisibility.hidden.remove(name);
            } else {
                changed = PetVisibility.hidden.add(name);
            }
            if (changed) {
                li.cil.oc.core.impl.common.PacketSender.sendPetVisibility(name, null);
            }
        }
    }

    private void onRackMountableMapping(PacketParser p) throws IOException {
        Rack entity = p.readBlockEntity(Rack.class);
        int mountableIndex = p.readInt();
        int nodeIndex = p.readInt();
        Direction side = p.readDirection();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Rack container && container.rack == entity) {
                entity.connect(mountableIndex, nodeIndex - 1, side);
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onRackRelayState(PacketParser p) throws IOException {
        Rack entity = p.readBlockEntity(Rack.class);
        boolean enabled = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Rack container && container.rack == entity) {
                entity.isRelayEnabled = enabled;
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onRobotAssemblerStart(PacketParser p) {
        Assembler entity = p.readBlockEntity(Assembler.class);
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Assembler container && container.assembler == entity) {
                boolean creative = player.getAbilities().instabuild;
                if (entity.start(creative)) {
                    if (entity.output != null) Achievement.onAssemble(entity.output, p.player);
                }
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onRobotStateRequest(PacketParser p) {
        var opt = p.readBlockEntity(RobotProxy.class);
        if (opt != null) {
            var pos = opt.getBlockPos();
            var level = opt.getLevel();
            if (level != null) {
                level.sendBlockUpdated(pos, opt.getBlockState(), opt.getBlockState(), 3);
            }
        }
    }

    private void onServerPower(PacketParser p) throws IOException {
        Rack entity = p.readBlockEntity(Rack.class);
        int index = p.readInt();
        if (entity == null) return;
        var mountable = entity.getMountable(index);
        if (!(mountable instanceof Server server)) return;
        boolean setPower = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Server container && container.server != null && container.server == server) {
                trySetComputerPower(server.machine(), setPower, player);
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onMachineItemStateRequest(PacketParser p) {
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.core.impl.common.container.Tablet) {
                var stack = p.readItemStack();
                var isRunning = li.cil.oc.core.impl.common.item.Tablet.get(stack, p.player).machine().isRunning();
                li.cil.oc.core.impl.common.PacketSender.sendMachineItemState(player, stack, isRunning);
            } else {
                logForgedPacket(player);
            }
        }
    }

    private void onTextBufferInit(PacketParser p) throws IOException {
        String address = p.readUTF();
        if (p.player instanceof ServerPlayer entity) {
            ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
            if (opt instanceof TextBuffer buffer) {
                if (!(buffer.host() instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) || screen.isOrigin()) {
                    CompoundTag nbt = new CompoundTag();
                    buffer.data().save(nbt, entity.level().registryAccess());
                    nbt.putInt("maxWidth", buffer.getMaximumWidth());
                    nbt.putInt("maxHeight", buffer.getMaximumHeight());
                    nbt.putInt("viewportWidth", buffer.getViewportWidth());
                    nbt.putInt("viewportHeight", buffer.getViewportHeight());
                    li.cil.oc.core.impl.common.PacketSender.sendTextBufferInit(address, nbt, entity);
                }
            }
        }
    }

    private void onWaypointLabel(PacketParser p) throws IOException {
        Waypoint entity = p.readBlockEntity(Waypoint.class);
        String label = p.readUTF();
        if (label.length() > 32) label = label.substring(0, 32);
        if (entity != null && p.player instanceof ServerPlayer player) {
            double dx = player.getX() - (entity.getBlockPos().getX() + 0.5);
            double dy = player.getY() - (entity.getBlockPos().getY() + 0.5);
            double dz = player.getZ() - (entity.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz <= 64) {
                if (!label.equals(entity.label)) {
                    entity.label = label;
                    li.cil.oc.core.impl.common.PacketSender.sendWaypointLabel(entity);
                }
            }
        }
    }
}
