package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

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
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        Level world = context.getLevel();
        var player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!world.isClientSide && player.isShiftKeyDown()) {
            BlockPos pos = context.getClickedPos();
            Direction side = context.getClickedFace();
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag data;
            if (cd == null || cd.isEmpty()) {
                data = new CompoundTag();
            } else {
                data = cd.copyTag();
            }
            data.putIntArray(Settings.namespace + "coord", new int[]{
                    pos.getX(), pos.getY(), pos.getZ(), world.dimension().location().hashCode(), side.ordinal()
            });
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        return InteractionResult.PASS;
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag data = cd != null ? cd.copyTag() : null;
        boolean hasCoord = data != null && data.contains(Settings.namespace + "coord");
        String linkedKey = hasCoord ? "tooltip.opencomputers.upgrademf.linked" : "tooltip.opencomputers.upgrademf.unlinked";
        tooltip.add(Component.literal(Component.translatable(linkedKey).getString().replaceAll("\\[nl]", "\n").trim()));
    }
}
