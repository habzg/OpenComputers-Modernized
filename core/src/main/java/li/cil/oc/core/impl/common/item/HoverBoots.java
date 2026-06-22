package li.cil.oc.core.impl.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.HoverBootsData;
import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HoverBoots extends ArmorItem implements Chargeable {
    public static final ResourceLocation HOVER_BOOTS_TEXTURE = ResourceLocation.fromNamespaceAndPath("opencomputers", "textures/model/drone.png");

    public HoverBoots(Properties properties) {
        super(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public ResourceLocation getArmorTexture(@NotNull ItemStack stack, @NotNull Entity entity, @NotNull EquipmentSlot slot, ArmorMaterial.@NotNull Layer layer, boolean innerModel) {
        return HOVER_BOOTS_TEXTURE;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        List<Component> extended = getExtendedTooltip(stack);
        tooltip.addAll(extended);
    }

    @SuppressWarnings("unused")
    protected List<Component> getExtendedTooltip(ItemStack stack) {
        return new ArrayList<>();
    }

    @SuppressWarnings("unused")
    public double maxCharge(ItemStack stack) {
        return Settings.get().bufferHoverBoots;
    }

    public double getCharge(ItemStack stack) {
        return new HoverBootsData(stack).charge;
    }

    @SuppressWarnings("unused")

    public void setCharge(ItemStack stack, double amount) {
        var data = new HoverBootsData(stack);
        data.charge = Math.clamp(amount, 0, maxCharge(stack));
        data.save(stack);
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slotId, boolean isSelected) {
        super.inventoryTick(stack, level, entity, slotId, isSelected);
        if (!level.isClientSide && entity instanceof LivingEntity living && slotId == EquipmentSlot.FEET.getIndex()) {
            if (!Settings.get().ignorePower && getCharge(stack) == 0) {
                if (living.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null) {
                    living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1, false, false));
                }
            }
        }
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        var data = new HoverBootsData(stack);
        double remainder;
        if (amount < 0) {
            remainder = Math.min(0, data.charge + amount);
            if (!simulate) {
                data.charge = Math.max(0, data.charge + amount);
            }
        } else {
            remainder = -Math.min(0, Settings.get().bufferHoverBoots - (data.charge + amount));
            if (!simulate) {
                data.charge = Math.min(Settings.get().bufferHoverBoots, data.charge + amount);
            }
        }
        if (!simulate) {
            data.save(stack);
        }
        return remainder;
    }

    @Override
    public boolean onEntityItemUpdate(@NotNull ItemStack stack, @NotNull ItemEntity entity) {
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
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDamaged(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public boolean isDamageable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public int getMaxDamage(@NotNull ItemStack stack) {
        return (int) Settings.get().bufferHoverBoots;
    }

    @Override
    public int getDamage(@NotNull ItemStack stack) {
        var data = new HoverBootsData(stack);
        return (int) (Settings.get().bufferHoverBoots * (1 - data.charge / Settings.get().bufferHoverBoots));
    }

    @Override
    public void setDamage(@NotNull ItemStack stack, int damage) {
        charge(stack, -damage, false);
        super.setDamage(stack, 0);
    }
}
