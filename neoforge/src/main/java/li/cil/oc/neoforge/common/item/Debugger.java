package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.Network;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class Debugger extends DelegateItem implements Environment {
    private static final Logger LOGGER = LoggerFactory.getLogger(Debugger.class);
    private static Node node = null;

    public Debugger(Properties properties) {
        super(properties);
    }

    private static synchronized Node getOrCreateNode(Debugger instance) {
        if (node == null) {
            var builder = Network.newNode(instance, Visibility.Network);
            if (builder != null) {
                node = builder.create();
            }
        }
        return node;
    }

    private static void reconnect(Node[] nodes) {
        if (node == null) return;
        node.remove();
        Network.joinNewNetwork(node);
        for (var n : nodes) {
            if (n != null) node.connect(n);
        }
    }

    private static String nodeInfo(Node n) {
        if (n == null) return "null";
        return "{address = " + n.address() + ", reachability = " + n.reachability() +
                (n instanceof ComponentConnector cc ? componentInfo(cc) + connectorInfo(cc) :
                        n instanceof Component c ? componentInfo(c) :
                        n instanceof Connector c2 ? connectorInfo(c2) : "") + "}";
    }

    private static String componentInfo(Component c) {
        return ", type = component, name = " + c.name() + ", visibility = " + c.visibility();
    }

    private static String connectorInfo(Connector c) {
        return ", type = connector, buffer = " + c.localBuffer() + ", bufferSize = " + c.localBufferSize();
    }

    private static String messageInfo(Message m) {
        return "{name = " + m + ", source = " + nodeInfo(m.source()) + ", data = [" + Arrays.toString(m.data()) + "]}";
    }

    @Override
    public Node node() {
        return node;
    }

    @Override
    public void onConnect(Node node) {
        LOGGER.info("[NETWORK DEBUGGER] New node in network: {}", nodeInfo(node));
    }

    @Override
    public void onDisconnect(Node node) {
        LOGGER.info("[NETWORK DEBUGGER] Node removed from network: {}", nodeInfo(node));
    }

    @Override
    public void onMessage(Message message) {
        LOGGER.info("[NETWORK DEBUGGER] Received message: {}", messageInfo(message));
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide) return InteractionResult.PASS;
        if (context.getPlayer() instanceof FakePlayer) return InteractionResult.PASS;
        var n = getOrCreateNode(this);
        if (n == null) return InteractionResult.PASS;
        var pos = context.getClickedPos();
        var side = context.getClickedFace();
        BlockEntity te = world.getBlockEntity(pos);
        if (te instanceof SidedEnvironment sided) {
            reconnect(new Node[]{sided.sidedNode(side)});
            return InteractionResult.SUCCESS;
        } else if (te instanceof Environment env) {
            reconnect(new Node[]{env.node()});
            return InteractionResult.SUCCESS;
        } else {
            n.remove();
            return InteractionResult.SUCCESS;
        }
    }
}
