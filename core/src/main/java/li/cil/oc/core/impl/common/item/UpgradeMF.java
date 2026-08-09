package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import org.jetbrains.annotations.NotNull;

public class UpgradeMF extends DelegateItem implements ItemTier {

    @SuppressWarnings("unused")
    public UpgradeMF(Properties properties) {
        super(properties);
    }

    @Override
    public int tier() {
        return 0;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag data = cd != null ? cd.copyTag() : null;
        boolean hasCoord = data != null && data.contains(OCSettings.namespace + "coord");
        String linkedKey = hasCoord ? "tooltip.opencomputers.upgrademf.linked" : "tooltip.opencomputers.upgrademf.unlinked";
        tooltip.add(Component.literal(Component.translatable(linkedKey).getString().replaceAll("\\[nl]", "\n").trim()));
    }
}
