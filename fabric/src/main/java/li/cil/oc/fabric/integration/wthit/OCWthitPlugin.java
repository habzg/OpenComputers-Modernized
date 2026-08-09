package li.cil.oc.fabric.integration.wthit;

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
import li.cil.oc.fabric.common.init.Items;
import li.cil.oc.fabric.common.blockentity.RobotProxy;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.ICommonRegistrar;
import mcp.mobius.waila.api.IDataWriter;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.IServerAccessor;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.ITooltipComponent;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.IWailaCommonPlugin;
import mcp.mobius.waila.api.WailaConstants;
import mcp.mobius.waila.api.component.ItemComponent;
import mcp.mobius.waila.api.component.NamedItemListComponent;
import mcp.mobius.waila.api.data.BlockingDataProvider;
import mcp.mobius.waila.api.data.ItemData;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class OCWthitPlugin implements IWailaCommonPlugin, IWailaClientPlugin {
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

    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(OCNodeInfoProvider.INSTANCE, BlockEntity.class, 1000);
        registrar.body(OCRackItemProvider.INSTANCE, li.cil.oc.core.impl.common.blockentity.Rack.class, 1100);
        registrar.icon(OCDroneIconProvider.INSTANCE, li.cil.oc.core.impl.common.entity.Drone.class, 1000);
        registrar.icon(OCPrintIconProvider.INSTANCE, li.cil.oc.fabric.common.block.Print.class, 900);
        registrar.icon(OCChameliumIconProvider.INSTANCE, li.cil.oc.core.impl.common.block.ChameliumBlock.class, 900);
        registrar.head(OCBlockNameProvider.INSTANCE, li.cil.oc.core.impl.common.block.AbstractBlock.class, 1000);
        registrar.head(OCEntityNameProvider.INSTANCE, li.cil.oc.core.impl.common.entity.Drone.class, 1000);
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

    private enum OCBlockNameProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!config.getBoolean(CONFIG_BLOCK_NAME)) return;
            ItemStack stack = accessor.getStack();
            if (stack.isEmpty()) return;
            net.minecraft.world.item.Rarity rarity = stack.getRarity();
            if (rarity == net.minecraft.world.item.Rarity.COMMON && !(accessor.getBlock() instanceof li.cil.oc.core.impl.common.block.Print)) {
                return;
            }
            net.minecraft.network.chat.MutableComponent name = Component.empty().append(stack.getHoverName());
            if (rarity != net.minecraft.world.item.Rarity.COMMON) {
                name = name.withStyle(rarity.color());
            } else {
                name = name.withStyle(net.minecraft.ChatFormatting.WHITE);
            }
            tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, name);
        }
    }

    private enum OCEntityNameProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public void appendHead(ITooltip tooltip, IEntityAccessor accessor, IPluginConfig config) {
            if (!config.getBoolean(CONFIG_DRONE_NAME)) return;
            if (!(accessor.getEntity() instanceof li.cil.oc.core.impl.common.entity.Drone drone)) return;
            net.minecraft.world.item.Rarity rarity = li.cil.oc.core.impl.util.Rarity.byTier(drone.tier());
            if (rarity == net.minecraft.world.item.Rarity.COMMON) return;
            String ocName = drone.name();
            tooltip.setLine(WailaConstants.OBJECT_NAME_TAG,
                    Component.empty().append(ocName.isEmpty() ? accessor.getEntity().getName() : Component.literal(ocName))
                            .withStyle(rarity.color()));
        }
    }

    private enum OCNodeInfoProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (!config.getBoolean(CONFIG_NODE_INFO)) return;
            CompoundTag data = accessor.getData().raw();
            if (data.isEmpty()) return;

            if (data.contains(TAG_CHARGE_SPEED)) {
                int speed = (int) (data.getDouble(TAG_CHARGE_SPEED) * 100);
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.chargerspeed", speed + "%"));
            }
            if (data.contains(TAG_PROGRESS)) {
                double progress = data.getDouble(TAG_PROGRESS);
                int timeRemaining = data.getInt(TAG_TIME_REMAINING);
                String timeStr = timeRemaining < 60
                        ? String.format("0:%02d", timeRemaining)
                        : String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
                tooltip.addLine(Component.translatable("gui.opencomputers.assembler.progress", String.format("%.0f", progress), timeStr));
                if (data.contains(TAG_OUTPUT)) {
                    String output = data.getString(TAG_OUTPUT);
                    tooltip.addLine(Component.literal("Building: ").append(Component.translatable(output)));
                }
            }
            if (data.contains(TAG_SIGNAL_STRENGTH)) {
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.wirelessstrength", data.getDouble(TAG_SIGNAL_STRENGTH)));
            }

            int side = accessor.getSide().ordinal();
            if (data.contains(TAG_NODES)) {
                ListTag nodes = data.getList(TAG_NODES, Tag.TAG_COMPOUND);
                if (side < nodes.size()) {
                    readNode(tooltip, nodes.getCompound(side));
                }
            } else {
                readNode(tooltip, data);
            }

            if (data.contains(TAG_RACK_MOUNTABLE_NODES)) {
                BlockEntity be = accessor.getBlockEntity();
                if (be instanceof li.cil.oc.core.impl.common.blockentity.Rack rack) {
                    Direction facing = accessor.getSide();
                    if (facing == rack.facing()) {
                        BlockHitResult hit = accessor.getBlockHitResult();
                        float hitY = (float) (hit.getLocation().y - hit.getBlockPos().getY());
                        var slotOpt = rack.slotAt(facing, 0, hitY, 0);
                        if (slotOpt.isPresent()) {
                            ListTag mountableNodes = data.getList(TAG_RACK_MOUNTABLE_NODES, Tag.TAG_COMPOUND);
                            int slot = slotOpt.get();
                            if (slot < mountableNodes.size()) {
                                CompoundTag mountableTag = mountableNodes.getCompound(slot);
                                if (mountableTag.contains(TAG_SUB_NODES)) {
                                    ListTag subNodes = mountableTag.getList(TAG_SUB_NODES, Tag.TAG_COMPOUND);
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

        private static void readNode(ITooltip tooltip, CompoundTag tag) {
            if (tag.contains(TAG_ADDRESS)) {
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.address", tag.getString(TAG_ADDRESS)));
            }
            if (tag.contains(TAG_BUFFER) && tag.contains(TAG_BUFFER_SIZE)) {
                double buffer = tag.getDouble(TAG_BUFFER);
                double bufferSize = tag.getDouble(TAG_BUFFER_SIZE);
                if (bufferSize > 0) {
                    tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.storedenergy", String.format("%.1f/%.1f", buffer, bufferSize)));
                }
            }
            if (tag.contains(TAG_COMPONENT_NAME)) {
                String name = tag.getString(TAG_COMPONENT_NAME);
                if (!name.isEmpty()) {
                    tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.componentname", name));
                }
            }
        }
    }

    private enum OCDroneIconProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public ITooltipComponent getIcon(IEntityAccessor accessor, IPluginConfig config) {
            if (!config.getBoolean(CONFIG_DRONE_ICON)) return null;
            return new ItemComponent(Items.DRONE);
        }
    }

    private enum OCPrintIconProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public @NotNull ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = accessor.getBlock().getCloneItemStack(
                    accessor.getLevel(),
                    accessor.getPosition(),
                    accessor.getBlockState()
            );
            return new ItemComponent(stack);
        }
    }

    private enum OCChameliumIconProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public @NotNull ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = accessor.getBlock().getCloneItemStack(
                    accessor.getLevel(),
                    accessor.getPosition(),
                    accessor.getBlockState()
            );
            return new ItemComponent(stack);
        }
    }

    private enum OCRackItemProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getData().raw();
            if (!data.contains(TAG_RACK_ITEMS)) return;

            ListTag items = data.getList(TAG_RACK_ITEMS, Tag.TAG_COMPOUND);
            if (items.isEmpty()) return;

            var stacks = new ArrayList<ItemStack>();
            for (int i = 0; i < items.size(); i++) {
                CompoundTag itemTag = items.getCompound(i);
                String id = itemTag.getString("id");
                int count = itemTag.getInt("count");
                String name = itemTag.getString("name");

                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
                ItemStack stack = new ItemStack(item, count);
                stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
                stacks.add(stack);
            }

            if (!stacks.isEmpty()) {
                tooltip.addLine(new NamedItemListComponent(stacks, stacks.size()));
            }
        }
    }
}
