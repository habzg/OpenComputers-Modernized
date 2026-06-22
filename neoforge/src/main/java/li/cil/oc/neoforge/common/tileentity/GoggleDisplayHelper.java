package li.cil.oc.neoforge.common.tileentity;

import li.cil.oc.core.impl.integration.util.WirelessRedstone;
import li.cil.oc.neoforge.common.block.ChameliumBlock;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GoggleDisplayHelper {
    private static final float DEFAULT_SPACE_WIDTH = 4.0F;

    private GoggleDisplayHelper() {
    }

    private static String gogglesIndent() {
        var font = Minecraft.getInstance().font;
        int spaceWidth = font.width(" ");
        int indents = spaceWidth == DEFAULT_SPACE_WIDTH ? 4 : Mth.ceil(DEFAULT_SPACE_WIDTH * 4 / spaceWidth);
        return " ".repeat(indents);
    }

    public static boolean appendWirelessInfo(List<Component> tooltip, BlockEntity be) {
        boolean found = false;
        if (be instanceof li.cil.oc.core.impl.common.tileentity.Case c) {
            for (int i = 0; i < c.getContainerSize(); i++) {
                found |= appendFreq(tooltip, c.getItem(i));
            }
        } else if (be instanceof li.cil.oc.neoforge.common.tileentity.Robot r) {
            for (int i = 0; i < r.getContainerSize(); i++) {
                found |= appendFreq(tooltip, r.getItem(i));
            }
        }
        return found;
    }

    private static boolean appendFreq(List<Component> tooltip, ItemStack stack) {
        int freq = readWirelessFrequency(stack);
        if (freq < 0) return false;
        String pad = gogglesIndent();
        if (!WirelessRedstone.canHandleFrequency("create", freq)) {
            tooltip.add(Component.literal(pad + "Unsupported Wireless Frequency: " + freq).withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal(pad + "Use a frequency from 0 to 255 for Create support.").withStyle(ChatFormatting.GRAY));
        } else if (isProviderDisabled(stack)) {
            tooltip.add(Component.literal(pad + "Wireless Frequency: " + freq).withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.literal(pad + "Create provider is disabled for this card.").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal(pad + "Enable it with setWirelessProviders().").withStyle(ChatFormatting.GRAY));
        } else {
            tooltip.add(Component.literal(pad + "Wireless Frequency: " + freq).withStyle(ChatFormatting.GOLD));
            DyeColor c1 = ChameliumBlock.dyeColorFromFrequency(freq);
            tooltip.add(Component.literal(pad + "Slot 1: Chamelium (").append(Component.translatable("color.minecraft." + c1.getName())).append(")").withStyle(ChatFormatting.GRAY));
            if (freq >= 16) {
                DyeColor c2 = ChameliumBlock.dyeColorFromFrequency(freq >> 4);
                tooltip.add(Component.literal(pad + "Slot 2: Chamelium (").append(Component.translatable("color.minecraft." + c2.getName())).append(")").withStyle(ChatFormatting.GRAY));
            }
        }
        return true;
    }

    private static int readWirelessFrequency(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return -1;
        CompoundTag tag = customData.copyTag();
        CompoundTag ocData = tag.getCompound("oc:data");
        if (!ocData.contains("wirelessFrequency")) return -1;
        return ocData.getInt("wirelessFrequency");
    }

    private static boolean isProviderDisabled(ItemStack stack) {
        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData == null || customData.isEmpty()) return false;
        CompoundTag tag = customData.copyTag();
        CompoundTag ocData = tag.getCompound("oc:data");
        if (!ocData.contains("enabledProviders")) return false;
        var list = ocData.getList("enabledProviders", 8);
        if (list.isEmpty()) return false;
        Set<String> enabled = new HashSet<>();
        for (int i = 0; i < list.size(); i++) {
            enabled.add(list.getString(i));
        }
        return !enabled.contains("create");
    }
}
