package li.cil.oc.core.impl.common;

import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.PackedColor;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.io.IOException;


public abstract class PacketSender {
    private static PacketSender INSTANCE;

    public static void setInstance(PacketSender instance) {
        INSTANCE = instance;
    }
    protected abstract PacketBuilderBase<?> createBuilder(PacketType type) ;
    public static void sendAdapterState(BlockEntity t, byte compressedSides) {
        INSTANCE.sendAdapterStateImpl(t, compressedSides);
    }

    protected void sendAdapterStateImpl(BlockEntity t, byte compressedSides) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.AdapterState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeByte(compressedSides);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendAnalyze(String address, ServerPlayer player) {
        INSTANCE.sendAnalyzeImpl(address, player);
    }

    protected void sendAnalyzeImpl(String address, ServerPlayer player) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.Analyze)) {
            try {
                pb.writeUTF(address);
                pb.sendToPlayer(player);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendChargerState(BlockEntity t, double chargeSpeed, boolean hasPower) {
        INSTANCE.sendChargerStateImpl(t, chargeSpeed, hasPower);
    }

    protected void sendChargerStateImpl(BlockEntity t, double chargeSpeed, boolean hasPower) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ChargerState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeDouble(chargeSpeed);
                pb.writeBoolean(hasPower);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendClientLog(String line, ServerPlayer player) {
        INSTANCE.sendClientLogImpl(line, player);
    }

    protected abstract void sendClientLogImpl(String line, ServerPlayer player) ;

    public static void sendClipboard(ServerPlayer player, String text) {
        INSTANCE.sendClipboardImpl(player, text);
    }

    protected void sendClipboardImpl(ServerPlayer player, String text) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.Clipboard)) {
            try {
                pb.writeUTF(text);
                pb.sendToPlayer(player);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendColorChange(BlockEntity t, int color) {
        INSTANCE.sendColorChangeImpl(t, color);
    }

    protected void sendColorChangeImpl(BlockEntity t, int color) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ColorChange)) {
            try {
                pb.writeTileEntity(t);
                pb.writeInt(color);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendComputerState(BlockEntity t, boolean isRunning, boolean hasErrored) {
        INSTANCE.sendComputerStateImpl(t, isRunning, hasErrored);
    }

    protected void sendComputerStateImpl(BlockEntity t, boolean isRunning, boolean hasErrored) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ComputerState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(isRunning);
                pb.writeBoolean(hasErrored);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendComputerUserList(BlockEntity t, String[] list) {
        INSTANCE.sendComputerUserListImpl(t, list);
    }

    protected void sendComputerUserListImpl(BlockEntity t, String[] list) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ComputerUserList)) {
            try {
                pb.writeTileEntity(t);
                pb.writeInt(list.length);
                for (String s : list) pb.writeUTF(s);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendContainerUpdate(AbstractContainerMenu c, CompoundTag nbt, ServerPlayer player) {
        INSTANCE.sendContainerUpdateImpl(c, nbt, player);
    }

    protected void sendContainerUpdateImpl(AbstractContainerMenu c, CompoundTag nbt, ServerPlayer player) {
        if (!nbt.isEmpty()) {
            try (PacketBuilderBase<?> pb = createBuilder(PacketType.ContainerUpdate)) {
                try {
                    pb.writeByte(c.containerId);
                    pb.writeNBT(nbt);
                    pb.sendToPlayer(player);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void sendDisassemblerActive(BlockEntity t, boolean active) {
        INSTANCE.sendDisassemblerActiveImpl(t, active);
    }

    protected void sendDisassemblerActiveImpl(BlockEntity t, boolean active) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.DisassemblerActiveChange)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(active);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendFloppyChange(BlockEntity t, ItemStack stack) {
        INSTANCE.sendFloppyChangeImpl(t, stack);
    }

    protected void sendFloppyChangeImpl(BlockEntity t, ItemStack stack) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.FloppyChange)) {
            pb.writeTileEntity(t);
            var level = t.getLevel();
            pb.writeItemStack(stack, level != null ? level.registryAccess() : null);
            pb.sendToPlayersNearTileEntity(t);
        }
    }

    public static void sendHologramClear(BlockEntity t) {
        INSTANCE.sendHologramClearImpl(t);
    }

    protected void sendHologramClearImpl(BlockEntity t) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramClear)) {
            pb.writeTileEntity(t);
            pb.sendToPlayersNearTileEntity(t);
        }
    }

    public static void sendHologramColor(BlockEntity t, int index, int value) {
        INSTANCE.sendHologramColorImpl(t, index, value);
    }

    protected void sendHologramColorImpl(BlockEntity t, int index, int value) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramColor)) {
            try {
                pb.writeTileEntity(t);
                pb.writeInt(index);
                pb.writeInt(value);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendHologramPowerChange(BlockEntity t, boolean hasPower) {
        INSTANCE.sendHologramPowerChangeImpl(t, hasPower);
    }

    protected void sendHologramPowerChangeImpl(BlockEntity t, boolean hasPower) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramPowerChange)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(hasPower);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendHologramScale(BlockEntity t, double scale) {
        INSTANCE.sendHologramScaleImpl(t, scale);
    }

    protected void sendHologramScaleImpl(BlockEntity t, double scale) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramScale)) {
            try {
                pb.writeTileEntity(t);
                pb.writeDouble(scale);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendHologramArea(BlockEntity t, byte dirtyFromX, byte dirtyUntilX, byte dirtyFromZ, byte dirtyUntilZ, int[] volume, int width) {
        INSTANCE.sendHologramAreaImpl(t, dirtyFromX, dirtyUntilX, dirtyFromZ, dirtyUntilZ, volume, width);
    }

    protected abstract void sendHologramAreaImpl(BlockEntity t, byte dirtyFromX, byte dirtyUntilX, byte dirtyFromZ, byte dirtyUntilZ, int[] volume, int width) ;

    public static void sendHologramValues(BlockEntity t, java.util.BitSet dirty, int[] volume, int width) {
        INSTANCE.sendHologramValuesImpl(t, dirty, volume, width);
    }

    protected abstract void sendHologramValuesImpl(BlockEntity t, java.util.BitSet dirty, int[] volume, int width) ;

    public static void sendHologramOffset(BlockEntity t, double translationX, double translationY, double translationZ) {
        INSTANCE.sendHologramOffsetImpl(t, translationX, translationY, translationZ);
    }

    protected void sendHologramOffsetImpl(BlockEntity t, double translationX, double translationY, double translationZ) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramTranslation)) {
            try {
                pb.writeTileEntity(t);
                pb.writeDouble(translationX);
                pb.writeDouble(translationY);
                pb.writeDouble(translationZ);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendHologramRotation(BlockEntity t, float rotationAngle, float rotationX, float rotationY, float rotationZ) {
        INSTANCE.sendHologramRotationImpl(t, rotationAngle, rotationX, rotationY, rotationZ);
    }

    protected void sendHologramRotationImpl(BlockEntity t, float rotationAngle, float rotationX, float rotationY, float rotationZ) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramRotation)) {
            try {
                pb.writeTileEntity(t);
                pb.writeFloat(rotationAngle);
                pb.writeFloat(rotationX);
                pb.writeFloat(rotationY);
                pb.writeFloat(rotationZ);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendHologramRotationSpeed(BlockEntity t, float rotationSpeed, float rotationSpeedX, float rotationSpeedY, float rotationSpeedZ) {
        INSTANCE.sendHologramRotationSpeedImpl(t, rotationSpeed, rotationSpeedX, rotationSpeedY, rotationSpeedZ);
    }

    protected void sendHologramRotationSpeedImpl(BlockEntity t, float rotationSpeed, float rotationSpeedX, float rotationSpeedY, float rotationSpeedZ) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.HologramRotationSpeed)) {
            try {
                pb.writeTileEntity(t);
                pb.writeFloat(rotationSpeed);
                pb.writeFloat(rotationSpeedX);
                pb.writeFloat(rotationSpeedY);
                pb.writeFloat(rotationSpeedZ);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendLootDisks(ServerPlayer p) {
        INSTANCE.sendLootDisksImpl(p);
    }

    protected abstract void sendLootDisksImpl(ServerPlayer p) ;

    public static void sendNetSplitterState(BlockEntity t, boolean isInverted, byte compressedSides) {
        INSTANCE.sendNetSplitterStateImpl(t, isInverted, compressedSides);
    }

    protected void sendNetSplitterStateImpl(BlockEntity t, boolean isInverted, byte compressedSides) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.NetSplitterState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(isInverted);
                pb.writeByte(compressedSides);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendParticleEffect(BlockPosition position, String name, int count, double velocity, Direction direction) {
        INSTANCE.sendParticleEffectImpl(position, name, count, velocity, direction);
    }

    protected void sendParticleEffectImpl(BlockPosition position, String name, int count, double velocity, Direction direction) {
        if (count <= 0) return;
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ParticleEffect)) {
            try {
                pb.writeInt(position.level().dimension().location().hashCode());
                pb.writeInt(position.x());
                pb.writeInt(position.y());
                pb.writeInt(position.z());
                pb.writeDouble(velocity);
                pb.writeDirection(direction);
                pb.writeUTF(name);
                pb.writeByte((byte) count);
                pb.sendToNearbyPlayers(position.level(), position.x(), position.y(), position.z(), li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance / 2.0);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendPowerState(BlockEntity t, double buffer, double bufferSize) {
        INSTANCE.sendPowerStateImpl(t, buffer, bufferSize);
    }

    protected void sendPowerStateImpl(BlockEntity t, double buffer, double bufferSize) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.PowerState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeDouble(buffer);
                pb.writeDouble(bufferSize);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendPrinting(BlockEntity t, boolean printing) {
        INSTANCE.sendPrintingImpl(t, printing);
    }

    protected void sendPrintingImpl(BlockEntity t, boolean printing) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.PrinterState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(printing);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendRobotAssembling(BlockEntity t, boolean assembling) {
        INSTANCE.sendRobotAssemblingImpl(t, assembling);
    }

    protected void sendRobotAssemblingImpl(BlockEntity t, boolean assembling) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RobotAssemblingState)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(assembling);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendSwitchActivity(BlockEntity t) {
        INSTANCE.sendSwitchActivityImpl(t);
    }

    protected void sendSwitchActivityImpl(BlockEntity t) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.SwitchActivity)) {
            pb.writeTileEntity(t);
            pb.sendToPlayersNearTileEntity(t, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance);
        }
    }

    public static void sendTextBufferInit(String address, CompoundTag value, ServerPlayer player) {
        INSTANCE.sendTextBufferInitImpl(address, value, player);
    }

    protected abstract void sendTextBufferInitImpl(String address, CompoundTag value, ServerPlayer player) ;

    public static void sendTextBufferPowerChange(String address, boolean hasPower, li.cil.oc.api.network.EnvironmentHost host) {
        INSTANCE.sendTextBufferPowerChangeImpl(address, hasPower, host);
    }

    protected void sendTextBufferPowerChangeImpl(String address, boolean hasPower, li.cil.oc.api.network.EnvironmentHost host) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.TextBufferPowerChange)) {
            try {
                pb.writeUTF(address);
                pb.writeBoolean(hasPower);
                pb.sendToPlayersNearHost(host, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendScreenTouchMode(BlockEntity t, boolean value) {
        INSTANCE.sendScreenTouchModeImpl(t, value);
    }

    protected void sendScreenTouchModeImpl(BlockEntity t, boolean value) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.ScreenTouchMode)) {
            try {
                pb.writeTileEntity(t);
                pb.writeBoolean(value);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendSound(Level world, double x, double y, double z, int frequency, int duration) {
        INSTANCE.sendSoundImpl(world, x, y, z, frequency, duration);
    }

    protected void sendSoundImpl(Level world, double x, double y, double z, int frequency, int duration) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.Sound)) {
            try {
                BlockPosition blockPos = new BlockPosition(x, y, z);
                pb.writeInt(world.dimension().location().hashCode());
                pb.writeInt(blockPos.x());
                pb.writeInt(blockPos.y());
                pb.writeInt(blockPos.z());
                pb.writeShort((short) frequency);
                pb.writeShort((short) duration);
                pb.sendToNearbyPlayers(world, x, y, z, li.cil.oc.core.impl.Settings.get().maxNetworkClientSoundPacketDistance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendSound(Level world, double x, double y, double z, String pattern) {
        INSTANCE.sendSoundImpl(world, x, y, z, pattern);
    }

    protected void sendSoundImpl(Level world, double x, double y, double z, String pattern) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.SoundPattern)) {
            try {
                BlockPosition blockPos = new BlockPosition(x, y, z);
                pb.writeInt(world.dimension().location().hashCode());
                pb.writeInt(blockPos.x());
                pb.writeInt(blockPos.y());
                pb.writeInt(blockPos.z());
                pb.writeUTF(pattern);
                pb.sendToNearbyPlayers(world, x, y, z, li.cil.oc.core.impl.Settings.get().maxNetworkClientSoundPacketDistance);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendTransposerActivity(BlockEntity t) {
        INSTANCE.sendTransposerActivityImpl(t);
    }

    protected void sendTransposerActivityImpl(BlockEntity t) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.TransposerActivity)) {
            pb.writeTileEntity(t);
            pb.sendToPlayersNearTileEntity(t, li.cil.oc.core.impl.Settings.get().maxNetworkClientEffectPacketDistance / 2.0);
        }
    }

    public static void appendTextBufferColorChange(PacketBuilderBase<?> pb, PackedColor.Color foreground, PackedColor.Color background) {
        INSTANCE.appendTextBufferColorChangeImpl(pb, foreground, background);
    }

    protected void appendTextBufferColorChangeImpl(PacketBuilderBase<?> pb, PackedColor.Color foreground, PackedColor.Color background) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiColorChange);
            pb.writeInt(foreground.value());
            pb.writeBoolean(foreground.isPalette());
            pb.writeInt(background.value());
            pb.writeBoolean(background.isPalette());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferCopy(PacketBuilderBase<?> pb, int col, int row, int w, int h, int tx, int ty) {
        INSTANCE.appendTextBufferCopyImpl(pb, col, row, w, h, tx, ty);
    }

    protected void appendTextBufferCopyImpl(PacketBuilderBase<?> pb, int col, int row, int w, int h, int tx, int ty) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiCopy);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeInt(w);
            pb.writeInt(h);
            pb.writeInt(tx);
            pb.writeInt(ty);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferDepthChange(PacketBuilderBase<?> pb, li.cil.oc.api.internal.TextBuffer.ColorDepth value) {
        INSTANCE.appendTextBufferDepthChangeImpl(pb, value);
    }

    protected void appendTextBufferDepthChangeImpl(PacketBuilderBase<?> pb, li.cil.oc.api.internal.TextBuffer.ColorDepth value) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiDepthChange);
            pb.writeInt(value.ordinal());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferFill(PacketBuilderBase<?> pb, int col, int row, int w, int h, int c) {
        INSTANCE.appendTextBufferFillImpl(pb, col, row, w, h, c);
    }

    protected void appendTextBufferFillImpl(PacketBuilderBase<?> pb, int col, int row, int w, int h, int c) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiFill);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeInt(w);
            pb.writeInt(h);
            pb.writeMedium(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferPaletteChange(PacketBuilderBase<?> pb, int index, int color) {
        INSTANCE.appendTextBufferPaletteChangeImpl(pb, index, color);
    }

    protected void appendTextBufferPaletteChangeImpl(PacketBuilderBase<?> pb, int index, int color) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiPaletteChange);
            pb.writeInt(index);
            pb.writeInt(color);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferResolutionChange(PacketBuilderBase<?> pb, int w, int h) {
        INSTANCE.appendTextBufferResolutionChangeImpl(pb, w, h);
    }

    protected void appendTextBufferResolutionChangeImpl(PacketBuilderBase<?> pb, int w, int h) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiResolutionChange);
            pb.writeInt(w);
            pb.writeInt(h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferViewportResolutionChange(PacketBuilderBase<?> pb, int w, int h) {
        INSTANCE.appendTextBufferViewportResolutionChangeImpl(pb, w, h);
    }

    protected void appendTextBufferViewportResolutionChangeImpl(PacketBuilderBase<?> pb, int w, int h) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiViewportResolutionChange);
            pb.writeInt(w);
            pb.writeInt(h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferMaxResolutionChange(PacketBuilderBase<?> pb, int w, int h) {
        INSTANCE.appendTextBufferMaxResolutionChangeImpl(pb, w, h);
    }

    protected void appendTextBufferMaxResolutionChangeImpl(PacketBuilderBase<?> pb, int w, int h) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiMaxResolutionChange);
            pb.writeInt(w);
            pb.writeInt(h);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferSet(PacketBuilderBase<?> pb, int col, int row, String s, boolean vertical) {
        INSTANCE.appendTextBufferSetImpl(pb, col, row, s, vertical);
    }

    protected void appendTextBufferSetImpl(PacketBuilderBase<?> pb, int col, int row, String s, boolean vertical) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiSet);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeUTF(s);
            pb.writeBoolean(vertical);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferBitBlt(PacketBuilderBase<?> pb, int col, int row, int w, int h, String owner, int id, int fromCol, int fromRow) {
        INSTANCE.appendTextBufferBitBltImpl(pb, col, row, w, h, owner, id, fromCol, fromRow);
    }

    protected void appendTextBufferBitBltImpl(PacketBuilderBase<?> pb, int col, int row, int w, int h, String owner, int id, int fromCol, int fromRow) {
        try {
            pb.writePacketType(PacketType.TextBufferBitBlt);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeInt(w);
            pb.writeInt(h);
            pb.writeUTF(owner);
            pb.writeInt(id);
            pb.writeInt(fromCol);
            pb.writeInt(fromRow);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferRamInit(PacketBuilderBase<?> pb, String address, int id, CompoundTag nbt) {
        INSTANCE.appendTextBufferRamInitImpl(pb, address, id, nbt);
    }

    protected void appendTextBufferRamInitImpl(PacketBuilderBase<?> pb, String address, int id, CompoundTag nbt) {
        try {
            pb.writePacketType(PacketType.TextBufferRamInit);
            pb.writeUTF(address);
            pb.writeInt(id);
            pb.writeNBT(nbt);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferRamDestroy(PacketBuilderBase<?> pb, String owner, int id) {
        INSTANCE.appendTextBufferRamDestroyImpl(pb, owner, id);
    }

    protected void appendTextBufferRamDestroyImpl(PacketBuilderBase<?> pb, String owner, int id) {
        try {
            pb.writePacketType(PacketType.TextBufferRamDestroy);
            pb.writeUTF(owner);
            pb.writeInt(id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferRawSetText(PacketBuilderBase<?> pb, int col, int row, int[][] text) {
        INSTANCE.appendTextBufferRawSetTextImpl(pb, col, row, text);
    }

    protected void appendTextBufferRawSetTextImpl(PacketBuilderBase<?> pb, int col, int row, int[][] text) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiRawSetText);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeShort((short) text.length);
            for (int[] line : text) {
                pb.writeShort((short) line.length);
                for (int i : line) pb.writeMedium(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferRawSetBackground(PacketBuilderBase<?> pb, int col, int row, int[][] color) {
        INSTANCE.appendTextBufferRawSetBackgroundImpl(pb, col, row, color);
    }

    protected void appendTextBufferRawSetBackgroundImpl(PacketBuilderBase<?> pb, int col, int row, int[][] color) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiRawSetBackground);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeShort((short) color.length);
            for (int[] line : color) {
                pb.writeShort((short) line.length);
                for (int i : line) pb.writeInt(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void appendTextBufferRawSetForeground(PacketBuilderBase<?> pb, int col, int row, int[][] color) {
        INSTANCE.appendTextBufferRawSetForegroundImpl(pb, col, row, color);
    }

    protected void appendTextBufferRawSetForegroundImpl(PacketBuilderBase<?> pb, int col, int row, int[][] color) {
        try {
            pb.writePacketType(PacketType.TextBufferMultiRawSetForeground);
            pb.writeInt(col);
            pb.writeInt(row);
            pb.writeShort((short) color.length);
            for (int[] line : color) {
                pb.writeShort((short) line.length);
                for (int i : line) pb.writeInt(i);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void sendRobotMove(BlockEntity t, BlockPosition position, Direction direction) {
        INSTANCE.sendRobotMoveImpl(t, position, direction);
    }

    protected abstract void sendRobotMoveImpl(BlockEntity t, BlockPosition position, Direction direction) ;

    public static void sendRobotAnimateSwing(BlockEntity t, int animationTicksTotal) {
        INSTANCE.sendRobotAnimateSwingImpl(t, animationTicksTotal);
    }

    protected abstract void sendRobotAnimateSwingImpl(BlockEntity t, int animationTicksTotal) ;

    public static void sendRobotAnimateTurn(BlockEntity t, byte turnAxis, int animationTicksTotal) {
        INSTANCE.sendRobotAnimateTurnImpl(t, turnAxis, animationTicksTotal);
    }

    protected abstract void sendRobotAnimateTurnImpl(BlockEntity t, byte turnAxis, int animationTicksTotal) ;

    public static void sendRobotInventory(BlockEntity t, int slot, ItemStack stack) {
        INSTANCE.sendRobotInventoryImpl(t, slot, stack);
    }

    protected abstract void sendRobotInventoryImpl(BlockEntity t, int slot, ItemStack stack) ;

    public static void sendRobotLightChange(BlockEntity t, int lightColor) {
        INSTANCE.sendRobotLightChangeImpl(t, lightColor);
    }

    protected abstract void sendRobotLightChangeImpl(BlockEntity t, int lightColor) ;

    public static void sendRobotNameChange(BlockEntity t, String name) {
        INSTANCE.sendRobotNameChangeImpl(t, name);
    }

    protected abstract void sendRobotNameChangeImpl(BlockEntity t, String name) ;

    public static void sendRobotSelectedSlotChange(BlockEntity t, int selectedSlot) {
        INSTANCE.sendRobotSelectedSlotChangeImpl(t, selectedSlot);
    }

    protected abstract void sendRobotSelectedSlotChangeImpl(BlockEntity t, int selectedSlot) ;

    public static void sendFileSystemActivity(li.cil.oc.api.network.Node node, li.cil.oc.api.network.EnvironmentHost host, String name) {
        INSTANCE.sendFileSystemActivityImpl(node, host, name);
    }

    protected abstract void sendFileSystemActivityImpl(li.cil.oc.api.network.Node node, li.cil.oc.api.network.EnvironmentHost host, String name) ;

    public static void sendNetworkActivity(li.cil.oc.api.network.Node node, li.cil.oc.api.network.EnvironmentHost host) {
        INSTANCE.sendNetworkActivityImpl(node, host);
    }

    protected abstract void sendNetworkActivityImpl(li.cil.oc.api.network.Node node, li.cil.oc.api.network.EnvironmentHost host) ;

    public static void sendNanomachineConfiguration(Player player) {
        INSTANCE.sendNanomachineConfigurationImpl(player);
    }

    protected abstract void sendNanomachineConfigurationImpl(Player player) ;

    public static void sendNanomachineInputs(Player player) {
        INSTANCE.sendNanomachineInputsImpl(player);
    }

    protected abstract void sendNanomachineInputsImpl(Player player) ;

    public static void sendNanomachinePower(Player player) {
        INSTANCE.sendNanomachinePowerImpl(player);
    }

    protected abstract void sendNanomachinePowerImpl(Player player) ;

    public static void sendPetVisibility(String name, ServerPlayer player) {
        INSTANCE.sendPetVisibilityImpl(name, player);
    }

    protected abstract void sendPetVisibilityImpl(String name, ServerPlayer player) ;

    public static void sendRackInventory(BlockEntity t, ItemStack[] items) {
        INSTANCE.sendRackInventoryImpl(t, items);
    }

    protected abstract void sendRackInventoryImpl(BlockEntity t, ItemStack[] items) ;

    public static void sendRackMountableData(BlockEntity t, int mountable, CompoundTag data) {
        INSTANCE.sendRackMountableDataImpl(t, mountable, data);
    }

    protected abstract void sendRackMountableDataImpl(BlockEntity t, int mountable, CompoundTag data) ;

    public static void sendRaidChange(BlockEntity t, boolean[] slots) {
        INSTANCE.sendRaidChangeImpl(t, slots);
    }

    protected abstract void sendRaidChangeImpl(BlockEntity t, boolean[] slots) ;

    public static void sendRedstoneState(BlockEntity t, boolean outputEnabled, int[] output) {
        INSTANCE.sendRedstoneStateImpl(t, outputEnabled, output);
    }

    protected abstract void sendRedstoneStateImpl(BlockEntity t, boolean outputEnabled, int[] output) ;

    public static void sendWaypointLabel(li.cil.oc.core.impl.common.tileentity.Waypoint t) {
        INSTANCE.sendWaypointLabelImpl(t);
    }

    protected void sendWaypointLabelImpl(li.cil.oc.core.impl.common.tileentity.Waypoint t) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.WaypointLabel)) {
            try {
                pb.writeTileEntity(t);
                pb.writeUTF(t.label);
                pb.sendToPlayersNearTileEntity(t);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendMachineItemState(ServerPlayer player, ItemStack stack, boolean isRunning) {
        INSTANCE.sendMachineItemStateImpl(player, stack, isRunning);
    }

    protected void sendMachineItemStateImpl(ServerPlayer player, ItemStack stack, boolean isRunning) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.MachineItemStateResponse)) {
            try {
                pb.writeItemStack(stack, player.level().registryAccess());
                pb.writeBoolean(isRunning);
                pb.sendToPlayer(player);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void sendRotatableState(BlockEntity t, Direction pitch, Direction yaw) {
        INSTANCE.sendRotatableStateImpl(t, pitch, yaw);
    }

    protected void sendRotatableStateImpl(BlockEntity t, Direction pitch, Direction yaw) {
        try (PacketBuilderBase<?> pb = createBuilder(PacketType.RotatableState)) {
            pb.writeTileEntity(t);
            pb.writeDirection(pitch);
            pb.writeDirection(yaw);
            pb.sendToPlayersNearTileEntity(t);
        }
    }
}
