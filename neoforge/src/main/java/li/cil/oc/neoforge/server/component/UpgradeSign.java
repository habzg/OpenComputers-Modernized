package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.event.SignChangeEvent;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.UpgradeSignBase;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.NotNull;

public abstract class UpgradeSign extends UpgradeSignBase {
    @Override
    protected @NotNull Player getSignPlayer() {
        if (host() instanceof li.cil.oc.api.internal.Robot) {
            return ((li.cil.oc.api.internal.Robot) host()).player();
        }
        return FakePlayerFactory.get((ServerLevel) host().level(), OCSettings.get().fakePlayerProfile);
    }

    @Override
    protected boolean checkSignBreak(@NotNull Player player, @NotNull BlockPos pos, @NotNull BlockState state) {
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(host().level(), pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    protected boolean fireSignPreEvent(@NotNull SignBlockEntity blockEntity, String @NotNull [] lines) {
        SignChangeEvent.Pre event = new SignChangeEvent.Pre(blockEntity, lines);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    @Override
    protected void fireSignPostEvent(@NotNull SignBlockEntity blockEntity, String @NotNull [] lines) {
        NeoForge.EVENT_BUS.post(new SignChangeEvent.Post(blockEntity, lines));
    }
}
