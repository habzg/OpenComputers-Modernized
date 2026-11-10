package li.cil.oc.core.impl.common.blockentity.traits;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.ExtendedNBT;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public abstract class HubBlockEntity extends BlockEntity implements Hub {
  public record QueuedPacket(Direction sourceSide, Packet packet) {
  }

  public final Plug[] plugs = new Plug[6];
  public final Queue<QueuedPacket> packetQueue = new LinkedList<>();
  protected boolean isChangeScheduled = false;
  public int relayDelay = OCSettings.get().switchDefaultRelayDelay;
  public int relayAmount = OCSettings.get().switchDefaultRelayAmount;
  public int maxQueueSize = OCSettings.get().switchDefaultMaxQueueSize;
  public final int relayBaseDelay = OCSettings.get().switchDefaultRelayDelay;
  public final int relayAmountPerUpgrade = OCSettings.get().switchRelayAmountUpgrade;
  public final int queueSizePerUpgrade = OCSettings.get().switchQueueSizeUpgrade;
  public final double relayDelayPerUpgrade = OCSettings.get().switchRelayDelayUpgrade;
  public final int relayBaseAmount = OCSettings.get().switchDefaultRelayAmount;
  public final int queueBaseSize = OCSettings.get().switchDefaultMaxQueueSize;
  public int relayCooldown = -1;
  public long lastMessage = 0;
  private final int[] packetsPerCycle = new int[20];
  private int packetsPerCycleIndex = 0;

  public HubBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
    super(type, pos, state);
    for (var side : Direction.values()) {
      if (plugs[side.ordinal()] == null) {
        plugs[side.ordinal()] = createPlug(side);
      }
    }
  }

  @Override
  public li.cil.oc.api.network.Node node() {
    return null;
  }

  public Level level() {
    return getLevel();
  }

  @Override
  public double xPosition() {
    return worldPosition.getX() + 0.5;
  }

  @Override
  public double yPosition() {
    return worldPosition.getY() + 0.5;
  }

  @Override
  public double zPosition() {
    return worldPosition.getZ() + 0.5;
  }

  @Override
  public void markChanged() {
    isChangeScheduled = true;
  }

  @Override
  public boolean isConnected() {
    for (Plug plug : plugs) {
      if (plug != null && plug.node() != null && plug.node().address() != null && plug.node().network() != null) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void onConnect(li.cil.oc.api.network.Node node) {
  }

  @Override
  public void onMessage(li.cil.oc.api.network.Message message) {
  }

  @Override
  public Direction[] validDirections() {
    return Direction.values();
  }

  @Override
  public Node sidedNode(Direction side) {
    if (side == null) return null;
    Plug plug = plugs[side.ordinal()];
    if (plug != null) return plug.node();
    return null;
  }

  @Override
  public boolean canConnect(Direction side) {
    return true;
  }

  @Override
  public void enqueuePacket(Direction sourceSide, Packet packet) {
    tryEnqueuePacket(sourceSide, packet);
  }

  @Override
  public boolean tryEnqueuePacket(Direction sourceSide, Packet packet) {
    synchronized (packetQueue) {
      if (packet.ttl() > 0 && packetQueue.size() < maxQueueSize) {
        packetQueue.offer(new QueuedPacket(sourceSide, packet.hop()));
        if (relayCooldown < 0) relayCooldown = relayDelay - 1;
        return true;
      }
      return false;
    }
  }

  protected void relayPacket(Direction sourceSide, Packet packet) {
    for (var side : Direction.values()) {
      if (sourceSide == null || !sourceSide.equals(side)) {
        var node = sidedNode(side);
        if (node != null) {
          node.sendToReachable("network.message", packet);
        }
      }
    }
  }

  public void onSwitchActivity() {
    lastMessage = System.currentTimeMillis();
  }

  protected void addPacketSample(int count) {
    packetsPerCycle[packetsPerCycleIndex] = count;
    packetsPerCycleIndex = (packetsPerCycleIndex + 1) % packetsPerCycle.length;
  }

  public int packetsPerCycleAvg() {
    int sum = 0;
    for (var v : packetsPerCycle) sum += v;
    return sum / packetsPerCycle.length;
  }

  @Override
  public Plug createPlug(Direction side) {
    return new Plug() {
      private final Node plugNode = createNode(this);
      private final Direction plugSide = side;

      @Override
      public Direction side() {
        return plugSide;
      }

      @Override
      public Node node() {
        return plugNode;
      }

      @Override
      public boolean isPrimary() {
        for (var other : plugs) {
          if (other != null && other.node() != null && other.node().network() == plugNode.network()) {
            return other == this;
          }
        }
        return false;
      }

      @Override
      public List<Plug> plugsInOtherNetworks() {
        var result = new ArrayList<Plug>();
        for (var other : plugs) {
          if (other != null && other != this && other.node() != null && other.node().network() != plugNode.network()) {
            result.add(other);
          }
        }
        return result;
      }

      @Override
      public boolean isConnected() {
        return plugNode != null && plugNode.address() != null && plugNode.network() != null;
      }

      @Override
      public void initialize() {
      }

      @Override
      public void dispose() {
      }

      @Override
      public void readFromNBTForServer(CompoundTag nbt) {
      }

      @Override
      public void writeToNBTForServer(CompoundTag nbt) {
      }

      @Override
      public void readFromNBTForClient(CompoundTag nbt) {
      }

      @Override
      public void writeToNBTForClient(CompoundTag nbt) {
      }

      public Level level() {
        return getLevel();
      }

      @Override
      public double xPosition() {
        return worldPosition.getX() + 0.5;
      }

      @Override
      public double yPosition() {
        return worldPosition.getY() + 0.5;
      }

      @Override
      public double zPosition() {
        return worldPosition.getZ() + 0.5;
      }

      @Override
      public void markChanged() {
      }

      @Override
      public void onConnect(Node node) {
        onPlugConnect(this, node);
      }

      @Override
      public void onDisconnect(Node node) {
        onPlugDisconnect(this, node);
      }

      @Override
      public void onMessage(Message message) {
        if (isPrimary()) {
          onPlugMessage(this, message);
        }
      }
    };
  }

  @Override
  public void onPlugConnect(Plug plug, Node node) {
    connectComponents();
  }

  @Override
  public void onPlugDisconnect(Plug plug, Node node) {
  }

  @Override
  public void onPlugMessage(Plug plug, Message message) {
    if ("network.message".equals(message.name()) && !plugsExists(message.source())) {
      var data = message.data();
      if (data.length > 0 && data[0] instanceof Packet packet) {
        tryEnqueuePacket(plug.side(), packet);
      }
    }
  }

  private boolean plugsExists(Node node) {
    for (var plug : plugs) {
      if (plug != null && plug.node() == node) return true;
    }
    return false;
  }

  @Override
  public Node createNode(Plug plug) {
    return li.cil.oc.api.Network.newNode(plug, Visibility.Network).create();
  }

  @Override
  public void initialize() {
    super.initialize();
    if (level() != null && isServer()) li.cil.oc.api.Network.joinOrCreateNetwork(this);
  }

  @Override
  public void dispose() {
    super.dispose();
    if (isServer()) {
      for (var plug : plugs) {
        if (plug != null && plug.node() != null) {
          plug.node().remove();
        }
      }
    }
  }

  @Override
  public void updateEntity() {
    super.updateEntity();
    if (isChangeScheduled) {
      super.setChanged();
      isChangeScheduled = false;
    }
    if (isServer()) {
      synchronized (packetQueue) {
        if (relayCooldown > 0) {
          relayCooldown--;
        } else {
          relayCooldown = -1;
          if (!packetQueue.isEmpty()) {
            int toProcess = Math.min(packetQueue.size(), relayAmount);
            addPacketSample(toProcess);
            for (int i = 0; i < toProcess; i++) {
              var entry = packetQueue.poll();
              if (entry != null) {
                relayPacket(entry.sourceSide(), entry.packet());
              }
            }
            if (!packetQueue.isEmpty()) {
              relayCooldown = relayDelay - 1;
            }
          } else if (relayDelay > 0 && level() != null && level().getGameTime() % relayDelay == 0) {
            addPacketSample(0);
          }
        }
      }
    }
  }

  protected void connectComponents() {
  }

  @SuppressWarnings("unused")
  protected void disconnectComponents() {
  }

  private static final String PlugsTag = OCSettings.namespace + "plugs";
  private static final String QueueTag = OCSettings.namespace + "queue";
  private static final String SideTag = "side";
  private static final String RelayCooldownTag = OCSettings.namespace + "relayCooldown";

  @Override
  public void readFromNBTForServer(CompoundTag nbt) {
    super.readFromNBTForServer(nbt);
    var provider = getEffectiveProvider();
    var plugList = nbt.getList(PlugsTag, Tag.TAG_COMPOUND);
    for (int i = 0; i < plugList.size() && i < plugs.length; i++) {
      var plugNbt = plugList.getCompound(i);
      if (plugs[i] != null && plugs[i].node() != null && provider != null) {
        plugs[i].node().load(plugNbt, provider);
      }
    }
    var queueList = nbt.getList(QueueTag, Tag.TAG_COMPOUND);
    synchronized (packetQueue) {
      for (int i = 0; i < queueList.size(); i++) {
        var tag = queueList.getCompound(i);
        var side = ExtendedNBT.getDirection(tag, SideTag);
        var packet = li.cil.oc.api.Network.newPacket(tag);
        packetQueue.add(new QueuedPacket(side, packet));
      }
      if (nbt.contains(RelayCooldownTag)) {
        relayCooldown = nbt.getInt(RelayCooldownTag);
      }
    }
  }

  @Override
  public void writeToNBTForServer(CompoundTag nbt) {
    super.writeToNBTForServer(nbt);
    if (isServer()) {
      var provider = getEffectiveProvider();
      var plugList = new ListTag();
      for (var plug : plugs) {
        var plugNbt = new CompoundTag();
        if (plug != null && plug.node() != null && provider != null) {
          plug.node().save(plugNbt, provider);
        }
        plugList.add(plugNbt);
      }
      nbt.put(PlugsTag, plugList);
      var queueList = new ListTag();
      synchronized (packetQueue) {
        for (var entry : packetQueue) {
          var tag = new CompoundTag();
          ExtendedNBT.setDirection(tag, SideTag, entry.sourceSide());
          entry.packet().save(tag);
          queueList.add(tag);
        }
        if (relayCooldown > 0) {
          nbt.putInt(RelayCooldownTag, relayCooldown);
        }
      }
      nbt.put(QueueTag, queueList);
    }
  }

  @Override
  public void readFromNBTForClient(CompoundTag nbt) {
    super.readFromNBTForClient(nbt);
  }

  @Override
  public void writeToNBTForClient(CompoundTag nbt) {
    super.writeToNBTForClient(nbt);
  }
}
