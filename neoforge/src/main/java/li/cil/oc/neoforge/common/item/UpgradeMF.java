package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class UpgradeMF extends DelegateItem {
    public UpgradeMF(Properties properties) {
        super(properties);
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
            data.putIntArray(OCSettings.namespace + "coord", new int[]{
                    pos.getX(), pos.getY(), pos.getZ(), world.dimension().location().hashCode(), side.ordinal()
            });
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
