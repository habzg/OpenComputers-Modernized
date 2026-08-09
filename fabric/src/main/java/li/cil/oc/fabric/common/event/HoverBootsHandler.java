package li.cil.oc.fabric.common.event;

import java.util.stream.Stream;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.HoverBoots;
import li.cil.oc.core.impl.util.ItemColorizer;
import li.cil.oc.core.impl.util.PlayerUtils;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class HoverBootsHandler {
    private static boolean processingFallDamage = false;

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (player instanceof FakePlayer) continue;
                var nbt = PlayerUtils.persistedData(player);
                boolean hadHoverBoots = nbt.getBoolean(OCSettings.namespace + "hasHoverBoots");
                boolean hasHoverBoots = false;

                if (!player.isShiftKeyDown()) {
                    var bootsStack = findHoverBoots(player);
                    if (bootsStack != null) {
                        HoverBoots boots = (HoverBoots) bootsStack.getItem();
                        if (!OCSettings.get().ignorePower) {
                            if (player.onGround() && !player.getAbilities().instabuild
                                    && player.level().getGameTime() % OCSettings.get().tickFrequency == 0) {
                                double velocity = player.getDeltaMovement().lengthSqr();
                                if (velocity > 0.015f) {
                                    boots.charge(bootsStack, -OCSettings.get().hoverBootMove, false);
                                }
                            }
                            hasHoverBoots = boots.getCharge(bootsStack) > 0;
                        } else {
                            hasHoverBoots = true;
                        }
                    }
                }

                if (hasHoverBoots != hadHoverBoots) {
                    nbt.putBoolean(OCSettings.namespace + "hasHoverBoots", hasHoverBoots);
                }

                if (hasHoverBoots) {
                    if (player.onGround()) {
                        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 100, 3, false, false, true));
                    } else {
                        var jumpBoost = player.getEffect(MobEffects.JUMP);
                        if (jumpBoost != null && jumpBoost.getAmplifier() == 3) {
                            player.removeEffect(MobEffects.JUMP);
                        }
                    }
                    if (!player.onGround() && player.fallDistance < 5 && player.getDeltaMovement().y < 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, true));
                    } else {
                        player.removeEffect(MobEffects.SLOW_FALLING);
                    }
                } else {
                    var jumpBoost = player.getEffect(MobEffects.JUMP);
                    if (jumpBoost != null && jumpBoost.getAmplifier() == 3) {
                        player.removeEffect(MobEffects.JUMP);
                    }
                    player.removeEffect(MobEffects.SLOW_FALLING);
                    if (!OCSettings.get().ignorePower) {
                        var bootsStack = findHoverBoots(player);
                        if (bootsStack != null) {
                            HoverBoots boots = (HoverBoots) bootsStack.getItem();
                            if (boots.getCharge(bootsStack) == 0 && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null) {
                                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
                            }
                        }
                    }
                }

                var stepHeightAttr = player.getAttribute(Attributes.STEP_HEIGHT);
                if (stepHeightAttr != null) {
                    stepHeightAttr.setBaseValue(hasHoverBoots ? 1.0 : 0.5);
                }
            }

            for (var serverLevel : server.getAllLevels()) {
                for (var entity : serverLevel.getAllEntities()) {
                    if (!(entity instanceof ItemEntity itemEntity)) continue;
                    var stack = itemEntity.getItem();
                    if (!(stack.getItem() instanceof HoverBoots)) continue;
                    if (!ItemColorizer.hasColor(stack)) continue;
                    BlockPos pos = itemEntity.blockPosition();
                    BlockState state = serverLevel.getBlockState(pos);
                    if (state.is(Blocks.WATER_CAULDRON)) {
                        int cauldronLevel = state.getValue(LayeredCauldronBlock.LEVEL);
                        if (cauldronLevel > 0) {
                            ItemColorizer.removeColor(stack);
                            LayeredCauldronBlock.lowerFillLevel(state, serverLevel, pos);
                        }
                    }
                }
            }
        });

        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (processingFallDamage) return true;
            if (!source.is(DamageTypes.FALL)) return true;
            if (!(entity instanceof ServerPlayer player) || player instanceof FakePlayer) return true;

            var bootsStack = findHoverBoots(player);
            if (bootsStack == null) return true;

            HoverBoots boots = (HoverBoots) bootsStack.getItem();
            double cost = -OCSettings.get().hoverBootAbsorb;
            boolean isCreative = OCSettings.get().ignorePower || player.getAbilities().instabuild;
            if (isCreative || boots.charge(bootsStack, cost, true) == 0) {
                if (!isCreative) boots.charge(bootsStack, cost, false);
                float newAmount = (float) Math.ceil((amount + 3) * 0.3 - 3);
                if (newAmount > 0) {
                    processingFallDamage = true;
                    try {
                        player.hurt(source, newAmount);
                    } finally {
                        processingFallDamage = false;
                    }
                }
                return false;
            }
            return true;
        });
    }

    private static ItemStack findHoverBoots(Player player) {
        return equippedArmor(player)
                .filter(stack -> stack.getItem() instanceof HoverBoots)
                .findFirst()
                .orElse(null);
    }

    private static Stream<ItemStack> equippedArmor(Player player) {
        return Stream.of(
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD)
        );
    }
}
