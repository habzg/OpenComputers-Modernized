package li.cil.oc.core.impl.common.recipe;

import java.util.ArrayList;
import java.util.List;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ItemColorizer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ColorizeVariants {
  public record Variant(ItemStack input, ItemStack dye, ItemStack output) {
  }

  public static List<Variant> colorize(Item target) {
    List<Variant> variants = new ArrayList<>();
    for (DyeColor color : DyeColor.values()) {
      ItemStack output = new ItemStack(target);
      ItemColorizer.setColor(output, Color.dyeRgb(color));
      variants.add(new Variant(new ItemStack(target), new ItemStack(DyeItem.byColor(color)), output));
    }
    return variants;
  }

  public static List<Variant> decolorize(Item target) {
    return List.of(new Variant(new ItemStack(target), new ItemStack(Items.WATER_BUCKET), new ItemStack(target)));
  }

  private ColorizeVariants() {
  }
}