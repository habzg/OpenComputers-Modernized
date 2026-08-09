package li.cil.oc.core.impl.common.item;

import java.util.List;
import java.util.function.BiFunction;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public class UpgradeTank extends DelegateItem implements ItemTier {
    private static BiFunction<HolderLookup.Provider, CompoundTag, String> fluidTooltipProvider = (r, d) -> null;

    public static void setFluidTooltipProvider(BiFunction<HolderLookup.Provider, CompoundTag, String> provider) {
        fluidTooltipProvider = provider;
    }

    @SuppressWarnings("unused")
    public UpgradeTank(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(OCSettings.namespace + "data")) {
                CompoundTag data = tag.getCompound(OCSettings.namespace + "data");
                var registries = context.registries();
                if (registries != null) {
                    String fluidInfo = fluidTooltipProvider.apply(registries, data);
                    if (fluidInfo != null) {
                        tooltip.add(Component.literal(fluidInfo));
                    }
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
