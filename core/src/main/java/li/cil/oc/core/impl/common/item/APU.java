package li.cil.oc.core.impl.common.item;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.traits.CPULike;
import li.cil.oc.core.impl.common.item.traits.GPULike;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class APU extends DelegateItem implements ItemTier, CPULike, GPULike {
    private final int tier;

    public APU(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int cpuTier() {
        return Math.min(Tier.Three, tier + 1);
    }

    @Override
    public int cpuTierForComponents() {
        return tier + 1;
    }

    @Override
    public int gpuTier() {
        return tier;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return useCPU(level, player, hand, player.getItemInHand(hand));
    }

    @Override
    protected List<Object> tooltipData() {
        List<Object> data = new ArrayList<>();
        data.add(OCSettings.get().cpuComponentSupport[cpuTierForComponents()]);
        data.addAll(gpuTooltipData());
        return data;
    }
}
