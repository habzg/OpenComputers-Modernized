package li.cil.oc.core.impl.client;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

public final class Textures {
  public static final ResourceLocation fontAntiAliased = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/font/chars.png");
  public static final ResourceLocation fontAliased = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/font/chars_aliased.png");

  public static final ResourceLocation guiBackground = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/background.png");
  public static final ResourceLocation guiBar = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/bar.png");
  public static final ResourceLocation guiBorders = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/borders.png");
  public static final ResourceLocation guiButtonDriveMode = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_drive_mode.png");
  public static final ResourceLocation guiButtonPower = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_power.png");
  public static final ResourceLocation guiButtonRange = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_range.png");
  public static final ResourceLocation guiButtonRun = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_run.png");
  public static final ResourceLocation guiButtonScroll = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_scroll.png");
  public static final ResourceLocation guiButtonSide = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_side.png");
  public static final ResourceLocation guiButtonRelay = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/button_switch.png");
  public static final ResourceLocation guiComputer = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/computer.png");
  public static final ResourceLocation guiDatabase = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/database.png");
  public static final ResourceLocation guiDatabase1 = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/database1.png");
  public static final ResourceLocation guiDatabase2 = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/database2.png");
  public static final ResourceLocation guiDisassembler = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/disassembler.png");
  public static final ResourceLocation guiDrive = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/drive.png");
  public static final ResourceLocation guiDrone = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/drone.png");
  public static final ResourceLocation guiKeyboardMissing = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/keyboard_missing.png");
  public static final ResourceLocation guiManual = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/manual.png");
  public static final ResourceLocation guiManualHome = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/manual_home.png");
  public static final ResourceLocation guiManualMissingItem = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/manual_missing_item.png");
  public static final ResourceLocation guiManualTab = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/manual_tab.png");
  public static final ResourceLocation guiPrinter = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/printer.png");
  public static final ResourceLocation guiPrinterInk = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/printer_ink.png");
  public static final ResourceLocation guiPrinterMaterial = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/printer_material.png");
  public static final ResourceLocation guiPrinterProgress = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/printer_progress.png");
  public static final ResourceLocation guiRack = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/rack.png");
  public static final ResourceLocation guiRaid = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/raid.png");
  public static final ResourceLocation guiRange = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/range.png");
  public static final ResourceLocation guiRobot = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/robot.png");
  public static final ResourceLocation guiRobotNoScreen = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/robot_noscreen.png");
  public static final ResourceLocation guiRobotAssembler = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/robot_assembler.png");
  public static final ResourceLocation guiRobotSelection = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/robot_selection.png");
  public static final ResourceLocation guiServer = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/server.png");
  public static final ResourceLocation guiSlot = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/slot.png");
  public static final ResourceLocation guiUpgradeTab = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/upgrade_tab.png");
  public static final ResourceLocation guiWaypoint = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/waypoint.png");

  public static final ResourceLocation blockCaseFrontOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/case_front_on");
  public static final ResourceLocation blockCaseFrontError = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/case_front_error");
  public static final ResourceLocation blockCaseFrontActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/case_front_activity");
  public static final ResourceLocation blockDiskDriveFrontActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/diskdrivefrontactivity");
  public static final ResourceLocation blockHologram = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/hologram_effect.png");
  public static final ResourceLocation blockMicrocontrollerFrontLight = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/microcontrollerfrontlight");
  public static final ResourceLocation blockMicrocontrollerFrontOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/microcontrollerfronton");
  public static final ResourceLocation blockMicrocontrollerFrontError = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/microcontrollerfronterror");
  public static final ResourceLocation blockRaidFrontError = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/raid_front_error");
  public static final ResourceLocation blockRaidFrontActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/raid_front_activity");
  public static final ResourceLocation blockRobot = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/robot.png");
  public static final ResourceLocation blockRackDiskDriveActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_disk_drive_activity");
  public static final ResourceLocation blockRackServerOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_server_on");
  public static final ResourceLocation blockRackServerError = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_server_error");
  public static final ResourceLocation blockRackServerActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_server_activity");
  public static final ResourceLocation blockRackServerNetworkActivity = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_server_network_activity");
  public static final ResourceLocation blockRackTerminalServerOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_terminal_server_on");
  public static final ResourceLocation blockRackTerminalServerPresence = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/rack_terminal_server_presence");
  public static final ResourceLocation blockRackServer = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/rack_server");
  public static final ResourceLocation blockRackDiskDrive = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/rack_disk_drive");
  public static final ResourceLocation blockRackTerminalServer = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/rack_terminal_server");

  public static final ResourceLocation upgradeCrafting = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/upgradecrafting.png");
  public static final ResourceLocation upgradeGenerator = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/upgradegenerator.png");
  public static final ResourceLocation upgradeInventory = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/model/upgradeinventory.png");

  public static final ResourceLocation blockGeolyzerTopOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/geolyzer_top_on");
  public static final ResourceLocation blockAdapterOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/adapter_on");
  public static final ResourceLocation blockPowerDistributorTopOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/powerdistributor_top_on");
  public static final ResourceLocation blockPowerDistributorSideOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/powerdistributor_side_on");
  public static final ResourceLocation blockNetSplitterOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/netsplitter_on");
  public static final ResourceLocation blockSwitchSideOn = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/switch_side_on");
  public static final ResourceLocation blockDisassemblerTopOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/disassembler_top_on");
  public static final ResourceLocation blockDisassemblerSideOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/disassembler_side_on");
  public static final ResourceLocation blockAssemblerTopOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/assembler_top_on");
  public static final ResourceLocation blockAssemblerSideAssemblingSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/assembler_side_assembling");
  public static final ResourceLocation blockAssemblerSideOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/assembler_side_on");
  public static final ResourceLocation blockChargerFrontOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/charger_front_on");
  public static final ResourceLocation blockChargerSideOnSprite = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "block/overlay/charger_side_on");

  public static final ResourceLocation overlayNanomachines = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/nanomachines_power.png");
  public static final ResourceLocation overlayNanomachinesBar = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/gui/nanomachines_power_bar.png");

  public static void init(TextureManager tm) {
    tm.bindForSetup(fontAntiAliased);
    tm.bindForSetup(fontAliased);
    tm.bindForSetup(guiBackground);
    tm.bindForSetup(guiBar);
    tm.bindForSetup(guiBorders);
    tm.bindForSetup(guiButtonPower);
    tm.bindForSetup(guiButtonRange);
    tm.bindForSetup(guiButtonRun);
    tm.bindForSetup(guiButtonSide);
    tm.bindForSetup(guiComputer);
    tm.bindForSetup(guiDrone);
    tm.bindForSetup(guiKeyboardMissing);
    tm.bindForSetup(guiRaid);
    tm.bindForSetup(guiRange);
    tm.bindForSetup(guiRobot);
    tm.bindForSetup(guiRobotAssembler);
    tm.bindForSetup(guiRobotSelection);
    tm.bindForSetup(guiServer);
    tm.bindForSetup(guiSlot);
    tm.bindForSetup(blockHologram);
    tm.bindForSetup(blockRobot);
    tm.bindForSetup(upgradeCrafting);
    tm.bindForSetup(upgradeGenerator);
    tm.bindForSetup(upgradeInventory);
  }
}
