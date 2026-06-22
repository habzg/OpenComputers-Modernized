package li.cil.oc.core.impl.server.network;

import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.WirelessEndpoint;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.SideTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Network implements li.cil.oc.api.detail.NetworkAPI, Distributor {
    public static boolean cbMultipartAvailable = false;
    public static java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, Node> simpleComponentHandler = null;
    public static java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, Node> multipartNodeHandler = null;
    public static java.util.function.Function<net.minecraft.world.level.block.entity.BlockEntity, Integer> multipartColorHandler = null;
    public static java.util.function.BiPredicate<net.minecraft.world.level.block.entity.BlockEntity, Direction> multipartCanConnectHandler = null;
    public static java.util.function.BiFunction<net.minecraft.world.level.block.entity.BlockEntity, Direction, Node> capabilityNodeHandler = null;

    private static final Logger LOGGER = LoggerFactory.getLogger(Network.class);

    @Override
    public double globalBuffer() {
        return this.globalBuffer;
    }

    @Override
    public double globalBufferSize() {
        return this.globalBufferSize;
    }

    @Override
    public void globalBuffer_$eq(double value) {
        this.globalBuffer = value;
    }

    @Override
    public void globalBufferSize_$eq(double value) {
        this.globalBufferSize = value;
    }

    final Map<String, Vertex> data;
    final Wrapper wrapper;
    private final List<Connector> connectors = new ArrayList<>();
    double globalBuffer = 0.0;
    double globalBufferSize = 0.0;

    public static final Network INSTANCE = new Network();

    private Network() {
        this.data = new HashMap<>();
        this.wrapper = new Wrapper();
    }

    private Network(li.cil.oc.core.impl.server.network.Node node) {
        this.data = new HashMap<>();
        this.wrapper = new Wrapper();
        addNew(node);
        node.onConnect(node);
        syncVertices();
    }

    private Network(Map<String, Vertex> subgraph) {
        this.data = subgraph;
        this.wrapper = new Wrapper();
        syncVertices();
    }

    private static Iterable<Node> toIterable(Node first, Collection<Node> rest) {
        List<Node> result = new ArrayList<>();
        result.add(first);
        result.addAll(rest);
        return result;
    }

    public void joinNewNetwork(Node node) {
        if (node instanceof li.cil.oc.core.impl.server.network.Node && node.network() == null) {
            new Network((li.cil.oc.core.impl.server.network.Node) node);
        }
    }

    public static Node getNetworkNode(BlockEntity tileEntity, Direction side) {
        if (tileEntity instanceof SidedEnvironment) {
            return ((SidedEnvironment) tileEntity).sidedNode(side);
        } else if (tileEntity instanceof Environment) {
            if (tileEntity instanceof li.cil.oc.api.network.SidedComponent) {
                if (((li.cil.oc.api.network.SidedComponent) tileEntity).canConnectNode(side)) {
                    return ((Environment) tileEntity).node();
                } else {
                    return null;
                }
            }
            return ((Environment) tileEntity).node();
        } else if (tileEntity instanceof li.cil.oc.api.network.SimpleComponent) {
            if (simpleComponentHandler != null) return simpleComponentHandler.apply(tileEntity);
            return null;
        } else if (cbMultipartAvailable) {
            return getMultiPartNode(tileEntity);
        } else if (capabilityNodeHandler != null) {
            return capabilityNodeHandler.apply(tileEntity, side);
        }
        return null;
    }

    private static Node getMultiPartNode(BlockEntity tileEntity) {
        if (multipartNodeHandler != null) return multipartNodeHandler.apply(tileEntity);
        return null;
    }

    private static int cableColor(BlockEntity tileEntity) {
        if (tileEntity instanceof li.cil.oc.core.impl.common.tileentity.Cable) {
            return ((li.cil.oc.core.impl.common.tileentity.Cable) tileEntity).color();
        } else if (cbMultipartAvailable) {
            return cableColorCBMultipart(tileEntity);
        }
        return Color.LightGray;
    }

    private static int cableColorCBMultipart(BlockEntity tileEntity) {
        if (multipartColorHandler != null) return multipartColorHandler.apply(tileEntity);
        return Color.LightGray;
    }

    private static boolean canConnectBasedOnColor(BlockEntity te1, BlockEntity te2) {
        int c1 = cableColor(te1);
        int c2 = cableColor(te2);
        return c1 == c2 || c1 == Color.LightGray || c2 == Color.LightGray;
    }

    private static boolean canConnectFromSideCBMultipart(BlockEntity tileEntity, Direction side) {
        if (multipartCanConnectHandler != null) return multipartCanConnectHandler.test(tileEntity, side);
        return true;
    }

    public NodeBuilder newNode(Environment host, Visibility reachability) {
        return new NodeBuilder(host, reachability);
    }

    public static boolean isServer() {
        return SideTracker.isServer();
    }

    private static List<Map<String, Vertex>> searchGraphs(List<Vertex> seeds) {
        Set<Vertex> seen = new HashSet<>();
        List<Map<String, Vertex>> result = new ArrayList<>();
        for (Vertex seed : seeds) {
            if (seen.contains(seed)) continue;
            Map<String, Vertex> addressed = new HashMap<>();
            Queue<Vertex> queue = new LinkedList<>();
            queue.add(seed);
            seen.add(seed);
            while (!queue.isEmpty()) {
                Vertex node = queue.poll();
                addressed.put(node.data.address(), node);
                for (Edge e : node.edges) {
                    Vertex other = e.other(node);
                    if (!seen.contains(other)) {
                        seen.add(other);
                        queue.add(other);
                    }
                }
            }
            result.add(addressed);
        }
        return result;
    }

    private void syncVertices() {
        for (Vertex vertex : data.values()) {
            if (vertex.data instanceof Connector) {
                addConnector((Connector) vertex.data);
            }
            vertex.data.network_$eq(wrapper);
        }
    }

    public void remap(li.cil.oc.core.impl.server.network.Node remappedNode, String newAddress) {
        Vertex node = data.get(remappedNode.address());
        if (node != null) {
            List<li.cil.oc.core.impl.server.network.Node> neighbors = node.edges.stream()
                    .map(e -> e.other(node).data)
                    .toList();
            node.data.remove();
            node.data.address_$eq(newAddress);
            while (data.containsKey(node.data.address())) {
                node.data.address_$eq(UUID.randomUUID().toString());
            }
            if (neighbors.isEmpty()) {
                addNew(node.data);
            } else {
                for (li.cil.oc.core.impl.server.network.Node n : neighbors) {
                    n.connect(node.data);
                }
            }
        } else {
            throw new AssertionError("Node believes it belongs to a network it doesn't.");
        }
    }

    public boolean connect(li.cil.oc.core.impl.server.network.Node nodeA, li.cil.oc.core.impl.server.network.Node nodeB) {
        if (nodeA == null) throw new NullPointerException("nodeA");
        if (nodeB == null) throw new NullPointerException("nodeB");
        if (nodeA == nodeB) throw new IllegalArgumentException("Cannot connect a node to itself.");

        boolean containsA = contains(nodeA);
        boolean containsB = contains(nodeB);

        if (!containsA && !containsB)
            throw new IllegalArgumentException("At least one of the nodes must already be in this network.");

        if (containsA && containsB) {
            Vertex oldNodeA = vertex(nodeA);
            Vertex oldNodeB = vertex(nodeB);
            boolean alreadyConnected = oldNodeA.edges.stream().anyMatch(e -> e.isBetween(oldNodeA, oldNodeB));
            if (!alreadyConnected) {
                new Edge(oldNodeA, oldNodeB);
                if (oldNodeA.data.reachability() == Visibility.Neighbors)
                    oldNodeB.data.onConnect(oldNodeA.data);
                if (oldNodeB.data.reachability() == Visibility.Neighbors)
                    oldNodeA.data.onConnect(oldNodeB.data);
                return true;
            }
            return false;
        } else if (containsA) {
            add(vertex(nodeA), nodeB);
        } else {
            add(vertex(nodeB), nodeA);
        }
        return true;
    }

    public boolean disconnect(li.cil.oc.core.impl.server.network.Node nodeA, li.cil.oc.core.impl.server.network.Node nodeB) {
        if (nodeA == nodeB) throw new IllegalArgumentException("Cannot disconnect a node from itself.");

        boolean containsA = contains(nodeA);
        boolean containsB = contains(nodeB);

        if (!containsA || !containsB)
            throw new IllegalArgumentException("Both of the nodes must be in this network.");

        Vertex oldNodeA = vertex(nodeA);
        Vertex oldNodeB = vertex(nodeB);

        var edge = oldNodeA.edges.stream().filter(e -> e.isBetween(oldNodeA, oldNodeB)).findFirst().orElse(null);
        if (edge != null) {
            handleSplit(edge.remove());
            if (edge.left.data.reachability() == Visibility.Neighbors)
                edge.right.data.onDisconnect(edge.left.data);
            if (edge.right.data.reachability() == Visibility.Neighbors)
                edge.left.data.onDisconnect(edge.right.data);
            return true;
        }
        return false;
    }

    public boolean remove(li.cil.oc.core.impl.server.network.Node node) {
        Vertex entry = data.remove(node.address());
        if (entry != null) {
            if (node instanceof Connector) {
                removeConnector((Connector) node);
            }
            node.network_$eq(null);
            List<Map<String, Vertex>> subGraphs = entry.remove();
            List<Node> targets = new ArrayList<>();
            targets.add(node);
            switch (entry.data.reachability()) {
                case Neighbors:
                    for (Edge e : entry.edges) {
                        targets.add(e.other(entry).data);
                    }
                    break;
                case Network:
                    for (Map<String, Vertex> sg : subGraphs) {
                        targets.addAll(sg.values().stream().map(v -> (Node) v.data).toList());
                    }
                    break;
            }
            handleSplit(subGraphs);
            for (Node t : targets) {
                ((li.cil.oc.core.impl.server.network.Node) t).onDisconnect(node);
            }
            return true;
        }
        return false;
    }

    public li.cil.oc.core.impl.server.network.Node node(String address) {
        Vertex v = data.get(address);
        return v != null ? v.data : null;
    }

    public Collection<Node> nodes() {
        return data.values().stream().map(v -> (Node) v.data).collect(Collectors.toList());
    }

    public Collection<Node> reachableNodes(Node reference) {
        Set<Node> referenceNeighbors = new HashSet<>(neighbors(reference));
        return nodes().stream()
                .filter(node -> node != reference && (node.reachability() == Visibility.Network ||
                        (node.reachability() == Visibility.Neighbors && referenceNeighbors.contains(node))))
                .collect(Collectors.toList());
    }

    public Collection<Node> reachingNodes(Node reference) {
        if (reference.reachability() == Visibility.Network) {
            return nodes().stream().filter(node -> node != reference).collect(Collectors.toList());
        } else if (reference.reachability() == Visibility.Neighbors) {
            Set<Node> referenceNeighbors = new HashSet<>(neighbors(reference));
            return nodes().stream()
                    .filter(node -> node != reference && referenceNeighbors.contains(node))
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    public Collection<Node> neighbors(Node node) {
        Vertex v = data.get(node.address());
        if (v != null && v.data == node) {
            return v.edges.stream().map(e -> (Node) e.other(v).data).collect(Collectors.toList());
        } else {
            throw new IllegalArgumentException("Node must be in this network.");
        }
    }

    public void sendToAddress(Node source, String target, String name, Object... args) {
        if (source.network() != wrapper)
            throw new IllegalArgumentException("Source node must be in this network.");
        Vertex v = data.get(target);
        if (v != null && v.data.canBeReachedFrom(source)) {
            send(source, Collections.singletonList(v.data), name, args);
        }
    }

    public void sendToNeighbors(Node source, String name, Object... args) {
        if (source.network() != wrapper)
            throw new IllegalArgumentException("Source node must be in this network.");
        send(source, neighbors(source).stream()
                .filter(n -> n.reachability() != Visibility.None)
                .collect(Collectors.toList()), name, args);
    }

    public void sendToReachable(Node source, String name, Object... args) {
        if (source.network() != wrapper)
            throw new IllegalArgumentException("Source node must be in this network.");
        send(source, reachableNodes(source), name, args);
    }

    public void sendToVisible(Node source, String name, Object... args) {
        if (source.network() != wrapper)
            throw new IllegalArgumentException("Source node must be in this network.");
        send(source, reachableNodes(source).stream()
                .filter(n -> n instanceof li.cil.oc.api.network.Component && ((li.cil.oc.api.network.Component) n).canBeSeenFrom(source))
                .collect(Collectors.toList()), name, args);
    }

    private boolean contains(li.cil.oc.core.impl.server.network.Node node) {
        return node.network() == wrapper && data.containsKey(node.address());
    }

    private Vertex vertex(Node node) {
        return data.get(node.address());
    }

    private Vertex addNew(li.cil.oc.core.impl.server.network.Node node) {
        Vertex newNode = new Vertex(node);
        if (node.address() == null || data.containsKey(node.address())) {
            node.address_$eq(UUID.randomUUID().toString());
        }
        data.put(node.address(), newNode);
        if (node instanceof Connector) {
            addConnector((Connector) node);
        }
        node.network_$eq(wrapper);
        return newNode;
    }

    private boolean add(Vertex oldNode, li.cil.oc.core.impl.server.network.Node addedNode) {
        List<AbstractMap.SimpleEntry<Node, Iterable<Node>>> connects = new ArrayList<>();

        if (addedNode.network() == null) {
            Vertex newNode = addNew(addedNode);
            new Edge(oldNode, newNode);
            switch (addedNode.reachability()) {
                case None:
                    connects.add(new AbstractMap.SimpleEntry<>(addedNode, Collections.singletonList(addedNode)));
                    break;
                case Neighbors:
                    connects.add(new AbstractMap.SimpleEntry<>(addedNode, toIterable(addedNode, neighbors(addedNode))));
                    for (Node n : reachingNodes(addedNode)) {
                        connects.add(new AbstractMap.SimpleEntry<>(n, Collections.singletonList(addedNode)));
                    }
                    break;
                case Network:
                    connects.add(new AbstractMap.SimpleEntry<>(addedNode, toIterable(addedNode, nodes().stream().filter(n -> n != addedNode).collect(Collectors.toList()))));
                    for (Node n : reachingNodes(addedNode)) {
                        connects.add(new AbstractMap.SimpleEntry<>(n, Collections.singletonList(addedNode)));
                    }
                    break;
            }

            addedNode.onConnect(addedNode);
            List<Node> visibleNodes = nodes().stream()
                    .filter(n -> n.reachability() == Visibility.Network)
                    .toList();
            for (Node n : visibleNodes) {
                connects.add(new AbstractMap.SimpleEntry<>(n, nodes()));
            }
        } else if (addedNode.network() == oldNode.data.network()) {
            return false;
        } else {
            Network otherNetwork = ((Wrapper) addedNode.network()).network;

            Vertex[] duplicates = otherNetwork.data.entrySet().stream()
                    .filter(e -> data.containsKey(e.getKey()))
                    .map(Map.Entry::getValue)
                    .toArray(Vertex[]::new);
            Network otherNetworkAfterReaddress;
            if (duplicates.length == 0) {
                otherNetworkAfterReaddress = otherNetwork;
            } else {
                for (Vertex vertex : duplicates) {
                    li.cil.oc.core.impl.server.network.Node node = vertex.data;
                    li.cil.oc.core.impl.server.network.Node[] neighbors = vertex.edges.stream()
                            .map(e -> e.other(vertex).data)
                            .toArray(li.cil.oc.core.impl.server.network.Node[]::new);

                    String newAddress;
                    do {
                        newAddress = UUID.randomUUID().toString();
                    } while (data.containsKey(newAddress) || otherNetwork.data.containsKey(newAddress));

                    node.remove();
                    node.address_$eq(newAddress);
                    joinNewNetwork(node);

                    if (node.address().equals(newAddress)) {
                        for (li.cil.oc.core.impl.server.network.Node n : neighbors) {
                            if (n.network() != null) {
                                n.connect(node);
                            }
                        }
                    } else {
                        LOGGER.error("I can't see this happening any other way than someone directly setting node addresses, which they shouldn't. So yeah. Shit'll be borked. Deal with it.");
                        node.remove();
                    }
                }
                otherNetworkAfterReaddress = ((Wrapper) duplicates[0].data.network()).network;
            }

            if (addedNode.network() != null && ((Wrapper) addedNode.network()).network == otherNetworkAfterReaddress) {
                if (addedNode.reachability() == Visibility.Neighbors)
                    connects.add(new AbstractMap.SimpleEntry<>(addedNode, Collections.singletonList(oldNode.data)));
                if (oldNode.data.reachability() == Visibility.Neighbors)
                    connects.add(new AbstractMap.SimpleEntry<>(oldNode.data, Collections.singletonList(addedNode)));

                List<Node> oldNodes = new ArrayList<>(nodes());
                List<Node> newNodes = new ArrayList<>(otherNetworkAfterReaddress.nodes());
                List<Node> oldVisibleNodes = oldNodes.stream()
                        .filter(n -> n.reachability() == Visibility.Network)
                        .toList();
                List<Node> newVisibleNodes = newNodes.stream()
                        .filter(n -> n.reachability() == Visibility.Network)
                        .toList();

                for (Node n : newVisibleNodes) {
                    connects.add(new AbstractMap.SimpleEntry<>(n, oldNodes));
                }
                for (Node n : oldVisibleNodes) {
                    connects.add(new AbstractMap.SimpleEntry<>(n, newNodes));
                }

                data.putAll(otherNetworkAfterReaddress.data);
                connectors.addAll(otherNetworkAfterReaddress.connectors);
                globalBuffer += otherNetworkAfterReaddress.globalBuffer;
                globalBufferSize += otherNetworkAfterReaddress.globalBufferSize;
                for (Vertex v : otherNetworkAfterReaddress.data.values()) {
                    if (v.data instanceof Connector) {
                        ((Connector) v.data).distributor_$eq(wrapper);
                    }
                    v.data.network_$eq(wrapper);
                }
                otherNetworkAfterReaddress.data.clear();
                otherNetworkAfterReaddress.connectors.clear();

                new Edge(oldNode, vertex(addedNode));
            } else {
                return add(oldNode, addedNode);
            }
        }

        for (AbstractMap.SimpleEntry<Node, Iterable<Node>> pair : connects) {
            for (Node n : pair.getValue()) {
                ((li.cil.oc.core.impl.server.network.Node) n).onConnect(pair.getKey());
            }
        }

        return true;
    }

    private void handleSplit(List<Map<String, Vertex>> subGraphs) {
        if (subGraphs.size() > 1) {
            List<List<Node>> nodeLists = subGraphs.stream()
                    .map(sg -> sg.values().stream().map(v -> (Node) v.data).collect(Collectors.toList()))
                    .toList();
            List<List<Node>> visibleNodeLists = nodeLists.stream()
                    .map(list -> list.stream().filter(n -> n.reachability() == Visibility.Network).collect(Collectors.toList()))
                    .toList();

            data.clear();
            connectors.clear();
            globalBuffer = 0;
            globalBufferSize = 0;
            data.putAll(subGraphs.getFirst());
            for (Vertex v : data.values()) {
                if (v.data instanceof Connector) {
                    addConnector((Connector) v.data);
                }
            }
            for (int i = 1; i < subGraphs.size(); i++) {
                new Network(subGraphs.get(i));
            }

            for (int i = 0; i < subGraphs.size(); i++) {
                List<Node> nodesA = nodeLists.get(i);
                List<Node> visibleNodesA = visibleNodeLists.get(i);
                for (int j = i + 1; j < subGraphs.size(); j++) {
                    List<Node> nodesB = nodeLists.get(j);
                    List<Node> visibleNodesB = visibleNodeLists.get(j);
                    for (Node node : visibleNodesA) {
                        for (Node n : nodesB) {
                            ((li.cil.oc.core.impl.server.network.Node) n).onDisconnect(node);
                        }
                    }
                    for (Node node : visibleNodesB) {
                        for (Node n : nodesA) {
                            ((li.cil.oc.core.impl.server.network.Node) n).onDisconnect(node);
                        }
                    }
                }
            }
        }
    }

    private void send(Node source, Iterable<Node> targets, String name, Object... args) {
        Message message = new Message(source, name, args);
        for (Node target : targets) {
            target.host().onMessage(message);
            if (message.isCanceled) break;
        }
    }

    @Override
    public void addConnector(Connector connector) {
        if (connector.localBufferSize() > 0) {
            assert !connectors.contains(connector);
            connectors.add(connector);
            globalBuffer += connector.localBuffer();
            globalBufferSize += connector.localBufferSize();
        }
        connector.distributor_$eq(wrapper);
    }

    @Override
    public void removeConnector(Connector connector) {
        if (connector.localBufferSize() > 0) {
            assert connectors.contains(connector);
            connectors.remove(connector);
            globalBuffer -= connector.localBuffer();
            globalBufferSize -= connector.localBufferSize();
        }
    }

    @Override
    public double changeBuffer(double delta) {
        if (delta == 0) return 0;
        if (Settings.get().ignorePower) {
            return delta < 0 ? 0 : delta;
        }
        synchronized (this) {
            double oldBuffer = globalBuffer;
            globalBuffer = Math.clamp(globalBuffer + delta, 0, globalBufferSize);
            if (globalBuffer == oldBuffer) {
                return delta;
            }
            if (delta < 0) {
                double remaining = -delta;
                for (Connector connector : connectors) {
                    if (remaining <= 0) break;
                    if (connector.localBuffer() > 0) {
                        if (connector.localBuffer() < remaining) {
                            remaining -= connector.localBuffer();
                            connector.localBuffer_$eq(0);
                        } else {
                            connector.localBuffer_$eq(connector.localBuffer() - remaining);
                            remaining = 0;
                        }
                    }
                }
                return -remaining;
            } else {
                double remaining = delta;
                for (Connector connector : connectors) {
                    if (remaining <= 0) break;
                    if (connector.localBuffer() < connector.localBufferSize()) {
                        double space = connector.localBufferSize() - connector.localBuffer();
                        if (space < remaining) {
                            remaining -= space;
                            connector.localBuffer_$eq(connector.localBufferSize());
                        } else {
                            connector.localBuffer_$eq(connector.localBuffer() + remaining);
                            remaining = 0;
                        }
                    }
                }
                return remaining;
            }
        }
    }

    @Override
    public void joinOrCreateNetwork(Level level, BlockPos pos) {
        if (level != null && !level.isClientSide) {
            var tileEntity = level.getBlockEntity(pos);
            if (tileEntity != null) {
                joinOrCreateNetwork(tileEntity);
            }
        }
    }

    @Override
    public void joinOrCreateNetwork(BlockEntity tileEntity) {
        var level = tileEntity.getLevel();
        if (!tileEntity.isRemoved() && level != null && !level.isClientSide) {
            for (Direction side : Direction.values()) {
                BlockPos pos = tileEntity.getBlockPos().relative(side);
                if (level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
                    Node localNode = getNetworkNode(tileEntity, side);
                    BlockEntity neighborTileEntity = level.getBlockEntity(pos);
                    Node neighborNode = getNetworkNode(neighborTileEntity, side.getOpposite());
                    if (localNode instanceof li.cil.oc.core.impl.server.network.Node mutableLocal) {
                        if (neighborNode instanceof li.cil.oc.core.impl.server.network.Node mutableNeighbor && neighborNode != mutableLocal && neighborNode.network() != null) {
                            boolean canConnectColor = canConnectBasedOnColor(tileEntity, neighborTileEntity);
                            boolean canConnectCBMultipart = !cbMultipartAvailable ||
                                    (canConnectFromSideCBMultipart(tileEntity, side) && canConnectFromSideCBMultipart(neighborTileEntity, side.getOpposite()));
                            if (canConnectColor && canConnectCBMultipart) {
                                mutableNeighbor.connect(mutableLocal);
                            } else {
                                mutableLocal.disconnect(mutableNeighbor);
                            }
                        }
                        if (mutableLocal.network() == null) {
                            joinNewNetwork(mutableLocal);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void joinWirelessNetwork(WirelessEndpoint endpoint) {
        var h = li.cil.oc.core.util.WirelessNetworkHelper.get();
        if (h != null) h.add(endpoint);
    }

    @Override
    public void updateWirelessNetwork(WirelessEndpoint endpoint) {
        var h = li.cil.oc.core.util.WirelessNetworkHelper.get();
        if (h != null) h.update(endpoint);
    }

    @Override
    public void leaveWirelessNetwork(WirelessEndpoint endpoint) {
        var h = li.cil.oc.core.util.WirelessNetworkHelper.get();
        if (h != null) h.remove(endpoint);
    }

    @Override
    public void leaveWirelessNetwork(WirelessEndpoint endpoint, String dimension) {
        var h = li.cil.oc.core.util.WirelessNetworkHelper.get();
        if (h != null) h.remove(endpoint, dimension);
    }

    @Override
    public void sendWirelessPacket(WirelessEndpoint source, double strength, li.cil.oc.api.network.Packet packet) {
        var h = li.cil.oc.core.util.WirelessNetworkHelper.get();
        if (h != null) {
            for (WirelessEndpoint endpoint : h.computeReachableFrom(source, strength)) {
                endpoint.receivePacket(packet, source);
            }
        }
    }

    @Override
    public Packet newPacket(String source, String destination, int port, Object[] data) {
        Packet packet = new Packet(source, destination, port, data);
        if (packet.size > Settings.get().maxNetworkPacketSize) {
            throw new IllegalArgumentException("packet too big (max " + Settings.get().maxNetworkPacketSize + ")");
        }
        return packet;
    }

    @Override
    public Packet newPacket(CompoundTag nbt) {
        String src = nbt.getString("source");
        String dest = nbt.contains("dest") ? nbt.getString("dest") : null;
        int port = nbt.getInt("port");
        int ttl = nbt.getInt("ttl");
        int dataLength = nbt.getInt("dataLength");
        Object[] data = new Object[dataLength];
        for (int i = 0; i < dataLength; i++) {
            String key = "data" + i;
            if (nbt.contains(key)) {
                Tag tag = nbt.get(key);
                if (tag instanceof net.minecraft.nbt.ByteTag) {
                    data[i] = ((net.minecraft.nbt.ByteTag) tag).getAsByte() == 1;
                } else if (tag instanceof net.minecraft.nbt.ShortTag) {
                    data[i] = ((net.minecraft.nbt.ShortTag) tag).getAsShort();
                } else if (tag instanceof net.minecraft.nbt.IntTag) {
                    data[i] = ((net.minecraft.nbt.IntTag) tag).getAsInt();
                } else if (tag instanceof net.minecraft.nbt.LongTag) {
                    data[i] = ((net.minecraft.nbt.LongTag) tag).getAsLong();
                } else if (tag instanceof net.minecraft.nbt.FloatTag) {
                    data[i] = ((net.minecraft.nbt.FloatTag) tag).getAsFloat();
                } else if (tag instanceof net.minecraft.nbt.DoubleTag) {
                    data[i] = ((net.minecraft.nbt.DoubleTag) tag).getAsDouble();
                } else if (tag instanceof net.minecraft.nbt.StringTag) {
                    data[i] = tag.getAsString();
                } else if (tag instanceof net.minecraft.nbt.ByteArrayTag) {
                    data[i] = ((net.minecraft.nbt.ByteArrayTag) tag).getAsByteArray();
                }
            }
        }
        return new Packet(src, dest, port, data, ttl);
    }

    public static class NodeBuilder implements li.cil.oc.api.detail.Builder.NodeBuilder {
        private final Environment host;
        private final Visibility reachability;

        public NodeBuilder(Environment host, Visibility reachability) {
            this.host = host;
            this.reachability = reachability;
        }

        @Override
        public @NotNull Network.ComponentBuilder withComponent(@NotNull String name, @NotNull Visibility visibility) {
            return new Network.ComponentBuilder(host, reachability, name, visibility);
        }

        @Override
        public @NotNull Network.ComponentBuilder withComponent(@NotNull String name) {
            return withComponent(name, reachability);
        }

        @Override
        public @NotNull Network.ConnectorBuilder withConnector(double bufferSize) {
            return new Network.ConnectorBuilder(host, reachability, bufferSize);
        }

        @Override
        public @NotNull Network.ConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public @NotNull Node create() {
            if (isServer()) {
                return new li.cil.oc.core.impl.server.network.Node() {
                    private String _address;
                    private li.cil.oc.api.network.Network _network;

                    @Override
                    public Environment host() {
                        return host;
                    }

                    @Override
                    public Visibility reachability() {
                        return reachability;
                    }

                    @Override
                    public String address() {
                        return _address;
                    }

                    @Override
                    public void address_$eq(String value) {
                        _address = value;
                    }

                    @Override
                    public li.cil.oc.api.network.Network network() {
                        return _network;
                    }

                    @Override
                    public void network_$eq(li.cil.oc.api.network.Network value) {
                        _network = value;
                    }
                };
            }
            return null;
        }
    }

    public static class ComponentBuilder implements li.cil.oc.api.detail.Builder.ComponentBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;

        public ComponentBuilder(Environment host, Visibility reachability, String name, Visibility visibility) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
        }

        @Override
        public @NotNull Network.ComponentConnectorBuilder withConnector(double bufferSize) {
            return new Network.ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize);
        }

        @Override
        public @NotNull Network.ComponentConnectorBuilder withConnector() {
            return withConnector(0);
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public @NotNull li.cil.oc.api.network.Component create() {
            if (isServer()) {
                return new li.cil.oc.core.impl.server.network.Component() {
                    private String _address;
                    private li.cil.oc.api.network.Network _network;
                    private Visibility _visibilityValue = visibility;

                    @Override
                    public Environment host() {
                        return host;
                    }

                    @Override
                    public Visibility reachability() {
                        return reachability;
                    }

                    @Override
                    public String address() {
                        return _address;
                    }

                    @Override
                    public void address_$eq(String value) {
                        _address = value;
                    }

                    @Override
                    public li.cil.oc.api.network.Network network() {
                        return _network;
                    }

                    @Override
                    public void network_$eq(li.cil.oc.api.network.Network value) {
                        _network = value;
                    }

                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public Visibility visibility() {
                        return _visibilityValue;
                    }

                    @Override
                    public void _visibility(Visibility v) {
                        _visibilityValue = v;
                    }
                };
            }
            return null;
        }
    }

    public static class ConnectorBuilder implements li.cil.oc.api.detail.Builder.ConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final double bufferSize;

        public ConnectorBuilder(Environment host, Visibility reachability, double bufferSize) {
            this.host = host;
            this.reachability = reachability;
            this.bufferSize = bufferSize;
        }

        @Override
        public @NotNull Network.ComponentConnectorBuilder withComponent(@NotNull String name, @NotNull Visibility visibility) {
            return new Network.ComponentConnectorBuilder(host, reachability, name, visibility, bufferSize);
        }

        @Override
        public @NotNull Network.ComponentConnectorBuilder withComponent(@NotNull String name) {
            return withComponent(name, reachability);
        }

        @Override
        @SuppressWarnings("DataFlowIssue")
        public @NotNull Connector create() {
            if (isServer()) {
                return new li.cil.oc.core.impl.server.network.Connector() {
                    private String _address;
                    private li.cil.oc.api.network.Network _network;
                    private double _localBuffer = 0;
                    private double _localBufferSize = bufferSize;
                    private Distributor _distributor;

                    @Override
                    public Environment host() {
                        return host;
                    }

                    @Override
                    public Visibility reachability() {
                        return reachability;
                    }

                    @Override
                    public String address() {
                        return _address;
                    }

                    @Override
                    public void address_$eq(String value) {
                        _address = value;
                    }

                    @Override
                    public li.cil.oc.api.network.Network network() {
                        return _network;
                    }

                    @Override
                    public void network_$eq(li.cil.oc.api.network.Network value) {
                        _network = value;
                    }

                    @Override
                    public double localBufferSize() {
                        return _localBufferSize;
                    }

                    @Override
                    public void localBufferSize_$eq(double value) {
                        _localBufferSize = value;
                    }

                    @Override
                    public double localBuffer() {
                        return _localBuffer;
                    }

                    @Override
                    public void localBuffer_$eq(double value) {
                        _localBuffer = value;
                    }

                    @Override
                    public Distributor distributor() {
                        return _distributor;
                    }

                    @Override
                    public void distributor_$eq(Distributor value) {
                        _distributor = value;
                    }
                };
            }
            return null;
        }
    }

    public static class ComponentConnectorBuilder implements li.cil.oc.api.detail.Builder.ComponentConnectorBuilder {
        private final Environment host;
        private final Visibility reachability;
        private final String name;
        private final Visibility visibility;
        private final double bufferSize;

        public ComponentConnectorBuilder(Environment host, Visibility reachability, String name, Visibility visibility, double bufferSize) {
            this.host = host;
            this.reachability = reachability;
            this.name = name;
            this.visibility = visibility;
            this.bufferSize = bufferSize;
        }

        @Override
        public ComponentConnector create() {
            if (isServer()) {
                return new li.cil.oc.core.impl.server.network.ComponentConnector() {
                    private String _address;
                    private li.cil.oc.api.network.Network _network;
                    private Visibility _visibilityValue = visibility;
                    private double _localBuffer = 0;
                    private double _localBufferSize = bufferSize;
                    private Distributor _distributor;

                    @Override
                    public Environment host() {
                        return host;
                    }

                    @Override
                    public Visibility reachability() {
                        return reachability;
                    }

                    @Override
                    public String address() {
                        return _address;
                    }

                    @Override
                    public void address_$eq(String value) {
                        _address = value;
                    }

                    @Override
                    public li.cil.oc.api.network.Network network() {
                        return _network;
                    }

                    @Override
                    public void network_$eq(li.cil.oc.api.network.Network value) {
                        _network = value;
                    }

                    @Override
                    public String name() {
                        return name;
                    }

                    @Override
                    public Visibility visibility() {
                        return _visibilityValue;
                    }

                    @Override
                    public void _visibility(Visibility v) {
                        _visibilityValue = v;
                    }

                    @Override
                    public double localBufferSize() {
                        return _localBufferSize;
                    }

                    @Override
                    public void localBufferSize_$eq(double value) {
                        _localBufferSize = value;
                    }

                    @Override
                    public double localBuffer() {
                        return _localBuffer;
                    }

                    @Override
                    public void localBuffer_$eq(double value) {
                        _localBuffer = value;
                    }

                    @Override
                    public Distributor distributor() {
                        return _distributor;
                    }

                    @Override
                    public void distributor_$eq(Distributor value) {
                        _distributor = value;
                    }
                };
            }
            return null;
        }
    }

    static class Vertex {
        final li.cil.oc.core.impl.server.network.Node data;
        final List<Edge> edges = new ArrayList<>();

        Vertex(li.cil.oc.core.impl.server.network.Node data) {
            this.data = data;
        }

        List<Map<String, Vertex>> remove() {
            for (Edge edge : edges) {
                edge.other(this).edges.remove(edge);
            }
            return searchGraphs(edges.stream().map(e -> e.other(this)).collect(Collectors.toList()));
        }

        @Override
        public String toString() {
            return data + " [" + edges.size() + "]";
        }
    }

    record Edge(Vertex left, Vertex right) {
        Edge(Vertex left, Vertex right) {
            this.left = left;
            this.right = right;
            left.edges.add(this);
            right.edges.add(this);
        }

        Vertex other(Vertex side) {
            return side == left ? right : left;
        }

        boolean isBetween(Vertex a, Vertex b) {
            return (a == left && b == right) || (b == left && a == right);
        }

        List<Map<String, Vertex>> remove() {
            left.edges.remove(this);
            right.edges.remove(this);
            return searchGraphs(Arrays.asList(left, right));
        }
    }

    private static class Message implements li.cil.oc.api.network.Message {
        final Node source;
        final String name;
        final Object[] data;
        boolean isCanceled = false;

        Message(Node source, String name, Object[] data) {
            this.source = source;
            this.name = name;
            this.data = data;
        }

        @Override
        public Node source() {
            return source;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public Object[] data() {
            return data;
        }

        @Override
        public void cancel() {
            isCanceled = true;
        }
    }

    public static class Packet implements li.cil.oc.api.network.Packet {
        public final int size;

        @Override
        public int size() {
            return this.size;
        }

        @Override
        public Object[] data() {
            return data;
        }

        @Override
        public int port() {
            return port;
        }

        @Override
        public String destination() {
            return destination;
        }

        @Override
        public String source() {
            return source;
        }

        public final String source;
        public final String destination;
        public final int port;
        public final Object[] data;
        public final int ttl;

        public Packet(String source, String destination, int port, Object[] data) {
            this(source, destination, port, data, Settings.get().initialNetworkPacketTTL);
        }

        public Packet(String source, String destination, int port, Object[] data, int ttl) {
            this.source = source;
            this.destination = destination;
            this.port = port;
            this.data = data != null ? data : new Object[0];
            this.ttl = ttl;
            if (this.data.length > Settings.get().maxNetworkPacketParts) {
                throw new IllegalArgumentException("packet has too many parts");
            }
            int s = this.data.length * 2;
            for (Object arg : this.data) {
                switch (arg) {
                    case null -> s += 1;
                    case Boolean ignored -> s += 1;
                    case Byte ignored -> s += 2;
                    case Short ignored -> s += 2;
                    case Integer ignored -> s += 4;
                    case Long ignored -> s += 8;
                    case Float ignored -> s += 4;
                    case Double ignored -> s += 8;
                    case String string -> s += Math.max(string.length(), 1);
                    case byte[] bytes -> s += Math.max(bytes.length, 1);
                    default -> throw new IllegalArgumentException("unsupported data type");
                }
            }
            this.size = s;
        }

        @Override
        public int ttl() {
            return ttl;
        }

        @Override
        public Packet hop() {
            return new Packet(source, destination, port, data, ttl - 1);
        }

        @Override
        public void save(CompoundTag nbt) {
            nbt.putString("source", source);
            if (destination != null && !destination.isEmpty()) {
                nbt.putString("dest", destination);
            }
            nbt.putInt("port", port);
            nbt.putInt("ttl", ttl);
            nbt.putInt("dataLength", data.length);
            for (int i = 0; i < data.length; i++) {
                String key = "data" + i;
                Object value = data[i];
                switch (value) {
                    case null -> {}
                    case Boolean b -> nbt.putBoolean(key, b);
                    case Byte b -> nbt.putShort(key, b);
                    case Short aShort -> nbt.putShort(key, aShort);
                    case Integer integer -> nbt.putInt(key, integer);
                    case Long l -> nbt.putLong(key, l);
                    case Float v -> nbt.putFloat(key, v);
                    case Double v -> nbt.putDouble(key, v);
                    case String s -> nbt.putString(key, s);
                    case byte[] bytes -> nbt.putByteArray(key, bytes);
                    default ->
                            LOGGER.warn("Unexpected type while saving network packet: {}", value.getClass().getName());
                }
            }
        }

        @Override
        public String toString() {
            return "{source = " + source + ", destination = " + destination + ", port = " + port + ", data = [" + Arrays.toString(data) + "]}";
        }
    }

    public class Wrapper implements li.cil.oc.api.network.Network, Distributor {
        public final Network network = Network.this;

        @Override
        public boolean connect(Node nodeA, Node nodeB) {
            return network.connect((li.cil.oc.core.impl.server.network.Node) nodeA, (li.cil.oc.core.impl.server.network.Node) nodeB);
        }

        @Override
        public boolean disconnect(Node nodeA, Node nodeB) {
            return network.disconnect((li.cil.oc.core.impl.server.network.Node) nodeA, (li.cil.oc.core.impl.server.network.Node) nodeB);
        }

        @Override
        public boolean remove(Node node) {
            return network.remove((li.cil.oc.core.impl.server.network.Node) node);
        }

        @Override
        public Node node(String address) {
            return network.node(address);
        }

        @Override
        public Collection<Node> nodes() {
            return network.nodes();
        }

        @Override
        public Collection<Node> nodes(Node reference) {
            return network.reachableNodes(reference);
        }

        @Override
        public Collection<Node> neighbors(Node node) {
            return network.neighbors(node);
        }

        @Override
        public void sendToAddress(Node source, String target, String name, Object... data) {
            network.sendToAddress(source, target, name, data);
        }

        @Override
        public void sendToNeighbors(Node source, String name, Object... data) {
            network.sendToNeighbors(source, name, data);
        }

        @Override
        public void sendToReachable(Node source, String name, Object... data) {
            network.sendToReachable(source, name, data);
        }

        @Override
        public void sendToVisible(Node source, String name, Object... data) {
            network.sendToVisible(source, name, data);
        }

        @Override
        public double globalBuffer() {
            return network.globalBuffer;
        }

        @Override
        public void globalBuffer_$eq(double value) {
            network.globalBuffer = value;
        }

        @Override
        public double globalBufferSize() {
            return network.globalBufferSize;
        }

        @Override
        public void globalBufferSize_$eq(double value) {
            network.globalBufferSize = value;
        }

        @Override
        public void addConnector(Connector connector) {
            network.addConnector(connector);
        }

        @Override
        public void removeConnector(Connector connector) {
            network.removeConnector(connector);
        }

        @Override
        public double changeBuffer(double delta) {
            return network.changeBuffer(delta);
        }

    }
}
