package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.Items;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.util.InventoryUtils;
import li.cil.oc.core.impl.util.ItemUtils;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Random;

public class Present extends DelegateItem {
    private static final Logger LOGGER = LoggerFactory.getLogger(Present.class);
    private static final Random rng = new Random();
    private static ItemStack[] presents = null;

    public Present(Item.Properties properties) {
        super(properties);
    }

    private static ItemStack nextPresent() {
        if (presents == null) {
            initPresents();
        }
        return presents[rng.nextInt(presents.length)].copy();
    }

    private static void initPresents() {
        var result = new ArrayList<ItemStack>();
        add(result, Constants.ItemName.ArrowKeys, 520);
        add(result, Constants.ItemName.ButtonGroup, 460);
        add(result, Constants.ItemName.NumPad, 410);
        add(result, Constants.ItemName.Disk, 370);
        add(result, Constants.ItemName.Transistor, 350);
        add(result, Constants.ItemName.Floppy, 340);
        add(result, Constants.ItemName.PrintedCircuitBoard, 320);
        add(result, Constants.ItemName.ChipTier1, 290);
        add(result, Constants.ItemName.EEPROM, 250);
        add(result, Constants.ItemName.Interweb, 220);
        add(result, Constants.ItemName.Card, 190);
        add(result, Constants.ItemName.Analyzer, 170);
        add(result, Constants.ItemName.SignUpgrade, 150);
        add(result, Constants.ItemName.InventoryUpgrade, 130);
        add(result, Constants.ItemName.CraftingUpgrade, 110);
        add(result, Constants.ItemName.TankUpgrade, 90);
        add(result, Constants.ItemName.PistonUpgrade, 80);
        add(result, Constants.ItemName.LeashUpgrade, 70);
        add(result, Constants.ItemName.AngelUpgrade, 55);
        add(result, Constants.ItemName.RedstoneCardTier1, 50);
        add(result, Constants.ItemName.RAMTier1, 48);
        add(result, Constants.ItemName.ControlUnit, 46);
        add(result, Constants.ItemName.Alu, 45);
        add(result, Constants.ItemName.BatteryUpgradeTier1, 43);
        add(result, Constants.ItemName.NetworkCard, 38);
        add(result, Constants.ItemName.WirelessNetworkCardTier1, 37);
        add(result, Constants.ItemName.HDDTier1, 36);
        add(result, Constants.ItemName.GeneratorUpgrade, 35);
        add(result, Constants.ItemName.CPUTier1, 31);
        add(result, Constants.ItemName.MicrocontrollerCaseTier1, 30);
        add(result, Constants.ItemName.DroneCaseTier1, 25);
        add(result, Constants.ItemName.UpgradeContainerTier1, 23);
        add(result, Constants.ItemName.CardContainerTier1, 23);
        add(result, Constants.ItemName.GraphicsCardTier1, 19);
        add(result, Constants.ItemName.RedstoneCardTier2, 17);
        add(result, Constants.ItemName.RAMTier2, 15);
        add(result, Constants.ItemName.DatabaseUpgradeTier1, 15);
        add(result, Constants.ItemName.ChipTier2, 15);
        add(result, Constants.ItemName.ComponentBusTier1, 13);
        add(result, Constants.ItemName.BatteryUpgradeTier2, 12);
        add(result, Constants.ItemName.WirelessNetworkCardTier2, 11);
        add(result, Constants.ItemName.RAMTier3, 10);
        add(result, Constants.ItemName.ServerTier1, 10);
        add(result, Constants.ItemName.InternetCard, 9);
        add(result, Constants.ItemName.Terminal, 9);
        add(result, Constants.ItemName.SolarGeneratorUpgrade, 9);
        add(result, Constants.ItemName.HDDTier2, 7);
        add(result, Constants.ItemName.NavigationUpgrade, 7);
        add(result, Constants.ItemName.InventoryControllerUpgrade, 7);
        add(result, Constants.ItemName.TankControllerUpgrade, 7);
        add(result, Constants.ItemName.CPUTier2, 6);
        add(result, Constants.ItemName.MicrocontrollerCaseTier2, 6);
        add(result, Constants.ItemName.ComponentBusTier2, 6);
        add(result, Constants.ItemName.TabletCaseTier1, 5);
        add(result, Constants.ItemName.UpgradeContainerTier2, 5);
        add(result, Constants.ItemName.CardContainerTier2, 5);
        add(result, Constants.ItemName.GraphicsCardTier2, 4);
        add(result, Constants.ItemName.RAMTier4, 4);
        add(result, Constants.ItemName.DroneCaseTier2, 4);
        add(result, Constants.ItemName.DatabaseUpgradeTier2, 4);
        add(result, Constants.ItemName.ServerTier2, 4);
        add(result, Constants.ItemName.ChipTier3, 3);
        add(result, Constants.ItemName.ComponentBusTier3, 3);
        add(result, Constants.ItemName.TractorBeamUpgrade, 3);
        add(result, Constants.ItemName.BatteryUpgradeTier3, 3);
        add(result, Constants.ItemName.ExperienceUpgrade, 2);
        add(result, Constants.ItemName.RAMTier5, 2);
        add(result, Constants.ItemName.UpgradeContainerTier3, 2);
        add(result, Constants.ItemName.CardContainerTier3, 2);
        add(result, Constants.ItemName.TabletCaseTier2, 1);
        add(result, Constants.ItemName.HDDTier3, 1);
        add(result, Constants.ItemName.ChunkloaderUpgrade, 1);
        add(result, Constants.ItemName.CPUTier3, 1);
        add(result, Constants.ItemName.GraphicsCardTier3, 1);
        add(result, Constants.ItemName.ServerTier3, 1);
        add(result, Constants.ItemName.DatabaseUpgradeTier3, 1);
        add(result, Constants.ItemName.RAMTier6, 1);
        presents = result.toArray(new ItemStack[0]);
    }

    private static void add(ArrayList<ItemStack> result, String name, int weight) {
        var item = Items.get(name);
        if (item != null) {
            var stack = item.createItemStack(1);
            if (ItemUtils.getIngredients(stack).length > 0) {
                for (int i = 0; i < weight; i++) {
                    result.add(stack);
                }
            }
        } else {
            LOGGER.warn("Oops, trying to add '{}' as a present even though it doesn't exist!", name);
        }
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (stack.getCount() > 0) {
            stack.shrink(1);
            if (!level.isClientSide) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(),
                        SoundEvents.PLAYER_LEVELUP,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.2f, 1f);
                var present = nextPresent();
                InventoryUtils.addToPlayerInventory(present, player);
            }
        }
        return InteractionResultHolder.consume(stack);
    }
}
