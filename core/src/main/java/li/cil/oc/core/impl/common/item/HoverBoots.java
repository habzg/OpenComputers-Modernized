package li.cil.oc.core.impl.common.item;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.CraftHandler;
import li.cil.oc.core.impl.common.item.data.HoverBootsData;
import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class HoverBoots extends ArmorItem implements Chargeable {
  public HoverBoots(Properties properties) {
        super(ArmorMaterials.DIAMOND, ArmorItem.Type.BOOTS, properties.rarity(Rarity.UNCOMMON));
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack stack, @NotNull Level level, @NotNull Player player) {
        super.onCraftedBy(stack, level, player);
        CraftHandler.onItemCrafted(stack, level, player);
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
        return OCSettings.get().bufferHoverBoots;
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
            if (!OCSettings.get().ignorePower && getCharge(stack) == 0) {
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
            remainder = -Math.min(0, OCSettings.get().bufferHoverBoots - (data.charge + amount));
            if (!simulate) {
                data.charge = Math.min(OCSettings.get().bufferHoverBoots, data.charge + amount);
            }
        }
        if (!simulate) {
            data.save(stack);
        }
        return remainder;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        double charge = getCharge(stack);
        double max = maxCharge(stack);
        return max > 0 ? (int) Math.round(13.0 * charge / max) : 0;
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        double charge = getCharge(stack);
        double max = maxCharge(stack);
        var fill = max > 0 ? charge / max : 0;
        int r = (int) (255 - fill * 255);
        int g = (int) (fill * 255);
        return (r << 16) | (g << 8);
    }
}
