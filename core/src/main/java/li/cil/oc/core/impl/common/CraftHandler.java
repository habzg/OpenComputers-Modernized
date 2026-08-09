package li.cil.oc.core.impl.common;

import java.util.Calendar;
import java.util.function.Predicate;
import li.cil.oc.api.API;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.util.InventoryUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class CraftHandler {
    private CraftHandler() {
    }

    public static Predicate<Player> isFakePlayer = player -> false;

    public static void onItemCrafted(ItemStack stack, Level level, Player player) {
        if (player instanceof ServerPlayer serverPlayer && !isFakePlayer.test(player) && !level.isClientSide) {
            if (OCSettings.get().presentChance > 0 && !isRecraftItem(stack) &&
                    player.getRandom().nextFloat() < OCSettings.get().presentChance && timeForPresents()) {
                var present = API.items.get(Constants.ItemName.Present).createItemStack(1);
                level.playSeededSound(serverPlayer, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                        SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 0.2f, 1f, serverPlayer.getRandom().nextLong());
                InventoryUtils.addToPlayerInventory(present, serverPlayer);
            }
        }

        Achievement.onCraft(stack, player);
    }

    private static boolean isRecraftItem(ItemStack stack) {
        ItemInfo info = API.items.get(stack);
        return info == API.items.get(Constants.ItemName.NavigationUpgrade) ||
                info == API.items.get(Constants.BlockName.Microcontroller) ||
                info == API.items.get(Constants.ItemName.Drone) ||
                info == API.items.get(Constants.BlockName.Robot) ||
                info == API.items.get(Constants.ItemName.Tablet);
    }

    private static boolean timeForPresents() {
        var now = Calendar.getInstance();
        int month = now.get(Calendar.MONTH);
        int dayOfMonth = now.get(Calendar.DAY_OF_MONTH);
        return (month == Calendar.DECEMBER && dayOfMonth > 24) || (month == Calendar.JANUARY && dayOfMonth < 7) ||
                (month == Calendar.FEBRUARY && dayOfMonth == 14) ||
                (month == Calendar.APRIL && dayOfMonth == 22) ||
                (month == Calendar.MAY && dayOfMonth == 1) ||
                (month == Calendar.OCTOBER && dayOfMonth == 3) ||
                (month == Calendar.DECEMBER && dayOfMonth == 14);
    }
}
