package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.AccessContext;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TpsCard extends li.cil.oc.api.prefab.ManagedEnvironment implements DeviceInfo {
    @SuppressWarnings("unused")
    public final EnvironmentHost host;
    @SuppressWarnings("unused")
    public final Node node = Network.newNode(this, Visibility.Neighbors)
            .withComponent("tps_card", Visibility.Neighbors)
            .create();
    private final Map<String, String> deviceInfo;
    public AccessContext access = null;

    public TpsCard(EnvironmentHost host) {
        this.host = host;
        deviceInfo = Map.of(DeviceAttribute.Class, DeviceClass.Generic, DeviceAttribute.Description, "TPS information", DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    private Object[] withWorld(int dim, java.util.function.Function<ServerLevel, Object[]> f) {
        var server = SideTracker.getCurrentServer();
        for (ServerLevel w : server.getAllLevels()) {
            if (w.dimension().location().hashCode() == dim) return f.apply(w);
        }
        return ResultWrapper.result(null, "Dimension not loaded: " + dim);
    }


    @SuppressWarnings("unchecked")
    private Iterable<ChunkHolder> getChunks(ServerChunkCache cache) {
        try {
            var chunkMap = cache.chunkMap;
            var method = ChunkMap.class.getDeclaredMethod("getChunks");
            method.setAccessible(true);
            return (Iterable<ChunkHolder>) method.invoke(chunkMap);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Callback(doc = "function(dimension:number):number -- ms taken by the dimension.")
    public Object[] getTickTimeInDim(Context context, Arguments args) {
        return ResultWrapper.result(0.0);
    }

    @Callback(doc = "function():number -- Overall tick time of the server.")
    public Object[] getOverallTickTime(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        return ResultWrapper.result((double) server.getCurrentSmoothedTickTime());
    }

    @Callback(doc = "function():table -- Returns a table with dimension id as key and name as value.")
    public Object[] getAllDims(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        Map<Integer, String> dims = new HashMap<>();
        for (ServerLevel w : server.getAllLevels()) {
            dims.put(w.dimension().location().hashCode(), w.dimension().location().toString());
        }
        return ResultWrapper.result(dims);
    }

    @Callback(doc = "function():table -- Returns a table with dimension id and tick time.")
    public Object[] getAllTickTimes(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        Map<Integer, Double> times = new HashMap<>();
        for (ServerLevel w : server.getAllLevels()) {
            times.put(w.dimension().location().hashCode(), (double) server.getCurrentSmoothedTickTime());
        }
        return ResultWrapper.result(times);
    }

    @Callback(doc = "function(dimension:number):string -- Returns the name for the dimension.")
    public Object[] getNameForDim(Context context, Arguments args) {
        return withWorld(args.checkInteger(0), w -> ResultWrapper.result(w.dimension().location().toString()));
    }

    @Callback(doc = "function(time:number):number -- Converts ms to TPS.")
    public Object[] convertTickTimeIntoTps(Context context, Arguments args) {
        double tps = 1000.0 / args.checkDouble(0);
        return ResultWrapper.result(Math.min(tps, 20.0));
    }

    @Callback(doc = "function():number -- Returns the overall amount of TE loaded.")
    public Object[] getOverallTileEntitiesLoaded(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        int count = 0;
        for (ServerLevel w : server.getAllLevels()) {
            var cache = w.getChunkSource();
            for (var holder : getChunks(cache)) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) {
                    count += chunk.getBlockEntities().size();
                }
            }
        }
        return ResultWrapper.result((double) count);
    }

    @Callback(doc = "function():number -- Returns the overall amount of chunks loaded.")
    public Object[] getOverallChunksLoaded(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        int count = 0;
        for (ServerLevel w : server.getAllLevels()) count += w.getChunkSource().getLoadedChunksCount();
        return ResultWrapper.result((double) count);
    }

    @SuppressWarnings("unused")
    @Callback(doc = "function():number -- Returns the overall amount of entities loaded.")
    public Object[] getOverallEntitiesLoaded(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        int count = 0;
        for (ServerLevel w : server.getAllLevels()) {
            for (Entity e : w.getEntities().getAll()) count++;
        }
        return ResultWrapper.result((double) count);
    }

    @SuppressWarnings("unused")
    @Callback(doc = "function():number -- Returns the number of dimensions loaded.")
    public Object[] getOverallDimsLoaded(Context context, Arguments args) {
        var server = SideTracker.getCurrentServer();
        int count = 0;
        for (ServerLevel w : server.getAllLevels()) count++;
        return ResultWrapper.result((double) count);
    }

    @Callback(doc = "function(dimension:number):table -- Returns entity class names and counts.")
    public Object[] getEntitiesListForDim(Context context, Arguments args) {
        return withWorld(args.checkInteger(0), w -> {
            Map<String, Integer> map = new HashMap<>();
            for (Entity e : w.getEntities().getAll()) {
                String name = e.getClass().getName();
                map.put(name, map.getOrDefault(name, 0) + 1);
            }
            return ResultWrapper.result(map);
        });
    }

    @Callback(doc = "function(dimension:number):table -- Returns TE class names and counts.")
    public Object[] getTileEntitiesListForDim(Context context, Arguments args) {
        return withWorld(args.checkInteger(0), w -> {
            Map<String, Integer> map = new HashMap<>();
            var cache = w.getChunkSource();
            for (var holder : getChunks(cache)) {
                LevelChunk chunk = holder.getTickingChunk();
                if (chunk != null) {
                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        String name = be.getClass().getName();
                        map.put(name, map.getOrDefault(name, 0) + 1);
                    }
                }
            }
            return ResultWrapper.result(map);
        });
    }

    @Callback(doc = "function(dimension:number):number -- Returns chunks loaded.")
    public Object[] getChunksLoadedForDim(Context context, Arguments args) {
        return withWorld(args.checkInteger(0), w -> ResultWrapper.result((double) w.getChunkSource().getLoadedChunksCount()));
    }

    @Callback(doc = "function(className:string, dimension:number):table -- Coordinates of matching entities.")
    public Object[] getCoordinatesForEntityClassInDim(Context context, Arguments args) {
        if (access == null) return ResultWrapper.result(null, "Access denied");
        String className = args.checkString(0);
        int dim = args.checkInteger(1);
        return withWorld(dim, w -> {
            java.util.List<double[]> coords = new java.util.ArrayList<>();
            for (Entity e : w.getEntities().getAll()) {
                if (e.getClass().getName().equals(className)) {
                    coords.add(new double[]{e.getX(), e.getY(), e.getZ()});
                }
            }
            return ResultWrapper.result(coords.toArray());
        });
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        access = AccessContext.load(nbt);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        if (access != null) access.save(nbt);
    }
}
