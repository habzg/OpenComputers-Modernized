package li.cil.oc.neoforge.server;

import li.cil.oc.api.internal.Server;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.common.item.traits.FileSystemLike;
import li.cil.oc.core.impl.common.Achievement;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.component.TextBuffer;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.item.data.DriveData;
import li.cil.oc.core.impl.common.tileentity.Assembler;
import li.cil.oc.core.impl.common.tileentity.Rack;
import li.cil.oc.core.impl.common.tileentity.Screen;
import li.cil.oc.core.impl.common.tileentity.Waypoint;
import li.cil.oc.core.impl.common.tileentity.traits.Computer;
import li.cil.oc.core.server.PetVisibility;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.io.IOException;

public final class PacketHandler extends li.cil.oc.neoforge.common.PacketHandler {
    public static final PacketHandler INSTANCE = new PacketHandler();
    private static final Marker securityMarker = MarkerFactory.getMarker("SuspiciousPackets");

    private PacketHandler() {
    }

    private static void logForgedPacket(ServerPlayer player) {
        OpenComputers.log().warn(securityMarker, "Player {} tried to send GUI packets without opening them", player.getGameProfile());
    }

    public static void onComputerPower(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Computer entity = p.readTileEntity(Computer.class);
        boolean setPower = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.neoforge.common.container.Player container) {
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

    public static void onServerPower(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Rack entity = p.readTileEntity(Rack.class);
        int index = p.readInt();
        if (entity == null) return;
        var mountable = entity.getMountable(index);
        if (!(mountable instanceof Server server)) return;
        boolean setPower = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.neoforge.common.container.Server container && container.server != null && container.server == server) {
                trySetComputerPower(server.machine(), setPower, player);
            } else {
                logForgedPacket(player);
            }
        }
    }

    public static void onCopyToAnalyzer(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String text = p.readUTF();
        int line = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), text);
        if (opt instanceof TextBuffer buffer) {
            buffer.copyToAnalyzer(line, p.player);
        }
    }

    public static void onDriveLock(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) {
        if (p.player instanceof ServerPlayer player) {
            net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
            var item = heldItem.getItem();
            if (item instanceof FileSystemLike) {
                DriveData.lock(heldItem, player);
            }
        }
    }

    public static void onDriveMode(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        boolean unmanaged = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
            var item = heldItem.getItem();
            if (item instanceof FileSystemLike) {
                DriveData.setUnmanaged(heldItem, unmanaged);
            }
        }
    }

    public static void onDronePower(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Drone entity = p.readEntity(Drone.class);
        boolean power = p.readBoolean();
        if (p.player instanceof ServerPlayer player) {
            if (player.containerMenu instanceof li.cil.oc.neoforge.common.container.Drone c && entity != null && c.drone == entity) {
                Drone drone = c.drone;
                if (power) drone.preparePowerUp();
                trySetComputerPower(drone.machine(), power, player);
            } else {
                logForgedPacket(player);
            }
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

    public static void onKeyDown(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        char key = p.readChar();
        int code = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.keyDown(key, code, p.player);
        }
    }

    public static void onKeyUp(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        char key = p.readChar();
        int code = p.readInt();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.keyUp(key, code, p.player);
        }
    }

    public static void onClipboard(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        String copy = p.readUTF();
        if (copy.length() > li.cil.oc.core.impl.Settings.get().maxClipboard) return;
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.clipboard(copy, p.player);
        }
    }

    public static void onDropFile(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        String fileName = p.readUTF();
        String fileContent = p.readUTF();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.dropFile(fileName, fileContent, p.player);
        }
    }

    public static void onMouseClick(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
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

    public static void onMouseUp(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        float x = p.readFloat();
        float y = p.readFloat();
        byte button = p.readByte();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.mouseUp(x, y, button, p.player);
        }
    }

    public static void onMouseScroll(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        float x = p.readFloat();
        float y = p.readFloat();
        byte button = p.readByte();
        ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
        if (opt instanceof li.cil.oc.api.internal.TextBuffer buffer) {
            buffer.mouseScroll(x, y, button, p.player);
        }
    }

    public static void onDatabaseSetSlot(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        byte slot = p.readByte();
        net.minecraft.world.item.ItemStack stack = p.readItemStack();
        if (p.player.containerMenu instanceof li.cil.oc.neoforge.common.container.Database db) {
            int rows = (int) Math.ceil(Math.sqrt(db.otherInventory.getContainerSize()));
            if (slot >= 0 && slot < rows * rows) {
                db.setItem(slot, 0, stack);
            }
        }
    }

    public static void onPetVisibility(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
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
                PacketSender.sendPetVisibility(player.getScoreboardName(), null);
            }
        }
    }

    public static void onRackMountableMapping(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Rack entity = p.readTileEntity(Rack.class);
        int mountableIndex = p.readInt();
        int nodeIndex = p.readInt();
        Direction side = p.readDirection();
        if (entity != null) {
            if (p.player instanceof ServerPlayer player && entity.isUseableByPlayer(player)) {
                entity.connect(mountableIndex, nodeIndex - 1, side);
            }
        }
    }

    public static void onRackRelayState(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Rack entity = p.readTileEntity(Rack.class);
        boolean enabled = p.readBoolean();
        if (entity != null) {
            if (p.player instanceof ServerPlayer player && entity.isUseableByPlayer(player)) {
                entity.isRelayEnabled = enabled;
            }
        }
    }

    public static void onRobotAssemblerStart(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) {
        Assembler entity = p.readTileEntity(Assembler.class);
        if (entity != null) {
            boolean creative = p.player instanceof ServerPlayer player && player.getAbilities().instabuild;
            if (entity.start(creative)) {
                if (entity.output != null) Achievement.onAssemble(entity.output, p.player);
            }
        }
    }

    public static void onRobotStateRequest(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) {
        var opt = p.readTileEntity(li.cil.oc.neoforge.common.tileentity.Robot.class);
        if (opt != null) {
            BlockPos pos = opt.getBlockPos();
            opt.level().sendBlockUpdated(pos, opt.level().getBlockState(pos), opt.level().getBlockState(pos), 3);
        }
    }

    public static void onMachineItemStateRequest(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) {
        if (p.player instanceof net.minecraft.server.level.ServerPlayer player) {
            var stack = p.readItemStack();
            var isRunning = li.cil.oc.neoforge.common.item.Tablet.get(stack, p.player).machine().isRunning();
            PacketSender.sendMachineItemState(player, stack, isRunning);
        }
    }

    public static void onTextBufferInit(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        String address = p.readUTF();
        if (p.player instanceof ServerPlayer entity) {
            ManagedEnvironment opt = ComponentTracker.INSTANCE.get(p.player.level(), address);
            if (opt instanceof TextBuffer buffer) {
                if (!(buffer.host() instanceof Screen screen) || screen.isOrigin()) {
                    CompoundTag nbt = new CompoundTag();
                    buffer.data().save(nbt, entity.level().registryAccess());
                    nbt.putInt("maxWidth", buffer.getMaximumWidth());
                    nbt.putInt("maxHeight", buffer.getMaximumHeight());
                    nbt.putInt("viewportWidth", buffer.getViewportWidth());
                    nbt.putInt("viewportHeight", buffer.getViewportHeight());
                    PacketSender.sendTextBufferInit(address, nbt, entity);
                }
            }
        }
    }

    public static void onWaypointLabel(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) throws IOException {
        Waypoint entity = p.readTileEntity(Waypoint.class);
        String label = p.readUTF();
        if (label.length() > 32) label = label.substring(0, 32);
        if (entity != null && p.player instanceof ServerPlayer player) {
            double dx = player.getX() - (entity.getBlockPos().getX() + 0.5);
            double dy = player.getY() - (entity.getBlockPos().getY() + 0.5);
            double dz = player.getZ() - (entity.getBlockPos().getZ() + 0.5);
            if (dx * dx + dy * dy + dz * dz <= 64) {
                if (!label.equals(entity.label)) {
                    entity.label = label;
                    PacketSender.sendWaypointLabel(entity);
                }
            }
        }
    }

    @Override
    protected Level world(Player player, int dimension) {
        return player instanceof ServerPlayer sp ? sp.serverLevel() : null;
    }

    @Override
    public void dispatch(li.cil.oc.neoforge.common.PacketHandler.PacketParser p) {
        try {
            switch (p.packetType) {
                case ComputerPower -> onComputerPower(p);
                case CopyToAnalyzer -> onCopyToAnalyzer(p);
                case DriveLock -> onDriveLock(p);
                case DriveMode -> onDriveMode(p);
                case DronePower -> onDronePower(p);
                case KeyDown -> onKeyDown(p);
                case KeyUp -> onKeyUp(p);
                case Clipboard -> onClipboard(p);
                case DropFile -> onDropFile(p);
                case MouseClickOrDrag -> onMouseClick(p);
                case MouseScroll -> onMouseScroll(p);
                case DatabaseSetSlot -> onDatabaseSetSlot(p);
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
                default -> {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
