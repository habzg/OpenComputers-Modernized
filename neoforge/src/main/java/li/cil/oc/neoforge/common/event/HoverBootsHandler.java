package li.cil.oc.neoforge.common.event;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.HoverBoots;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.stream.Stream;

public final class HoverBootsHandler {
    @SuppressWarnings("unused")
    @SubscribeEvent
    public static void onLivingTick(PlayerTickEvent.Post e) {
        var player = e.getEntity();
        if (!(player instanceof FakePlayer)) {
            var nbt = player.getPersistentData();
            boolean hadHoverBoots = nbt.getBoolean(Settings.namespace + "hasHoverBoots");

            boolean hasHoverBoots = false;

            if (!player.isShiftKeyDown()) {
                var bootsStack = equippedArmor(player)
                        .filter(stack -> stack.getItem() instanceof HoverBoots)
                        .findFirst();
                if (bootsStack.isPresent()) {
                    HoverBoots boots = (HoverBoots) bootsStack.get().getItem();
                    if (!Settings.get().ignorePower) {
                        if (player.onGround() && !player.getAbilities().instabuild
                                && player.level().getGameTime() % Settings.get().tickFrequency == 0) {
                            double velocity = player.getDeltaMovement().lengthSqr();
                            if (velocity > 0.015f) {
                                boots.charge(bootsStack.get(), -Settings.get().hoverBootMove, false);
                            }
                        }
                        hasHoverBoots = boots.getCharge(bootsStack.get()) > 0;
                    } else {
                        hasHoverBoots = true;
                    }
                }
            }

            if (hasHoverBoots != hadHoverBoots) {
                nbt.putBoolean(Settings.namespace + "hasHoverBoots", hasHoverBoots);
            }

            if (hasHoverBoots) {
                if (!player.onGround() && player.fallDistance < 5 && player.getDeltaMovement().y < 0) {
                    player.setDeltaMovement(player.getDeltaMovement().multiply(1, 0.9, 1));
                }
            } else if (!Settings.get().ignorePower) {
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
                double cost = -Settings.get().hoverBootAbsorb;
                boolean isCreative = Settings.get().ignorePower || player.getAbilities().instabuild;
                if (isCreative || boots.charge(stack, cost, true) == 0) {
                    if (!isCreative) boots.charge(stack, cost, false);
                    e.setDistance(e.getDistance() * 0.3f);
                }
            });
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent e) {
        if (e.getEntity() instanceof Player player && !(player instanceof FakePlayer) && !player.isShiftKeyDown()) {
            equippedArmor(player).filter(stack -> stack.getItem() instanceof HoverBoots).findFirst().ifPresent(stack -> {
                HoverBoots boots = (HoverBoots) stack.getItem();
                double cost = -Settings.get().hoverBootJump;
                boolean isCreative = Settings.get().ignorePower || player.getAbilities().instabuild;
                if (isCreative || boots.charge(stack, cost, true) == 0) {
                    if (!isCreative) boots.charge(stack, cost, false);
                    if (player.isSprinting())
                        player.push(player.getDeltaMovement().x * 0.5, 0.4, player.getDeltaMovement().z * 0.5);
                    else
                        player.push(0, 0.4, 0);
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
