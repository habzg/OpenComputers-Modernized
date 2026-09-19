package li.cil.oc.core.impl.common.item;

import java.util.List;
import li.cil.oc.core.impl.ClientTerminalOpener;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Terminal extends DelegateItem {
  @SuppressWarnings("unused")
  public Terminal(Item.Properties properties) {
    super(properties.stacksTo(1));
  }

  private static ClientTerminalOpener terminalOpener = (player, stack, key, address) -> {
  };

  public static void setTerminalOpener(ClientTerminalOpener handler) {
    terminalOpener = handler;
  }

  public boolean hasServer(ItemStack stack) {
    if (stack.isEmpty()) return false;
    CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
    return cd != null && !cd.isEmpty() && cd.copyTag().contains(OCSettings.namespace + "server");
  }

  @Override
  public void appendHoverText(@NotNull ItemStack stack, Item.@NotNull TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
    super.appendHoverText(stack, context, tooltip, flag);
    if (hasServer(stack)) {
      CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
      if (cd == null) return;
      String server = cd.copyTag().getString(OCSettings.namespace + "server");
      String shown = server.length() > 13 ? server.substring(0, 13) + "." : server;
      tooltip.add(Component.literal("§8" + shown + "§7"));
    }
  }

  @Override
  public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
    var stack = player.getItemInHand(hand);
    if (stack.isEmpty()) return super.use(level, player, hand);
    if (player.isShiftKeyDown()) return super.use(level, player, hand);
    CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
    if (cd == null || cd.isEmpty()) return super.use(level, player, hand);
    CompoundTag tag = cd.copyTag();
    String key = tag.getString(OCSettings.namespace + "key");
    String address = tag.getString(OCSettings.namespace + "server");
    if (key.isEmpty() || address.isEmpty()) {
      return super.use(level, player, hand);
    }
    if (level.isClientSide) {
      terminalOpener.handle(player, stack, key, address);
    }
    player.swing(hand);
    return super.use(level, player, hand);
  }
}
