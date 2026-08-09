package li.cil.oc.neoforge;

import li.cil.oc.api.API;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.LootManager;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.neoforge.common.init.Items;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class CreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, OpenComputers.ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB =
            TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.literal("OpenComputers"))
                    .icon(() -> API.items.get(Constants.BlockName.CaseTier1).createItemStack(1))
                    .displayItems((params, output) -> {
                        // Blocks
                        add(output, Constants.BlockName.Adapter);
                        add(output, Constants.BlockName.Assembler);
                        add(output, Constants.BlockName.Cable);
                        add(output, Constants.BlockName.Capacitor);
                        add(output, Constants.BlockName.CaseTier1);
                        add(output, Constants.BlockName.CaseTier3);
                        add(output, Constants.BlockName.CaseTier2);
                        add(output, Constants.BlockName.ChameliumBlock);
                        add(output, Constants.BlockName.Charger);
                        add(output, Constants.BlockName.Disassembler);
                        add(output, Constants.BlockName.DiskDrive);
                        add(output, Constants.BlockName.Geolyzer);
                        add(output, Constants.BlockName.HologramTier1);
                        add(output, Constants.BlockName.HologramTier2);
                        add(output, Constants.BlockName.Keyboard);
                        add(output, Constants.BlockName.MotionSensor);
                        add(output, Constants.BlockName.PowerConverter);
                        add(output, Constants.BlockName.PowerDistributor);
                        add(output, Constants.BlockName.Printer);
                        add(output, Constants.BlockName.Raid);
                        add(output, Constants.BlockName.Redstone);
                        add(output, Constants.BlockName.Relay);
                        add(output, Constants.BlockName.ScreenTier1);
                        add(output, Constants.BlockName.ScreenTier3);
                        add(output, Constants.BlockName.ScreenTier2);
                        add(output, Constants.BlockName.Rack);
                        add(output, Constants.BlockName.Waypoint);
                        add(output, Constants.BlockName.CaseCreative);
                        add(output, Constants.BlockName.Endstone);
                        add(output, Constants.BlockName.NetSplitter);
                        add(output, Constants.BlockName.Transposer);
                        add(output, Constants.BlockName.CarpetedCapacitor);

                        // Items
                        add(output, Constants.ItemName.Acid);
                        add(output, Constants.ItemName.Alu);
                        add(output, Constants.ItemName.ArrowKeys);
                        add(output, Constants.ItemName.ButtonGroup);
                        add(output, Constants.ItemName.Card);
                        add(output, Constants.ItemName.Chamelium);
                        add(output, Constants.ItemName.ControlUnit);
                        add(output, Constants.ItemName.CuttingWire);
                        add(output, Constants.ItemName.DiamondChip);
                        add(output, Constants.ItemName.Disk);
                        add(output, Constants.ItemName.DroneCaseTier1);
                        add(output, Constants.ItemName.DroneCaseTier2);
                        add(output, Constants.ItemName.DroneCaseCreative);
                        add(output, Constants.ItemName.InkCartridge);
                        add(output, Constants.ItemName.InkCartridgeEmpty);
                        add(output, Constants.ItemName.Interweb);
                        add(output, Constants.ItemName.ChipTier1);
                        add(output, Constants.ItemName.ChipTier2);
                        add(output, Constants.ItemName.ChipTier3);
                        add(output, Constants.ItemName.MicrocontrollerCaseTier1);
                        add(output, Constants.ItemName.MicrocontrollerCaseTier2);
                        add(output, Constants.ItemName.MicrocontrollerCaseCreative);
                        add(output, Constants.ItemName.NumPad);
                        add(output, Constants.ItemName.PrintedCircuitBoard);
                        add(output, Constants.ItemName.RawCircuitBoard);
                        add(output, Constants.ItemName.TabletCaseTier1);
                        add(output, Constants.ItemName.TabletCaseTier2);
                        add(output, Constants.ItemName.TabletCaseCreative);
                        add(output, Constants.ItemName.Transistor);
                        add(output, Constants.ItemName.Analyzer);
                        add(output, Constants.ItemName.Debugger);
                        add(output, Constants.ItemName.Manual);
                        add(output, Constants.ItemName.Nanomachines);
                        add(output, Constants.ItemName.Terminal);
                        add(output, Constants.ItemName.TexturePicker);
                        add(output, Constants.ItemName.Wrench);
                        add(output, Constants.ItemName.HoverBoots);
                        add(output, Constants.ItemName.APUTier1);
                        add(output, Constants.ItemName.APUTier2);
                        add(output, Constants.ItemName.APUCreative);
                        add(output, Constants.ItemName.ComponentBusTier1);
                        add(output, Constants.ItemName.ComponentBusTier2);
                        add(output, Constants.ItemName.ComponentBusTier3);
                        add(output, Constants.ItemName.CPUTier1);
                        add(output, Constants.ItemName.CPUTier2);
                        add(output, Constants.ItemName.CPUTier3);
                        add(output, Constants.ItemName.DiskDriveMountable);
                        add(output, Constants.ItemName.RAMTier1);
                        add(output, Constants.ItemName.RAMTier2);
                        add(output, Constants.ItemName.RAMTier3);
                        add(output, Constants.ItemName.RAMTier4);
                        add(output, Constants.ItemName.RAMTier5);
                        add(output, Constants.ItemName.RAMTier6);
                        add(output, Constants.ItemName.ServerTier1);
                        add(output, Constants.ItemName.ServerTier2);
                        add(output, Constants.ItemName.ServerTier3);
                        add(output, Constants.ItemName.ServerCreative);
                        add(output, Constants.ItemName.TerminalServer);
                        add(output, Constants.ItemName.DataCardTier1);
                        add(output, Constants.ItemName.DataCardTier2);
                        add(output, Constants.ItemName.DataCardTier3);
                        add(output, Constants.ItemName.DebugCard);
                        add(output, Constants.ItemName.GraphicsCardTier1);
                        add(output, Constants.ItemName.GraphicsCardTier2);
                        add(output, Constants.ItemName.GraphicsCardTier3);
                        add(output, Constants.ItemName.InternetCard);
                        add(output, Constants.ItemName.LinkedCard);
                        add(output, Constants.ItemName.NetworkCard);
                        add(output, Constants.ItemName.RedstoneCardTier1);
                        if (BundledRedstone.isAvailable()) {
                            add(output, Constants.ItemName.RedstoneCardTier2);
                        }
                        add(output, Constants.ItemName.WirelessNetworkCardTier2);
                        add(output, Constants.ItemName.ComponentBusCreative);
                        add(output, Constants.ItemName.AngelUpgrade);
                        add(output, Constants.ItemName.BatteryUpgradeTier1);
                        add(output, Constants.ItemName.BatteryUpgradeTier2);
                        add(output, Constants.ItemName.BatteryUpgradeTier3);
                        add(output, Constants.ItemName.ChunkloaderUpgrade);
                        add(output, Constants.ItemName.CardContainerTier1);
                        add(output, Constants.ItemName.CardContainerTier2);
                        add(output, Constants.ItemName.CardContainerTier3);
                        add(output, Constants.ItemName.UpgradeContainerTier1);
                        add(output, Constants.ItemName.UpgradeContainerTier2);
                        add(output, Constants.ItemName.UpgradeContainerTier3);
                        add(output, Constants.ItemName.CraftingUpgrade);
                        add(output, Constants.ItemName.DatabaseUpgradeTier1);
                        add(output, Constants.ItemName.DatabaseUpgradeTier2);
                        add(output, Constants.ItemName.DatabaseUpgradeTier3);
                        add(output, Constants.ItemName.ExperienceUpgrade);
                        add(output, Constants.ItemName.GeneratorUpgrade);
                        add(output, Constants.ItemName.HoverUpgradeTier1);
                        add(output, Constants.ItemName.HoverUpgradeTier2);
                        add(output, Constants.ItemName.InventoryUpgrade);
                        add(output, Constants.ItemName.InventoryControllerUpgrade);
                        add(output, Constants.ItemName.LeashUpgrade);
                        add(output, Constants.ItemName.MFU);
                        add(output, Constants.ItemName.NavigationUpgrade);
                        add(output, Constants.ItemName.PistonUpgrade);
                        add(output, Constants.ItemName.SignUpgrade);
                        add(output, Constants.ItemName.SolarGeneratorUpgrade);
                        add(output, Constants.ItemName.StickyPistonUpgrade);
                        add(output, Constants.ItemName.TankUpgrade);
                        add(output, Constants.ItemName.TankControllerUpgrade);
                        add(output, Constants.ItemName.TractorBeamUpgrade);
                        add(output, Constants.ItemName.TradingUpgrade);
                        add(output, Constants.ItemName.WirelessNetworkCardTier1);
                        add(output, Constants.ItemName.EEPROM);
                        add(output, Constants.ItemName.Floppy);
                        add(output, Constants.ItemName.HDDTier1);
                        add(output, Constants.ItemName.HDDTier2);
                        add(output, Constants.ItemName.HDDTier3);

                        // Special items
                        try {
                            var stack = Items.createConfiguredDrone();
                            output.accept(stack);
                            li.cil.oc.neoforge.integration.util.JEI.hide(stack);
                        } catch (Exception ignored) {
                        }
                        try {
                            var stack = Items.createConfiguredMicrocontroller();
                            output.accept(stack);
                            li.cil.oc.neoforge.integration.util.JEI.hide(stack);
                        } catch (Exception ignored) {
                        }
                        try {
                            var stack = Items.createConfiguredRobot();
                            output.accept(stack);
                            li.cil.oc.neoforge.integration.util.JEI.hide(stack);
                        } catch (Exception ignored) {
                        }
                        try {
                            var stack = Items.createConfiguredTablet();
                            output.accept(stack);
                            li.cil.oc.neoforge.integration.util.JEI.hide(stack);
                        } catch (Exception ignored) {
                        }
                        try {
                            output.accept(Items.createChargedHoverBoots());
                        } catch (Exception ignored) {
                        }
                        for (ItemStack stack : LootManager.disksForClient) {
                            if (stack != null && !stack.isEmpty()) {
                                output.accept(stack.copyWithCount(1));
                            }
                        }
                        for (ItemStack stack : Items.registeredItems) {
                            if (stack != null && !stack.isEmpty()) {
                                output.accept(stack.copyWithCount(1));
                            }
                        }
                        add(output, Constants.ItemName.LuaBios);
                    })
                    .build());

    private CreativeTab() {
    }

    private static void add(CreativeModeTab.Output output, String itemName) {
        try {
            var info = API.items.get(itemName);
            if (info != null) {
                ItemStack stack = info.createItemStack(1);
                if (!stack.isEmpty()) {
                    output.accept(stack);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
