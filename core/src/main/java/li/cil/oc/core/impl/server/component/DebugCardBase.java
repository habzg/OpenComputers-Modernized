package li.cil.oc.core.impl.server.component;

import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Packet;
import li.cil.oc.api.network.SidedEnvironment;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.impl.util.ExtendedNBT;
import li.cil.oc.core.impl.util.FluidUtils;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.Log;
import li.cil.oc.core.server.network.DebugNetwork;
import li.cil.oc.core.util.FluidHandler;
import li.cil.oc.core.util.FluidStack;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ServerLevelData;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreAccess;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

public abstract class DebugCardBase extends AbstractManagedEnvironment implements DebugNetwork.DebugNode {
    public final Node node;
    public AccessContext access = null;
    private Node remoteNode = null;
    private int[] remoteNodePosition = null;

    protected abstract Object[] platformScanContentsAt(Level world, int x, int y, int z);

    protected abstract boolean platformIsModLoaded(String name);

    @SuppressWarnings("unused")
    protected abstract Object[] platformRunCommand(Context context, Arguments args);

    @SuppressWarnings("unused")
    protected abstract Object[] platformGetPlayers(Context context);

    protected abstract boolean platformSendToClipboard(String playerName, String text);

    public DebugCardBase(EnvironmentHost host) {
        super(host);
        this.node = Network.newNode(this, Visibility.Neighbors)
                .withComponent("debug")
                .withConnector()
                .create();
    }

    private static void checkAccess(AccessContext ctx) {
        String msg = OCSettings.get().debugCardAccess.checkAccess(ctx);
        if (msg != null) throw new RuntimeException(msg);
    }

    private void checkAccess() {
        checkAccess(access);
    }

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(((Connector) node).changeBuffer(args.checkDouble(0)));
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(host().xPosition());
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(host().yPosition());
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(host().zPosition());
    }

    @Callback(doc = "function():userdata -- Get the world object of the container.")
    public Object[] getWorld(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(new WorldValue(host().level()));
    }

    @Callback(doc = "function(name:string):userdata -- Get the entity of a player.")
    public Object[] getPlayer(Context context, Arguments args) {
        checkAccess();
        String name = args.checkString(0);
        return ResultWrapper.result(createPlayerValue(name));
    }

    protected abstract PlayerValue createPlayerValue(String name);

    @Callback(doc = "function():table -- Get a list of currently logged-in players.")
    public Object[] getPlayers(Context context, Arguments args) {
        checkAccess();
        return platformGetPlayers(context);
    }

    @Callback(doc = "function():userdata -- Get the scoreboard object for the world")
    public Object[] getScoreboard(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(new ScoreboardValue(host().level()));
    }

    @Callback(doc = "function(x: number, y: number, z: number):boolean, string, table -- returns contents at the location in the host world")
    public Object[] scanContentsAt(Context context, Arguments args) {
        checkAccess();
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        return platformScanContentsAt(host().level(), x, y, z);
    }

    @Callback(doc = "function(name:string):boolean -- Get whether a mod or API is loaded.")
    public Object[] isModLoaded(Context context, Arguments args) {
        checkAccess();
        return ResultWrapper.result(platformIsModLoaded(args.checkString(0)));
    }

    @Callback(doc = "function(command:string):number -- Runs an arbitrary command using a fake player.")
    public Object[] runCommand(Context context, Arguments args) {
        checkAccess();
        return platformRunCommand(context, args);
    }

    @Callback(doc = "function(x:number, y:number, z:number):boolean -- Add a component block at the specified coordinates to the computer network.")
    public Object[] connectToBlock(Context context, Arguments args) {
        checkAccess();
        int x = args.checkInteger(0);
        int y = args.checkInteger(1);
        int z = args.checkInteger(2);
        Node other = findNode(x, y, z);
        if (other == null) {
            return ResultWrapper.result(null, "no node found at this position");
        }
        if (remoteNode != null) node.disconnect(remoteNode);
        remoteNode = other;
        remoteNodePosition = new int[]{x, y, z};
        node.connect(other);
        return ResultWrapper.result(true);
    }

    private Node findNode(int x, int y, int z) {
        var debugPos = new net.minecraft.core.BlockPos(x, y, z);
        if (host().level().hasChunk(debugPos.getX() >> 4, debugPos.getZ() >> 4)) {
            BlockEntity te = host().level().getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
            if (te instanceof SidedEnvironment) {
                for (Direction dir : Direction.values()) {
                    Node n = ((SidedEnvironment) te).sidedNode(dir);
                    if (n != null) return n;
                }
            } else if (te instanceof Environment) {
                return ((Environment) te).node();
            }
        }
        return null;
    }

    @Callback(doc = "function():userdata -- Test method for user-data and general value conversion.")
    public Object[] test(Context context, Arguments args) {
        checkAccess();
        Map<Object, Object> v1 = new HashMap<>();
        v1.put("a", true);
        v1.put("b", "test");
        Map<Object, Object> v2 = new HashMap<>();
        v2.put(10, "zxc");
        v2.put(false, v1);
        v1.put("c", v2);
        return ResultWrapper.result(v2, new TestValue(), host().level());
    }

    @Callback(doc = "function(player:string, text:string) -- Sends text to the specified player's clipboard if possible.")
    public Object[] sendToClipboard(Context context, Arguments args) {
        checkAccess();
        String playerName = args.checkString(0);
        String text = args.checkString(1);
        if (platformSendToClipboard(playerName, text)) {
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(false, "no such player");
    }

    @Callback(doc = "function(address:string, data...) -- Sends data to the debug card with the specified address.")
    public Object[] sendToDebugCard(Context context, Arguments args) {
        checkAccess();
        String destination = args.checkString(0);
        DebugNetwork.DebugNode endpoint = DebugNetwork.getEndpoint(destination);
        if (endpoint != null && endpoint != this) {
            Object[] data = new Object[args.count() - 1];
            for (int i = 1; i < args.count(); i++) data[i - 1] = args.checkAny(i);
            Packet packet = Network.newPacket(node.address(), destination, 0, data);
            endpoint.receivePacket(packet);
        }
        return ResultWrapper.result();
    }

    @Override
    public void receivePacket(Packet packet) {
        Object[] data = packet.data();
        int dataLen = (data != null) ? data.length : 0;
        Object[] signal = new Object[4 + dataLen];
        signal[0] = "debug_message";
        signal[1] = packet.source();
        signal[2] = packet.port();
        signal[3] = 0.0;
        if (dataLen > 0) {
            System.arraycopy(data, 0, signal, 4, dataLen);
        }
        node.sendToReachable("computer.signal", signal);
    }

    @Override
    public String address() {
        return node != null ? node.address() : "debug";
    }

    @Override
    public void onConnect(Node node) {
        super.onConnect(node);
        if (node == this.node) {
            DebugNetwork.add(this);
            if (remoteNodePosition != null) {
                remoteNode = findNode(remoteNodePosition[0], remoteNodePosition[1], remoteNodePosition[2]);
                this.node.connect(remoteNode);
            }
        }
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            DebugNetwork.remove(this);
            if (remoteNode != null) {
                remoteNode.disconnect(node);
            }
        } else if (node == remoteNode) {
            remoteNode = null;
            remoteNodePosition = null;
        }
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        access = AccessContext.load(nbt);
        if (nbt.contains(OCSettings.namespace + "remoteX")) {
            int rx = nbt.getInt(OCSettings.namespace + "remoteX");
            int ry = nbt.getInt(OCSettings.namespace + "remoteY");
            int rz = nbt.getInt(OCSettings.namespace + "remoteZ");
            remoteNodePosition = new int[]{rx, ry, rz};
        }
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (access != null) access.save(nbt);
        if (remoteNodePosition != null) {
            nbt.putInt(OCSettings.namespace + "remoteX", remoteNodePosition[0]);
            nbt.putInt(OCSettings.namespace + "remoteY", remoteNodePosition[1]);
            nbt.putInt(OCSettings.namespace + "remoteZ", remoteNodePosition[2]);
        }
    }

    public static class AccessContext extends li.cil.oc.core.impl.util.AccessContext {
        public AccessContext(String player, String nonce) {
            super(player, nonce);
        }

        public static AccessContext load(CompoundTag nbt) {
            li.cil.oc.core.impl.util.AccessContext ctx = li.cil.oc.core.impl.util.AccessContext.load(nbt);
            return ctx != null ? new AccessContext(ctx.player(), ctx.nonce()) : null;
        }

        public static void remove(CompoundTag nbt) {
            li.cil.oc.core.impl.util.AccessContext.remove(nbt);
        }
    }

    public static class TestValue extends AbstractValue {
        public String value = "hello";

        @Override
        public Object apply(Context context, Arguments arguments) {
            Log.get().info("TestValue.apply({})", java.util.Arrays.toString(arguments.toArray()));
            return value;
        }

        @Override
        public void unapply(Context context, Arguments arguments) {
            Log.get().info("TestValue.unapply({})", java.util.Arrays.toString(arguments.toArray()));
            value = arguments.checkString(1);
        }

        @Override
        public Object[] call(Context context, Arguments arguments) {
            Log.get().info("TestValue.call({})", java.util.Arrays.toString(arguments.toArray()));
            return arguments.toArray();
        }

        @Override
        public void dispose(Context context) {
            super.dispose(context);
            Log.get().info("TestValue.dispose()");
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            value = nbt.getString("value");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putString("value", value);
        }
    }

    public class WorldValue extends AbstractValue {
        public final Level world;

        @SuppressWarnings("unused")
        public WorldValue() {
            this(null);
        }

        public WorldValue(Level world) {
            this.world = world;
        }

        @Callback(doc = "function():string -- Gets the name of the current dimension.")
        public Object[] getDimensionName(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result(world.dimension().location().toString());
        }

        @Callback(doc = "function():number -- Gets the seed of the world.")
        public Object[] getSeed(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result(((ServerLevel) world).getSeed());
        }

        @Callback(doc = "function():boolean -- Returns whether it is currently raining.")
        public Object[] isRaining(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result(world.isRaining());
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(value:boolean) -- Sets whether it is currently raining.")
        public Object[] setRaining(Context context, Arguments args) {
            checkAccess();
            world.getLevelData().setRaining(args.checkBoolean(0));
            return null;
        }

        @Callback(doc = "function():boolean -- Returns whether it is currently thundering.")
        public Object[] isThundering(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result(world.isThundering());
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(value:boolean) -- Sets whether it is currently thundering.")
        public Object[] setThundering(Context context, Arguments args) {
            checkAccess();
            ((ServerLevelData) world.getLevelData()).setThundering(args.checkBoolean(0));
            return null;
        }

        @Callback(doc = "function():number -- Get the current world time.")
        public Object[] getTime(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result((double) world.getGameTime());
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(value:number) -- Set the current world time.")
        public Object[] setTime(Context context, Arguments args) {
            checkAccess();
            ((ServerLevel) world).setDayTime((long) args.checkDouble(0));
            return null;
        }

        @Callback(doc = "function():number, number, number -- Get the current spawn point coordinates.")
        public Object[] getSpawnPoint(Context context, Arguments args) {
            checkAccess();
            var spawn = world.getLevelData().getSpawnPos();
            return ResultWrapper.result(
                    (double) spawn.getX(),
                    (double) spawn.getY(),
                    (double) spawn.getZ());
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(x:number, y:number, z:number) -- Set the spawn point coordinates.")
        public Object[] setSpawnPoint(Context context, Arguments args) {
            checkAccess();
            ((ServerLevel) world).setDefaultSpawnPos(new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), 0);
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(x:number, y:number, z:number, sound:string, range:number) -- Play a sound at the specified coordinates.")
        public Object[] playSoundAt(Context context, Arguments args) {
            checkAccess();
            int x = args.checkInteger(0), y = args.checkInteger(1), z = args.checkInteger(2);
            String soundName = args.checkString(3);
            int range = args.checkInteger(4);
            var soundLocation = ResourceLocation.tryParse(soundName);
            if (soundLocation == null) {
                soundLocation = ResourceLocation.withDefaultNamespace(soundName);
            }
            var soundEvent = BuiltInRegistries.SOUND_EVENT.get(soundLocation);
            if (soundEvent != null) {
                world.playSound(null, x, y, z, soundEvent, net.minecraft.sounds.SoundSource.MASTER, range, 1.0f);
            }
            return null;
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the ID of the block at the specified coordinates.")
        public Object[] getBlockId(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result((double) Block.getId(world.getBlockState(new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)))));
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Gets the block state for the block at the specified position.")
        public Object[] getBlockState(Context context, Arguments args) {
            checkAccess();
            BlockPos pos = new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            BlockState state = world.getBlockState(pos);
            return ResultWrapper.result(state);
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Check whether the block at the specified coordinates is loaded.")
        public Object[] isLoaded(Context context, Arguments args) {
            checkAccess();
            var isLoadedPos = new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            return ResultWrapper.result(world.hasChunk(isLoadedPos.getX() >> 4, isLoadedPos.getZ() >> 4));
        }

        @Callback(doc = "function(x:number, y:number, z:number):boolean -- Check whether the block at the specified coordinates has a block entity.")
        public Object[] hasTileEntity(Context context, Arguments args) {
            checkAccess();
            BlockPos blockPos = new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            BlockState state = world.getBlockState(blockPos);
            return ResultWrapper.result(state.hasBlockEntity());
        }

        @Callback(doc = "function(x:number, y:number, z:number):table -- Get the NBT of the block at the specified coordinates.")
        public Object[] getTileNBT(Context context, Arguments args) {
            checkAccess();
            int x = args.checkInteger(0), y = args.checkInteger(1), z = args.checkInteger(2);
            BlockEntity te = world.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
            if (te != null) {
                CompoundTag nbt;
                nbt = te.saveWithFullMetadata(world.registryAccess());
                return ResultWrapper.result(ExtendedNBT.toTypedMap(nbt));
            }
            return null;
        }

        @Callback(doc = "function(x:number, y:number, z:number, nbt:table):boolean -- Set the NBT of the block at the specified coordinates.")
        public Object[] setTileNBT(Context context, Arguments args) {
            checkAccess();
            int x = args.checkInteger(0), y = args.checkInteger(1), z = args.checkInteger(2);
            BlockEntity te = world.getBlockEntity(new net.minecraft.core.BlockPos(x, y, z));
            if (te != null) {
                Map<?, ?> table = args.checkTable(3);
                Tag converted = ExtendedNBT.typedMapToNbt(table);
                if (converted instanceof CompoundTag compoundNbt) {
                    te.loadWithComponents(compoundNbt, world.registryAccess());
                    te.setChanged();
                    world.sendBlockUpdated(new net.minecraft.core.BlockPos(x, y, z), world.getBlockState(new net.minecraft.core.BlockPos(x, y, z)), world.getBlockState(new net.minecraft.core.BlockPos(x, y, z)), 3);
                    return ResultWrapper.result(true);
                }
                return ResultWrapper.result(null, "invalid nbt");
            }
            return ResultWrapper.result(null, "no block entity");
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light opacity of the block at the specified coordinates.")
        public Object[] getLightOpacity(Context context, Arguments args) {
            checkAccess();
            BlockPos pos = new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            return ResultWrapper.result((double) world.getBlockState(pos).getLightBlock(world, pos));
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get the light value (emission) of the block at the specified coordinates.")
        public Object[] getLightValue(Context context, Arguments args) {
            checkAccess();
            BlockPos pos = new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2));
            return ResultWrapper.result((double) world.getBrightness(LightLayer.BLOCK, pos));
        }

        @Callback(doc = "function(x:number, y:number, z:number):number -- Get whether the block at the specified coordinates is directly under the sky.")
        public Object[] canSeeSky(Context context, Arguments args) {
            checkAccess();
            return ResultWrapper.result(world.canSeeSky(new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2))));
        }

        @Callback(doc = "function(x:number, y:number, z:number, id:number or string):number -- Set the block at the specified coordinates.")
        public Object[] setBlock(Context context, Arguments args) {
            checkAccess();
            BlockState state;
            if (args.isInteger(3)) {
                state = Block.stateById(args.checkInteger(3));
            } else {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(args.checkString(3)));
                state = block.defaultBlockState();
            }
            return ResultWrapper.result(world.setBlock(new net.minecraft.core.BlockPos(args.checkInteger(0), args.checkInteger(1), args.checkInteger(2)), state, 3));
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(x1:number, y1:number, z1:number, x2:number, y2:number, z2:number, id:number or string):number -- Set all blocks in the area defined by the two corner points.")
        public Object[] setBlocks(Context context, Arguments args) {
            checkAccess();
            int xMin = args.checkInteger(0), yMin = args.checkInteger(1), zMin = args.checkInteger(2);
            int xMax = args.checkInteger(3), yMax = args.checkInteger(4), zMax = args.checkInteger(5);
            BlockState state;
            if (args.isInteger(6)) {
                state = Block.stateById(args.checkInteger(6));
            } else {
                Block block = BuiltInRegistries.BLOCK.get(ResourceLocation.parse(args.checkString(6)));
                state = block.defaultBlockState();
            }
            int minX = Math.min(xMin, xMax), maxX = Math.max(xMin, xMax);
            int minY = Math.min(yMin, yMax), maxY = Math.max(yMin, yMax);
            int minZ = Math.min(zMin, zMax), maxZ = Math.max(zMin, zMax);
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        world.setBlock(new net.minecraft.core.BlockPos(x, y, z), state, 3);
                    }
                }
            }
            return null;
        }

        @Callback(doc = "function(id:string, count:number, damage:number, nbt:string, x:number, y:number, z:number, side:number):boolean - Insert an item stack into the inventory at the specified location. NBT tag is expected in JSON format.")
        public Object[] insertItem(Context context, Arguments args) {
            checkAccess();
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(args.checkString(0)));
            if (item == BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                throw new IllegalArgumentException("invalid item id");
            }
            int count = args.checkInteger(1);
            int damage = args.checkInteger(2);
            String tagJson = args.optString(3, "");
            CompoundTag tag = null;
            if (!Strings.isNullOrEmpty(tagJson)) {
                try {
                    tag = TagParser.parseTag(tagJson);
                } catch (Exception e) {
                    throw new IllegalArgumentException("invalid nbt tag");
                }
            }
            BlockPosition position = BlockPosition.apply(args.checkDouble(4), args.checkDouble(5), args.checkDouble(6), world);
            ExtendedArguments.checkSideAny(args, 7);
            Container inventory = InventoryUtils.inventoryAt(position);
            if (inventory != null) {
                ItemStack stack = new ItemStack(item, count);
                if (damage > 0) stack.setDamageValue(damage);
                if (tag != null) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return ResultWrapper.result(InventoryUtils.insertIntoInventory(stack, inventory, null));
            }
            return ResultWrapper.result(null, "no inventory");
        }

        @Callback(doc = "function(x:number, y:number, z:number, slot:number[, count:number]):number - Reduce the size of an item stack in the inventory at the specified location.")
        public Object[] removeItem(Context context, Arguments args) {
            checkAccess();
            BlockPosition position = BlockPosition.apply(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2), world);
            Container inventory = InventoryUtils.inventoryAt(position);
            if (inventory != null) {
                int slot = ExtendedArguments.checkSlot(args, inventory, 3);
                int count = args.optInteger(4, 64);
                ItemStack removed = inventory.removeItem(slot, count);
                if (removed.isEmpty()) return ResultWrapper.result(0);
                return ResultWrapper.result(removed.getCount());
            }
            return ResultWrapper.result(null, "no inventory");
        }

        @Callback(doc = "function(id:string, amount:number, x:number, y:number, z:number, side:number):boolean - Insert some fluid into the tank at the specified location.")
        public Object[] insertFluid(Context context, Arguments args) {
            checkAccess();
            String fluidId = args.checkString(0);
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidId));
            if (fluid == Fluids.EMPTY) {
                throw new IllegalArgumentException("invalid fluid id");
            }
            int amount = args.checkInteger(1);
            BlockPosition position = BlockPosition.apply(args.checkDouble(2), args.checkDouble(3), args.checkDouble(4), world);
            ExtendedArguments.checkSideAny(args, 5);
            FluidHandler handler = FluidUtils.fluidHandlerAt(position);
            if (handler != null) {
                return ResultWrapper.result(handler.fill(new FluidStack(BuiltInRegistries.FLUID.getKey(fluid).toString(), amount), false));
            }
            return ResultWrapper.result(null, "no tank");
        }

        @Callback(doc = "function(amount:number, x:number, y:number, z:number, side:number):boolean - Remove some fluid from a tank at the specified location.")
        public Object[] removeFluid(Context context, Arguments args) {
            checkAccess();
            int amount = args.checkInteger(0);
            BlockPosition position = BlockPosition.apply(args.checkDouble(1), args.checkDouble(2), args.checkDouble(3), world);
            ExtendedArguments.checkSideAny(args, 4);
            FluidHandler handler = FluidUtils.fluidHandlerAt(position);
            if (handler != null) {
                return ResultWrapper.result(handler.drain(amount, false));
            }
            return ResultWrapper.result(null, "no tank");
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putInt("dimension", world.dimension().location().hashCode());
        }
    }

    public abstract class PlayerValue extends AbstractValue {
        public String name;

        public PlayerValue() {
            this(null);
        }

        public PlayerValue(String name) {
            this.name = name;
        }

        protected abstract ServerPlayer getServerPlayer(String name);

        private Object[] withPlayer(java.util.function.Function<ServerPlayer, Object[]> f) {
            checkAccess();
            ServerPlayer player = getServerPlayer(name);
            if (player == null) return ResultWrapper.result(null, "player is offline");
            return f.apply(player);
        }

        @Callback(doc = "function():userdata -- Get the player's world object.")
        public Object[] getWorld(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result(new WorldValue(p.serverLevel())));
        }

        @Callback(doc = "function():string -- Get the player's game type.")
        public Object[] getGameType(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result(p.gameMode.getGameModeForPlayer().getName()));
        }

        @Callback(doc = "function(gametype:string) -- Set the player's game type (survival, creative, adventure).")
        public Object[] setGameType(Context context, Arguments args) {
            return withPlayer(p -> {
                String gametype = args.checkString(0);
                net.minecraft.world.level.GameType found = net.minecraft.world.level.GameType.SURVIVAL;
                for (net.minecraft.world.level.GameType gt : net.minecraft.world.level.GameType.values()) {
                    if (gt.getName().equals(gametype)) {
                        found = gt;
                        break;
                    }
                }
                p.setGameMode(found);
                return null;
            });
        }

        @Callback(doc = "function():number, number, number -- Get the player's position.")
        public Object[] getPosition(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result(p.getX(), p.getY(), p.getZ()));
        }

        @Callback(doc = "function(x:number, y:number, z:number) -- Set the player's position.")
        public Object[] setPosition(Context context, Arguments args) {
            return withPlayer(p -> {
                p.teleportTo(args.checkDouble(0), args.checkDouble(1), args.checkDouble(2));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's health.")
        public Object[] getHealth(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result((double) p.getHealth()));
        }

        @Callback(doc = "function():number -- Get the player's max health.")
        public Object[] getMaxHealth(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result((double) p.getMaxHealth()));
        }

        @Callback(doc = "function(health:number) -- Set the player's health.")
        public Object[] setHealth(Context context, Arguments args) {
            return withPlayer(p -> {
                p.setHealth((float) args.checkDouble(0));
                return null;
            });
        }

        @Callback(doc = "function():number -- Get the player's level")
        public Object[] getLevel(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result((double) p.experienceLevel));
        }

        @Callback(doc = "function():number -- Get the player's total experience")
        public Object[] getExperienceTotal(Context context, Arguments args) {
            return withPlayer(p -> ResultWrapper.result((double) p.totalExperience));
        }

        @Callback(doc = "function(level:number) -- Add a level to the player's experience level")
        public Object[] addExperienceLevel(Context context, Arguments args) {
            return withPlayer(p -> {
                p.giveExperienceLevels(args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function(level:number) -- Remove a level from the player's experience level")
        public Object[] removeExperienceLevel(Context context, Arguments args) {
            return withPlayer(p -> {
                p.giveExperienceLevels(-args.checkInteger(0));
                return null;
            });
        }

        @Callback(doc = "function() -- Clear the players inventory")
        public Object[] clearInventory(Context context, Arguments args) {
            return withPlayer(p -> {
                p.getInventory().clearContent();
                return null;
            });
        }

        @Callback(doc = "function(id:string, amount:number, meta:number[, nbt:string]):number -- Adds the item stack to the players inventory")
        public Object[] insertItem(Context context, Arguments args) {
            return withPlayer(p -> {
                Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(args.checkString(0)));
                if (item == BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace("air"))) {
                    throw new IllegalArgumentException("invalid item id");
                }
                int amount = args.checkInteger(1);
                int damage = args.checkInteger(2);
                String tagJson = args.checkString(3);
                CompoundTag tag = null;
                if (!Strings.isNullOrEmpty(tagJson)) {
                    try {
                        tag = TagParser.parseTag(tagJson);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("invalid nbt tag");
                    }
                }
                ItemStack stack = new ItemStack(item, amount);
                if (damage > 0) stack.setDamageValue(damage);
                if (tag != null) stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                int before = stack.getCount();
                InventoryUtils.addToPlayerInventory(stack, p);
                return ResultWrapper.result(before - stack.getCount());
            });
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            name = nbt.getString("name");
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putString("name", name);
        }
    }

    public class ScoreboardValue extends AbstractValue {
        public final Scoreboard scoreboard;

        @SuppressWarnings("unused")
        public ScoreboardValue() {
            this.scoreboard = null;
        }

        public ScoreboardValue(Level world) {
            this.scoreboard = world != null ? world.getScoreboard() : null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(team:string) - Add a team to the scoreboard")
        public Object[] addTeam(Context context, Arguments args) {
            checkAccess();
            String team = args.checkString(0);
            scoreboard.addPlayerTeam(team);
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(teamName: string) - Remove a team from the scoreboard")
        public Object[] removeTeam(Context context, Arguments args) {
            checkAccess();
            String teamName = args.checkString(0);
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team != null) {
                scoreboard.removePlayerTeam(team);
            }
            return null;
        }

        @Callback(doc = "function(player:string, team:string):boolean - Add a player to a team")
        public Object[] addPlayerToTeam(Context context, Arguments args) {
            checkAccess();
            String player = args.checkString(0);
            String team = args.checkString(1);
            PlayerTeam playerTeam = scoreboard.getPlayerTeam(team);
            if (playerTeam == null) return ResultWrapper.result(false);
            return ResultWrapper.result(scoreboard.addPlayerToTeam(player, playerTeam));
        }

        @Callback(doc = "function(player:string):boolean - Remove a player from their team")
        public Object[] removePlayerFromTeams(Context context, Arguments args) {
            checkAccess();
            String player = args.checkString(0);
            return ResultWrapper.result(scoreboard.removePlayerFromTeam(player));
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(player:string, team:string):boolean - Remove a player from a specific team")
        public Object[] removePlayerFromTeam(Context context, Arguments args) {
            checkAccess();
            String player = args.checkString(0);
            String teamName = args.checkString(1);
            PlayerTeam team = scoreboard.getPlayerTeam(teamName);
            if (team != null) {
                scoreboard.removePlayerFromTeam(player, team);
            }
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(objectiveName:string, objectiveCriteria:string) - Create a new objective for the scoreboard")
        public Object[] addObjective(Context context, Arguments args) {
            checkAccess();
            String objName = args.checkString(0);
            String objType = args.checkString(1);
            ObjectiveCriteria criteria = ObjectiveCriteria.byName(objType).orElse(ObjectiveCriteria.DUMMY);
            scoreboard.addObjective(objName, criteria, Component.literal(objName), ObjectiveCriteria.RenderType.INTEGER, false, null);
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(objectiveName:string) - Remove an objective from the scoreboard")
        public Object[] removeObjective(Context context, Arguments args) {
            checkAccess();
            String objName = args.checkString(0);
            Objective objective = scoreboard.getObjective(objName);
            if (objective != null) {
                scoreboard.removeObjective(objective);
            }
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Sets the score of a player for a certain objective")
        public Object[] setPlayerScore(Context context, Arguments args) {
            checkAccess();
            String name = args.checkString(0);
            Objective objective = scoreboard.getObjective(args.checkString(1));
            if (objective == null) throw new RuntimeException("objective not found");
            int scoreVal = args.checkInteger(2);
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective);
            score.set(scoreVal);
            return null;
        }

        @Callback(doc = "function(playerName:string, objectiveName:string):int - Gets the score of a player for a certain objective")
        public Object[] getPlayerScore(Context context, Arguments args) {
            checkAccess();
            String name = args.checkString(0);
            Objective objective = scoreboard.getObjective(args.checkString(1));
            if (objective == null) throw new RuntimeException("objective not found");
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective);
            return ResultWrapper.result((double) score.get());
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Increases the score of a player for a certain objective")
        public Object[] increasePlayerScore(Context context, Arguments args) {
            checkAccess();
            String name = args.checkString(0);
            Objective objective = scoreboard.getObjective(args.checkString(1));
            if (objective == null) throw new RuntimeException("objective not found");
            int scoreVal = args.checkInteger(2);
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective);
            score.add(scoreVal);
            return null;
        }

        @SuppressWarnings("SameReturnValue")
        @Callback(doc = "function(playerName:string, objectiveName:string, score:int) - Decrease the score of a player for a certain objective")
        public Object[] decreasePlayerScore(Context context, Arguments args) {
            checkAccess();
            String name = args.checkString(0);
            Objective objective = scoreboard.getObjective(args.checkString(1));
            if (objective == null) throw new RuntimeException("objective not found");
            int scoreVal = args.checkInteger(2);
            ScoreAccess score = scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly(name), objective);
            score.add(-scoreVal);
            return null;
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
        }
    }

    public interface CommandSenderBase {
        @SuppressWarnings("unused")
        void prepare();

        @SuppressWarnings("unused")
        void sendSystemMessage(net.minecraft.network.chat.Component message);
    }
}
