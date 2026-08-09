package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.opencomputers.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.NotNull;

public class UpgradeExperience extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeExperience(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag nbt = Item.getDataTag(stack);
        double experience = Math.max(0, nbt.getDouble(OCSettings.namespace + "xp"));
        if (experience > 0) {
            int lvl = Math.min((int) (Math.pow(experience - OCSettings.get().baseXpToLevel, 1.0 / OCSettings.get().exponentialXpGrowth) / OCSettings.get().constantXpGrowth), 30);
            double xpForLevel = xpForLevel(lvl);
            double xpForNext = xpForLevel(lvl + 1);
            double xpNeeded = xpForNext - xpForLevel;
            double xpProgress = Math.max(0, experience - xpForLevel);
            double reportedLevel = lvl + xpProgress / xpNeeded;
            tooltip.add(Component.translatable("tooltip.opencomputers.robotlevel", String.format("%.1f", reportedLevel)));
        }
    }

    private static double xpForLevel(int level) {
        if (level == 0) return 0;
        return OCSettings.get().baseXpToLevel + Math.pow(level * OCSettings.get().constantXpGrowth, OCSettings.get().exponentialXpGrowth);
    }

    @Override
    public int tier() {
        return 0;
    }
}
