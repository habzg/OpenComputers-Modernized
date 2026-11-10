package li.cil.oc.core.impl.common.blockentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.impl.common.blockentity.traits.BlockEntity;
import li.cil.oc.core.impl.common.blockentity.traits.Computer;
import li.cil.oc.core.impl.common.blockentity.traits.Hub;
import li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor;
import li.cil.oc.core.impl.common.item.data.MicrocontrollerData;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.RotationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class Microcontroller extends BlockEntity implements PowerAcceptor, Hub, Computer, li.cil.oc.api.internal.Microcontroller, DeviceInfo, WorldlyContainer {

  public static BlockEntityType<Microcontroller> TYPE;
  public final MicrocontrollerData info = new MicrocontrollerData();
  public final boolean[] outputSides = new boolean[6];
  public final Node snooperNode;
  public final Node[] componentNodes;
  private final Map<String, String> deviceInfo = Map.of(
    DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.System,
    DeviceInfo.DeviceAttribute.Description, "Microcontroller",
    DeviceInfo.DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor,
    DeviceInfo.DeviceAttribute.Product, "Cubicle",
    DeviceInfo.DeviceAttribute.Capacity, String.valueOf(getSizeInventory())
  );
  private Direction _facing = Direction.SOUTH;
  private Machine _machine;
  private CompoundTag pendingMachineNbt;
  private li.cil.oc.api.network.ManagedEnvironment[] _components;
  private boolean _isRunning = false;
  private boolean _hasErrored = false;

  public Microcontroller(BlockPos pos, BlockState state) {
    super(TYPE, pos, state);
    for (int i = 0; i < 6; i++) outputSides[i] = true;
    snooperNode = li.cil.oc.api.Network.newNode(this, Visibility.Network)
      .withComponent("microcontroller")
      .withConnector(OCSettings.get().bufferMicrocontroller)
      .create();
    componentNodes = new Node[6];
    for (int i = 0; i < 6; i++) {
      componentNodes[i] = li.cil.oc.api.Network.newNode(this, Visibility.Network)
        .withComponent("microcontroller")
        .create();
    }
    for (var side : Direction.values()) {
      plugs[side.ordinal()] = createPlug(side);
    }
  }

  public final Plug[] plugs = new Plug[6];

  @Override
  public Node node() {
    return null;
  }

  @Override
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

  private volatile boolean isChangeScheduled = false;

  @Override
  public void markChanged() {
    isChangeScheduled = true;
  }

  @Override
  public boolean isConnected() {
    return snooperNode != null && snooperNode.address() != null && snooperNode.network() != null;
  }

  @Override
  public void onConnect(Node node) {
  }

  @Override
  public void onMessage(li.cil.oc.api.network.Message message) {
    if ("network.message".equals(message.name()) && message.source().network() == snooperNode.network()) {
      for (Direction side : Direction.values()) {
        if (outputSides[side.ordinal()] && side != facing()) {
          Node node = sidedNode(side);
          if (node != null) {
            node.sendToReachable(message.name(), message.data());
          }
        }
      }
    }
  }

  public void onNeighborChanged() {
    ae2OnNeighborChanged();
  }

  @Override
  public void initialize() {
    super.initialize();
    if (isServer() && getLevel() != null) {
      EventHandlerDelegate.get().scheduleServer(this);
      EventHandlerDelegate.get().scheduleServer(this::checkRedstoneInputChanged);
      if (pendingMachineNbt != null) {
        Machine m = machine();
        if (m != null) {
          m.load(pendingMachineNbt, getEffectiveProvider());
          setRunning(m.isRunning());
        }
        pendingMachineNbt = null;
      }
      if (machine() != null && machine().node() != null) {
        if (machine().node().network() == null) {
          li.cil.oc.api.Network.joinNewNetwork(machine().node());
        }
        machine().node().connect(snooperNode);
      }
    }
  }

  @Override
  public int tier() {
    return info.tier;
  }

  @Override
  public String runSound() {
    return null;
  }

  @Override
  public java.util.Map<String, String> getDeviceInfo() {
    return deviceInfo;
  }

  @Override
  public Direction facing() {
    var state = getBlockState();
    if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
      return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
    }
    return _facing;
  }

  @Override
  public void facing(Direction value) {
    _facing = value;
    var state = getBlockState();
    if (getLevel() != null && state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
      getLevel().setBlockAndUpdate(worldPosition, state.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, value));
    }
  }

  @Override
  public void onRotationChanged() {
    if (isServer()) {
      PacketSender.sendRotatableState(this, pitch(), yaw());
    } else {
      var level = getLevel();
      if (level != null) {
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
      }
    }
    var level = getLevel();
    if (level != null) {
      level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }
    checkRedstoneInputChanged();
  }

  @Override
  public Hub.Plug createPlug(Direction side) {
    return new Hub.Plug() {
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
      public List<Hub.Plug> plugsInOtherNetworks() {
        var result = new ArrayList<Hub.Plug>();
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

      @Override
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
  public void enqueuePacket(Direction sourceSide, li.cil.oc.api.network.Packet packet) {
  }

  @Override
  public boolean tryEnqueuePacket(Direction sourceSide, li.cil.oc.api.network.Packet packet) {
    return false;
  }

  @Override
  public Direction[] validDirections() {
    return Direction.values();
  }

  @Override
  public boolean canConnect(Direction side) {
    return side != facing();
  }

  @Override
  public Node sidedNode(Direction side) {
    if (side == facing()) return null;
    var plug = plugs[side.ordinal()];
    return plug != null ? plug.node() : null;
  }

  protected boolean hasConnector(Direction side) {
    return side != facing();
  }

  protected Connector connector(Direction side) {
    return side != facing() ? (Connector) snooperNode : null;
  }

  @Override
  public boolean canConnectPower(Direction side) {
    return hasConnector(side);
  }

  @Override
  public double tryChangeBuffer(Direction side, double amount) {
    return tryChangeBuffer(side, amount, true);
  }

  @Override
  public double tryChangeBuffer(Direction side, double amount, boolean doReceive) {
    if (isClient() || OCSettings.get().ignorePower) return 0;
    var c = connector(side);
    if (c != null) {
      double cappedAmount = Math.clamp(amount, 0, Math.min(energyThroughput(), globalDemand(side)));
      if (doReceive) return cappedAmount - c.changeBuffer(cappedAmount);
      return cappedAmount;
    }
    return 0;
  }

  @Override
  public double globalBuffer(Direction side) {
    var c = connector(side);
    return c != null ? c.globalBuffer() : 0.0;
  }

  @Override
  public double globalBufferSize(Direction side) {
    var c = connector(side);
    return c != null ? c.globalBufferSize() : 0.0;
  }

  @Override
  public double globalDemand(Direction side) {
    var c = connector(side);
    return c != null ? Math.clamp(c.globalBufferSize() - c.globalBuffer(), 0, energyThroughput()) : 0.0;
  }

  @Override
  public double energyThroughput() {
    return OCSettings.get().caseRate[Tier.One];
  }

  @Override
  public net.minecraft.core.Direction toLocal(net.minecraft.core.Direction global) {
    return RotationHelper.toLocal(pitch(), yaw(), global);
  }

  @Override
  public net.minecraft.core.Direction toGlobal(net.minecraft.core.Direction local) {
    return RotationHelper.toGlobal(pitch(), yaw(), local);
  }

  @Override
  public Node[] onAnalyze(Player player, Direction side, float hitX, float hitY, float hitZ) {
    if (side != facing()) return new Node[]{componentNodes[side.ordinal()]};
    return new Node[]{machine().node()};
  }

  @Override
  public boolean isUseableByPlayer(net.minecraft.world.entity.player.Player player) {
    return false;
  }

  @Override
  public void markDirty() {
    setChanged();
    if (isServer()) {
      setOutputEnabled(hasRedstoneCard());
    }
  }

  private final int[] _input = new int[]{-1, -1, -1, -1, -1, -1};
  private final int[] _output = new int[6];
  private final int[][] _bundledInput = new int[6][16];
  private final int[][] _bundledOutput = new int[6][16];
  private boolean _isOutputEnabled = false;

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
    int max = 0;
    for (int v : _input) max = Math.max(max, Math.max(v, 0));
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
    var newValue = li.cil.oc.core.impl.integration.util.BundledRedstone.computeInput(
      li.cil.oc.core.impl.util.BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), side);
    if (oldValue != newValue) {
      _input[side.ordinal()] = newValue;
      onRedstoneInputChanged(side.ordinal(), oldValue, newValue);
    }
    setBundledInput(side, li.cil.oc.core.impl.integration.util.BundledRedstone.computeBundledInput(
      li.cil.oc.core.impl.util.BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()), side));
  }

  @Override
  public void onRedstoneOutputChanged(Direction side) {
    if (getLevel() != null && !getLevel().isClientSide()) {
      getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
    }
    syncRedstoneState();
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
  public void onMachineConnect(li.cil.oc.api.network.Node node) {
  }

  @Override
  public void onMachineDisconnect(li.cil.oc.api.network.Node node) {
  }

  @Override
  public java.lang.Iterable<li.cil.oc.api.network.ManagedEnvironment> installedComponents() {
    return java.util.Collections.emptyList();
  }

  @Override
  public java.util.EnumSet<li.cil.oc.api.util.StateAware.State> getCurrentState() {
    if (machine() != null && machine().isRunning())
      return java.util.EnumSet.of(li.cil.oc.api.util.StateAware.State.IsWorking);
    return java.util.EnumSet.noneOf(li.cil.oc.api.util.StateAware.State.class);
  }

  @Override
  public java.lang.Iterable<ItemStack> internalComponents() {
    return info.components;
  }

  @Override
  public int componentSlot(String address) {
    var comps = componentEnvironments();
    for (int i = 0; i < comps.length; i++) {
      if (comps[i] != null && comps[i].node() != null && address.equals(comps[i].node().address())) return i;
    }
    return -1;
  }

  @Callback(doc = "function():boolean -- Starts the microcontroller. Returns true if the state changed.")
  public Object[] start(Context context, Arguments args) {
    return (Object[]) result(!machine().isPaused() && machine().start());
  }

  @Callback(doc = "function():boolean -- Stops the microcontroller. Returns true if the state changed.")
  public Object[] stop(Context context, Arguments args) {
    return (Object[]) result(machine().stop());
  }

  @Callback(direct = true, doc = "function():boolean -- Returns whether the microcontroller is running.")
  public Object[] isRunning(Context context, Arguments args) {
    return (Object[]) result(machine().isRunning());
  }

  @Callback(direct = true, doc = "function():string -- Returns the reason the microcontroller crashed, if applicable.")
  public Object[] lastError(Context context, Arguments args) {
    return (Object[]) result(machine().lastError());
  }

  @Callback(direct = true, doc = "function(side:number):boolean -- Get whether network messages are sent via the specified side.")
  public Object[] isSideOpen(Context context, Arguments args) {
    var side = ExtendedArguments.checkSideExcept(args, 0, facing());
    return (Object[]) result(outputSides[side.ordinal()]);
  }

  @Callback(doc = "function(side:number, open:boolean):boolean -- Set whether network messages are sent via the specified side.")
  public Object[] setSideOpen(Context context, Arguments args) {
    var side = ExtendedArguments.checkSideExcept(args, 0, facing());
    boolean oldValue = outputSides[side.ordinal()];
    outputSides[side.ordinal()] = args.checkBoolean(1);
    return (Object[]) result(oldValue);
  }

  @Override
  public void updateEntity() {
    super.updateEntity();
    if (isChangeScheduled) {
      isChangeScheduled = false;
      setChanged();
    }
    if (isServer() && level().getGameTime() % OCSettings.get().tickFrequency == 0) {
      for (var side : Direction.values()) {
        if (side != facing()) {
          if (sidedNode(side) instanceof Connector connector) {
            double demand = ((Connector) snooperNode).globalBufferSize() - ((Connector) snooperNode).globalBuffer();
            double available = demand + connector.changeBuffer(-demand);
            ((Connector) snooperNode).changeBuffer(available);
          }
        }
      }
    }
    if (isServer() && isConnected()) {
      Machine m = machine();
      if (m != null) {
        m.update();
        boolean running = m.isRunning();
        String lastError = m.lastError();
        boolean errored = lastError != null;
        if (_isRunning != running || _hasErrored != errored) {
          _isRunning = running;
          _hasErrored = errored;
          setChanged();
          PacketSender.sendComputerState(this, isRunning(), hasErrored());
        }
        updateComponents();
      }
    }
  }

  @Override
  public void connectItemNode(Node node) {
    if (machine() != null && machine().node() != null && node != null) {
      li.cil.oc.api.Network.joinNewNetwork(machine().node());
      machine().node().connect(node);
    }
  }

  @Override
  public Node createNode(Plug plug) {
    return li.cil.oc.api.Network.newNode(plug, Visibility.Network).withConnector().create();
  }

  @Override
  public void onPlugConnect(Plug plug, Node node) {
    if (node == plug.node()) {
      li.cil.oc.api.Network.joinNewNetwork(machine().node());
      machine().node().connect(snooperNode);
      connectComponents();
    }
    if (plug.isPrimary()) plug.node().connect(componentNodes[plug.side().ordinal()]);
    else componentNodes[plug.side().ordinal()].remove();
  }

  @Override
  public void onPlugDisconnect(Plug plug, Node node) {
    if (plug.isPrimary() && node != plug.node()) plug.node().connect(componentNodes[plug.side().ordinal()]);
    else componentNodes[plug.side().ordinal()].remove();
    if (node == plug.node()) disconnectComponents();
  }

  @Override
  public void onPlugMessage(Plug plug, Message message) {
    if ("network.message".equals(message.name()) && message.source().network() != snooperNode.network()) {
      snooperNode.sendToReachable(message.name(), message.data());
    }
  }

  @Override
  public void readFromNBTForServer(CompoundTag nbt) {
    var provider = getEffectiveProvider();
    if (provider != null) info.load(nbt.getCompound(OCSettings.namespace + "info"), provider);
    var plugList = nbt.getList(OCSettings.namespace + "plugs", Tag.TAG_COMPOUND);
    for (int i = 0; i < Math.min(plugList.size(), plugs.length); i++) {
      if (plugs[i] != null && plugs[i].node() != null && provider != null) {
        plugs[i].node().load(plugList.getCompound(i), provider);
      }
    }
    var tagList = nbt.getList(OCSettings.namespace + "componentNodes", Tag.TAG_COMPOUND);
    for (int i = 0; i < Math.min(tagList.size(), componentNodes.length); i++)
      if (provider != null) componentNodes[i].load(tagList.getCompound(i), provider);
    if (provider != null)
      snooperNode.load(nbt.getCompound(OCSettings.namespace + "snooper"), provider);
    super.readFromNBTForServer(nbt);
    if (nbt.contains(OCSettings.namespace + "computer")) {
      pendingMachineNbt = nbt.getCompound(OCSettings.namespace + "computer").copy();
    }
  }

  @Override
  public void writeToNBTForServer(CompoundTag nbt) {
    super.writeToNBTForServer(nbt);
    if (_machine != null) {
      var computerTag = new CompoundTag();
      _machine.save(computerTag, getEffectiveProvider());
      nbt.put(OCSettings.namespace + "computer", computerTag);
    }
    ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "info", t -> info.save(t, getEffectiveProvider()));
    byte[] outputBytes = new byte[outputSides.length];
    for (int i = 0; i < outputSides.length; i++) outputBytes[i] = (byte) (outputSides[i] ? 1 : 0);
    nbt.putByteArray(OCSettings.namespace + "outputs", outputBytes);
    var tagList = new ListTag();
    for (var n : componentNodes) {
      var tag = new CompoundTag();
      n.save(tag, getEffectiveProvider());
      tagList.add(tag);
    }
    nbt.put(OCSettings.namespace + "componentNodes", tagList);
    var plugList = new ListTag();
    for (var plug : plugs) {
      var tag = new CompoundTag();
      if (plug != null && plug.node() != null) {
        plug.node().save(tag, getEffectiveProvider());
      }
      plugList.add(tag);
    }
    nbt.put(OCSettings.namespace + "plugs", plugList);
    ExtendedNBT.setNewCompoundTag(nbt, OCSettings.namespace + "snooper", t -> snooperNode.save(t, getEffectiveProvider()));
  }

  @Override
  public void readFromNBTForClient(CompoundTag nbt) {
    var level = getLevel();
    if (level != null) info.load(nbt.getCompound("info"), level.registryAccess());
    super.readFromNBTForClient(nbt);
  }

  @Override
  public void writeToNBTForClient(CompoundTag nbt) {
    super.writeToNBTForClient(nbt);
    var level = getLevel();
    if (level != null)
      ExtendedNBT.setNewCompoundTag(nbt, "info", t -> info.save(t, level.registryAccess()));
  }

  @Override
  public ItemStack[] items() {
    return info.components.toArray(new ItemStack[0]);
  }

  @Override
  public void updateItems(int slot, ItemStack stack) {
    if (slot >= 0 && slot < info.components.size()) {
      info.components.set(slot, stack);
    }
  }

  @Override
  public int getContainerSize() {
    return info.components.size();
  }

  @Override
  public int getSizeInventory() {
    return info.components.size();
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
  public void dropSlot(int slot) {
    dropSlot(slot, 1, null);
  }

  @Override
  public void dropSlot(int slot, int count, Direction direction) {
    var stack = getItem(slot);
    if (!stack.isEmpty()) {
      if (direction != null) spawnStackInWorld(stack, direction);
      else spawnStackInWorld(stack);
    }
  }

  @Override
  public void dropAllSlots() {
    for (int i = 0; i < getContainerSize(); i++) {
      dropSlot(i);
    }
  }

  @Override
  public void clearContent() {
  }

  @Override
  public li.cil.oc.api.network.EnvironmentHost host() {
    return this;
  }

  @Override
  public li.cil.oc.api.network.ManagedEnvironment[] _components() {
    return _components;
  }

  @Override
  public void _components(li.cil.oc.api.network.ManagedEnvironment[] value) {
    _components = value;
  }

  @Override
  public boolean isSizeInventoryReady() {
    return true;
  }

  @Override
  public java.util.ArrayList<li.cil.oc.api.network.ManagedEnvironment> updatingComponents() {
    return new java.util.ArrayList<>();
  }

  @Override
  public Object[] getInterfaces(int side) {
    return new Object[0];
  }

  @Override
  public void spawnStackInWorld(ItemStack stack) {
    spawnStackInWorld(stack, null);
  }

  @Override
  public void spawnStackInWorld(ItemStack stack, Direction direction) {
    li.cil.oc.core.impl.util.InventoryUtils.spawnStackInWorld(
      li.cil.oc.core.impl.util.BlockPosition.apply(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), getLevel()),
      stack, direction, null);
  }

  @Override
  public boolean canPlaceItem(int slot, @NotNull ItemStack stack) {
    return false;
  }

  public int @NotNull [] getSlotsForFace(@NotNull Direction side) {
    return new int[0];
  }

  public boolean canPlaceItemThroughFace(int slot, @NotNull ItemStack stack, Direction side) {
    return false;
  }

  public boolean canTakeItemThroughFace(int slot, @NotNull ItemStack stack, @NotNull Direction side) {
    return false;
  }

  @Override
  public boolean hasErrored() {
    return _hasErrored;
  }

  @Override
  public void hasErrored(boolean value) {
    _hasErrored = value;
  }

  @Override
  public li.cil.oc.api.machine.Machine machine() {
    if (_machine == null && isServer() && getLevel() != null) {
      _machine = li.cil.oc.api.Machine.create(this);
      ((Connector) _machine.node()).setLocalBufferSize(0);
      _machine.setCostPerTick(OCSettings.get().microcontrollerCost);
    }
    return _machine;
  }

  @Override
  public boolean isRunning() {
    return _isRunning;
  }

  @Override
  public void setRunning(boolean value) {
    if (value != _isRunning) {
      _isRunning = value;
      if (value) _hasErrored = false;
      if (getLevel() != null) {
        BlockState current = getBlockState();
        getLevel().sendBlockUpdated(worldPosition, current, current, 3);
        getLevel().getLightEngine().checkBlock(worldPosition);
      }
    }
  }

  @Override
  public boolean isComponentSlot(int slot, ItemStack stack) {
    return true;
  }

  @Override
  public boolean canInteract(String player) {
    if (isServer() && machine() != null) {
      return machine().canInteract(player);
    }
    if (player == null) return false;
    if (!OCSettings.get().canComputersBeOwned) return true;
    if (users().isEmpty()) return true;
    return users().contains(player);
  }

  @Override
  public boolean hasRedstoneCard() {
    if (machine() != null && !machine().isRunning()) return false;
    for (int i = 0; i < getContainerSize(); i++) {
      var stack = getItem(i);
      if (!stack.isEmpty()) {
        var driver = li.cil.oc.api.API.driver.driverFor(stack, getClass());
        if (driver != null && li.cil.oc.core.impl.util.ComponentDriverHelper.isRedstoneCard(driver)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public java.util.Set<String> users() {
    return java.util.Collections.emptySet();
  }

  @Override
  public void setUsers(Iterable<String> list) {
  }

  @Override
  public ItemStack getStackInSlot(int slot) {
    if (slot >= 0 && slot < getSizeInventory()) {
      ItemStack stack = info.components.get(slot);
      return stack == null ? ItemStack.EMPTY : stack;
    }
    return ItemStack.EMPTY;
  }

  public ItemStack changeEEPROM(ItemStack newEeprom) {
    for (int i = 0; i < info.components.size(); i++) {
      var compInfo = li.cil.oc.api.Items.get(info.components.get(i));
      if (compInfo != null && Constants.ItemName.EEPROM.equals(compInfo.name())) {
        var oldEeprom = info.components.get(i);
        setItem(i, newEeprom);
        return oldEeprom;
      }
    }
    int lastSlot = getSizeInventory() - 1;
    var existing = getItem(lastSlot);
    if (!existing.isEmpty()) {
      setItem(lastSlot, newEeprom);
      return existing;
    }
    setItem(lastSlot, newEeprom);
    return null;
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
      for (var componentNode : componentNodes) {
        if (componentNode != null) componentNode.remove();
      }
      if (snooperNode != null) snooperNode.remove();
    }
    Machine m = machine();
    if (m != null) {
      if (isServer() && m.node() != null) m.node().remove();
      m.stop();
    }
  }
}
