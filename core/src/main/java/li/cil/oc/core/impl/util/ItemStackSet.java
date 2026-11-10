package li.cil.oc.core.impl.util;

import java.io.Serial;
import java.util.LinkedHashSet;
import net.minecraft.world.item.ItemStack;

/** An approximation of Scala's mutable.Set[ItemStack] */
public final class ItemStackSet extends LinkedHashSet<ItemStack> {
  @Serial
  private static final long serialVersionUID = 2032603309901398960L;

  @Override
  public boolean add(ItemStack stack) {
    if (stack == null) return false;
    for (var existing : this) {
      if (ItemStack.matches(existing, stack)) return false;
    }
    return super.add(stack);
  }

  @Override
  public boolean contains(Object o) {
    if (o instanceof ItemStack stack) {
      for (var existing : this) {
        if (ItemStack.matches(existing, stack)) return true;
      }
      return false;
    }
    return super.contains(o);
  }

  @Override
  public boolean remove(Object o) {
    if (o instanceof ItemStack stack) {
      for (var existing : this) {
        if (ItemStack.matches(existing, stack)) return super.remove(existing);
      }
      return false;
    }
    return super.remove(o);
  }
}
