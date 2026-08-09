package li.cil.oc.neoforge.integration.wthit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.blockentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.common.blockentity.Case;
import li.cil.oc.core.impl.common.blockentity.Microcontroller;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.neoforge.common.blockentity.RobotProxy;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import mcp.mobius.waila.api.data.BlockingDataProvider;
import mcp.mobius.waila.api.data.ItemData;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

@SuppressWarnings("unused")
public class OCWthitCommonPlugin implements IWailaCommonPlugin {
    static final String TAG_ADDRESS = "oc_address";
    static final String TAG_BUFFER = "oc_buffer";
    static final String TAG_BUFFER_SIZE = "oc_buffer_size";
    static final String TAG_COMPONENT_NAME = "oc_component_name";
    static final String TAG_NODES = "oc_nodes";
    static final String TAG_CHARGE_SPEED = "oc_charge_speed";
    static final String TAG_PROGRESS = "oc_progress";
    static final String TAG_TIME_REMAINING = "oc_time_remaining";
    static final String TAG_OUTPUT = "oc_output";
    static final String TAG_SIGNAL_STRENGTH = "oc_signal_strength";
    static final String TAG_RACK_ITEMS = "oc_rack_items";
    static final String TAG_RACK_MOUNTABLE_NODES = "oc_rack_mountable_nodes";
    static final String TAG_SUB_NODES = "oc_sub_nodes";

    static final ResourceLocation CONFIG_NODE_INFO = ResourceLocation.parse("opencomputers:node_info");
    static final ResourceLocation CONFIG_DRONE_ICON = ResourceLocation.parse("opencomputers:drone_icon");
    static final ResourceLocation CONFIG_BLOCK_NAME = ResourceLocation.parse("opencomputers:block_name");
    static final ResourceLocation CONFIG_DRONE_NAME = ResourceLocation.parse("opencomputers:drone_name");

    @Override
    public void register(ICommonRegistrar registrar) {
        registrar.featureConfig(CONFIG_NODE_INFO, false);
        registrar.featureConfig(CONFIG_DRONE_ICON, true);
        registrar.featureConfig(CONFIG_BLOCK_NAME, true);
        registrar.featureConfig(CONFIG_DRONE_NAME, true);

        registrar.blockData(new BlockingDataProvider<>(ItemData.TYPE), Case.class, 900);
        registrar.blockData(new BlockingDataProvider<>(ItemData.TYPE), Microcontroller.class, 900);

        registrar.blockData((data, accessor, config) -> {
            RobotProxy proxy = (RobotProxy) accessor.getTarget();
            Set<Integer> excluded = proxy.robot.componentSlots();
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < proxy.getContainerSize(); i++) {
                if (!excluded.contains(i)) {
                    ItemStack stack = proxy.getItem(i);
                    if (!stack.isEmpty()) items.add(stack);
                }
            }
            data.add(ItemData.TYPE, res -> res.add(ItemData.of(config).add(items)));
        }, RobotProxy.class, 900);

        registrar.entityData((data, accessor, config) -> {
            Agent agent = (Agent) accessor.getTarget();
            List<ItemStack> items = new ArrayList<>();
            addItems(items, agent.equipmentInventory());
            addItems(items, agent.mainInventory());
            data.add(ItemData.TYPE, res -> res.add(ItemData.of(config).add(items)));
        }, li.cil.oc.core.impl.common.entity.Drone.class, 900);

        registrar.blockData((data, accessor, config) -> {
            data.blockAll(ItemData.TYPE);
            Rack rack = (Rack) accessor.getTarget();
            Map<String, CompoundTag> merged = new LinkedHashMap<>();
            for (int i = 0; i < rack.getContainerSize(); i++) {
                ItemStack stack = rack.getItem(i);
                if (!stack.isEmpty()) {
                    String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    String name = stack.getHoverName().getString();
                    String key = id + "\0" + name;
                    if (merged.containsKey(key)) {
                        CompoundTag existing = merged.get(key);
                        existing.putInt("count", existing.getInt("count") + stack.getCount());
                    } else {
                        CompoundTag itemTag = new CompoundTag();
                        itemTag.putString("id", id);
                        itemTag.putInt("count", stack.getCount());
                        itemTag.putString("name", name);
                        merged.put(key, itemTag);
                    }
                }
            }
            data.raw().put(TAG_RACK_ITEMS, new ListTag());
            data.raw().getList(TAG_RACK_ITEMS, Tag.TAG_COMPOUND).addAll(merged.values());
        }, Rack.class, 900);

        registrar.blockData(this::appendNodeData, BlockEntity.class, 1000);
    }

    private void appendNodeData(IDataWriter data, IServerAccessor<BlockEntity> accessor, IPluginConfig config) {
        if (!config.getBoolean(CONFIG_NODE_INFO)) return;
        BlockEntity be = accessor.getTarget();
        CompoundTag tag = data.raw();
        if (be instanceof SidedEnvironment sided) {
            ListTag nodes = new ListTag();
            for (Direction side : Direction.values()) {
                Node node = sided.sidedNode(side);
                nodes.add(writeNode(node, new CompoundTag()));
            }
            tag.put(TAG_NODES, nodes);
        } else if (be instanceof Environment env) {
            writeNode(env.node(), tag);
        }

        if (be instanceof li.cil.oc.core.impl.common.blockentity.Assembler assembler) {
            ignoreSidedness(tag, assembler.node());
            if (assembler.isAssembling()) {
                tag.putDouble(TAG_PROGRESS, assembler.progress());
                tag.putInt(TAG_TIME_REMAINING, assembler.timeRemaining());
                if (assembler.output != null && !assembler.output.isEmpty()) {
                    tag.putString(TAG_OUTPUT, assembler.output.getDescriptionId());
                }
            }
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Printer printer) {
            ignoreSidedness(tag, printer.node());
            if (printer.isPrinting()) {
                tag.putDouble(TAG_PROGRESS, printer.progress());
                tag.putInt(TAG_TIME_REMAINING, printer.timeRemaining());
            }
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Charger charger) {
            tag.putDouble(TAG_CHARGE_SPEED, charger.chargeSpeed);
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Keyboard keyboard) {
            ignoreSidedness(tag, keyboard.node());
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Hologram hologram) {
            ignoreSidedness(tag, hologram.node());
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Screen screen) {
            ignoreSidedness(tag, screen.node());
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive diskDrive) {
            tag.remove(TAG_ADDRESS);
            tag.remove(TAG_BUFFER);
            tag.remove(TAG_BUFFER_SIZE);
            tag.remove(TAG_COMPONENT_NAME);
            Node fsNode = diskDrive.filesystemNode();
            if (fsNode != null) writeNode(fsNode, tag);
        } else if (be instanceof li.cil.oc.core.impl.common.blockentity.Relay relay) {
            tag.putDouble(TAG_SIGNAL_STRENGTH, relay.strength);
        } else if (be instanceof Rack rack) {
            ListTag mountableNodes = new ListTag();
            for (int slot = 0; slot < rack.getContainerSize(); slot++) {
                var mountable = rack.getMountable(slot);
                CompoundTag nodeTag = new CompoundTag();
                if (mountable != null) {
                    writeNode(mountable.node(), nodeTag);
                    if (mountable instanceof li.cil.oc.core.impl.common.component.TerminalServer ts) {
                        ListTag subNodes = new ListTag();
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
            tag.put(TAG_RACK_MOUNTABLE_NODES, mountableNodes);
        }
    }

    private static void ignoreSidedness(CompoundTag tag, Node node) {
        tag.remove(TAG_NODES);
        CompoundTag nodeTag = writeNode(node, new CompoundTag());
        ListTag nodes = new ListTag();
        for (Direction side : Direction.values()) {
            nodes.add(nodeTag.copy());
        }
        tag.put(TAG_NODES, nodes);
    }

    static CompoundTag writeNode(Node node, CompoundTag tag) {
        if (node != null && node.reachability() != Visibility.None && !(node.host() instanceof NotAnalyzable)) {
            if (node.address() != null) tag.putString(TAG_ADDRESS, node.address());
            if (node instanceof Connector connector) {
                tag.putDouble(TAG_BUFFER, connector.localBuffer());
                tag.putDouble(TAG_BUFFER_SIZE, connector.localBufferSize());
            }
            if (node instanceof li.cil.oc.api.network.Component component) {
                tag.putString(TAG_COMPONENT_NAME, component.name());
            }
        }
        return tag;
    }

    private static void addItems(List<ItemStack> items, Container container) {
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) items.add(stack);
        }
    }
}
