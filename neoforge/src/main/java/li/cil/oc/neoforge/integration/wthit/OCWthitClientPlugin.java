package li.cil.oc.neoforge.integration.wthit;

import codechicken.multipart.block.TileMultipart;
import codechicken.multipart.util.PartRayTraceResult;
import li.cil.oc.neoforge.common.init.Items;
import li.cil.oc.neoforge.integration.cbmultipart.CablePart;
import mcp.mobius.waila.api.IBlockAccessor;
import mcp.mobius.waila.api.IBlockComponentProvider;
import mcp.mobius.waila.api.IClientRegistrar;
import mcp.mobius.waila.api.IEntityAccessor;
import mcp.mobius.waila.api.IEntityComponentProvider;
import mcp.mobius.waila.api.IPluginConfig;
import mcp.mobius.waila.api.ITooltip;
import mcp.mobius.waila.api.ITooltipComponent;
import mcp.mobius.waila.api.IWailaClientPlugin;
import mcp.mobius.waila.api.IWailaConfig;
import mcp.mobius.waila.api.WailaConstants;
import mcp.mobius.waila.api.component.ItemComponent;
import mcp.mobius.waila.api.component.NamedItemListComponent;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@SuppressWarnings("unused")
public class OCWthitClientPlugin implements IWailaClientPlugin {
    @Override
    public void register(IClientRegistrar registrar) {
        registrar.body(OCNodeInfoProvider.INSTANCE, BlockEntity.class, 1000);
        registrar.body(OCRackItemProvider.INSTANCE, li.cil.oc.core.impl.common.tileentity.Rack.class, 1100);
        registrar.icon(OCDroneIconProvider.INSTANCE, li.cil.oc.core.impl.common.entity.Drone.class, 1000);
        registrar.icon(OCPrintIconProvider.INSTANCE, li.cil.oc.neoforge.common.block.Print.class, 900);
        registrar.icon(OCChameliumIconProvider.INSTANCE, li.cil.oc.neoforge.common.block.ChameliumBlock.class, 900);
        registrar.head(OCCableMultipartProvider.INSTANCE, TileMultipart.class, 900);
        registrar.icon(OCCableMultipartProvider.INSTANCE, TileMultipart.class, 900);
    }

    private enum OCNodeInfoProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getData().raw();
            if (data.isEmpty()) return;

            if (data.contains(OCWthitCommonPlugin.TAG_CHARGE_SPEED)) {
                int speed = (int) (data.getDouble(OCWthitCommonPlugin.TAG_CHARGE_SPEED) * 100);
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.chargerspeed", speed + "%"));
            }
            if (data.contains(OCWthitCommonPlugin.TAG_PROGRESS)) {
                double progress = data.getDouble(OCWthitCommonPlugin.TAG_PROGRESS);
                int timeRemaining = data.getInt(OCWthitCommonPlugin.TAG_TIME_REMAINING);
                String timeStr = timeRemaining < 60
                        ? String.format("0:%02d", timeRemaining)
                        : String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
                tooltip.addLine(Component.translatable("gui.opencomputers.assembler.progress", String.format("%.0f", progress), timeStr));
                if (data.contains(OCWthitCommonPlugin.TAG_OUTPUT)) {
                    String output = data.getString(OCWthitCommonPlugin.TAG_OUTPUT);
                    tooltip.addLine(Component.literal("Building: ").append(Component.translatable(output)));
                }
            }
            if (data.contains(OCWthitCommonPlugin.TAG_SIGNAL_STRENGTH)) {
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.wirelessstrength", data.getDouble(OCWthitCommonPlugin.TAG_SIGNAL_STRENGTH)));
            }

            int side = accessor.getSide().ordinal();
            if (data.contains(OCWthitCommonPlugin.TAG_NODES)) {
                ListTag nodes = data.getList(OCWthitCommonPlugin.TAG_NODES, Tag.TAG_COMPOUND);
                if (side < nodes.size()) {
                    readNode(tooltip, nodes.getCompound(side));
                }
            } else {
                readNode(tooltip, data);
            }

            if (data.contains(OCWthitCommonPlugin.TAG_RACK_MOUNTABLE_NODES)) {
                BlockEntity be = accessor.getBlockEntity();
                if (be instanceof li.cil.oc.core.impl.common.tileentity.Rack rack) {
                    Direction facing = accessor.getSide();
                    if (facing == rack.facing()) {
                        BlockHitResult hit = accessor.getBlockHitResult();
                        float hitY = (float) (hit.getLocation().y - hit.getBlockPos().getY());
                        var slotOpt = rack.slotAt(facing, 0, hitY, 0);
                        if (slotOpt.isPresent()) {
                            ListTag mountableNodes = data.getList(OCWthitCommonPlugin.TAG_RACK_MOUNTABLE_NODES, Tag.TAG_COMPOUND);
                            int slot = slotOpt.get();
                            if (slot < mountableNodes.size()) {
                                CompoundTag mountableTag = mountableNodes.getCompound(slot);
                                if (mountableTag.contains(OCWthitCommonPlugin.TAG_SUB_NODES)) {
                                    ListTag subNodes = mountableTag.getList(OCWthitCommonPlugin.TAG_SUB_NODES, Tag.TAG_COMPOUND);
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
            if (tag.contains(OCWthitCommonPlugin.TAG_ADDRESS)) {
                tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.address", tag.getString(OCWthitCommonPlugin.TAG_ADDRESS)));
            }
            if (tag.contains(OCWthitCommonPlugin.TAG_BUFFER) && tag.contains(OCWthitCommonPlugin.TAG_BUFFER_SIZE)) {
                double buffer = tag.getDouble(OCWthitCommonPlugin.TAG_BUFFER);
                double bufferSize = tag.getDouble(OCWthitCommonPlugin.TAG_BUFFER_SIZE);
                if (bufferSize > 0) {
                    tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.storedenergy", String.format("%.1f/%.1f", buffer, bufferSize)));
                }
            }
            if (tag.contains(OCWthitCommonPlugin.TAG_COMPONENT_NAME)) {
                String name = tag.getString(OCWthitCommonPlugin.TAG_COMPONENT_NAME);
                if (!name.isEmpty()) {
                    tooltip.addLine(Component.translatable("gui.opencomputers.analyzer.componentname", name));
                }
            }
        }
    }

    private enum OCDroneIconProvider implements IEntityComponentProvider {
        INSTANCE;

        @Override
        public @NotNull ITooltipComponent getIcon(IEntityAccessor accessor, IPluginConfig config) {
            return new ItemComponent(Items.DRONE.get());
        }
    }

    private enum OCPrintIconProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public @NotNull ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = accessor.getBlock().getCloneItemStack(
                    accessor.getBlockState(),
                    accessor.getBlockHitResult(),
                    accessor.getLevel(),
                    accessor.getPosition(),
                    accessor.getPlayer()
            );
            return new ItemComponent(stack);
        }
    }

    private enum OCChameliumIconProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public @NotNull ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
            ItemStack stack = accessor.getBlock().getCloneItemStack(
                    accessor.getBlockState(),
                    accessor.getBlockHitResult(),
                    accessor.getLevel(),
                    accessor.getPosition(),
                    accessor.getPlayer()
            );
            return new ItemComponent(stack);
        }
    }

    private enum OCCableMultipartProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendHead(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            if (getHitCablePart(accessor) != null) {
                tooltip.setLine(WailaConstants.OBJECT_NAME_TAG, IWailaConfig.get().getFormatter().blockName(
                        li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.Cable).block().getName()));
            }
        }

        @Override
        public @Nullable ITooltipComponent getIcon(IBlockAccessor accessor, IPluginConfig config) {
            if (getHitCablePart(accessor) != null) {
                return new ItemComponent(
                        new ItemStack(li.cil.oc.api.Items.get(li.cil.oc.core.Constants.BlockName.Cable).block()));
            }
            return null;
        }

        @Nullable
        private static CablePart getHitCablePart(IBlockAccessor accessor) {
            BlockEntity te = accessor.getBlockEntity();
            if (!(te instanceof TileMultipart)) return null;
            BlockHitResult hitResult = accessor.getBlockHitResult();
            if (hitResult instanceof PartRayTraceResult partHit && partHit.part instanceof CablePart cablePart) {
                return cablePart;
            }
            return null;
        }
    }

    private enum OCRackItemProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendBody(ITooltip tooltip, IBlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getData().raw();
            if (!data.contains(OCWthitCommonPlugin.TAG_RACK_ITEMS)) return;

            ListTag items = data.getList(OCWthitCommonPlugin.TAG_RACK_ITEMS, Tag.TAG_COMPOUND);
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
