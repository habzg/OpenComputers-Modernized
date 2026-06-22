package li.cil.oc.neoforge.integration.appeng;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.security.IActionHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import com.google.common.collect.ImmutableSet;
import li.cil.oc.api.Persistable;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.prefab.AbstractValue;
import li.cil.oc.core.impl.server.driver.Registry;
import li.cil.oc.core.impl.util.DatabaseAccess;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public interface NetworkControl<AETile extends IActionHost> extends Persistable, ManagedEnvironment {
    AETile tile();

    @SuppressWarnings("EmptyMethod")
    Node node();

    default KeyCounter allItems() {
        var node = tile().getActionableNode();
        if (node == null) return new KeyCounter();
        var grid = node.getGrid();
        if (grid == null) return new KeyCounter();
        var storage = AEUtil.getGridStorage(grid);
        var items = new KeyCounter();
        storage.getAvailableStacks(items);
        var crafting = AEUtil.getGridCrafting(grid);
        for (var key : crafting.getCraftables(k -> true)) {
            if (items.get(key) <= 0) {
                items.add(key, 0);
            }
        }
        return items;
    }

    default HashMap<Object, Object> convert(AEItemKey itemKey, long size, boolean isCraftable, ICraftingService crafting) {
        var hash = new HashMap<>();
        if (itemKey == null) return hash;
        long potentialAmount = size;
        if (size <= 0 && isCraftable && crafting != null) {
            potentialAmount = getPatternOutputAmount(itemKey, crafting);
        }
        var itemStack = itemKey.toStack(Math.clamp(potentialAmount, 1, Integer.MAX_VALUE));
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

    static long getPatternOutputAmount(AEItemKey itemKey, ICraftingService crafting) {
        for (var pattern : crafting.getCraftingFor(itemKey)) {
            for (var output : pattern.getOutputs()) {
                if (itemKey.equals(output.what())) {
                    return output.amount();
                }
            }
        }
        return 0;
    }

    @Callback(doc = "function():table -- Get a list of tables representing the available CPUs in the network.")
    default Object[] getCpus(Context context, Arguments args) {
        var buffer = new ArrayList<Map<String, Object>>();
        var node = tile().getActionableNode();
        if (node == null) return ResultWrapper.result((Object) buffer.toArray());
        var grid = node.getGrid();
        if (grid == null) return ResultWrapper.result((Object) buffer.toArray());
        var crafting = AEUtil.getGridCrafting(grid);
        for (var cpu : crafting.getCpus()) {
            var map = new LinkedHashMap<String, Object>();
            var name = cpu.getName();
            map.put("name", name != null ? name.getString() : null);
            map.put("storage", cpu.getAvailableStorage());
            map.put("coprocessors", cpu.getCoProcessors());
            map.put("busy", cpu.isBusy());
            buffer.add(map);
        }
        return ResultWrapper.result((Object) buffer.toArray());
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of known item recipes. These can be used to issue crafting requests.")
    default Object[] getCraftables(Context context, Arguments args) {
        var filter = parseFilter(args);
        var builder = new ArrayList<>();
        var node = tile().getActionableNode();
        if (node == null) return ResultWrapper.result((Object) builder.toArray());
        var grid = node.getGrid();
        if (grid == null) return ResultWrapper.result((Object) builder.toArray());
        var crafting = AEUtil.getGridCrafting(grid);
        for (var key : crafting.getCraftables(k -> true)) {
            if (!(key instanceof AEItemKey itemKey)) continue;
            long patternAmount = getPatternOutputAmount(itemKey, crafting);
            var converted = convert(itemKey, 0, true, crafting);
            if (filter.isEmpty() || matches(converted, filter)) {
                builder.add(new NetworkControl.Craftable(tile(), itemKey, patternAmount));
            }
        }
        return ResultWrapper.result((Object) builder.toArray());
    }

    @Callback(doc = "function([filter:table]):table -- Get a list of the stored items in the network.")
    default Object[] getItemsInNetwork(Context context, Arguments args) {
        var filter = parseFilter(args);
        var result = new ArrayList<>();
        var all = allItems();
        var node = tile().getActionableNode();
        var grid = node != null ? node.getGrid() : null;
        var crafting = grid != null ? AEUtil.getGridCrafting(grid) : null;
        for (var entry : all) {
            if (!(entry.getKey() instanceof AEItemKey itemKey)) continue;
            boolean isCraftable = crafting != null && crafting.isCraftable(itemKey);
            var converted = convert(itemKey, entry.getLongValue(), isCraftable, crafting);
            if (matches(converted, filter)) {
                result.add(converted);
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    @Callback(doc = "function([filter:table, dbAddress:string, startSlot:number, count:number]):boolean -- Store items matching the specified filter in the database.")
    default Object[] store(Context context, Arguments args) {
        var filter = parseFilter(args);
        var database = args.optString(1, null);
        var db = database != null
                ? DatabaseAccess.database(node(), database)
                : DatabaseAccess.databases(node()).stream().findFirst()
                .orElseThrow(() -> new IllegalArgumentException("no database upgrade found"));
        var all = allItems();
        var node = tile().getActionableNode();
        var grid = node != null ? node.getGrid() : null;
        var crafting = grid != null ? AEUtil.getGridCrafting(grid) : null;
        var items = new ArrayList<GenericStack>();
        for (var entry : all) {
            if (!(entry.getKey() instanceof AEItemKey itemKey)) continue;
            boolean isCraftable = crafting != null && crafting.isCraftable(itemKey);
            var converted = convert(itemKey, entry.getLongValue(), isCraftable, crafting);
            if (matches(converted, filter)) {
                long potentialAmount = entry.getLongValue();
                if (potentialAmount <= 0 && isCraftable) {
                    potentialAmount = getPatternOutputAmount(itemKey, crafting);
                }
                items.add(new GenericStack(entry.getKey(), potentialAmount));
            }
        }
        var offset = Math.max(0, args.optInteger(2, 1) - 1);
        int count = args.optInteger(3, Integer.MAX_VALUE);
        count = Math.clamp(db.size() - offset, 0, count);
        count = Math.min(count, items.size());
        int slot = offset;
        for (int i = 0; i < count; i++) {
            var stack = items.get(i);
            if (!(stack.what() instanceof AEItemKey itemKey)) continue;
            var itemStack = itemKey.toStack((int) Math.min(stack.amount(), Integer.MAX_VALUE));
            if (itemStack.isEmpty()) continue;
            while (slot < db.size() && !db.getStackInSlot(slot).isEmpty()) slot++;
            if (slot >= db.size()) break;
            if (db.getStackInSlot(slot).isEmpty()) {
                db.setStackInSlot(slot, itemStack);
            }
        }
        return ResultWrapper.result(true);
    }

    @Callback(doc = "function():table -- Get a list of the stored fluids in the network.")
    default Object[] getFluidsInNetwork(Context context, Arguments args) {
        var result = new ArrayList<>();
        var node = tile().getActionableNode();
        if (node == null) return ResultWrapper.result((Object) result.toArray());
        var grid = node.getGrid();
        if (grid == null) return ResultWrapper.result((Object) result.toArray());
        var storage = AEUtil.getGridStorage(grid);
        var all = new KeyCounter();
        storage.getAvailableStacks(all);
        for (var entry : all) {
            if (entry.getKey() instanceof appeng.api.stacks.AEFluidKey fluidKey) {
                result.add(fluidKey.toStack((int) Math.min(entry.getLongValue(), Integer.MAX_VALUE)));
            }
        }
        return ResultWrapper.result((Object) result.toArray());
    }

    @Callback(doc = "function():number -- Get the average power injection into the network.")
    default Object[] getAvgPowerInjection(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getAvgPowerInjection());
    }

    @Callback(doc = "function():number -- Get the average power usage of the network.")
    default Object[] getAvgPowerUsage(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getAvgPowerUsage());
    }

    @Callback(doc = "function():number -- Get the idle power usage of the network.")
    default Object[] getIdlePowerUsage(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getIdlePowerUsage());
    }

    @Callback(doc = "function():number -- Get the maximum stored power in the network.")
    default Object[] getMaxStoredPower(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getMaxStoredPower());
    }

    @Callback(doc = "function():number -- Get the stored power in the network.")
    default Object[] getStoredPower(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getStoredPower());
    }

    @Callback(doc = "function():boolean -- True if the AE network is considered online.")
    default Object[] isNetworkPowered(Context context, Arguments args) {
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(false);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).isNetworkPowered());
    }

    @Callback(doc = "function():number -- Returns the energy demand on the AE network.")
    default Object[] getEnergyDemand(Context context, Arguments args) {
        context.consumeCallBudget(1.5);
        var grid = gridOf(tile());
        if (grid == null) return ResultWrapper.result(0.0);
        return ResultWrapper.result(AEUtil.getGridEnergy(grid).getEnergyDemand(Double.MAX_VALUE));
    }

    private static IGrid gridOf(IActionHost host) {
        if (host == null) return null;
        var node = host.getActionableNode();
        return node != null ? node.getGrid() : null;
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
        var list = new java.util.ArrayList<>();
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

    class Craftable extends AbstractValue implements ICraftingRequester {
        private final Set<ICraftingLink> links = new HashSet<>();
        private IActionHost controller;
        private AEItemKey stack;
        private long amount;
        private EphemeralDelayData delayData;

        public Craftable(IActionHost controller, AEItemKey stack, long amount) {
            this.controller = controller;
            this.stack = stack;
            this.amount = amount;
        }

        @Override
        public ImmutableSet<ICraftingLink> getRequestedJobs() {
            return ImmutableSet.copyOf(links);
        }

        @Override
        public void jobStateChange(ICraftingLink link) {
            links.remove(link);
        }

        @Override
        public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
            return 0;
        }

        @Override
        public IGridNode getActionableNode() {
            return controller != null ? controller.getActionableNode() : null;
        }

        @Callback(doc = "function():table -- Returns the item stack representation of the crafting result.")
        public Object[] getItemStack(Context context, Arguments args) {
            if (stack != null) {
                return ResultWrapper.result(stack.toStack(Math.clamp(amount, 1, Integer.MAX_VALUE)));
            }
            return ResultWrapper.result((Object) null);
        }

        @Callback(doc = "function():number -- Returns the number of requests in progress.")
        public Object[] requesting(Context context, Arguments args) {
            var actionableNode = getActionableNode();
            if (actionableNode == null) return ResultWrapper.result(null, "no ae grid");
            var grid = actionableNode.getGrid();
            if (grid == null) return ResultWrapper.result(null, "no ae grid");
            return ResultWrapper.result(AEUtil.getGridCrafting(grid).getRequestedAmount(stack));
        }

        @Callback(doc = "function([amount:int=1, prioritizePower:boolean=true, cpuName:string]):userdata -- Requests item to be crafted, returning an object that allows tracking the crafting status.")
        public Object[] request(Context context, Arguments args) {
            if (delayData != null) {
                return ResultWrapper.result(null, "waiting for ae network to load");
            }
            if (controller == null || isControllerInvalid(controller)) {
                return ResultWrapper.result(null, "no controller");
            }
            var actionableNode = controller.getActionableNode();
            if (actionableNode == null) {
                return ResultWrapper.result(null, "no ae grid");
            }
            var grid = actionableNode.getGrid();
            if (grid == null) {
                return ResultWrapper.result(null, "no ae grid");
            }
            var craftingGrid = AEUtil.getGridCrafting(grid);
            var count = args.optInteger(0, 1);
            var prioritizePower = args.optBoolean(1, true);
            var cpuName = args.optString(2, "");

            var source = new MachineSource(controller);
            Level level = controller instanceof BlockEntity be ? be.getLevel() : null;

            var future = craftingGrid.beginCraftingCalculation(
                    level,
                    () -> source,
                    stack,
                    count,
                    CalculationStrategy.REPORT_MISSING_ITEMS
            );

            var status = new CraftingStatus();
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    var job = future.get();
                    Runnable submit = () -> {
                        ICraftingCPU cpu = null;
                        if (!cpuName.isEmpty()) {
                            for (var c : craftingGrid.getCpus()) {
                                var cName = c.getName();
                                if (cName != null && cpuName.equals(cName.getString())) {
                                    cpu = c;
                                    break;
                                }
                            }
                        }
                        var result = craftingGrid.submitJob(job, Craftable.this, cpu, prioritizePower, source);
                        if (result.successful() && result.link() != null) {
                            var link = result.link();
                            status.setLink(link);
                            links.add(link);
                        } else {
                            status.fail("missing resources?");
                        }
                    };
                    if (level != null && level.getServer() != null) {
                        level.getServer().execute(submit);
                    } else {
                        EventHandler.scheduleServer(submit);
                    }
                } catch (Exception e) {
                    OpenComputers.log().debug("Error submitting job to AE2.", e);
                    status.fail(e.toString());
                }
            });

            return ResultWrapper.result(status);
        }

        private static boolean isControllerInvalid(IActionHost controller) {
            if (controller instanceof BlockEntity be) {
                return be.isRemoved();
            }
            return controller.getActionableNode() == null;
        }

        private static final String DIM_KEY = "dimension";
        private static final String X_KEY = "x";
        private static final String Y_KEY = "y";
        private static final String Z_KEY = "z";
        private static final String LINKS_KEY = "links";
        private static final String STACK_KEY = "stack";
        private static final String AMOUNT_KEY = "amount";

        private static final int MAX_BACKOFF_TICKS = 100;
        private static final int BACKOFF_SCALE = 2;

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            if (nbt.contains(STACK_KEY)) {
                var key = AEKey.fromTagGeneric(provider, nbt.getCompound(STACK_KEY));
                if (key instanceof AEItemKey itemKey) {
                    stack = itemKey;
                }
            }
            if (nbt.contains(AMOUNT_KEY)) {
                amount = nbt.getLong(AMOUNT_KEY);
            }
            var linksList = nbt.getList(LINKS_KEY, Tag.TAG_COMPOUND);
            for (int i = 0; i < linksList.size(); i++) {
                var linkTag = linksList.getCompound(i);
                var link = StorageHelper.loadCraftingLink(linkTag, this);
                LinkCache.store(link);
                links.add(link);
            }
            if (nbt.contains(DIM_KEY)) {
                var dimension = nbt.getString(DIM_KEY);
                var x = nbt.getInt(X_KEY);
                var y = nbt.getInt(Y_KEY);
                var z = nbt.getInt(Z_KEY);
                delayData = new EphemeralDelayData(dimension, x, y, z);
                pushDelayLoadBackoff(1);
            }
        }

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            if (stack != null) {
                nbt.put(STACK_KEY, stack.toTagGeneric(provider));
            }
            nbt.putLong(AMOUNT_KEY, amount);
            var linksList = new ListTag();
            for (var link : links) {
                var linkTag = new CompoundTag();
                link.writeToNBT(linkTag);
                linksList.add(linkTag);
            }
            nbt.put(LINKS_KEY, linksList);
            if (controller instanceof BlockEntity be && be.getLevel() != null) {
                nbt.putString(DIM_KEY, be.getLevel().dimension().location().toString());
                nbt.putInt(X_KEY, be.getBlockPos().getX());
                nbt.putInt(Y_KEY, be.getBlockPos().getY());
                nbt.putInt(Z_KEY, be.getBlockPos().getZ());
            }
        }

        private boolean tryLoadGrid(String dimension, int x, int y, int z) {
            var server = li.cil.oc.core.impl.util.SideTracker.getCurrentServer();
            if (server == null) return false;
            var key = net.minecraft.resources.ResourceLocation.tryParse(dimension);
            if (key == null) return true;
            var world = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, key));
            if (world == null) return false;
            var be = world.getBlockEntity(new BlockPos(x, y, z));
            if (be == null) return false;
            if (!(be instanceof IActionHost host)) return true;
            var node = host.getActionableNode();
            if (node == null) return false;
            var grid = node.getGrid();
            if (grid == null) return true;
            controller = host;
            return true;
        }

        private void delayLoadGrid() {
            if (delayData != null) {
                if (tryLoadGrid(delayData.dimension, delayData.x, delayData.y, delayData.z)) {
                    delayData = null;
                } else {
                    pushDelayLoadBackoff(delayData.delay * BACKOFF_SCALE);
                }
            }
        }

        private void pushDelayLoadBackoff(int delay) {
            if (delayData != null) {
                delayData.delay = Math.min(delay, MAX_BACKOFF_TICKS);
                EventHandler.scheduleServer(this::delayLoadGrid, delayData.delay);
            }
        }

        private static final class EphemeralDelayData {
            final String dimension;
            final int x, y, z;
            int delay = 1;

            EphemeralDelayData(String dimension, int x, int y, int z) {
                this.dimension = dimension;
                this.x = x;
                this.y = y;
                this.z = z;
            }
        }
    }

    class CraftingStatus extends AbstractValue {
        private boolean isComputing = true;
        private ICraftingLink link;
        private boolean failed = false;
        private String reason = "no link";

        public void setLink(ICraftingLink value) {
            isComputing = false;
            link = value;
        }

        public void fail(String reason) {
            isComputing = false;
            failed = true;
            this.reason = "request failed (" + reason + ")";
        }

        private Object[] asCraft(java.util.function.Function<ICraftingLink, Object[]> f) {
            if (isComputing) return ResultWrapper.result(null, "computing");
            if (!failed && link != null) return f.apply(link);
            return ResultWrapper.result(false, reason);
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request has been canceled.")
        public Object[] isCanceled(Context context, Arguments args) {
            return asCraft(craft -> ResultWrapper.result(craft.isCanceled()));
        }

        @Callback(doc = "function():boolean -- Get whether the crafting request is done.")
        public Object[] isDone(Context context, Arguments args) {
            return asCraft(craft -> ResultWrapper.result(craft.isDone()));
        }

        @Callback(doc = "function():boolean -- Cancels the request. Returns false if the craft cannot be canceled or nil if the link is computing")
        public Object[] cancel(Context context, Arguments args) {
            return asCraft(craft -> {
                if (craft.isDone()) {
                    return ResultWrapper.result(false, "job already completed");
                }
                craft.cancel();
                return ResultWrapper.result(true);
            });
        }

        private static final String COMPUTING_KEY = "computing";
        private static final String LINK_ID_KEY = "link";
        private static final String FAILED_KEY = "failed";
        private static final String REASON_KEY = "reason";

        @Override
        public void save(CompoundTag nbt, HolderLookup.Provider provider) {
            super.save(nbt, provider);
            nbt.putBoolean(COMPUTING_KEY, isComputing);
            if (link != null) {
                nbt.putString(LINK_ID_KEY, link.getCraftingID().toString());
            }
            nbt.putBoolean(FAILED_KEY, failed);
            nbt.putString(REASON_KEY, reason);
        }

        @Override
        public void load(CompoundTag nbt, HolderLookup.Provider provider) {
            super.load(nbt, provider);
            if (nbt.contains(COMPUTING_KEY)) {
                isComputing = nbt.getBoolean(COMPUTING_KEY);
            }
            if (nbt.contains(LINK_ID_KEY)) {
                var id = nbt.getString(LINK_ID_KEY);
                LinkCache.store(this, id);
            }
            if (nbt.contains(FAILED_KEY)) {
                failed = nbt.getBoolean(FAILED_KEY);
            }
            if (nbt.contains(REASON_KEY)) {
                reason = nbt.getString(REASON_KEY);
            }
        }
    }

    final class LinkCache {
        private static final java.util.Map<String, ICraftingLink> linkCache = new java.util.concurrent.ConcurrentHashMap<>();
        private static final java.util.Map<String, CraftingStatus> statusCache = new java.util.concurrent.ConcurrentHashMap<>();

        static void store(ICraftingLink link) {
            var id = link.getCraftingID().toString();
            var status = statusCache.remove(id);
            if (status != null) {
                status.setLink(link);
            } else {
                linkCache.put(id, link);
            }
        }

        static void store(CraftingStatus status, String id) {
            var link = linkCache.remove(id);
            if (link != null) {
                status.setLink(link);
            } else {
                statusCache.put(id, status);
            }
        }
    }
}
