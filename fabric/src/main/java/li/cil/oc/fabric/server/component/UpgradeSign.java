package li.cil.oc.fabric.server.component;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.UpgradeSignBase;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public abstract class UpgradeSign extends UpgradeSignBase {
    @Override
    protected @NotNull Player getSignPlayer() {
        if (host() instanceof li.cil.oc.api.internal.Robot) {
            return ((li.cil.oc.api.internal.Robot) host()).player();
        }
        return FakePlayer.get((ServerLevel) host().level(), OCSettings.get().fakePlayerProfile);
    }

    @Override
    protected boolean checkSignBreak(@NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state) {
        var level = host().level();
        return PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(level, player, pos, state, level.getBlockEntity(pos));
    }

    @Override
    protected boolean fireSignPreEvent(@NotNull SignBlockEntity blockEntity, String @NotNull [] lines) {
        var event = new li.cil.oc.api.event.SignChangeEvent.Pre(blockEntity, lines);
        li.cil.oc.api.event.SignChangeEvent.Pre.EVENT.invoker().onSignChange(event);
        return !event.isCanceled();
    }

    @Override
    protected void fireSignPostEvent(@NotNull SignBlockEntity blockEntity, String @NotNull [] lines) {
        li.cil.oc.api.event.SignChangeEvent.Post.EVENT.invoker().onSignChange(new li.cil.oc.api.event.SignChangeEvent.Post(blockEntity, lines));
    }
}
