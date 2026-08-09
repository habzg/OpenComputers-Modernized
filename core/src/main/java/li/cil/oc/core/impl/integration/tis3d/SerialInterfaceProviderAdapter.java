package li.cil.oc.core.impl.integration.tis3d;

import java.util.ArrayDeque;
import java.util.Optional;
import java.util.Queue;

import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Adapter;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.tis3d.api.serial.SerialInterface;
import li.cil.tis3d.api.serial.SerialInterfaceProvider;
import li.cil.tis3d.api.serial.SerialProtocolDocumentationReference;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class SerialInterfaceProviderAdapter implements SerialInterfaceProvider {
    private static final SerialProtocolDocumentationReference DOCUMENTATION_REFERENCE =
            new SerialProtocolDocumentationReference(Component.literal("OpenComputers Adapter"), "opencomputersadapter.md");

    @Override
    public boolean matches(final Level level, final @NotNull BlockPos position, final @NotNull Direction side) {
        return level.getBlockEntity(position) instanceof Adapter;
    }

    @Override
    public @NotNull Optional<SerialInterface> getInterface(final Level level, final @NotNull BlockPos position, final @NotNull Direction face) {
        final Adapter adapter = (Adapter) level.getBlockEntity(position);
        return Optional.of(new SerialInterfaceAdapter(adapter));
    }

    @Override
    public @NotNull Optional<SerialProtocolDocumentationReference> getDocumentationReference() {
        return Optional.of(DOCUMENTATION_REFERENCE);
    }

    @Override
    public boolean stillValid(final @NotNull Level level, final @NotNull BlockPos position, final @NotNull Direction side, final @NotNull SerialInterface serialInterface) {
        return serialInterface instanceof SerialInterfaceAdapter adapter
                && adapter.tileEntity == level.getBlockEntity(position);
    }

    public static class SerialInterfaceAdapter implements Environment, SerialInterface {
        public static final int BufferCapacity = 128;

        private final Adapter tileEntity;
        private final Queue<Short> readBuffer = new ArrayDeque<>();
        private final Queue<Short> writeBuffer = new ArrayDeque<>();
        private boolean isReading = false;

        private final Node node = Network.newNode(this, Visibility.Network).withComponent("serial_port").create();

        public SerialInterfaceAdapter(final Adapter tileEntity) {
            this.tileEntity = tileEntity;
        }

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Callback
        public Object[] setReading(final Context context, final Arguments args) {
            isReading = args.checkBoolean(0);
            return null;
        }

        @Callback
        public Object[] read(final Context context, final Arguments args) {
            synchronized (readBuffer) {
                if (!readBuffer.isEmpty()) {
                    return ResultWrapper.result(readBuffer.remove());
                }
                return null;
            }
        }

        @Callback
        public Object[] write(final Context context, final Arguments args) {
            synchronized (writeBuffer) {
                if (writeBuffer.size() < BufferCapacity) {
                    writeBuffer.add((short) args.checkInteger(0));
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(false, "buffer full");
            }
        }

        // ------------------------------------------------------------------ //
        // SerialInterface

        @Override
        public boolean canWrite() {
            synchronized (readBuffer) {
                return isReading && readBuffer.size() < BufferCapacity;
            }
        }

        @Override
        public void write(final short value) {
            synchronized (readBuffer) {
                readBuffer.add(value);
            }
        }

        @Override
        public boolean canRead() {
            ensureConnected();
            synchronized (writeBuffer) {
                return !writeBuffer.isEmpty();
            }
        }

        @Override
        public short peek() {
            synchronized (writeBuffer) {
                final Short value = writeBuffer.peek();
                return value == null ? 0 : value;
            }
        }

        @Override
        public void skip() {
            synchronized (writeBuffer) {
                writeBuffer.remove();
            }
        }

        @Override
        public void reset() {
            synchronized (readBuffer) {
                synchronized (writeBuffer) {
                    readBuffer.clear();
                    writeBuffer.clear();
                    node.remove();
                }
            }
        }

        @Override
        public void load(final @NotNull CompoundTag tag) {
            node.load(tag, null);
            synchronized (writeBuffer) {
                writeBuffer.clear();
                for (int value : tag.getIntArray("writeBuffer")) {
                    writeBuffer.add((short) value);
                }
            }
            synchronized (readBuffer) {
                readBuffer.clear();
                for (int value : tag.getIntArray("readBuffer")) {
                    readBuffer.add((short) value);
                }
            }
            isReading = tag.getBoolean("isReading");
        }

        @Override
        public void save(final @NotNull CompoundTag tag) {
            node.save(tag, null);
            synchronized (writeBuffer) {
                final int[] buffer = new int[writeBuffer.size()];
                int i = 0;
                for (short value : writeBuffer) {
                    buffer[i++] = value;
                }
                tag.putIntArray("writeBuffer", buffer);
            }
            synchronized (readBuffer) {
                final int[] buffer = new int[readBuffer.size()];
                int i = 0;
                for (short value : readBuffer) {
                    buffer[i++] = value;
                }
                tag.putIntArray("readBuffer", buffer);
            }
            tag.putBoolean("isReading", isReading);
        }

        private void ensureConnected() {
            if (tileEntity.node().network() != node.network()) {
                tileEntity.node().connect(node);
            }
        }
    }
}
