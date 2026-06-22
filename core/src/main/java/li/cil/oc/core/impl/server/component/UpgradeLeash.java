package li.cil.oc.core.impl.server.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.server.component.traits.WorldAware;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.ExtendedArguments;
import li.cil.oc.core.util.ResultWrapper;
import li.cil.oc.core.util.Tasks;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UpgradeLeash extends li.cil.oc.api.prefab.ManagedEnvironment implements WorldAware, DeviceInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpgradeLeash.class);
    public static final int MaxLeashedEntities = 8;
    public final Entity host;
    public final Node node = Network.newNode(this, Visibility.Network)
            .withComponent("leash")
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Leash");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "FlockControl (FC-3LS)");
        put(DeviceAttribute.Capacity, String.valueOf(MaxLeashedEntities));
    }};
    private final Set<UUID> leashedEntities = new HashSet<>();

    public UpgradeLeash(Entity host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    @Override
    public BlockPosition position() {
        return BlockPosition.apply(host);
    }

    @Callback(doc = "function(side:number):boolean -- Tries to put an entity on the specified side of the device onto a leash.")
    public Object[] leash(Context context, Arguments args) {
        if (leashedEntities.size() >= MaxLeashedEntities)
            return ResultWrapper.result(null, "too many leashed entities");
        Direction side = ExtendedArguments.checkSideAny(args, 0);
        AABB nearBounds = position().bounds();
        AABB farBounds = nearBounds.move(side.getStepX() * 2.0, side.getStepY() * 2.0, side.getStepZ() * 2.0);
        AABB bounds = nearBounds.minmax(farBounds);
        Mob entity = entitiesInBounds(bounds, Mob.class).stream().filter(e -> !e.isLeashed() && e.canBeLeashed()).findFirst().orElse(null);
        if (entity != null) {
            entity.setLeashedTo(host, true);
            leashedEntities.add(entity.getUUID());
            context.pause(0.1);
            return ResultWrapper.result(true);
        }
        return ResultWrapper.result(null, "no unleashed entity");
    }

    @Callback(doc = "function() -- Unleashes all currently leashed entities.")
    @SuppressWarnings("SameReturnValue")
    public Object @Nullable [] unleash(Context context, Arguments args) {
        unleashAll();
        return null;
    }

    @Override
    public void onDisconnect(Node node) {
        super.onDisconnect(node);
        if (node == this.node) {
            unleashAll();
        }
    }

    private void unleashAll() {
        for (Mob entity : entitiesInBounds(position().bounds().inflate(5), Mob.class)) {
            if (leashedEntities.contains(entity.getUUID()) && entity.getLeashHolder() == host) {
                entity.dropLeash(true, false);
            }
        }
        leashedEntities.clear();
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        ListTag list = nbt.getList("leashedEntities", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            leashedEntities.add(UUID.fromString(list.getString(i)));
        }
        Tasks.schedule(() -> {
            Set<UUID> foundEntities = new HashSet<>();
            for (Mob entity : entitiesInBounds(position().bounds().inflate(5), Mob.class)) {
                if (leashedEntities.contains(entity.getUUID())) {
                    entity.setLeashedTo(host, true);
                    foundEntities.add(entity.getUUID());
                }
            }
            Set<UUID> missing = new HashSet<>(leashedEntities);
            missing.removeAll(foundEntities);
            if (!missing.isEmpty()) {
                LOGGER.info("Could not find {} leashed entities after loading!", missing.size());
                leashedEntities.removeAll(missing);
            }
        });
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        ListTag list = new ListTag();
        for (UUID id : leashedEntities) {
            list.add(StringTag.valueOf(id.toString()));
        }
        nbt.put("leashedEntities", list);
    }
}
