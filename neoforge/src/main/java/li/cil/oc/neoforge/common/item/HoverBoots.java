package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.jetbrains.annotations.NotNull;

public class HoverBoots extends li.cil.oc.core.impl.common.item.HoverBoots {
  public static final ResourceLocation HOVER_BOOTS_TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/model/drone.png");
  public HoverBoots(Properties properties) {
        super(properties);
    }

    @Override
    public ResourceLocation getArmorTexture(@NotNull ItemStack ignoredStack, @NotNull Entity ignoredEntity, @NotNull EquipmentSlot ignoredSlot, ArmorMaterial.@NotNull Layer ignoredLayer, boolean ignoredInnerModel) {
        return HOVER_BOOTS_TEXTURE;
    }

    @Override
    public boolean onEntityItemUpdate(@NotNull ItemStack ignoredStack, @NotNull ItemEntity entity) {
        if (!entity.level().isClientSide && ItemColorizer.hasColor(entity.getItem())) {
            var pos = entity.blockPosition();
            var state = entity.level().getBlockState(pos);
            if (state.is(Blocks.WATER_CAULDRON)) {
                int level = state.getValue(LayeredCauldronBlock.LEVEL);
                if (level > 0) {
                    ItemColorizer.removeColor(entity.getItem());
                    LayeredCauldronBlock.lowerFillLevel(state, entity.level(), pos);
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean doesSneakBypassUse(@NotNull ItemStack stack, LevelReader level, @NotNull BlockPos pos, @NotNull Player player) {
        if (level.getBlockEntity(pos) instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive) return true;
        return super.doesSneakBypassUse(stack, level, pos, player);
    }
}
