package li.cil.oc.core.impl.common.blockentity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Optional;
import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.network.Analyzable;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.common.blockentity.traits.BundledRedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.ComponentInventory;
import li.cil.oc.core.impl.common.blockentity.traits.Hub;
import li.cil.oc.core.impl.common.blockentity.traits.HubBlockEntity;
import li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.blockentity.traits.PowerBalancer;
import li.cil.oc.core.impl.common.blockentity.traits.RedstoneAware;
import li.cil.oc.core.impl.common.blockentity.traits.Rotatable;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.jetbrains.annotations.NotNull;

public class Rack extends HubBlockEntity implements PowerAcceptor, PowerBalancer, ComponentInventory, Rotatable, BundledRedstoneAware, Analyzable, li.cil.oc.api.internal.Rack, li.cil.oc.api.util.StateAware {
  public static BlockEntityType<?> TYPE;

  public boolean isRelayEnabled = false;
  public final CompoundTag[] lastData = new CompoundTag[getContainerSize()];
  public final boolean[] hasChanged = new boolean[getContainerSize()];
  public final Direction[][] nodeMapping = new Direction[getContainerSize()][4];
  public final Node[][] snifferNodes = new Node[getContainerSize()][3];
  private ManagedEnvironment[] _components;
  private final ArrayList<ManagedEnvironment> updatingComponents = new ArrayList<>();
  protected boolean _isOutputEnabled = false;
  private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
  private final int[] _output = new int[6];
  private final int[][] _bundledInput = new int[6][16];
  private final int[][] _bundledOutput = new int[6][16];
  private final ItemStack[] items = li.cil.oc.core.impl.common.inventory.Inventory.filledItems(getContainerSize());
  private Direction facing = Direction.SOUTH;

  public Rack(BlockEntityType<?> type, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    super(type, pos, state);
    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
      facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
    }
    java.util.Arrays.fill(hasChanged, true);
    for (int i = 0; i < snifferNodes.length; i++) {
      for (int j = 0; j < snifferNodes[i].length; j++) {
        snifferNodes[i][j] = Network.newNode(this, Visibility.Neighbors).create();
      }
    }
  }

  public Rack(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
    this(TYPE, pos, state);
  }

  @Override
  public @NotNull BlockEntityType<?> getType() {
    return TYPE != null ? TYPE : super.getType();
  }

  public void connect(int slot, int connectableIndex, @org.jetbrains.annotations.Nullable Direction side) {
    var newSide = (side == null || side == Direction.SOUTH) ? null : side;
    var oldSide = nodeMapping[slot][connectableIndex + 1];
    if (oldSide == newSide) return;

    var mountable = getMountable(slot);
    if (mountable != null && oldSide != null) {
      if (connectableIndex == -1) {
        var node = mountable.node();
        var plug = sidedNode(toGlobal(oldSide));
        if (node != null && plug != null) {
          node.disconnect(plug);
        }
      } else {
        snifferNodes[slot][connectableIndex].remove();
      }
    }

    nodeMapping[slot][connectableIndex + 1] = newSide;
    super.setChanged();

    if (mountable != null && newSide != null) {
      if (connectableIndex == -1) {
        var node = mountable.node();
        var plug = sidedNode(toGlobal(newSide));
        if (node != null && plug != null) {
          node.connect(plug);
        }
      } else if (connectableIndex < mountable.getConnectableCount()) {
        var connectable = mountable.getConnectableAt(connectableIndex);
        if (connectable != null && connectable.node() != null) {
          if (connectable.node().network() == null) {
            Network.joinNewNetwork(connectable.node());
          }
          connectable.node().connect(snifferNodes[slot][connectableIndex]);
        }
      }
    }
  }

  private void reconnect(Direction plugSide) {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      var mapping = nodeMapping[slot];
      if (mapping[0] != null && toGlobal(mapping[0]) == plugSide) {
        var mountable = getMountable(slot);
        var busNode = sidedNode(plugSide);
        if (busNode != null && mountable != null && mountable.node() != null && busNode != mountable.node()) {
          Network.joinNewNetwork(mountable.node());
          busNode.connect(mountable.node());
        }
      }
      for (int ci = 0; ci < 3; ci++) {
        if (mapping[ci + 1] != null && toGlobal(mapping[ci + 1]) == plugSide) {
          var mountable = getMountable(slot);
          if (mountable != null && ci < mountable.getConnectableCount()) {
            var connectable = mountable.getConnectableAt(ci);
            if (connectable != null && connectable.node() != null) {
              if (connectable.node().network() == null) {
                Network.joinNewNetwork(connectable.node());
              }
              connectable.node().connect(snifferNodes[slot][ci]);
            }
          }
        }
      }
    }
  }

  private void sendPacketToMountables(@org.jetbrains.annotations.Nullable Direction sourceSide, Packet packet) {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      var mapping = nodeMapping[slot];
      for (int ci = 0; ci < 3; ci++) {
        if (mapping[ci + 1] != null && sourceSide == toGlobal(mapping[ci + 1])) {
          var mountable = getMountable(slot);
          if (mountable != null && ci < mountable.getConnectableCount()) {
            var connectable = mountable.getConnectableAt(ci);
            if (connectable != null) {
              connectable.receivePacket(packet);
            }
          }
        }
      }
    }
  }

  @Override
  public boolean tryEnqueuePacket(Direction sourceSide, Packet packet) {
    sendPacketToMountables(sourceSide, packet);
    if (isRelayEnabled) {
      return super.tryEnqueuePacket(sourceSide, packet);
    }
    return true;
  }

  @Override
  protected void relayPacket(Direction sourceSide, Packet packet) {
    if (isRelayEnabled) {
      super.relayPacket(sourceSide, packet);
    }
  }

  @Override
  public void onPlugConnect(Hub.Plug plug, Node node) {
    super.onPlugConnect(plug, node);
    connectComponents();
    reconnect(plug.side());
  }

  @Override
  public Node createNode(Hub.Plug plug) {
    return Network.newNode(plug, Visibility.Network)
      .withConnector(OCSettings.get().bufferDistributor)
      .create();
  }

  @Override
  public void connectComponents() {
    ComponentInventory.super.connectComponents();
  }

  @Override
  public void disconnectComponents() {
    ComponentInventory.super.disconnectComponents();
  }

  @Override
  public void initialize() {
    super.initialize();
    if (isServer()) {
      EventHandlerDelegate.get().scheduleServer(this);
      EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
    }
  }

  @Override
  public void dispose() {
    super.dispose();
    disconnectComponents();
  }

  @Override
  public void onMessage(Message message) {
    super.onMessage(message);
    if ("network.message".equals(message.name()) && message.data().length > 0 && message.data()[0] instanceof Packet packet) {
      relayIfMessageFromConnectable(message, packet);
    }
  }

  public void onNeighborChanged() {
    ae2OnNeighborChanged();
    checkRedstoneInputChanged();
  }

  private void relayIfMessageFromConnectable(Message message, Packet packet) {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      var mountable = getMountable(slot);
      if (mountable == null) continue;
      var mapping = nodeMapping[slot];
      for (int ci = 0; ci < 3; ci++) {
        if (mapping[ci + 1] != null && ci < mountable.getConnectableCount()) {
          var connectable = mountable.getConnectableAt(ci);
          if (connectable != null && connectable.node() == message.source()) {
            var plug = sidedNode(toGlobal(mapping[ci + 1]));
            if (plug != null) {
              plug.sendToReachable("network.message", packet);
              relayToConnectablesOnSide(message, packet, mapping[ci + 1]);
            }
            return;
          }
        }
      }
    }
  }

  private void relayToConnectablesOnSide(Message message, Packet packet, Direction sourceSide) {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      var mountable = getMountable(slot);
      if (mountable == null) continue;
      var mapping = nodeMapping[slot];
      for (int ci = 0; ci < 3; ci++) {
        if (mapping[ci + 1] == sourceSide && ci < mountable.getConnectableCount()) {
          var connectable = mountable.getConnectableAt(ci);
          if (connectable != null && connectable.node() != message.source()) {
            snifferNodes[slot][ci].sendToNeighbors("network.message", packet);
          }
        }
      }
    }
  }

  @Override
  public boolean canConnect(Direction side) {
    return side != facing();
  }

  @Override
  public Node sidedNode(Direction side) {
    if (side == facing()) return null;
    return super.sidedNode(side);
  }

  @Override
  public boolean canConnectEnergy(Direction side) {
    return side != facing();
  }

  public Optional<Connector> connector(Direction side) {
    if (side == facing()) return Optional.empty();
    var node = sidedNode(side);
    if (node instanceof Connector connector) return Optional.of(connector);
    return Optional.empty();
  }

  @Override
  public double energyThroughput() {
    return OCSettings.get().serverRackRate;
  }

  @Override
  public boolean canConnectPower(Direction side) {
    return side != facing();
  }

  @Override
  public double tryChangeBuffer(Direction side, double amount) {
    return tryChangeBuffer(side, amount, true);
  }

  @Override
  public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
    if (isClient() || OCSettings.get().ignorePower) return 0;
    var c = connector(side);
    if (c.isPresent()) {
      var conn = c.get();
      double limit = Math.min(Math.min(energyThroughput(), amount), globalDemand(side));
      double cappedAmount = Math.max(0, limit);
      if (doReceive) return cappedAmount - conn.changeBuffer(cappedAmount);
      return cappedAmount;
    }
    return 0;
  }

  @Override
  public double globalBuffer(Direction side) {
    var c = connector(side);
    return c.map(Connector::globalBuffer).orElse(0.0);
  }

  @Override
  public double globalBufferSize(Direction side) {
    var c = connector(side);
    return c.map(Connector::globalBufferSize).orElse(0.0);
  }

  @Override
  public double globalDemand(Direction side) {
    var c = connector(side);
    return c.map(connector -> Math.clamp(connector.globalBufferSize() - connector.globalBuffer(), 0, energyThroughput())).orElse(0.0);
  }

  private double _globalBuffer = 0;
  private double _globalBufferSize = 0;

  @Override
  public double globalBuffer() {
    return _globalBuffer;
  }

  @Override
  public void globalBuffer(double value) {
    _globalBuffer = value;
  }

  @Override
  public double globalBufferSize() {
    return _globalBufferSize;
  }

  @Override
  public void globalBufferSize(double value) {
    _globalBufferSize = value;
  }

  @Override
  public void updatePowerInformation() {
  }

  @Override
  public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
    var slot = slotAt(side, hitX, hitY, hitZ);
    if (slot.isPresent()) {
      var mountable = getMountable(slot.get());
      if (mountable instanceof Analyzable analyzable) {
        return analyzable.onAnalyze(player, side, hitX, hitY, hitZ);
      }
      return null;
    }
    return new Node[]{sidedNode(side)};
  }

  @Override
  public int indexOfMountable(RackMountable mountable) {
    var comps = componentEnvironments();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] == mountable) return i;
    }
    return -1;
  }

  @Override
  public RackMountable getMountable(int slot) {
    if (slot < 0 || slot >= componentEnvironments().length) return null;
    var c = componentEnvironments()[slot];
    if (c instanceof RackMountable rm) return rm;
    return null;
  }

  @Override
  public CompoundTag getMountableData(int slot) {
    if (slot < 0 || slot >= lastData.length) return null;
    return lastData[slot];
  }

  @Override
  public void markChanged(int slot) {
    synchronized (hasChanged) {
      hasChanged[slot] = true;
    }
    setOutputEnabled(hasRedstoneCard());
  }

  @Override
  public EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
    var result = EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
    for (var component : componentEnvironments()) {
      if (component instanceof li.cil.oc.api.util.StateAware sa) {
        result.addAll(sa.getCurrentState());
      }
    }
    return result;
  }

  @Override
  public Direction facing() {
    return facing;
  }

  @Override
  public void facing(Direction value) {
    if (facing == value) return;
    facing = value;
    var level = getLevel();
    if (level != null) {
      var state = getBlockState();
      if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
        level.setBlockAndUpdate(getBlockPos(), state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, value));
      }
    }
    onRotationChanged();
  }

  @Override
  public Direction toLocal(Direction global) {
    return RotationHelper.toLocal(pitch(), yaw(), global);
  }

  @Override
  public Direction toGlobal(Direction local) {
    return RotationHelper.toGlobal(pitch(), yaw(), local);
  }

  @Override
  public void onRotationChanged() {
    if (isServer()) {
      PacketSender.sendRotatableState(this, pitch(), yaw());
    } else {
      var level = getLevel();
      if (level != null) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      }
    }
    var level = getLevel();
    if (level != null) {
      level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
    }
    checkRedstoneInputChanged();
  }

  @Override
  public void onRedstoneInputChanged(int side, int oldValue, int newValue, int color) {
    for (int slot = 0; slot < getContainerSize(); slot++) {
      var mountable = getMountable(slot);
      if (mountable != null && mountable.node() != null) {
        var localSide = toLocal(Direction.from3DDataValue(side));
        mountable.node().sendToNeighbors("redstone.changed",
          new RedstoneAware.RedstoneChangedEventArgs(localSide, oldValue, newValue, color));
      }
    }
  }

  @Override
  public boolean isOutputEnabled() {
    return _isOutputEnabled;
  }

  @Override
  public void setOutputEnabled(boolean value) {
    if (value != _isOutputEnabled) {
      _isOutputEnabled = value;
      if (!value) {
        java.util.Arrays.fill(_output, 0);
      }
      onRedstoneOutputEnabledChanged();
    }
  }

  @Override
  public int[] input() {
    return _input;
  }

  @Override
  public int getInput(Direction side) {
    return Math.max(_input[side.ordinal()], 0);
  }

  @Override
  public void setInput(Direction side, int value) {
    var ord = side.ordinal();
    var old = _input[ord];
    _input[ord] = value;
    if (old >= 0 && old != value) {
      onRedstoneInputChanged(ord, old, value);
    }
  }

  @Override
  public void setInput(int[] values) {
    for (var side : Direction.values()) {
      int value = side.ordinal() < values.length ? values[side.ordinal()] : 0;
      setInput(side, value);
    }
  }

  @Override
  public int maxInput() {
    var max = 0;
    for (var v : _input) max = Math.max(max, Math.max(v, 0));
    return max;
  }

  @Override
  public int[] output() {
    var result = new int[Direction.values().length];
    for (var side : Direction.values()) {
      result[side.ordinal()] = _output[toLocal(side).ordinal()];
    }
    return result;
  }

  @Override
  public int[] outputLocal() {
    return _output;
  }

  @Override
  public int getOutput(Direction side) {
    return _output[toLocal(side).ordinal()];
  }

  @Override
  public void setOutput(Direction side, int value) {
    var ord = toLocal(side).ordinal();
    if (_output[ord] != value) {
      _output[ord] = value;
      onRedstoneOutputChanged(side);
    }
  }

  @Override
  public int[][] bundledInput() {
    return _bundledInput;
  }

  @Override
  public int[][] bundledOutput() {
    return _bundledOutput;
  }

  @Override
  public int getBundledInput(Direction side, int color) {
    return Math.max(_bundledInput[side.ordinal()][color], 0);
  }

  @Override
  public void setBundledInput(Direction side, int color, int value) {
    updateInput(bundledInput(), side, color, Math.max(value, 0));
  }

  @Override
  public void setBundledInput(Direction side, int[] values) {
    for (int color = 0; color < 16; color++) {
      setBundledInput(side, color, (values != null && color < values.length) ? values[color] : 0);
    }
  }

  @Override
  public int[] getBundledOutput(Direction side) {
    return _bundledOutput[toLocal(side).ordinal()];
  }

  @Override
  public int getBundledOutput(Direction side, int color) {
    return _bundledOutput[toLocal(side).ordinal()][color];
  }

  @Override
  public void setBundledOutput(Direction side, int color, int value) {
    var ord = toLocal(side).ordinal();
    var old = _bundledOutput[ord][color];
    if (old != value) {
      _bundledOutput[ord][color] = value;
      onRedstoneOutputChanged(side);
    }
  }

  @Override
  public void updateRedstoneInput(Direction side) {
    var oldValue = _input[side.ordinal()];
    var newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(position(), side);
    if (oldValue != newValue) {
      _input[side.ordinal()] = newValue;
      onRedstoneInputChanged(side.ordinal(), oldValue, newValue);
    }
    setBundledInput(side, li.cil.oc.core.impl.integration.util.BundledRedstone.computeBundledInput(position(), side));
  }

  @Override
  public void checkRedstoneInputChanged() {
    if (getLevel() != null && !getLevel().isClientSide) {
      for (Direction side : Direction.values()) {
        updateRedstoneInput(side);
      }
    }
  }

  @Override
  public int getContainerSize() {
    return 4;
  }

  @Override
  public int getMaxStackSize() {
    return 1;
  }

  @Override
  public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
    var driver = Driver.driverFor(stack);
    return driver != null && Slot.RackMountable.equals(driver.slot(stack));
  }

  @Override
  public ManagedEnvironment[] _components() {
    return _components;
  }

  @Override
  public void _components(ManagedEnvironment[] value) {
    _components = value;
  }

  @Override
  public boolean isSizeInventoryReady() {
    return true;
  }

  @Override
  public ArrayList<ManagedEnvironment> updatingComponents() {
    return updatingComponents;
  }

  @Override
  public li.cil.oc.api.network.EnvironmentHost host() {
    return this;
  }

  @Override
  public ItemStack[] items() {
    return items;
  }

  @Override
  public void updateItems(int slot, ItemStack stack) {
    items[slot] = stack;
  }

  @Override
  public boolean isUseableByPlayer(Player player) {
    return player != null && player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
  }

  @Override
  public boolean stillValid(@NotNull Player player) {
    return player.distanceToSqr(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5) <= 64;
  }

  @Override
  public void dropSlot(int slot) {
    dropSlot(slot, 1, null);
  }

  @Override
  public void dropSlot(int slot, int count, Direction direction) {
    var removed = removeItem(slot, count);
    if (!removed.isEmpty()) {
      if (direction != null) spawnStackInWorld(removed, direction);
      else spawnStackInWorld(removed);
    }
  }

  @Override
  public void dropAllSlots() {
    for (int i = 0; i < getContainerSize(); i++) {
      dropSlot(i);
    }
  }

  @Override
  public void spawnStackInWorld(ItemStack stack) {
    if (isServer()) {
      InventoryUtils.spawnStackInWorld(position(), stack, null, null);
    }
  }

  @Override
  public void spawnStackInWorld(ItemStack stack, Direction direction) {
    if (isServer()) {
      InventoryUtils.spawnStackInWorld(position(), stack, direction, null);
    }
  }

  @Override
  public int x() {
    return worldPosition.getX();
  }

  @Override
  public int y() {
    return worldPosition.getY();
  }

  @Override
  public int z() {
    return worldPosition.getZ();
  }

  @Override
  public void setChanged() {
    if (!isServerThread()) {
      runOnServerThread(this::setChanged);
      return;
    }
    super.setChanged();
    if (isServer()) {
      setOutputEnabled(hasRedstoneCard());
      ItemStack[] items = new ItemStack[getContainerSize()];
      for (int i = 0; i < items.length; i++) items[i] = getItem(i);
      PacketSender.sendRackInventory(this, items);
    } else {
      var level = getLevel();
      if (level != null) {
        level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
      }
    }
  }

  @Override
  public void onItemAdded(int slot, ItemStack stack) {
    if (isServer()) {
      for (int i = 0; i < 4; i++) {
        nodeMapping[slot][i] = null;
      }
      lastData[slot] = null;
      hasChanged[slot] = true;
    }
    ComponentInventory.super.onItemAdded(slot, stack);
  }

  @Override
  public void onItemRemoved(int slot, ItemStack stack) {
    if (isServer()) {
      for (int i = 0; i < 4; i++) {
        nodeMapping[slot][i] = null;
      }
      lastData[slot] = null;
    }
    ComponentInventory.super.onItemRemoved(slot, stack);
  }

  @Override
  public void connectItemNode(Node node) {
    if (node != null) {
      Network.joinNewNetwork(node);
    }
  }

  @Override
  public void updateEntity() {
    super.updateEntity();
    if (isServer()) {
      if (isConnected()) {
        var connectors = new Connector[Direction.values().length];
        for (var side : Direction.values()) {
          var node = sidedNode(side);
          if (node instanceof Connector c) connectors[side.ordinal()] = c;
        }
        var world = getLevel();
        if (world != null && world.getGameTime() % OCSettings.get().tickFrequency == 0) {
          var seen = new java.util.ArrayList<li.cil.oc.api.network.Network>();
          double sumBuffer = 0;
          double sumSize = 0;
          for (var connector : connectors) {
            if (connector != null && connector.network() != null && !seen.contains(connector.network())) {
              seen.add(connector.network());
              sumBuffer += connector.globalBuffer();
              sumSize += connector.globalBufferSize();
            }
          }
          if (sumSize > 0) {
            double ratio = sumBuffer / sumSize;
            seen.clear();
            for (var connector : connectors) {
              if (connector != null && connector.network() != null && !seen.contains(connector.network())) {
                seen.add(connector.network());
                connector.changeBuffer(connector.globalBufferSize() * ratio - connector.globalBuffer());
              }
            }
          }
          globalBuffer(sumBuffer);
          globalBufferSize(sumSize);
        }
        updatePowerInformation();

        var comps = componentEnvironments();
        for (int slot = 0; slot < comps.length; slot++) {
          if (comps[slot] instanceof RackMountable mountable) {
            if (hasChanged[slot]) {
              hasChanged[slot] = false;
              lastData[slot] = mountable.getData();
              PacketSender.sendRackMountableData(this, slot, lastData[slot]);
              var level = getLevel();
              if (level != null) {
                level.updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
              }
              setOutputEnabled(hasRedstoneCard());
            }

            if (mountable.node() instanceof Connector mountableConnector) {
              var remaining = OCSettings.get().serverRackRate;
              for (var outside : connectors) {
                if (outside != null && remaining > 0) {
                  var received = remaining + outside.changeBuffer(-remaining);
                  var rejected = mountableConnector.changeBuffer(received);
                  outside.changeBuffer(rejected);
                  remaining -= received - rejected;
                }
              }
            }
          }
        }

        updateComponents();
      }
    }
  }

  private static final String IsRelayEnabledTag = OCSettings.namespace + "isRelayEnabled";
  private static final String NodeMappingTag = OCSettings.namespace + "nodeMapping";
  private static final String LastDataTag = OCSettings.namespace + "lastData";
  private static final String FacingTag = OCSettings.namespace + "facing";

  @Override
  public void readFromNBTForServer(CompoundTag nbt) {
    super.readFromNBTForServer(nbt);
    if (nbt.contains(FacingTag)) {
      facing = Direction.from3DDataValue(nbt.getInt(FacingTag));
    }
    isRelayEnabled = nbt.getBoolean(IsRelayEnabledTag);
    var mappingList = nbt.getList(NodeMappingTag, Tag.TAG_INT_ARRAY);
    for (int i = 0; i < mappingList.size() && i < nodeMapping.length; i++) {
      var buses = mappingList.getIntArray(i);
      for (int j = 0; j < buses.length && j < nodeMapping[i].length; j++) {
        var id = buses[j];
        nodeMapping[i][j] = (id < 0 || id == Direction.SOUTH.ordinal()) ? null : Direction.from3DDataValue(id);
      }
    }
    var loadProv = getEffectiveProvider();
    if (loadProv != null) {
      load(nbt, loadProv);
    }
    _isOutputEnabled = hasRedstoneCard();
  }

  @Override
  public void writeToNBTForServer(CompoundTag nbt) {
    super.writeToNBTForServer(nbt);
    nbt.putInt(FacingTag, facing.ordinal());
    nbt.putBoolean(IsRelayEnabledTag, isRelayEnabled);
    var mappingList = new net.minecraft.nbt.ListTag();
    for (var buses : nodeMapping) {
      var arr = new int[buses.length];
      for (int j = 0; j < buses.length; j++) {
        arr[j] = buses[j] != null ? buses[j].ordinal() : -1;
      }
      mappingList.add(new net.minecraft.nbt.IntArrayTag(arr));
    }
    nbt.put(NodeMappingTag, mappingList);
    var provider = getEffectiveProvider();
    if (provider != null) {
      save(nbt, provider);
    }
  }

  @Override
  public void readFromNBTForClient(CompoundTag nbt) {
    super.readFromNBTForClient(nbt);
    if (nbt.contains(FacingTag)) {
      facing = Direction.from3DDataValue(nbt.getInt(FacingTag));
    }
    var data = nbt.getList(LastDataTag, Tag.TAG_COMPOUND);
    for (int i = 0; i < data.size() && i < lastData.length; i++) {
      lastData[i] = data.getCompound(i);
    }
    var provider = getEffectiveProvider();
    if (provider != null) {
      load(nbt, provider);
    }
    connectComponents();
  }

  @Override
  public void writeToNBTForClient(CompoundTag nbt) {
    super.writeToNBTForClient(nbt);
    nbt.putInt(FacingTag, facing.ordinal());
    var data = new net.minecraft.nbt.ListTag();
    for (var tag : lastData) {
      data.add(tag != null ? tag : new CompoundTag());
    }
    nbt.put(LastDataTag, data);
    var provider = getEffectiveProvider();
    if (provider != null) {
      var wasSavingForClients = BlockEntity.savingForClients;
      BlockEntity.savingForClients = true;
      try {
        save(nbt, provider);
      } finally {
        BlockEntity.savingForClients = wasSavingForClients;
      }
    }
  }

  public Optional<Integer> slotAt(Direction side, float ignoredHitX, float hitY, float ignoredHitZ) {
    if (side == facing()) {
      var globalY = (int) (hitY * 16);
      int l = 2, h = 14;
      var slot = (15 - globalY - l) * getContainerSize() / (h - l);
      return Optional.of(Math.clamp(slot, 0, getContainerSize() - 1));
    }
    return Optional.empty();
  }

  public boolean isWorking(RackMountable mountable) {
    return mountable.getCurrentState().contains(li.cil.oc.api.util.StateAware.State.IsWorking);
  }

  public boolean hasRedstoneCard() {
    for (var component : componentEnvironments()) {
      if (component instanceof li.cil.oc.api.network.EnvironmentHost host && component instanceof RackMountable rm && isWorking(rm)) {
        if (host instanceof net.minecraft.world.Container container) {
          for (int i = 0; i < container.getContainerSize(); i++) {
            var stack = container.getItem(i);
            var driver = li.cil.oc.api.API.driver.driverFor(stack, host.getClass());
            if (li.cil.oc.core.impl.util.ComponentDriverHelper.isRedstoneCard(driver)) {
              return true;
            }
          }
        }
      }
    }
    return false;
  }
}
