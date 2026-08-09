package li.cil.oc.fabric.client;

import java.io.IOException;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.fabric.server.PacketBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public final class PacketSender {
    private static long clipboardCooldown = 0L;

    public static void sendComputerPower(Computer t, boolean power) {
        try (var pb = new PacketBuilder(PacketType.ComputerPower)) {
            if (t instanceof li.cil.oc.core.impl.common.blockentity.Robot r) {
                var addr = r.computerAddress();
                if (addr != null && !addr.isEmpty()) {
                    pb.writeByte(1);
                    pb.writeUTF(addr);
                } else {
                    pb.writeByte(0);
                    pb.writeBlockEntity((net.minecraft.world.level.block.entity.BlockEntity) t);
                }
            } else {
                pb.writeByte(0);
                pb.writeBlockEntity((net.minecraft.world.level.block.entity.BlockEntity) t);
            }
            pb.writeBoolean(power);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDriveMode(boolean unmanaged) {
        try (var pb = new PacketBuilder(PacketType.DriveMode)) {
            pb.writeBoolean(unmanaged);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDriveLock() {
        try (var pb = new PacketBuilder(PacketType.DriveLock)) {
            pb.sendToServer();
        }
    }

    public static void sendDronePower(Drone e, boolean power) {
        try (var pb = new PacketBuilder(PacketType.DronePower)) {
            pb.writeEntity(e);
            pb.writeBoolean(power);
            pb.sendToServer();
        } catch (IOException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static void sendKeyDown(String address, char ch, int code) {
        try (var pb = new PacketBuilder(PacketType.KeyDown)) {
            pb.writeUTF(address);
            pb.writeChar(ch);
            pb.writeInt(code);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendKeyUp(String address, char ch, int code) {
        try (var pb = new PacketBuilder(PacketType.KeyUp)) {
            pb.writeUTF(address);
            pb.writeChar(ch);
            pb.writeInt(code);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendClipboard(String address, String value) {
        if (value != null && !value.isEmpty()) {
            if (value.length() > OCSettings.get().maxClipboard || System.currentTimeMillis() < clipboardCooldown) {
                var handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 1, 1));
            } else {
                clipboardCooldown = System.currentTimeMillis() + value.length() / 10;
                for (int i = 0; i < value.length(); i += 16 * 1024) {
                    try (var pb = new PacketBuilder(PacketType.Clipboard)) {
                        String part = value.substring(i, Math.min(i + 16 * 1024, value.length()));
                        pb.writeUTF(address);
                        pb.writeUTF(part);
                        pb.sendToServer();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public static void sendDropFile(String address, String name, String content) {
        int length = name.length() + content.length();
        if (length > 64 * 1024) {
            var handler = Minecraft.getInstance().getSoundManager();
            handler.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 1, 1));
        } else {
            try (var pb = new PacketBuilder(PacketType.DropFile)) {
                pb.writeUTF(address);
                pb.writeUTF(name);
                pb.writeUTF(content);
                pb.sendToServer();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendMouseClick(String address, double x, double y, boolean drag, int button) {
        try (var pb = new PacketBuilder(PacketType.MouseClickOrDrag)) {
            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeBoolean(drag);
            pb.writeByte(button);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMouseScroll(String address, double x, double y, int scroll) {
        try (var pb = new PacketBuilder(PacketType.MouseScroll)) {
            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeByte(scroll);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMouseUp(String address, double x, double y, int button) {
        try (var pb = new PacketBuilder(PacketType.MouseUp)) {
            pb.writeUTF(address);
            pb.writeFloat((float) x);
            pb.writeFloat((float) y);
            pb.writeByte(button);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendPetVisibility() {
        try (var pb = new PacketBuilder(PacketType.PetVisibility)) {
            pb.writeBoolean(!OCSettings.get().hideOwnPet);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRackMountableMapping(Rack t, int mountableIndex, int nodeIndex, Direction side) {
        try (var pb = new PacketBuilder(PacketType.RackMountableMapping)) {
            pb.writeBlockEntity(t);
            pb.writeInt(mountableIndex);
            pb.writeInt(nodeIndex);
            pb.writeDirection(side);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRackRelayState(Rack t, boolean enabled) {
        try (var pb = new PacketBuilder(PacketType.RackRelayState)) {
            pb.writeBlockEntity(t);
            pb.writeBoolean(enabled);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDatabaseSetSlot(int slot, ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        try (var pb = new PacketBuilder(PacketType.DatabaseSetSlot)) {
            pb.writeByte(slot);
            pb.writeItemStack(stack, level.registryAccess());
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRobotAssemblerStart(Assembler t) {
        try (var pb = new PacketBuilder(PacketType.RobotAssemblerStart)) {
            pb.writeBlockEntity(t);
            pb.sendToServer();
        }
    }

    public static void sendRobotStateRequest(int dimension, int x, int y, int z) {
        try (var pb = new PacketBuilder(PacketType.RobotStateRequest)) {
            pb.writeInt(dimension);
            pb.writeInt(x);
            pb.writeInt(y);
            pb.writeInt(z);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendServerPower(Rack t, int mountableIndex, boolean power) {
        try (var pb = new PacketBuilder(PacketType.ServerPower)) {
            pb.writeBlockEntity(t);
            pb.writeInt(mountableIndex);
            pb.writeBoolean(power);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendMachineItemStateRequest(ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        try (var pb = new PacketBuilder(PacketType.MachineItemStateRequest)) {
            pb.writeItemStack(stack, level.registryAccess());
            pb.sendToServer();
        }
    }

    public static void sendTextBufferInit(String address) {
        try (var pb = new PacketBuilder(PacketType.TextBufferInit)) {
            pb.writeUTF(address);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendWaypointLabel(Waypoint t) {
        try (var pb = new PacketBuilder(PacketType.WaypointLabel)) {
            pb.writeBlockEntity(t);
            pb.writeUTF(t.label);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendCopyToAnalyzer(String address, int line) {
        try (var pb = new PacketBuilder(PacketType.CopyToAnalyzer)) {
            pb.writeUTF(address);
            pb.writeInt(line);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
