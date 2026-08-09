package li.cil.oc.neoforge.common.event;

import java.util.stream.Stream;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.item.HoverBoots;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

public final class HoverBootsHandler {
    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onLivingTick(PlayerTickEvent.Post e) {
        var player = e.getEntity();
        if (!(player instanceof FakePlayer)) {
            var nbt = player.getPersistentData();
            boolean hadHoverBoots = nbt.getBoolean(OCSettings.namespace + "hasHoverBoots");

            boolean hasHoverBoots = false;

            if (!player.isShiftKeyDown()) {
                var bootsStack = equippedArmor(player)
                        .filter(stack -> stack.getItem() instanceof HoverBoots)
                        .findFirst();
                if (bootsStack.isPresent()) {
                    HoverBoots boots = (HoverBoots) bootsStack.get().getItem();
                    if (!OCSettings.get().ignorePower) {
                        if (player.onGround() && !player.getAbilities().instabuild
                                && player.level().getGameTime() % OCSettings.get().tickFrequency == 0) {
                            double velocity = player.getDeltaMovement().lengthSqr();
                            if (velocity > 0.015f) {
                                boots.charge(bootsStack.get(), -OCSettings.get().hoverBootMove, false);
                            }
                        }
                        hasHoverBoots = boots.getCharge(bootsStack.get()) > 0;
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
                    var bootsStack = equippedArmor(player)
                            .filter(stack -> stack.getItem() instanceof HoverBoots)
                            .findFirst();
                    if (bootsStack.isPresent()) {
                        HoverBoots boots = (HoverBoots) bootsStack.get().getItem();
                        if (boots.getCharge(bootsStack.get()) == 0 && player.getEffect(MobEffects.MOVEMENT_SLOWDOWN) == null) {
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
    }

    @SubscribeEvent
    public static void onFall(LivingFallEvent e) {
        if (e.getDistance() > 3 && e.getEntity() instanceof Player player && !(player instanceof FakePlayer)) {
            equippedArmor(player).filter(stack -> stack.getItem() instanceof HoverBoots).findFirst().ifPresent(stack -> {
                HoverBoots boots = (HoverBoots) stack.getItem();
                double cost = -OCSettings.get().hoverBootAbsorb;
                boolean isCreative = OCSettings.get().ignorePower || player.getAbilities().instabuild;
                if (isCreative || boots.charge(stack, cost, true) == 0) {
                    if (!isCreative) boots.charge(stack, cost, false);
                    e.setDistance(e.getDistance() * 0.3f);
                }
            });
        }
    }

    private static Stream<net.minecraft.world.item.ItemStack> equippedArmor(Player player) {
        return Stream.of(
                player.getItemBySlot(EquipmentSlot.FEET),
                player.getItemBySlot(EquipmentSlot.LEGS),
                player.getItemBySlot(EquipmentSlot.CHEST),
                player.getItemBySlot(EquipmentSlot.HEAD)
        );
    }
}
