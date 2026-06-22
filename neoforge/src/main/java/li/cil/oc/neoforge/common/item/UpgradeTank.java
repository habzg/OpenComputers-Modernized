package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpgradeTank extends DelegateItem implements ItemTier {

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
            if (tag.contains(Settings.namespace + "data")) {
                CompoundTag data = tag.getCompound(Settings.namespace + "data");
                var registries = context.registries();
                if (registries != null) {
                    FluidStack fluidStack = FluidStack.parse(registries, data).orElse(null);
                    if (fluidStack != null && !fluidStack.isEmpty()) {
                        tooltip.add(Component.literal(fluidStack.getHoverName().getString() + ": " + fluidStack.getAmount() + "/16000"));
                    }
                }
            }
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
