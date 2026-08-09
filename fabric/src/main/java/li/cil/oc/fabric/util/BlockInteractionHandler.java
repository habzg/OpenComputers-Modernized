package li.cil.oc.fabric.util;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockPosition;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BlockInteractionHandler implements li.cil.oc.core.impl.util.BlockInteractionHandler {
    @Override
    public Player getFakePlayer(Level world, BlockPosition pos) {
        if (world instanceof ServerLevel serverLevel) {
            Player player = FakePlayer.get(serverLevel, OCSettings.get().fakePlayerProfile);
            player.setPos(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
            return player;
        }
        return null;
    }

    @Override
    public boolean mayInteract(Level world, BlockPosition pos, Direction face) {
        if (!world.isLoaded(pos.toBlockPos()) || !world.getWorldBorder().isWithinBounds(pos.toBlockPos())) {
            return false;
        }
        if (!(world instanceof ServerLevel serverLevel)) {
            return true;
        }
        Player player = FakePlayer.get(serverLevel, OCSettings.get().fakePlayerProfile);
        player.setPos(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        BlockPos blockPos = new BlockPos(pos.x(), pos.y(), pos.z());
        BlockHitResult hit = new BlockHitResult(
                new Vec3(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5), face, blockPos, false);
        InteractionResult result = UseBlockCallback.EVENT.invoker().interact(player, world, InteractionHand.MAIN_HAND, hit);
        return result != InteractionResult.FAIL;
    }

    @Override
    public boolean checkBlockBreak(Level world, BlockPos pos, BlockState state) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return false;
        }
        Player player = FakePlayer.get(serverLevel, OCSettings.get().fakePlayerProfile);
        player.setPos(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
        boolean allowed = PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                world, player, pos, state, world.getBlockEntity(pos));
        return !allowed;
    }
}
