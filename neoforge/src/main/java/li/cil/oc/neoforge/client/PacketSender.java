package li.cil.oc.neoforge.client;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.entity.Drone;
import li.cil.oc.core.impl.common.tileentity.Assembler;
import li.cil.oc.core.impl.common.tileentity.Rack;
import li.cil.oc.core.impl.common.tileentity.Waypoint;
import li.cil.oc.core.impl.common.tileentity.traits.Computer;
import li.cil.oc.neoforge.common.PacketBuilder;
import li.cil.oc.neoforge.common.SimplePacketBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;

public final class PacketSender {
    private static long clipboardCooldown = 0L;

    public static void sendComputerPower(Computer t, boolean power) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ComputerPower)) {
            pb.writeTileEntity((net.minecraft.world.level.block.entity.BlockEntity) t);
            pb.writeBoolean(power);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDriveMode(boolean unmanaged) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DriveMode)) {
            pb.writeBoolean(unmanaged);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDriveLock() {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DriveLock)) {
            pb.sendToServer();
        }
    }

    public static void sendDronePower(Drone e, boolean power) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DronePower)) {
            pb.writeEntity(e);
            pb.writeBoolean(power);
            pb.sendToServer();
        } catch (IOException e2) {
            throw new RuntimeException(e2);
        }
    }

    public static void sendKeyDown(String address, char ch, int code) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.KeyDown)) {
            pb.writeUTF(address);
            pb.writeChar(ch);
            pb.writeInt(code);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendKeyUp(String address, char ch, int code) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.KeyUp)) {
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
            if (value.length() > Settings.get().maxClipboard || System.currentTimeMillis() < clipboardCooldown) {
                var handler = Minecraft.getInstance().getSoundManager();
                handler.play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_HARP.value(), 1, 1));
            } else {
                clipboardCooldown = System.currentTimeMillis() + value.length() / 10;
                for (int i = 0; i < value.length(); i += 16 * 1024) {
                    try (PacketBuilder.Compressed pb = new PacketBuilder.Compressed(PacketType.Clipboard)) {
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
            try (PacketBuilder.Compressed pb = new PacketBuilder.Compressed(PacketType.DropFile)) {
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseClickOrDrag)) {
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseScroll)) {
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MouseUp)) {
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.PetVisibility)) {
            pb.writeBoolean(!Settings.get().hideOwnPet);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRackMountableMapping(Rack t, int mountableIndex, int nodeIndex, Direction side) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackMountableMapping)) {
            pb.writeTileEntity(t);
            pb.writeInt(mountableIndex);
            pb.writeInt(nodeIndex);
            pb.writeDirection(side);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRackRelayState(Rack t, boolean enabled) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RackRelayState)) {
            pb.writeTileEntity(t);
            pb.writeBoolean(enabled);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendDatabaseSetSlot(int slot, ItemStack stack) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.DatabaseSetSlot)) {
            pb.writeByte(slot);
            pb.writeItemStack(stack, level.registryAccess());
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRobotAssemblerStart(Assembler t) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotAssemblerStart)) {
            pb.writeTileEntity(t);
            pb.sendToServer();
        }
    }

    public static void sendRobotStateRequest(int dimension, int x, int y, int z) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.RobotStateRequest)) {
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.ServerPower)) {
            pb.writeTileEntity(t);
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
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.MachineItemStateRequest)) {
            pb.writeItemStack(stack, level.registryAccess());
            pb.sendToServer();
        }
    }

    public static void sendTextBufferInit(String address) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.TextBufferInit)) {
            pb.writeUTF(address);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendWaypointLabel(Waypoint t) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.WaypointLabel)) {
            pb.writeTileEntity(t);
            pb.writeUTF(t.label);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendCopyToAnalyzer(String address, int line) {
        try (SimplePacketBuilder pb = new SimplePacketBuilder(PacketType.CopyToAnalyzer)) {
            pb.writeUTF(address);
            pb.writeInt(line);
            pb.sendToServer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
