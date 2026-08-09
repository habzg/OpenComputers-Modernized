package li.cil.oc.core.impl.common.nanomachines.provider;

import java.util.Collections;
import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractBehavior;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MagnetProvider extends ScalaProvider {
    public MagnetProvider() {
        super("9324d5ec-71f1-41c2-b51c-406e527668fc");
    }

    @Override
    public Iterable<Behavior> createScalaBehaviors(Player player) {
        return Collections.singletonList(new MagnetBehavior(player));
    }

    @Override
    public Behavior readBehaviorFromNBT(Player player, CompoundTag nbt) {
        return new MagnetBehavior(player);
    }

    public static class MagnetBehavior extends AbstractBehavior {
        public MagnetBehavior(Player player) {
            super(player);
        }

        @Override
        public String getNameHint() {
            return "magnet";
        }

        @Override
        public void update() {
            var world = player.level();
            if (!world.isClientSide) {
                double actualRange = OCSettings.get().nanomachineMagnetRange * li.cil.oc.api.Nanomachines.getController(player).getInputCount(this);
                var items = world.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(actualRange));
                for (var item : items) {
                    if (!item.hasPickUpDelay()) {
                        item.getItem();
                        var inv = player.getInventory();
                        boolean canPickup = false;
                        for (var stack : inv.items) {
                            if (stack.isEmpty() || (stack.getCount() < stack.getMaxStackSize() && ItemStack.isSameItem(stack, item.getItem()))) {
                                canPickup = true;
                                break;
                            }
                        }
                        if (canPickup) {
                            double dx = player.getX() - item.getX();
                            double dy = player.getY() - item.getY();
                            double dz = player.getZ() - item.getZ();
                            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
                            if (len > 0) {
                                dx /= len;
                                dy /= len;
                                dz /= len;
                                item.push(dx * 0.1, dy * 0.1, dz * 0.1);
                            }
                        }
                    }
                }
            }
        }
    }
}
