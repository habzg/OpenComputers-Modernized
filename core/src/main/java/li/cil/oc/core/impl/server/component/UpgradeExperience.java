package li.cil.oc.core.impl.server.component;

import java.util.Map;
import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.util.ResultWrapper;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class UpgradeExperience extends AbstractManagedEnvironment implements DeviceInfo {
    public static final int MaxLevel = 30;
    public final li.cil.oc.api.internal.Agent host;
    public final li.cil.oc.api.network.Node node = Network.newNode(this, Visibility.Network)
            .withComponent("experience")
            .withConnector(30 * OCSettings.get().bufferPerLevel)
            .create();
    private final java.util.Map<String, String> deviceInfo = new java.util.HashMap<>() {{
        put(DeviceAttribute.Class, DeviceClass.Generic);
        put(DeviceAttribute.Description, "Knowledge database");
        put(DeviceAttribute.Vendor, Constants.DeviceInfo.DefaultVendor);
        put(DeviceAttribute.Product, "ERSO (Event Recorder and Self-Optimizer)");
        put(DeviceAttribute.Capacity, "30");
    }};
    public double experience = 0.0;
    public int level = 0;

    public UpgradeExperience(li.cil.oc.api.internal.Agent host) {
        this.host = host;
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return deviceInfo;
    }

    private double xpForLevel(int level) {
        if (level == 0) return 0;
        return OCSettings.get().baseXpToLevel + Math.pow(level * OCSettings.get().constantXpGrowth, OCSettings.get().exponentialXpGrowth);
    }

    private double xpForNextLevel() {
        return xpForLevel(level + 1);
    }

    public void addExperience(double value) {
        if (level < MaxLevel) {
            experience = experience + value;
            if (experience >= xpForNextLevel()) {
                updateXpInfo();
            }
            var world = host.level();
            var player = host.player();
            var pos = player.blockPosition();
            var orb = new ExperienceOrb(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, (int) value);
            player.takeXpDelay = 0;
            orb.playerTouch(player);
        }
    }

    private void updateXpInfo() {
        int oldLevel = level;
        level = Math.min((int) (Math.pow(experience - OCSettings.get().baseXpToLevel, 1.0 / OCSettings.get().exponentialXpGrowth) / OCSettings.get().constantXpGrowth), 30);
        if (node != null) {
            if (level != oldLevel) {
                updateClient();
            }
            ((Connector) node).setLocalBufferSize(OCSettings.get().bufferPerLevel * level);
        }
    }

    private void updateClient() {
        if (host instanceof li.cil.oc.api.internal.Robot robot) {
            robot.synchronizeSlot(host.componentSlot(node.address()));
        }
    }

    @Callback(direct = true, doc = "function():number -- The current level of experience stored in this experience upgrade.")
    public Object[] level(Context context, Arguments args) {
        double xpNeeded = xpForNextLevel() - xpForLevel(level);
        double xpProgress = Math.max(0, experience - xpForLevel(level));
        return ResultWrapper.result(level + xpProgress / xpNeeded);
    }

    @Callback(doc = "function():boolean -- Tries to consume an enchanted item to add experience to the upgrade.")
    public Object[] consume(Context context, Arguments args) {
        if (level >= MaxLevel) {
            return ResultWrapper.result(null, "max level");
        }
        ItemStack stack = host.mainInventory().getItem(host.selectedSlot());
        if (stack.isEmpty()) {
            return ResultWrapper.result(null, "no item");
        }
        int xp = 0;
        if (stack.getItem() == Items.EXPERIENCE_BOTTLE) {
            xp += 3 + host.level().random.nextInt(5) + host.level().random.nextInt(5);
        } else {
            for (var entry : stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
                xp += entry.getKey().value().getMinCost(entry.getIntValue());
            }
            for (var entry : stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).entrySet()) {
                xp += entry.getKey().value().getMinCost(entry.getIntValue());
            }
            if (xp <= 0) {
                return ResultWrapper.result(null, "could not extract experience from item");
            }
        }
        ItemStack consumed = host.mainInventory().removeItem(host.selectedSlot(), 1);
        if (consumed.isEmpty()) {
            return ResultWrapper.result(null, "could not consume item");
        }
        addExperience(xp * OCSettings.get().constantXpGrowth);
        return ResultWrapper.result(true);
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        super.save(nbt, provider);
        nbt.putDouble(OCSettings.namespace + "xp", experience);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        super.load(nbt, provider);
        experience = Math.max(0, nbt.getDouble(OCSettings.namespace + "xp"));
        updateXpInfo();
    }
}
