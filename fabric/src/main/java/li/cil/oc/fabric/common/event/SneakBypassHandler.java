package li.cil.oc.fabric.common.event;

import li.cil.oc.core.impl.common.item.ItemUseFirst;
import li.cil.oc.core.impl.common.item.SneakBypass;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** This is needed because Fabric doesn't have anything like (Neo)Forge's doesSneakBypassUse */
public final class SneakBypassHandler {

  public static void init() {
    UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
      if (player.isSpectator()) return InteractionResult.PASS;
      var stack = player.getItemInHand(hand);
      if (!(player instanceof li.cil.oc.core.impl.server.agent.AgentPlayer) && stack.getItem() instanceof ItemUseFirst useFirst) {
        if (useFirst.useFirst(new UseOnContext(player, hand, hit)).consumesAction()) {
          return InteractionResult.SUCCESS;
        }
      }

      if (!player.isShiftKeyDown()) return InteractionResult.PASS;
      var pos = hit.getBlockPos();
      var main = player.getMainHandItem();
      var off = player.getOffhandItem();
      if (main.isEmpty() && off.isEmpty()) return InteractionResult.PASS;
      if (!SneakBypass.bypasses(main, world, pos) || !SneakBypass.bypasses(off, world, pos)) {
        return InteractionResult.PASS;
      }
      if (!world.isClientSide && !(player instanceof net.minecraft.server.level.ServerPlayer)) {
        return InteractionResult.PASS;
      }
      return activateBlock(player, world, hand, hit);
    });
  }

  private static InteractionResult activateBlock(Player player, Level world, InteractionHand hand, BlockHitResult hit) {
    var state = world.getBlockState(hit.getBlockPos());
    var result = state.useItemOn(player.getItemInHand(hand), world, player, hand, hit);
    if (result.consumesAction()) return InteractionResult.SUCCESS;
    if (result == ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION && hand == InteractionHand.MAIN_HAND) {
      if (state.useWithoutItem(world, player, hit).consumesAction()) return InteractionResult.SUCCESS;
    }
    return InteractionResult.PASS;
  }
}
