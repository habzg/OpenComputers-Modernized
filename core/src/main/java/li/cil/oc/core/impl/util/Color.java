package li.cil.oc.core.impl.util;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.core.impl.IDyeColorProvider;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

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
  public static final Map<String, Integer> dyeRgbByOreName = new HashMap<>();
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

    dyeRgbByOreName.put("dyeBlack", 0x1D1D21);
    dyeRgbByOreName.put("dyeRed", 0xB02E26);
    dyeRgbByOreName.put("dyeGreen", 0x5E7C16);
    dyeRgbByOreName.put("dyeBrown", 0x835432);
    dyeRgbByOreName.put("dyeBlue", 0x3C44AA);
    dyeRgbByOreName.put("dyePurple", 0x892AB8);
    dyeRgbByOreName.put("dyeCyan", 0x169C9C);
    dyeRgbByOreName.put("dyeLightGray", 0x9D9D97);
    dyeRgbByOreName.put("dyeGray", 0x474F52);
    dyeRgbByOreName.put("dyePink", 0xF38BAA);
    dyeRgbByOreName.put("dyeLime", 0x80C71F);
    dyeRgbByOreName.put("dyeYellow", 0xFED83D);
    dyeRgbByOreName.put("dyeLightBlue", 0x3AB3DA);
    dyeRgbByOreName.put("dyeMagenta", 0xC74EBD);
    dyeRgbByOreName.put("dyeOrange", 0xF9801D);
    dyeRgbByOreName.put("dyeWhite", 0xF9FFFE);
  }

  private static final IDyeColorProvider provider = li.cil.oc.core.impl.DyeColorProvider.INSTANCE;

  public static int byMeta(int meta) {
    return byOreName.get(dyes[15 - meta]);
  }

  public static String findDye(ItemStack stack) {
    if (stack.isEmpty()) return null;
    return provider.findDye(stack);
  }

  public static boolean isDye(ItemStack stack) {
    return findDye(stack) != null;
  }

  public static int dyeColor(ItemStack stack) {
    String name = findDye(stack);
    return name != null ? byOreName.get(name) : 0xFF00FF;
  }

  public static int dyeRgb(String oreName) {
    Integer value = dyeRgbByOreName.get(oreName);
    return value != null ? value : 0xFF00FF;
  }

  public static int dyeRgb(DyeColor color) {
    return dyeRgb(dyes[15 - color.getId()]);
  }
}
