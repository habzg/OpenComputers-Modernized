package li.cil.oc.neoforge.integration.top;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.blockentity.traits.NotAnalyzable;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Charger;
import li.cil.oc.core.impl.common.blockentity.DiskDrive;
import li.cil.oc.core.impl.common.blockentity.Hologram;
import li.cil.oc.core.impl.common.blockentity.Keyboard;
import li.cil.oc.core.impl.common.blockentity.Printer;
import li.cil.oc.core.impl.common.blockentity.Rack;
import li.cil.oc.core.impl.common.blockentity.Relay;
import li.cil.oc.core.impl.common.blockentity.Screen;
import li.cil.oc.neoforge.common.blockentity.RobotProxy;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public class OCProbeProvider implements IProbeInfoProvider {

    @Override
    public ResourceLocation getID() {
        return ResourceLocation.parse("opencomputers:default");
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player,
                             Level world, BlockState blockState, IProbeHitData data) {
        BlockEntity be = world.getBlockEntity(data.getPos());
        if (be == null) return;

        Direction side = data.getSideHit();

        Node node = null;
        if (be instanceof SidedEnvironment sided) {
            node = sided.sidedNode(side);
        } else if (be instanceof Environment env) {
            node = env.node();
        }

        if (be instanceof Assembler assembler) {
            node = assembler.node();
            if (assembler.isAssembling()) {
                double progress = assembler.progress();
                int timeRemaining = assembler.timeRemaining();
                String timeStr = timeRemaining < 60
                        ? String.format("0:%02d", timeRemaining)
                        : String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
                probeInfo.mcText(Component.translatable("gui.opencomputers.assembler.progress",
                        String.format("%.0f", progress), timeStr));
                if (assembler.output != null && !assembler.output.isEmpty()) {
                    probeInfo.mcText(Component.literal("Building: ")
                            .append(Component.translatable(assembler.output.getDescriptionId())));
                }
            }
        } else if (be instanceof Printer printer) {
            node = printer.node();
            if (printer.isPrinting()) {
                double progress = printer.progress();
                int timeRemaining = printer.timeRemaining();
                String timeStr = timeRemaining < 60
                        ? String.format("0:%02d", timeRemaining)
                        : String.format("%d:%02d", timeRemaining / 60, timeRemaining % 60);
                probeInfo.mcText(Component.translatable("gui.opencomputers.assembler.progress",
                        String.format("%.0f", progress), timeStr));
            }
        } else if (be instanceof Charger charger) {
            int speed = (int) (charger.chargeSpeed * 100);
            probeInfo.mcText(Component.translatable("gui.opencomputers.analyzer.chargerspeed", speed + "%"));
        } else if (be instanceof Keyboard keyboard) {
            node = keyboard.node();
        } else if (be instanceof Hologram hologram) {
            node = hologram.node();
        } else if (be instanceof Screen screen) {
            node = screen.node();
        } else if (be instanceof DiskDrive diskDrive) {
            node = diskDrive.filesystemNode();
        } else if (be instanceof Relay relay) {
            probeInfo.mcText(Component.translatable("gui.opencomputers.analyzer.wirelessstrength", relay.strength));
        } else if (be instanceof Rack rack) {
            if (side == rack.facing()) {
                node = null;
                handleRackFront(probeInfo, rack, data);
            }
            addRackItems(probeInfo, rack);
        } else if (be instanceof RobotProxy proxy && mode == ProbeMode.EXTENDED) {
            Set<Integer> excluded = proxy.robot.componentSlots();
            Map<String, ItemStack> merged = new LinkedHashMap<>();
            for (int i = 0; i < proxy.getContainerSize(); i++) {
                if (!excluded.contains(i)) {
                    ItemStack stack = proxy.getItem(i);
                    if (!stack.isEmpty()) {
                        mergeItem(merged, stack);
                    }
                }
            }
            for (ItemStack stack : merged.values()) {
                probeInfo.horizontal().item(stack).itemLabel(stack);
            }
        }

        if (node != null) {
            writeNode(probeInfo, node);
        }
    }

    private void handleRackFront(IProbeInfo probeInfo, Rack rack, IProbeHitData data) {
        Vec3 hitVec = data.getHitVec();
        float hitY = (float) (hitVec.y - data.getPos().getY());
        var slotOpt = rack.slotAt(data.getSideHit(), 0, hitY, 0);
        if (slotOpt.isEmpty()) return;

        var mountable = rack.getMountable(slotOpt.get());
        if (mountable == null) return;

        if (mountable instanceof li.cil.oc.core.impl.common.component.TerminalServer ts) {
            if (ts.bufferIfLoaded() != null && ts.bufferIfLoaded().node() != null) {
                writeNode(probeInfo, ts.bufferIfLoaded().node());
            }
            if (ts.keyboard() != null && ts.keyboard().node() != null) {
                writeNode(probeInfo, ts.keyboard().node());
            }
        } else {
            writeNode(probeInfo, mountable.node());
        }
    }

    private void addRackItems(IProbeInfo probeInfo, Rack rack) {
        Map<String, ItemStack> merged = new LinkedHashMap<>();
        for (int i = 0; i < rack.getContainerSize(); i++) {
            ItemStack stack = rack.getItem(i);
            if (!stack.isEmpty()) {
                mergeItem(merged, stack);
            }
        }
        for (ItemStack stack : merged.values()) {
            probeInfo.horizontal().item(stack).itemLabel(stack);
        }
    }

    private static void mergeItem(Map<String, ItemStack> merged, ItemStack stack) {
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "\0" + stack.getHoverName().getString();
        if (merged.containsKey(key)) {
            merged.get(key).grow(stack.getCount());
        } else {
            merged.put(key, stack.copy());
        }
    }

    private void writeNode(IProbeInfo probeInfo, Node node) {
        if (node == null || node.reachability() == Visibility.None) return;
        if (node.host() instanceof NotAnalyzable) return;

        if (node.address() != null) {
            probeInfo.mcText(Component.translatable("gui.opencomputers.analyzer.address", node.address()));
        }
        if (node instanceof Connector connector) {
            double buffer = connector.localBuffer();
            double bufferSize = connector.localBufferSize();
            if (bufferSize > 0) {
                probeInfo.mcText(Component.translatable("gui.opencomputers.analyzer.storedenergy",
                        String.format("%.1f/%.1f", buffer, bufferSize)));
            }
        }
        if (node instanceof li.cil.oc.api.network.Component component) {
            probeInfo.mcText(Component.translatable("gui.opencomputers.analyzer.componentname", component.name()));
        }
    }
}
