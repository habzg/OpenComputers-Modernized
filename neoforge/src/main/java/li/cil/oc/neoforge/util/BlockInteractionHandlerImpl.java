package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.BlockInteractionHandler;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class BlockInteractionHandlerImpl implements BlockInteractionHandler {
    @Override
    public Player getFakePlayer(Level level, BlockPosition pos) {
        Player player = FakePlayerFactory.get((ServerLevel) level, OCSettings.get().fakePlayerProfile);
        player.setPos(pos.x() + 0.5, pos.y() + 0.5, pos.z() + 0.5);
        return player;
    }

    @Override
    public boolean mayInteract(Level level, BlockPosition blockPos, Direction face) {
        var pos = new BlockPos(blockPos.x(), blockPos.y(), blockPos.z());
        var hitVec = new net.minecraft.world.phys.BlockHitResult(
                new net.minecraft.world.phys.Vec3(blockPos.x() + 0.5, blockPos.y() + 0.5, blockPos.z() + 0.5),
                face, pos, false
        );
        PlayerInteractEvent.RightClickBlock event = new PlayerInteractEvent.RightClickBlock(
                getFakePlayer(level, blockPos), net.minecraft.world.InteractionHand.MAIN_HAND, pos, hitVec
        );
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    public boolean checkBlockBreak(Level level, BlockPos pos, BlockState state) {
        BlockPosition bPos = new BlockPosition(pos.getX(), pos.getY(), pos.getZ(), level);
        var event = new BlockEvent.BreakEvent(level, pos, state, getFakePlayer(level, bPos));
        NeoForge.EVENT_BUS.post(event);
        return event.isCanceled();
    }
}
