package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.traits.CPULike;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class CPU extends DelegateItem implements ItemTier, CPULike {
    private final int tier;

    public CPU(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public int tier() {
        return tier;
    }

    @Override
    public int cpuTier() {
        return tier;
    }

    @Override
    public int cpuTierForComponents() {
        return tier;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player player, @NotNull InteractionHand hand) {
        return useCPU(level, player, hand, player.getItemInHand(hand));
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(OCSettings.get().cpuComponentSupport[cpuTierForComponents()]);
    }
}
