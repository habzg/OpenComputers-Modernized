package li.cil.oc.neoforge.common.nanomachines.provider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.nanomachines.provider.ScalaProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.TriState;

public class DisintegrationProvider extends ScalaProvider {
    public DisintegrationProvider() {
        super("c4e7e3c2-8069-4fbb-b08e-74b1bddcdfe7");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        return Collections.singletonList(new DisintegrationBehavior(player));
    }

    @Override
    public Behavior readBehaviorFromNBT(Player player, CompoundTag ignoredNbt) {
        return new DisintegrationBehavior(player);
    }

    public static class DisintegrationBehavior extends AbstractBehavior {
        public Map<BlockPos, SlowBreakInfo> breakingMap = new HashMap<>();
        public Map<BlockPos, SlowBreakInfo> breakingMapNew = new HashMap<>();

        public DisintegrationBehavior(Player player) {
            super(player);
        }

        @Override
        public void onDisable(DisableReason ignoredReason) {
            var world = player.level();
            for (var pos : breakingMap.keySet()) {
                world.destroyBlockProgress(pos.hashCode(), pos, -1);
            }
            breakingMap.clear();
        }

        @Override
        public void update() {
            var world = player.level();
            if (!world.isClientSide && player instanceof ServerPlayer playerMP && !(player instanceof FakePlayer)) {
                long now = world.getGameTime();
                BlockPos playerPos = player.blockPosition();
                int actualRange = OCSettings.get().nanomachineDisintegrationRange * li.cil.oc.api.Nanomachines.getController(player).getInputCount(this);

                for (int x = -actualRange; x <= actualRange; x++) {
                    for (int y = 0; y <= actualRange * 2; y++) {
                        for (int z = -actualRange; z <= actualRange; z++) {
                            BlockPos pos = playerPos.offset(x, y, z);
                            SlowBreakInfo info = breakingMap.get(pos);
                            if (info != null && info.checkTool(player)) {
                                breakingMapNew.put(pos, info);
                                info.update(world, player, now);
                            } else {
                                var event = CommonHooks.onLeftClickBlock(player, pos, net.minecraft.core.Direction.UP, net.minecraft.network.protocol.game.ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK);
                                boolean allowed = !event.isCanceled() && event.getUseBlock() != TriState.FALSE && event.getUseItem() != TriState.FALSE;
                                boolean adventureOk = !player.blockActionRestricted(world, pos, playerMP.gameMode.getGameModeForPlayer());
                                if (allowed && adventureOk && !world.isEmptyBlock(pos)) {
                                    BlockState state = world.getBlockState(pos);
                                    float hardness = state.getDestroyProgress(player, world, pos);
                                    if (hardness > 0) {
                                        int timeToBreak = (int) (1 / hardness);
                                        if (timeToBreak < 20 * 30) {
                                            ItemStack heldItem = player.getMainHandItem();
                                            SlowBreakInfo newInfo = new SlowBreakInfo(now, now + timeToBreak, pos, heldItem.isEmpty() ? null : heldItem.copy(), state.getBlock(), state);
                                            world.destroyBlockProgress(pos.hashCode(), pos, 0);
                                            breakingMapNew.put(pos, newInfo);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                for (var entry : breakingMap.entrySet()) {
                    BlockPos pos = entry.getKey();
                    SlowBreakInfo info = entry.getValue();
                    if (info.timeBroken < now) {
                        breakingMapNew.remove(pos);
                        info.finish(world, playerMP);
                    }
                }

                for (var pos : breakingMap.keySet()) {
                    if (!breakingMapNew.containsKey(pos)) {
                        world.destroyBlockProgress(pos.hashCode(), pos, -1);
                    }
                }

                var tmp = breakingMap;
                breakingMap.clear();
                breakingMap = breakingMapNew;
                breakingMapNew = tmp;
            }
        }
    }

    public static class SlowBreakInfo {
        public final long timeStarted;
        public final long timeBroken;
        public final BlockPos pos;
        public final ItemStack originalTool;
        public final Block block;
        public final BlockState state;
        public int lastDamageSent = 0;

        public SlowBreakInfo(long timeStarted, long timeBroken, BlockPos pos, ItemStack originalTool, Block block, BlockState state) {
            this.timeStarted = timeStarted;
            this.timeBroken = timeBroken;
            this.pos = pos;
            this.originalTool = originalTool;
            this.block = block;
            this.state = state;
        }

        public boolean checkTool(Player player) {
            ItemStack currentTool = player.getMainHandItem();
            if (currentTool.isEmpty() && originalTool == null) return true;
            if (currentTool.isEmpty() || originalTool == null) return false;
            return currentTool.getItem() == originalTool.getItem() &&
                    (currentTool.isDamageableItem() || currentTool.getDamageValue() == originalTool.getDamageValue());
        }

        @SuppressWarnings("unused")
        public void update(Level world, Player player, long now) {
            long timeTotal = timeBroken - timeStarted;
            if (timeTotal > 0) {
                long timeTaken = now - timeStarted;
                int damage = (int) (10 * timeTaken / timeTotal);
                if (damage != lastDamageSent) {
                    lastDamageSent = damage;
                    world.destroyBlockProgress(pos.hashCode(), pos, lastDamageSent);
                }
            }
        }

        public void finish(Level world, ServerPlayer player) {
            boolean sameBlock = world.getBlockState(pos).getBlock() == block;
            if (sameBlock) {
                world.destroyBlockProgress(pos.hashCode(), pos, -1);
                if (player.gameMode.destroyBlock(pos)) {
                    world.levelEvent(2001, pos, Block.getId(state));
                }
            }
        }
    }
}
