package li.cil.oc.neoforge.integration.jade;

import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.tileentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.common.tileentity.Assembler;
import li.cil.oc.core.impl.common.tileentity.Charger;
import li.cil.oc.core.impl.common.tileentity.DiskDrive;
import li.cil.oc.core.impl.common.tileentity.Hologram;
import li.cil.oc.core.impl.common.tileentity.Keyboard;
import li.cil.oc.core.impl.common.tileentity.Printer;
import li.cil.oc.core.impl.common.tileentity.Screen;
import li.cil.oc.neoforge.common.tileentity.Relay;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

@SuppressWarnings("unused")
public enum OCDataProvider implements IBlockComponentProvider, IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = ResourceLocation.parse("opencomputers:node_info");
    private static final String TAG_ADDRESS = "oc_address";
    private static final String TAG_BUFFER = "oc_buffer";
    private static final String TAG_BUFFER_SIZE = "oc_buffer_size";
    private static final String TAG_COMPONENT_NAME = "oc_component_name";
    private static final String TAG_NODES = "oc_nodes";
    private static final String TAG_CHARGE_SPEED = "oc_charge_speed";
    private static final String TAG_PROGRESS = "oc_progress";
    private static final String TAG_TIME_REMAINING = "oc_time_remaining";
    private static final String TAG_OUTPUT = "oc_output";
    private static final String TAG_SIGNAL_STRENGTH = "oc_signal_strength";
    private static final String TAG_RACK_MOUNTABLE_NODES = "oc_rack_mountable_nodes";
    private static final String TAG_SUB_NODES = "oc_sub_nodes";

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public void appendServerData(final CompoundTag data, final BlockAccessor accessor) {
        final BlockEntity be = accessor.getBlockEntity();

        if (be instanceof final SidedEnvironment sided) {
            final ListTag nodes = new ListTag();
            for (final Direction side : Direction.values()) {
                final Node node = sided.sidedNode(side);
                nodes.add(writeNode(node, new CompoundTag()));
            }
            data.put(TAG_NODES, nodes);
        } else if (be instanceof final Environment env) {
            writeNode(env.node(), data);
        }

        if (be instanceof final Assembler assembler) {
            ignoreSidedness(data, assembler.node());
            if (assembler.isAssembling()) {
                data.putDouble(TAG_PROGRESS, assembler.progress());
                data.putInt(TAG_TIME_REMAINING, assembler.timeRemaining());
                if (assembler.output != null && !assembler.output.isEmpty()) {
                    data.putString(TAG_OUTPUT, assembler.output.getDescriptionId());
                }
            }
        } else if (be instanceof final Printer printer) {
            ignoreSidedness(data, printer.node());
            if (printer.isPrinting()) {
                data.putDouble(TAG_PROGRESS, printer.progress());
                data.putInt(TAG_TIME_REMAINING, printer.timeRemaining());
            }
        } else if (be instanceof final Charger charger) {
            data.putDouble(TAG_CHARGE_SPEED, charger.chargeSpeed);
        } else if (be instanceof final Keyboard keyboard) {
            ignoreSidedness(data, keyboard.node());
        } else if (be instanceof final Hologram hologram) {
            ignoreSidedness(data, hologram.node());
        } else if (be instanceof final Screen screen) {
            ignoreSidedness(data, screen.node());
        } else if (be instanceof final DiskDrive diskDrive) {
            data.remove(TAG_ADDRESS);
            data.remove(TAG_BUFFER);
            data.remove(TAG_BUFFER_SIZE);
            data.remove(TAG_COMPONENT_NAME);
            final Node fsNode = diskDrive.filesystemNode();
            if (fsNode != null) {
                writeNode(fsNode, data);
            }
        } else if (be instanceof final Relay relay) {
            data.putDouble(TAG_SIGNAL_STRENGTH, relay.strength);
        } else if (be instanceof final li.cil.oc.core.impl.common.tileentity.Rack rack) {
            final ListTag mountableNodes = new ListTag();
            for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                final var mountable = rack.getMountable(slot);
                final CompoundTag nodeTag = new CompoundTag();
                if (mountable != null) {
                    writeNode(mountable.node(), nodeTag);
                    if (mountable instanceof final li.cil.oc.neoforge.common.component.TerminalServer ts) {
                        final ListTag subNodes = new ListTag();
                        if (ts.bufferIfLoaded() != null && ts.bufferIfLoaded().node() != null) {
                            subNodes.add(writeNode(ts.bufferIfLoaded().node(), new CompoundTag()));
                        }
                        if (ts.keyboard() != null && ts.keyboard().node() != null) {
                            subNodes.add(writeNode(ts.keyboard().node(), new CompoundTag()));
                        }
                        if (!subNodes.isEmpty()) {
                            nodeTag.put(TAG_SUB_NODES, subNodes);
                        }
                    }
                }
                mountableNodes.add(nodeTag);
            }
            data.put(TAG_RACK_MOUNTABLE_NODES, mountableNodes);
        }
    }

    private static void ignoreSidedness(final CompoundTag data, final Node node) {
        data.remove(TAG_NODES);
        final CompoundTag nodeTag = writeNode(node, new CompoundTag());
        final ListTag nodes = new ListTag();
        for (final Direction side : Direction.values()) {
            nodes.add(nodeTag.copy());
        }
        data.put(TAG_NODES, nodes);
    }

    private static CompoundTag writeNode(final Node node, final CompoundTag tag) {
        if (node != null && node.reachability() != Visibility.None && !(node.host() instanceof NotAnalyzable)) {
            if (node.address() != null) {
                tag.putString(TAG_ADDRESS, node.address());
            }
            if (node instanceof final Connector connector) {
                tag.putDouble(TAG_BUFFER, connector.localBuffer());
                tag.putDouble(TAG_BUFFER_SIZE, connector.localBufferSize());
            }
            if (node instanceof final li.cil.oc.api.network.Component component) {
                tag.putString(TAG_COMPONENT_NAME, component.name());
            }
        }
        return tag;
    }

    @Override
    public void appendTooltip(final ITooltip tooltip, final BlockAccessor accessor, final IPluginConfig config) {
        final CompoundTag data = accessor.getServerData();
        if (data.isEmpty()) return;

        if (data.contains(TAG_CHARGE_SPEED)) {
            final int speed = (int) (data.getDouble(TAG_CHARGE_SPEED) * 100);
            tooltip.add(Component.translatable("gui.opencomputers.analyzer.chargerspeed", speed + "%"));
        }
        if (data.contains(TAG_PROGRESS)) {
            final double progress = data.getDouble(TAG_PROGRESS);
            final int timeRemaining = data.getInt(TAG_TIME_REMAINING);
            final String timeStr = timeRemaining < 60
                    ? String.format("0:%02d", timeRemaining)
                    : String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
            tooltip.add(Component.translatable("gui.opencomputers.assembler.progress", String.format("%.0f", progress), timeStr));
            if (data.contains(TAG_OUTPUT)) {
                final String output = data.getString(TAG_OUTPUT);
                tooltip.add(Component.literal("Building: ").append(Component.translatable(output)));
            }
        }
        if (data.contains(TAG_SIGNAL_STRENGTH)) {
            tooltip.add(Component.translatable("gui.opencomputers.analyzer.wirelessstrength", data.getDouble(TAG_SIGNAL_STRENGTH)));
        }

        final int side = accessor.getSide().ordinal();
        if (data.contains(TAG_NODES)) {
            final ListTag nodes = data.getList(TAG_NODES, Tag.TAG_COMPOUND);
            if (side < nodes.size()) {
                readNode(tooltip, nodes.getCompound(side));
            }
        } else {
            readNode(tooltip, data);
        }

        if (data.contains(TAG_RACK_MOUNTABLE_NODES)) {
            final BlockEntity be = accessor.getBlockEntity();
            if (be instanceof final li.cil.oc.core.impl.common.tileentity.Rack rack) {
                final Direction facing = accessor.getSide();
                if (facing == rack.facing()) {
                    final var hit = accessor.getHitResult();
                    final float hitY = (float) (hit.getLocation().y - hit.getBlockPos().getY());
                    final var slotOpt = rack.slotAt(facing, 0, hitY, 0);
                    if (slotOpt.isPresent()) {
                        final ListTag mountableNodes = data.getList(TAG_RACK_MOUNTABLE_NODES, Tag.TAG_COMPOUND);
                        final int slot = slotOpt.get();
                        if (slot < mountableNodes.size()) {
                            final CompoundTag mountableTag = mountableNodes.getCompound(slot);
                            if (mountableTag.contains(TAG_SUB_NODES)) {
                                final ListTag subNodes = mountableTag.getList(TAG_SUB_NODES, Tag.TAG_COMPOUND);
                                for (int i = 0; i < subNodes.size(); i++) {
                                    readNode(tooltip, subNodes.getCompound(i));
                                }
                            } else {
                                readNode(tooltip, mountableTag);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void readNode(final ITooltip tooltip, final CompoundTag tag) {
        if (tag.contains(TAG_ADDRESS)) {
            tooltip.add(Component.translatable("gui.opencomputers.analyzer.address", tag.getString(TAG_ADDRESS)));
        }
        if (tag.contains(TAG_BUFFER) && tag.contains(TAG_BUFFER_SIZE)) {
            final double buffer = tag.getDouble(TAG_BUFFER);
            final double bufferSize = tag.getDouble(TAG_BUFFER_SIZE);
            if (bufferSize > 0) {
                tooltip.add(Component.translatable("gui.opencomputers.analyzer.storedenergy", String.format("%.1f/%.1f", buffer, bufferSize)));
            }
        }
        if (tag.contains(TAG_COMPONENT_NAME)) {
            final String name = tag.getString(TAG_COMPONENT_NAME);
            if (!name.isEmpty()) {
                tooltip.add(Component.translatable("gui.opencomputers.analyzer.componentname", name));
            }
        }
    }
}
