package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.data.NodeData;
import li.cil.oc.core.impl.common.item.traits.Chargeable;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class UpgradeBattery extends DelegateItem implements ItemTier, Chargeable {

    private final int tier;

    public UpgradeBattery(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var data = new NodeData(stack);
        double buffer = data.buffer != null ? data.buffer : 0.0;
        return Math.round(13.0f * (float) (buffer / OCSettings.get().bufferCapacitorUpgrades[tier]));
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var data = new NodeData(stack);
        double buffer = data.buffer != null ? data.buffer : 0.0;
        var fill = OCSettings.get().bufferCapacitorUpgrades[tier] > 0 ? buffer / OCSettings.get().bufferCapacitorUpgrades[tier] : 0;
        int r = (int) (255 - fill * 255);
        int g = (int) (fill * 255);
        return (r << 16) | (g << 8);
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of((int) OCSettings.get().bufferCapacitorUpgrades[tier]);
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        var data = new NodeData(stack);
        double buffer = data.buffer != null ? data.buffer : 0.0;
        double max = OCSettings.get().bufferCapacitorUpgrades[tier];
        double target = Math.clamp(buffer + amount, 0, max);
        double used = target - buffer;
        double unused = amount - used;
        if (!simulate && (used > Double.MIN_VALUE || used < -Double.MIN_VALUE)) {
            data.buffer = target;
            data.save(stack);
        }
        return unused;
    }

    @Override
    public double maxCharge(ItemStack stack) {
        return OCSettings.get().bufferCapacitorUpgrades[tier];
    }

    @Override
    public double getCharge(ItemStack stack) {
        var data = new NodeData(stack);
        return data.buffer != null ? data.buffer : 0.0;
    }

    @Override
    public void setCharge(ItemStack stack, double amount) {
        var data = new NodeData(stack);
        data.buffer = Math.clamp(amount, 0, maxCharge(stack));
        data.save(stack);
    }

    @Override
    public boolean canExtract(ItemStack stack) {
        return true;
    }
}
