package li.cil.oc.fabric.integration.refinedstorage2;

import com.refinedmods.refinedstorage.api.autocrafting.status.TaskStatus;
import com.refinedmods.refinedstorage.api.autocrafting.task.TaskState;
import com.refinedmods.refinedstorage.api.network.Network;
import com.refinedmods.refinedstorage.api.network.autocrafting.AutocraftingNetworkComponent;
import com.refinedmods.refinedstorage.api.network.energy.EnergyNetworkComponent;
import com.refinedmods.refinedstorage.api.network.impl.node.AbstractNetworkNode;
import com.refinedmods.refinedstorage.api.network.node.GraphNetworkComponent;
import com.refinedmods.refinedstorage.api.network.node.NetworkNodeActor;
import com.refinedmods.refinedstorage.api.network.storage.StorageNetworkComponent;
import com.refinedmods.refinedstorage.api.resource.ResourceAmount;
import com.refinedmods.refinedstorage.api.storage.Actor;
import com.refinedmods.refinedstorage.common.api.RefinedStorageApi;
import com.refinedmods.refinedstorage.common.api.support.network.InWorldNetworkNodeContainer;
import com.refinedmods.refinedstorage.common.support.resource.FluidResource;
import com.refinedmods.refinedstorage.common.support.resource.ItemResource;
import com.refinedmods.refinedstorage.common.support.resource.ResourceCodecs;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface NetworkControl extends Persistable, ManagedEnvironment {
    BlockEntity tile();

    @SuppressWarnings("EmptyMethod")
    Node node();

    default Network network() {
        return RS2Util.networkOf(tile());
    }

    default StorageNetworkComponent storage() {
        var network = network();
        return network != null ? network.getComponent(StorageNetworkComponent.class) : null;
    }

    default AutocraftingNetworkComponent autocrafting() {
        var network = network();
        return network != null ? network.getComponent(AutocraftingNetworkComponent.class) : null;
    }

    default HashMap<Object, Object> convert(ItemResource itemResource, long size, boolean isCraftable, AutocraftingNetworkComponent crafting) {
        var hash = new HashMap<>();
        if (itemResource == null) return hash;
        long potentialAmount = size;
        if (size <= 0 && isCraftable && crafting != null) {
            potentialAmount = getPatternOutputAmount(itemResource, crafting);
        }
        var itemStack = itemResource.toItemStack(Math.clamp(potentialAmount, 1, Integer.MAX_VALUE));
        var converted = Registry.INSTANCE.convertRecursively(itemStack, new java.util.IdentityHashMap<>());
        if (converted instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                if (entry.getKey() instanceof String s) {
                    hash.put(s, entry.getValue());
                }
            }
        }
        hash.put("isCraftable", isCraftable);
        hash.put("size", size);
        return hash;
    }

    static long getPatternOutputAmount(ItemResource itemResource, AutocraftingNetworkComponent crafting) {
        for (var pattern : crafting.getPatternsByOutput(itemResource)) {
            for (var output : pattern.layout().outputs()) {
                if (itemResource.equals(output.resource())) {
                    return output.amount();
                }
            }
        }
        return 0;
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of the stored items in the network.")
    default Object[] getItemsInNetwork(Context context, Arguments args) {
        var filter = parseFilter(args);
        var result = new ArrayList<>();
        var storage = storage();
        var crafting = autocrafting();
        if (storage == null) return ResultWrapper.result((Object) result.toArray());
        for (var resourceAmount : storage.getAll()) {
            if (!(resourceAmount.resource() instanceof ItemResource itemResource)) continue;
            boolean isCraftable = crafting != null && !crafting.getPatternsByOutput(itemResource).isEmpty();
            var converted = convert(itemResource, resourceAmount.amount(), isCraftable, crafting);
            if (matches(converted, filter)) {
                result.add(converted);
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    @Callback(doc = "function():table -- Get a list of the stored fluids in the network.")
    default Object[] getFluidsInNetwork(Context context, Arguments args) {
        var result = new ArrayList<>();
        var storage = storage();
        if (storage == null) return ResultWrapper.result((Object) result.toArray());
        for (var resourceAmount : storage.getAll()) {
            if (resourceAmount.resource() instanceof FluidResource fluidResource) {
                var fluid = fluidResource.fluid();
                result.add(new li.cil.oc.core.util.FluidStack(
                        net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid).toString(),
                        Math.clamp(resourceAmount.amount() / 81, 1, Integer.MAX_VALUE)));
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.")
    default Object[] getCraftables(Context context, Arguments args) {
        var filter = parseFilter(args);
        var builder = new ArrayList<>();
        var crafting = autocrafting();
        if (crafting == null) return ResultWrapper.result((Object) builder.toArray());
        for (var key : crafting.getOutputs()) {
            if (!(key instanceof ItemResource itemResource)) continue;
            long patternAmount = getPatternOutputAmount(itemResource, crafting);
            var converted = convert(itemResource, 0, true, crafting);
            if (filter.isEmpty() || matches(converted, filter)) {
                builder.add(new NetworkControl.Craftable(tile(), itemResource, patternAmount));
            }
        }
        return ResultWrapper.result((Object) builder.toArray());
    }

    @Callback(doc = "function([filter:table, dbAddress:string, startSlot:number, count:number]): bool -- Store items in the network matching the specified filter in the database with the specified address.")
    default Object[] store(Context context, Arguments args) {
        var filter = parseFilter(args);
        var database = args.optString(1, null);
        var db = database != null
                ? DatabaseAccess.database(node(), database)
                : DatabaseAccess.databases(node()).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no database upgrade found"));
        var storage = storage();
        var crafting = autocrafting();
        if (storage == null) return ResultWrapper.result(false);
        var items = new ArrayList<ResourceAmount>();
        for (var resourceAmount : storage.getAll()) {
            if (!(resourceAmount.resource() instanceof ItemResource itemResource)) continue;
            boolean isCraftable = crafting != null && !crafting.getPatternsByOutput(itemResource).isEmpty();
            var converted = convert(itemResource, resourceAmount.amount(), isCraftable, crafting);
            if (matches(converted, filter)) {
                long potentialAmount = resourceAmount.amount();
                if (potentialAmount <= 0 && isCraftable) {
                    potentialAmount = getPatternOutputAmount(itemResource, crafting);
                }
                items.add(new ResourceAmount(itemResource, potentialAmount));
            }
        }
        var offset = Math.max(0, args.optInteger(2, 1) - 1);
        int count = args.optInteger(3, Integer.MAX_VALUE);
        count = Math.clamp(db.size() - offset, 0, count);
        count = Math.min(count, items.size());
        int slot = offset;
        for (int i = 0; i < count; i++) {
            var amount = items.get(i);
            if (!(amount.resource() instanceof ItemResource itemResource)) continue;
            var itemStack = itemResource.toItemStack((int) Math.min(amount.amount(), Integer.MAX_VALUE));
            if (itemStack.isEmpty()) continue;
            while (slot < db.size() && !db.getStackInSlot(slot).isEmpty()) slot++;
            if (slot >= db.size()) break;
            if (db.getStackInSlot(slot).isEmpty()) {
                db.setStackInSlot(slot, itemStack);
            }
        }
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function():number -- Get the average power usage of the network.")
    default Object[] getAvgPowerUsage(Context context, Arguments args) {
        return ResultWrapper.result((double) totalEnergyUsage());
    }

    default long totalEnergyUsage() {
        var network = network();
        if (network == null) return 0;
        long usage = 0;
        var graph = network.getComponent(GraphNetworkComponent.class);
        for (var container : graph.getContainers(InWorldNetworkNodeContainer.class)) {
            if (container.getNode() instanceof AbstractNetworkNode node) {
                usage += node.getEnergyUsage();
            }
        }
        return usage;
    }

    @Callback(doc = "function():number -- Get the maximum stored power in the network.")
    default Object[] getMaxStoredPower(Context context, Arguments args) {
        var network = network();
        if (network == null) return ResultWrapper.result(0.0);
        var energy = network.getComponent(EnergyNetworkComponent.class);
        return ResultWrapper.result((double) energy.getCapacity());
    }

    @Callback(doc = "function():number -- Get the stored power in the network.")
    default Object[] getStoredPower(Context context, Arguments args) {
        var network = network();
        if (network == null) return ResultWrapper.result(0.0);
        var energy = network.getComponent(EnergyNetworkComponent.class);
        return ResultWrapper.result((double) energy.getStored());
    }

    @Callback(doc = "function():boolean -- True if the RS network is considered online")
    default Object[] isNetworkPowered(Context context, Arguments args) {
        var network = network();
        if (network == null) return ResultWrapper.result(false);
        if (!RefinedStorageApi.INSTANCE.isEnergyRequired()) return ResultWrapper.result(true);
        var energy = network.getComponent(EnergyNetworkComponent.class);
        return ResultWrapper.result(energy.getStored() > 0);
    }

    @Callback(doc = "function():table -- Get a list of the crafting tasks in the network.")
    default Object[] getCraftingTasks(Context context, Arguments args) {
        var result = new ArrayList<>();
        var crafting = autocrafting();
        if (crafting == null) return ResultWrapper.result((Object) result.toArray());
        for (var status : crafting.getStatuses()) {
            var map = new LinkedHashMap<>();
            map.put("id", status.info().id().toString());
            map.put("quantity", status.info().amount());
            map.put("state", status.state().toString());
            map.put("completion", status.percentageCompleted() * 100);
            if (status.info().resource() instanceof ItemResource itemResource) {
                map.put("resource", convert(itemResource, status.info().amount(), true, crafting));
            }
            result.add(map);
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    @Callback(doc = "function():table -- Get a list of the patterns known to the network.")
    default Object[] getPatterns(Context context, Arguments args) {
        var result = new ArrayList<>();
        var crafting = autocrafting();
        if (crafting == null) return ResultWrapper.result((Object) result.toArray());
        for (var pattern : crafting.getPatterns()) {
            var map = new LinkedHashMap<>();
            map.put("id", pattern.id().toString());
            map.put("patternType", pattern.layout().type().toString());
            var outputs = new ArrayList<>();
            for (var output : pattern.layout().outputs()) {
                if (output.resource() instanceof ItemResource itemResource) {
                    outputs.add(convert(itemResource, output.amount(), true, crafting));
                }
            }
            map.put("outputs", outputs.toArray());
            if (!outputs.isEmpty()) {
                map.put("primaryOutput", outputs.getFirst());
            }
            var inputs = new ArrayList<>();
            for (var ingredient : pattern.layout().ingredients()) {
                var options = new ArrayList<>();
                for (var key : ingredient.inputs()) {
                    if (key instanceof ItemResource itemResource) {
                        options.add(convert(itemResource, ingredient.amount(), true, crafting));
                    }
                }
                inputs.add(options.toArray());
            }
            map.put("inputs", inputs.toArray());
            result.add(map);
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    private static HashMap<Object, Object> parseFilter(Arguments args) {
        var hash = new HashMap<>();
        var table = args.optTable(0, Collections.emptyMap());
        var converted = Registry.INSTANCE.convertRecursively(table, new java.util.IdentityHashMap<>());
        if (converted instanceof Map<?, ?> map) {
            for (var entry : map.entrySet()) {
                hash.put(reduceLuaValue(entry.getKey()), reduceLuaValue(entry.getValue()));
            }
        }
        return hash;
    }

    private static Object reduceLuaValue(Object any) {
        if (any instanceof Map<?, ?> map) {
            if (isSequentialTable(map)) {
                return reduceSequentialTable(map);
            } else {
                return new HashMap<>(map);
            }
        }
        return any;
    }

    private static boolean isSequentialTable(Map<?, ?> map) {
        for (var entry : map.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (key instanceof String s) {
                if (!s.equals("n") || !(value instanceof Number)) return false;
            } else if (key instanceof Number n) {
                if (n.intValue() < 1) return false;
            } else {
                return false;
            }
        }
        return true;
    }

    private static Object[] reduceSequentialTable(Map<?, ?> map) {
        var list = new ArrayList<>();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String s) || !s.equals("n")) {
                list.add(reduceLuaValue(entry.getValue()));
            }
        }
        return list.toArray();
    }

    default boolean matches(Map<Object, Object> stack, Map<Object, Object> filter) {
        if (stack == null) return false;
        for (var entry : filter.entrySet()) {
            if (!contains(stack, entry.getKey(), entry.getValue())) return false;
        }
        return true;
    }

    private static boolean contains(Map<Object, Object> stack, Object key, Object value) {
        return stack.containsKey(key) && valueMatch(value, stack.get(key));
    }

    private static boolean valueMatch(Object a, Object b) {
        if ((a == null) != (b == null)) return false;
        if (a == b) return true;
        if (a instanceof Number aNum && b instanceof Number bNum) {
            return aNum.intValue() == bNum.intValue();
        }
        if (a instanceof Object[] aArr && b instanceof Object[] bArr) {
            for (var aElem : aArr) {
                if (!(aElem instanceof Map<?, ?> aMap)) return false;
                for (var aEntry : aMap.entrySet()) {
                    boolean found = false;
                    for (var bElem : bArr) {
                        if (!(bElem instanceof Map<?, ?> bMap)) continue;
                        for (var bEntry : bMap.entrySet()) {
                            if (valueMatch(aEntry.getKey(), bEntry.getKey())
                                    && valueMatch(aEntry.getValue(), bEntry.getValue())) {
                                found = true;
                                break;
                            }
                        }
                        if (found) break;
                    }
                    if (!found) return false;
                }
            }
            return true;
        }
        return false;
    }

    static BlockEntity resolveTile(String dimension, int x, int y, int z) {
        var server = SideTracker.getCurrentServer();
        if (server == null) return null;
        var key = net.minecraft.resources.ResourceLocation.tryParse(dimension);
        if (key == null) return null;
        var world = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, key));
        if (world == null) return null;
        return world.getBlockEntity(new BlockPos(x, y, z));
    }

    class Craftable extends AbstractValue {
        private BlockEntity tile;
        private ItemResource stack;
        private long amount;
        private String dimension;
        private int x, y, z;

        @SuppressWarnings("unused")
        public Craftable() {
        }

        public Craftable(BlockEntity tile, ItemResource stack, long amount) {
            this.tile = tile;
            this.stack = stack;
            this.amount = amount;
        }

        private BlockEntity resolveTile() {
            if (tile != null && tile.isRemoved()) tile = null;
            if (tile == null && dimension != null) {
                tile = NetworkControl.resolveTile(dimension, x, y, z);
            }
            return tile;
        }

        @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
        public Object[] getItemStack(Context ignoredContext, Arguments ignoredArgs) {
            if (stack != null) {
                return ResultWrapper.result(stack.toItemStack(Math.clamp(amount, 1, Integer.MAX_VALUE)));
            }
            return ResultWrapper.result((Object) null);
        }

        @Callback(doc = "function():number -- Get the number of items of this type currently being crafted / requested in the network.")
        public Object[] requesting(Context ignoredContext, Arguments ignoredArgs) {
            if (resolveTile() == null) {
                return ResultWrapper.result(0);
            }
            var network = RS2Util.networkOf(tile);
            if (network == null) {
                return ResultWrapper.result(0);
            }
            var crafting = network.getComponent(AutocraftingNetworkComponent.class);
            long requested = 0;
            for (var status : crafting.getStatuses()) {
                if (status.info().resource().equals(stack)) {
                    requested += status.info().amount();
                }
            }
            return ResultWrapper.result(requested);
        }

        @Callback(doc = "function([amount:int=1]):userdata -- Requests item to be crafted, returning an object that allows tracking the crafting status.")
        public Object[] request(Context ignoredContext, Arguments args) {
            if (resolveTile() == null) {
                return ResultWrapper.result(null, "no controller");
            }
            var network = RS2Util.networkOf(tile);
            if (network == null) {
                return ResultWrapper.result(null, "no rs network");
            }
            var crafting = network.getComponent(AutocraftingNetworkComponent.class);
            var count = Math.max(1, args.optInteger(0, 1));
            var actor = craftingActor(network);
            var result = crafting.ensureTask(stack, count, actor, com.refinedmods.refinedstorage.api.autocrafting.calculation.CancellationToken.NONE);
            var status = new CraftingStatus(tile, stack, count);
            switch (result) {
                case MISSING_RESOURCES -> status.fail("missing resources");
                case TASK_CREATED, TASK_ALREADY_RUNNING -> status.track();
            }
            return ResultWrapper.result(status);
        }

        private static Actor craftingActor(Network network) {
            var graph = network.getComponent(GraphNetworkComponent.class);
            for (var container : graph.getContainers(InWorldNetworkNodeContainer.class)) {
                var node = container.getNode();
              return new NetworkNodeActor(node);
            }
            return Actor.EMPTY;
        }

        private static final String TILE_DIM_KEY = "dimension";
        private static final String TILE_X_KEY = "x";
        private static final String TILE_Y_KEY = "y";
        private static final String TILE_Z_KEY = "z";
        private static final String STACK_KEY = "stack";
        private static final String AMOUNT_KEY = "amount";

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            if (nbt.contains(STACK_KEY)) {
                ResourceCodecs.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt.get(STACK_KEY))
                        .resultOrPartial(error -> {})
                        .filter(ItemResource.class::isInstance)
                        .map(ItemResource.class::cast)
                        .ifPresent(value -> stack = value);
            }
            if (nbt.contains(AMOUNT_KEY)) {
                amount = nbt.getLong(AMOUNT_KEY);
            }
            if (nbt.contains(TILE_DIM_KEY)) {
                dimension = nbt.getString(TILE_DIM_KEY);
                x = nbt.getInt(TILE_X_KEY);
                y = nbt.getInt(TILE_Y_KEY);
                z = nbt.getInt(TILE_Z_KEY);
                tile = NetworkControl.resolveTile(dimension, x, y, z);
            }
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            if (stack != null) {
                nbt.put(STACK_KEY, ResourceCodecs.CODEC.encodeStart(
                        provider.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
            }
            nbt.putLong(AMOUNT_KEY, amount);
            if (tile != null && tile.getLevel() != null) {
                nbt.putString(TILE_DIM_KEY, tile.getLevel().dimension().location().toString());
                nbt.putInt(TILE_X_KEY, tile.getBlockPos().getX());
                nbt.putInt(TILE_Y_KEY, tile.getBlockPos().getY());
                nbt.putInt(TILE_Z_KEY, tile.getBlockPos().getZ());
            }
        }
    }

    class CraftingStatus extends AbstractValue {
        private boolean failed = false;
        private String reason = "no task";
        private UUID taskId;
        private ItemResource stack;
        private long amount;
        private BlockEntity tile;
        private String dimension;
        private int x, y, z;

        @SuppressWarnings("unused")
        public CraftingStatus() {
        }

        public CraftingStatus(BlockEntity tile, ItemResource stack, long amount) {
            this.tile = tile;
            this.stack = stack;
            this.amount = amount;
        }

        @SuppressWarnings("SameParameterValue")
        void fail(String reason) {
            failed = true;
            this.reason = "request failed (" + reason + ")";
        }

        void track() {
            resolveTaskId();
        }

        private BlockEntity resolveTile() {
            if (tile != null && tile.isRemoved()) tile = null;
            if (tile == null && dimension != null) {
                tile = NetworkControl.resolveTile(dimension, x, y, z);
            }
            return tile;
        }

        private AutocraftingNetworkComponent crafting() {
            var tile = resolveTile();
            if (tile == null) return null;
            var network = RS2Util.networkOf(tile);
            return network != null ? network.getComponent(AutocraftingNetworkComponent.class) : null;
        }

        private void resolveTaskId() {
            if (taskId != null) return;
            var crafting = crafting();
            if (crafting == null) return;
            for (var status : crafting.getStatuses()) {
                if (status.info().resource().equals(stack) && status.info().amount() == amount) {
                    taskId = status.info().id().id();
                    return;
                }
            }
        }

        private boolean canceled;

        private TaskStatus findStatus() {
            if (taskId == null) {
                resolveTaskId();
            }
            if (taskId == null) return null;
            var crafting = crafting();
            if (crafting == null) return null;
            for (var status : crafting.getStatuses()) {
                if (status.info().id().id().equals(taskId)) {
                    return status;
                }
            }
            return null;
        }

        private boolean finished() {
            var crafting = crafting();
            if (crafting == null) return false;
            if (taskId == null) {
                resolveTaskId();
            }
            if (taskId == null) return true;
            return findStatus() == null;
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request is done (the task is no longer running).")
        public Object[] isDone(Context ignoredContext, Arguments ignoredArgs) {
            if (failed) return ResultWrapper.result(false, reason);
            return ResultWrapper.result(finished());
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request has been canceled through this API.")
        public Object[] isCanceled(Context ignoredContext, Arguments ignoredArgs) {
            if (failed) return ResultWrapper.result(false, reason);
            return ResultWrapper.result(canceled);
        }

        @Callback(doc = "function():boolean -- Cancels the request. Returns false if the craft cannot be canceled or nil if the link is computing")
        public Object[] cancel(Context ignoredContext, Arguments ignoredArgs) {
            if (failed) return ResultWrapper.result(false, reason);
            if (canceled) return ResultWrapper.result(false, "job already canceled");
            var crafting = crafting();
            if (crafting == null) return ResultWrapper.result(false, "no rs network");
            var status = findStatus();
            if (status == null) {
                return ResultWrapper.result(false, "no task");
            }
            if (status.state() == TaskState.COMPLETED) {
                return ResultWrapper.result(false, "job already completed");
            }
            crafting.cancel(status.info().id());
            canceled = true;
            return ResultWrapper.result(true);
        }

        private static final String FAILED_KEY = "failed";
        private static final String REASON_KEY = "reason";
        private static final String TASK_ID_KEY = "taskId";
        private static final String STACK_KEY = "stack";
        private static final String AMOUNT_KEY = "amount";
        private static final String CANCELED_KEY = "canceled";
        private static final String TILE_DIM_KEY = "dimension";
        private static final String TILE_X_KEY = "x";
        private static final String TILE_Y_KEY = "y";
        private static final String TILE_Z_KEY = "z";

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putBoolean(FAILED_KEY, failed);
            nbt.putString(REASON_KEY, reason);
            if (taskId != null) {
                nbt.putUUID(TASK_ID_KEY, taskId);
            }
            if (stack != null) {
                nbt.put(STACK_KEY, ResourceCodecs.CODEC.encodeStart(
                        provider.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow());
            }
            nbt.putLong(AMOUNT_KEY, amount);
            nbt.putBoolean(CANCELED_KEY, canceled);
            if (tile != null && tile.getLevel() != null) {
                nbt.putString(TILE_DIM_KEY, tile.getLevel().dimension().location().toString());
                nbt.putInt(TILE_X_KEY, tile.getBlockPos().getX());
                nbt.putInt(TILE_Y_KEY, tile.getBlockPos().getY());
                nbt.putInt(TILE_Z_KEY, tile.getBlockPos().getZ());
            }
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            if (nbt.contains(FAILED_KEY)) {
                failed = nbt.getBoolean(FAILED_KEY);
            }
            if (nbt.contains(REASON_KEY)) {
                reason = nbt.getString(REASON_KEY);
            }
            if (nbt.hasUUID(TASK_ID_KEY)) {
                taskId = nbt.getUUID(TASK_ID_KEY);
            }
            if (nbt.contains(STACK_KEY)) {
                ResourceCodecs.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), nbt.get(STACK_KEY))
                        .resultOrPartial(error -> {})
                        .filter(ItemResource.class::isInstance)
                        .map(ItemResource.class::cast)
                        .ifPresent(value -> stack = value);
            }
            if (nbt.contains(AMOUNT_KEY)) {
                amount = nbt.getLong(AMOUNT_KEY);
            }
            if (nbt.contains(CANCELED_KEY)) {
                canceled = nbt.getBoolean(CANCELED_KEY);
            }
            if (nbt.contains(TILE_DIM_KEY)) {
                dimension = nbt.getString(TILE_DIM_KEY);
                x = nbt.getInt(TILE_X_KEY);
                y = nbt.getInt(TILE_Y_KEY);
                z = nbt.getInt(TILE_Z_KEY);
                tile = NetworkControl.resolveTile(dimension, x, y, z);
            }
        }
    }
}
