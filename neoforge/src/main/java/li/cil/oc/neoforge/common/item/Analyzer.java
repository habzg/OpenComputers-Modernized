package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.Items;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.tileentity.Screen;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.jetbrains.annotations.NotNull;

public class Analyzer extends DelegateItem {

    public Analyzer(Properties properties) {
        super(properties);
    }

    public static boolean analyze(Object thing, Player player, int side, float hitX, float hitY, float hitZ) {
        Level world = player.level();
        if (thing instanceof Analyzable analyzable) {
            if (!world.isClientSide) {
                analyzeNodes(analyzable.onAnalyze(player, side, hitX, hitY, hitZ), player);
            }
            return true;
        }
        if (thing instanceof SidedEnvironment host) {
            if (!world.isClientSide) {
                analyzeNodes(new Node[]{host.sidedNode(Direction.from3DDataValue(side))}, player);
            }
            return true;
        }
        if (thing instanceof Environment host) {
            if (!world.isClientSide) {
                analyzeNodes(new Node[]{host.node()}, player);
            }
            return true;
        }
        return false;
    }

    private static void analyzeNodes(Node[] nodes, Player player) {
        if (nodes == null) return;
        if (player instanceof net.neoforged.neoforge.common.util.FakePlayer) return;
        for (Node node : nodes) {
            if (node == null) continue;
            if (node.host() instanceof Machine machine) {
                if (machine.lastError() != null) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.lasterror", net.minecraft.network.chat.Component.translatable(machine.lastError())));
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.components", machine.componentCount() + "/" + machine.maxComponents()));
                var users = machine.users();
                if (users.length != 0) {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (String s : users) {
                        if (!first) sb.append(", ");
                        sb.append(s);
                        first = false;
                    }
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.users", sb.toString()));
                }
            }
            if (node instanceof Connector connector) {
                if (connector.localBufferSize() > 0) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.storedenergy", String.format("%.2f/%.2f", connector.localBuffer(), connector.localBufferSize())));
                }
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.totalenergy", String.format("%.2f/%.2f", connector.globalBuffer(), connector.globalBufferSize())));
            }
            if (node instanceof Component component) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.componentname", component.getString()));
            }
            String address = node.address();
            if (address != null && !address.isEmpty()) {
                net.minecraft.network.chat.MutableComponent addr = net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.address", address);
                addr.setStyle(addr.getStyle().withClickEvent(new net.minecraft.network.chat.ClickEvent(net.minecraft.network.chat.ClickEvent.Action.RUN_COMMAND, "/oc_setclipboard " + address)));
                addr.setStyle(addr.getStyle().withHoverEvent(new net.minecraft.network.chat.HoverEvent(net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT, net.minecraft.network.chat.Component.translatable("gui.opencomputers.analyzer.copytoclipboard"))));
                player.sendSystemMessage(addr);
                PacketSender.sendAnalyze(address, (ServerPlayer) player);
            }
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null && !cd.isEmpty()) {
                CompoundTag tag = cd.copyTag();
                tag.remove(Settings.namespace + "clipboard");
                if (tag.isEmpty()) {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                } else {
                    stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                }
            }
        }
        return super.use(world, player, hand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos pos = context.getClickedPos();
        Direction side = context.getClickedFace();
        Vec3 clickLoc = context.getClickLocation();
        float hitX = (float) (clickLoc.x - pos.getX());
        float hitY = (float) (clickLoc.y - pos.getY());
        float hitZ = (float) (clickLoc.z - pos.getZ());

        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof Screen screen && side == screen.facing()) {
            if (player != null && player.isShiftKeyDown()) {
                screen.copyToAnalyzer(hitX, hitY, hitZ, player);
            } else {
                ItemStack stack = context.getItemInHand();
                CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
                if (cd != null && !cd.isEmpty()) {
                    CompoundTag tag = cd.copyTag();
                    if (tag.contains(Settings.namespace + "clipboard")) {
                        if (!world.isClientSide) {
                            screen.origin.buffer().clipboard(tag.getString(Settings.namespace + "clipboard"), player);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
            return InteractionResult.FAIL;
        }
        if (te != null && player != null) {
            analyze(te, player, side.ordinal(), hitX, hitY, hitZ);
        }
        return InteractionResult.SUCCESS;
    }

    public static class EventHandler {
        @SubscribeEvent
        @SuppressWarnings("unused")
        public static void onInteract(PlayerInteractEvent.EntityInteract e) {
            Player player = e.getEntity();
            ItemStack held = player.getItemInHand(e.getHand());
            var info = Items.get(held);
            if (info != null && info == Items.get(Constants.ItemName.Analyzer)) {
                if (analyze(e.getTarget(), player, 0, 0, 0, 0)) {
                    player.swing(e.getHand());
                    e.setCanceled(true);
                }
            }
        }
    }
}
