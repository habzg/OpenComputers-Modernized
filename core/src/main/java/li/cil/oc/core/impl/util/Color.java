package li.cil.oc.core.impl.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

public final class Color {
    public static final int Black = 0x444444;
    public static final int Red = 0xB3312C;
    public static final int Green = 0x339911;
    public static final int Brown = 0x51301A;
    public static final int Blue = 0x6666FF;
    public static final int Purple = 0x7B2FBE;
    public static final int Cyan = 0x66FFFF;
    public static final int LightGray = 0xABABAB;
    public static final int Gray = 0x666666;
    public static final int Pink = 0xD88198;
    public static final int Lime = 0x66FF66;
    public static final int Yellow = 0xFFFF66;
    public static final int LightBlue = 0xAAAAFF;
    public static final int Magenta = 0xC354CD;
    public static final int Orange = 0xEB8844;
    public static final int White = 0xF0F0F0;

    public static final String[] dyes = {
            "dyeBlack", "dyeRed", "dyeGreen", "dyeBrown",
            "dyeBlue", "dyePurple", "dyeCyan", "dyeLightGray",
            "dyeGray", "dyePink", "dyeLime", "dyeYellow",
            "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite"
    };

    public static final Map<String, Integer> byOreName = new HashMap<>();
    public static final int[] byTier = {LightGray, Yellow, Cyan, Magenta};

    static {
        byOreName.put("dyeBlack", Black);
        byOreName.put("dyeRed", Red);
        byOreName.put("dyeGreen", Green);
        byOreName.put("dyeBrown", Brown);
        byOreName.put("dyeBlue", Blue);
        byOreName.put("dyePurple", Purple);
        byOreName.put("dyeCyan", Cyan);
        byOreName.put("dyeLightGray", LightGray);
        byOreName.put("dyeGray", Gray);
        byOreName.put("dyePink", Pink);
        byOreName.put("dyeLime", Lime);
        byOreName.put("dyeYellow", Yellow);
        byOreName.put("dyeLightBlue", LightBlue);
        byOreName.put("dyeMagenta", Magenta);
        byOreName.put("dyeOrange", Orange);
        byOreName.put("dyeWhite", White);
    }

    public static int byMeta(int meta) {
        return byOreName.get(dyes[15 - meta]);
    }

    public static String findDye(ItemStack stack) {
        if (stack.isEmpty()) return null;
        Holder<net.minecraft.world.item.Item> itemHolder = BuiltInRegistries.ITEM.wrapAsHolder(stack.getItem());
        for (DyeColor dye : DyeColor.values()) {
            if (itemHolder.is(dye.getTag())) {
                return dyes[15 - dye.getId()];
            }
        }
        return null;
    }

    public static boolean isDye(ItemStack stack) {
        return findDye(stack) != null;
    }

    public static int dyeColor(ItemStack stack) {
        String name = findDye(stack);
        return name != null ? byOreName.get(name) : 0xFF00FF;
    }
}
