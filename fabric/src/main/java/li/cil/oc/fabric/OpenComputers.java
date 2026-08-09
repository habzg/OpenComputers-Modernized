package li.cil.oc.fabric;

import li.cil.oc.api.Machine;
import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.Wrench;
import li.cil.oc.core.Constants;
import li.cil.oc.core.Tags;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.CraftHandler;
import li.cil.oc.core.impl.common.Registrar;
import li.cil.oc.core.impl.common.nanomachines.provider.HungryProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.MagnetProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.ParticleProvider;
import li.cil.oc.core.impl.common.nanomachines.provider.PotionProvider;
import li.cil.oc.core.impl.common.template.DroneTemplate;
import li.cil.oc.core.impl.common.template.MicrocontrollerTemplate;
import li.cil.oc.core.impl.common.template.NavigationUpgradeTemplate;
import li.cil.oc.core.impl.common.template.RobotTemplate;
import li.cil.oc.core.impl.common.template.ServerTemplate;
import li.cil.oc.core.impl.common.template.TabletTemplate;
import li.cil.oc.core.impl.common.template.TemplateBlacklist;
import li.cil.oc.core.impl.integration.opencomputers.ConverterLinkedCard;
import li.cil.oc.core.impl.integration.opencomputers.ConverterNanomachines;
import li.cil.oc.core.impl.integration.opencomputers.DriverComponentBus;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerFloppy;
import li.cil.oc.core.impl.integration.opencomputers.DriverContainerUpgrade;
import li.cil.oc.core.impl.integration.opencomputers.DriverDataCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverEEPROM;
import li.cil.oc.core.impl.integration.opencomputers.DriverGeolyzer;
import li.cil.oc.core.impl.integration.opencomputers.DriverGraphicsCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverInternetCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverKeyboard;
import li.cil.oc.core.impl.integration.opencomputers.DriverLinkedCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverMemory;
import li.cil.oc.core.impl.integration.opencomputers.DriverMotionSensor;
import li.cil.oc.core.impl.integration.opencomputers.DriverNetworkCard;
import li.cil.oc.core.impl.integration.opencomputers.DriverScreen;
import li.cil.oc.core.impl.integration.opencomputers.DriverTerminalServer;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeAngel;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeBarcodeReader;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeBattery;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeExperience;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeGenerator;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeHover;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeInventory;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeLeash;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradePiston;
import li.cil.oc.core.impl.integration.opencomputers.DriverUpgradeSolarGenerator;
import li.cil.oc.core.impl.integration.opencomputers.DriverWirelessNetworkCard;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.server.machine.luac.NativeLua52Architecture;
import li.cil.oc.core.impl.server.machine.luac.NativeLua53Architecture;
import li.cil.oc.core.impl.server.machine.luac.NativeLua54Architecture;
import li.cil.oc.core.impl.server.machine.luaj.LuaJLuaArchitecture;
import li.cil.oc.core.impl.util.Color;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.fabric.client.ClientPacketSenderDelegate;
import li.cil.oc.fabric.common.GuiHandler;
import li.cil.oc.fabric.common.Loot;
import li.cil.oc.fabric.common.capability.OCBlockCapabilities;
import li.cil.oc.fabric.common.capability.SimpleComponentCapability;
import li.cil.oc.fabric.common.event.AngelUpgradeHandler;
import li.cil.oc.fabric.common.event.ChunkloaderUpgradeHandler;
import li.cil.oc.fabric.common.event.ExperienceUpgradeHandler;
import li.cil.oc.fabric.common.event.FileSystemAccessHandler;
import li.cil.oc.fabric.common.event.NetworkActivityHandler;
import li.cil.oc.fabric.common.event.RobotCommonHandler;
import li.cil.oc.fabric.common.event.WirelessNetworkCardHandler;
import li.cil.oc.fabric.common.init.Blocks;
import li.cil.oc.fabric.common.init.Entities;
import li.cil.oc.fabric.common.init.Items;
import li.cil.oc.fabric.common.init.Menus;
import li.cil.oc.fabric.common.init.BlockEntities;
import li.cil.oc.fabric.common.nanomachines.provider.DisintegrationProvider;
import li.cil.oc.fabric.integration.vanilla.EventHandlerVanilla;
import li.cil.oc.fabric.server.PacketSender;
import li.cil.oc.fabric.util.BlockInteractionHandler;
import li.cil.oc.fabric.util.FluidTransferHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

public final class OpenComputers implements ModInitializer {
    public static final String ID = Tags.MOD_ID;
    public static net.minecraft.server.MinecraftServer MINECRAFT_SERVER;
    private static final java.util.Map<Integer, String> CONTAINER_TITLES = new java.util.HashMap<>();

    static {
        CONTAINER_TITLES.put(GuiType.Adapter, "container.opencomputers.adapter");
        CONTAINER_TITLES.put(GuiType.Assembler, "container.opencomputers.assembler");
        CONTAINER_TITLES.put(GuiType.Case, "container.opencomputers.case");
        CONTAINER_TITLES.put(GuiType.Charger, "container.opencomputers.charger");
        CONTAINER_TITLES.put(GuiType.Database, "container.opencomputers.database");
        CONTAINER_TITLES.put(GuiType.Disassembler, "container.opencomputers.disassembler");
        CONTAINER_TITLES.put(GuiType.DiskDrive, "container.opencomputers.diskdrive");
        CONTAINER_TITLES.put(GuiType.DiskDriveMountable, "container.opencomputers.diskdrive");
        CONTAINER_TITLES.put(GuiType.DiskDriveMountableInRack, "container.opencomputers.diskdrive");
        CONTAINER_TITLES.put(GuiType.Drone, "container.opencomputers.drone");
        CONTAINER_TITLES.put(GuiType.Printer, "container.opencomputers.printer");
        CONTAINER_TITLES.put(GuiType.Rack, "container.opencomputers.rack");
        CONTAINER_TITLES.put(GuiType.Raid, "container.opencomputers.raid");
        CONTAINER_TITLES.put(GuiType.Relay, "container.opencomputers.relay");
        CONTAINER_TITLES.put(GuiType.Robot, "container.opencomputers.robot");
        CONTAINER_TITLES.put(GuiType.Server, "container.opencomputers.server");
        CONTAINER_TITLES.put(GuiType.ServerInRack, "container.opencomputers.server");
        CONTAINER_TITLES.put(GuiType.Tablet, "container.opencomputers.tabletwrapper");
        CONTAINER_TITLES.put(GuiType.TabletInner, "container.opencomputers.tabletwrapper");
    }

    public static CreativeModeTab CREATIVE_TAB;

    @Override
    public void onInitialize() {
        li.cil.oc.core.integration.ModIDs.setModResolver(net.fabricmc.loader.api.FabricLoader.getInstance()::isModLoaded);

        OCSettings.load(
                FabricLoader.getInstance().getConfigDir().resolve("OpenComputers.cfg").toFile(),
                FabricLoader.getInstance().getConfigDir().toFile(),
                FabricLoader.getInstance().getModContainer(ID)
                        .orElseThrow(() -> new RuntimeException("OpenComputers mod container not found"))
                        .getMetadata().getVersion().getFriendlyString()
        );

        SideTracker.setDedicatedServer(FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER);
        CraftHandler.isFakePlayer = player -> player instanceof FakePlayer;
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            SideTracker.setCurrentServer(() -> server);
            li.cil.oc.fabric.server.PacketBuilder.SERVER = server;
            MINECRAFT_SERVER = server;
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            li.cil.oc.core.impl.util.ThreadPoolFactory.safePools.forEach(li.cil.oc.core.impl.util.SafeThreadPool::waitForCompletion);
            li.cil.oc.fabric.common.asm.StaticSimpleEnvironment.onServerStopped();
            MINECRAFT_SERVER = null;
            li.cil.oc.fabric.server.PacketBuilder.SERVER = null;
            SideTracker.setCurrentServer(() -> null);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            li.cil.oc.core.impl.server.command.CommandHandler.register(dispatcher);
            li.cil.oc.core.impl.util.ThreadPoolFactory.safePools.forEach(li.cil.oc.core.impl.util.SafeThreadPool::newThreadPool);

            if (OCSettings.get().internetAccessConfigured()) {
                if (OCSettings.get().internetFilteringRulesInvalid()) {
                    log().warn("####################################################");
                    log().warn("#                                                  #");
                    log().warn("#  Could not parse Internet Card filtering rules!  #");
                    log().warn("#  Review the server log and adjust the filtering  #");
                    log().warn("#  list to ensure it is appropriately configured.  #");
                    log().warn("#   (config/OpenComputers.cfg => filteringRules)   #");
                    log().warn("# Internet access has been automatically disabled. #");
                    log().warn("#                                                  #");
                    log().warn("####################################################");
                } else if (!OCSettings.get().internetFilteringRulesObserved && environment == net.minecraft.commands.Commands.CommandSelection.DEDICATED) {
                    log().warn("####################################################");
                    log().warn("#                                                  #");
                    log().warn("#    It appears that you're running a dedicated    #");
                    log().warn("#  server with OpenComputers installed! Make sure  #");
                    log().warn("#  to review the Internet Card address filtering   #");
                    log().warn("#  list to ensure it is appropriately configured.  #");
                    log().warn("#   (config/OpenComputers.cfg => filteringRules)   #");
                    log().warn("#                                                  #");
                    log().warn("####################################################");
                } else {
                    log().info("Successfully applied {} Internet Card filtering rules.", OCSettings.get().internetFilteringRules.length);
                }
            }
        });

        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            var cache = li.cil.oc.core.impl.util.TabletCache.get();
            if (cache != null) {
                for (var level : server.getAllLevels()) {
                    cache.saveAll(level);
                }
            }
        });

        li.cil.oc.core.impl.server.fs.FileSystem.setEnvironmentFactory(li.cil.oc.core.impl.server.component.FileSystem::new);
        li.cil.oc.core.impl.server.component.traits.WorldAction.setHandler(new BlockInteractionHandler());
        li.cil.oc.core.util.Tasks.setScheduler(li.cil.oc.fabric.common.EventHandler::scheduleServer);
        li.cil.oc.core.impl.util.FluidUtils.setHandler(new FluidTransferHandler());
        li.cil.oc.core.util.ServerNetwork.setInstance(new li.cil.oc.core.impl.util.NeoServerNetwork());
        li.cil.oc.core.impl.util.SaveHandlerDelegate.setInstance(new li.cil.oc.core.impl.util.SaveHandlerDelegateImpl());
        li.cil.oc.core.util.ConverterRegistry.setInstance(new li.cil.oc.core.impl.util.NeoConverterRegistry());
        li.cil.oc.core.util.MachineStateHelper.setInstance(new li.cil.oc.core.util.MachineStateHelper() {
            @Override
            public boolean isInSynchronizedCall(li.cil.oc.api.machine.Machine machine) {
                var mb = (li.cil.oc.core.impl.server.machine.MachineBase) machine;
                for (var s : mb.state()) {
                    if (s.id == STATE_SYNCHRONIZED_CALL || s.id == STATE_SYNCHRONIZED_RETURN) return true;
                }
                return false;
            }
        });
        EventHandlerDelegate.setInstance(new li.cil.oc.fabric.util.EventHandlerDelegate());
        ContainerProviderDelegate.setInstance(new ContainerProviderDelegate() {
            @Override
            public MenuProvider getContainerProvider(int guiType, Level world, int x, int y, int z) {
                return new ExtendedScreenHandlerFactory<li.cil.oc.fabric.common.network.MenuData>() {
                    @Override
                    public @NotNull Component getDisplayName() {
                        String key = CONTAINER_TITLES.get(guiType);
                        if (key != null) return Component.translatable(key);
                        return Component.translatable("gui.opencomputers." + guiType);
                    }

                    @Override
                    public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
                        return GuiHandler.getServerGuiElement(containerId, guiType, player, world, x, y, z, "");
                    }

                    @Override
                    public li.cil.oc.fabric.common.network.MenuData getScreenOpeningData(ServerPlayer player) {
                        String address = "";
                        if (guiType == li.cil.oc.core.common.GuiType.Robot) {
                            var be = world.getBlockEntity(new net.minecraft.core.BlockPos(x, li.cil.oc.core.common.GuiType.extractY(y), z));
                            if (be instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy proxy) {
                                var addr = proxy.robot.computerAddress();
                                if (addr != null) address = addr;
                            }
                        }
                        return new li.cil.oc.fabric.common.network.MenuData(guiType, x, y, z, address);
                    }
                };
            }
        });
        li.cil.oc.core.impl.common.ComponentTracker.setServerTracker(li.cil.oc.fabric.server.ComponentTracker.INSTANCE);
        li.cil.oc.core.util.PacketBuilderFactory.setInstance(new li.cil.oc.core.util.PacketBuilderFactory() {
            @Override
            public Object createCompressed(li.cil.oc.core.common.PacketType type) {
                return new li.cil.oc.fabric.server.PacketBuilder.Compressed(type);
            }
        });
        li.cil.oc.core.util.ClientPacketSenderDelegate.setInstance(new ClientPacketSenderDelegate());
        li.cil.oc.core.impl.common.PacketSender.setInstance(new PacketSender());

        li.cil.oc.fabric.common.network.ServerNetworking.init();

        li.cil.oc.core.impl.server.network.Network.cbMultipartAvailable = false;

        li.cil.oc.core.impl.server.network.Network.simpleComponentHandler = (be) -> {
            if (be.getLevel() != null) {
                var provider = OCBlockCapabilities.SIMPLE_COMPONENT_PROVIDER.find(be.getLevel(), be.getBlockPos(), null);
                if (provider != null) return provider.node();
            }
            return li.cil.oc.fabric.common.asm.StaticSimpleEnvironment.node(be, (li.cil.oc.api.network.SimpleComponent) be);
        };

        li.cil.oc.core.impl.server.network.Network.capabilityNodeHandler = (blockEntity, side) -> {
            if (blockEntity.getLevel() != null) {
                var pos = blockEntity.getBlockPos();
                var level = blockEntity.getLevel();
                var sided = OCBlockCapabilities.SIDED_ENVIRONMENT.find(level, pos, side);
                if (sided != null) return sided.sidedNode(side);
                var env = OCBlockCapabilities.ENVIRONMENT.find(level, pos, side);
                if (env != null) return env.node();
            }
            return null;
        };

        li.cil.oc.core.impl.common.item.RedstoneCard.setProjectRedAvailable(() -> false);
        li.cil.oc.core.impl.common.item.Debugger.setFakePlayerCheck((player) -> player instanceof net.fabricmc.fabric.api.entity.FakePlayer);
        li.cil.oc.core.impl.common.item.Analyzer.setFakePlayerCheck((player) -> player instanceof net.fabricmc.fabric.api.entity.FakePlayer);

        li.cil.oc.core.impl.common.item.FloppyDisk.setOpenDriveScreen(() ->
                li.cil.oc.fabric.client.GuiHandler.openScreen(li.cil.oc.core.common.GuiType.Drive, 0, 0, 0));
        li.cil.oc.core.impl.common.item.HardDiskDrive.setOpenDriveScreen(() ->
                li.cil.oc.fabric.client.GuiHandler.openScreen(li.cil.oc.core.common.GuiType.Drive, 0, 0, 0));

        li.cil.oc.core.impl.common.item.Drone.setAgentOwnerProvider((player) -> {
            if (player instanceof li.cil.oc.fabric.server.agent.Player fakePlayer) {
                return new li.cil.oc.core.impl.common.item.Drone.PlayerOwner(fakePlayer.agent.ownerName(), fakePlayer.agent.ownerUUID());
            }
            return null;
        });

        li.cil.oc.core.impl.common.item.Tablet.setTerminalPacketSender((address, player) -> {
            try (var pb = new li.cil.oc.fabric.server.PacketBuilder(li.cil.oc.core.common.PacketType.OpenTabletTerminal)) {
                pb.writeUTF(address);
                pb.writeNBT(new net.minecraft.nbt.CompoundTag());
                pb.sendToPlayer(player);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });

        li.cil.oc.core.impl.util.PlayerUtils.setDataProvider(li.cil.oc.fabric.common.PlayerDataProvider.INSTANCE);

        li.cil.oc.core.impl.server.component.UpgradeGenerator.setFuelProvider(li.cil.oc.fabric.common.FuelProvider.INSTANCE);

        li.cil.oc.core.impl.common.item.UpgradeTank.setFluidTooltipProvider((registries, data) -> {
            if (data.contains("fluid")) {
                var fluidTag = data.getCompound("fluid");
                String id = fluidTag.getString("id");
                int amount = fluidTag.getInt("Amount");
                if (!id.isEmpty()) {
                    var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(net.minecraft.resources.ResourceLocation.parse(id));
                    var bucket = fluid.getBucket();
                    String name = bucket.getDescription().getString();
                    return name + ": " + amount + "/16000";
                }
            }
            return null;
        });

        Blocks.init();
        BlockEntities.init();
        Items.init();
        Menus.init();
        Entities.init();
        li.cil.oc.fabric.common.init.Recipes.init();
        li.cil.oc.fabric.common.recipe.ExtendedRecipe.init();
        li.cil.oc.fabric.common.recipe.LootRecraftingCondition.init();
        li.cil.oc.fabric.common.loot.OCLootModifier.init();

        OCBlockCapabilities.register(Blocks.ALL_BLOCKS);

        net.fabricmc.fabric.api.transfer.v1.item.ItemStorage.SIDED.registerForBlocks(
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof net.minecraft.world.Container container && side != null) {
                        return net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage.of(container, side);
                    }
                    return null;
                },
                Blocks.ALL_BLOCKS
        );

        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlocks(
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof li.cil.oc.core.util.FluidHandler handler) {
                        return new li.cil.oc.fabric.util.FluidHandlerStorage(handler);
                    }
                    return null;
                },
                Blocks.ALL_BLOCKS
        );

        OCBlockCapabilities.SIMPLE_COMPONENT_PROVIDER.registerForBlocks(
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof li.cil.oc.api.network.SimpleComponent sc) {
                        return new SimpleComponentCapability(blockEntity, sc);
                    }
                    return null;
                },
                Blocks.ALL_BLOCKS
        );

        li.cil.oc.core.impl.common.entity.Drone.setEntityType(Entities.DRONE);
        li.cil.oc.core.impl.common.entity.Drone.setControlFactory(li.cil.oc.fabric.server.component.Drone::new);
        li.cil.oc.core.impl.common.entity.Drone.setMenuOpener(li.cil.oc.fabric.util.DroneMenuOpener.INSTANCE);
        li.cil.oc.core.util.FluidTankHelper.setInstance(new li.cil.oc.fabric.util.FluidTankHelper());
        li.cil.oc.core.impl.util.DroneHelper.setInstance(new li.cil.oc.fabric.util.DroneHelper());
        li.cil.oc.core.impl.util.GeolyzerHostHelper.setInstance(new li.cil.oc.fabric.util.GeolyzerHostHelper());
        li.cil.oc.core.util.RobotChargeableFactory.setInstance(new li.cil.oc.fabric.util.RobotChargeableFactory());

        CREATIVE_TAB = Registry.register(
                BuiltInRegistries.CREATIVE_MODE_TAB,
                ResourceLocation.fromNamespaceAndPath(ID, "main"),
                FabricItemGroup.builder()
                        .title(Component.literal("OpenComputers"))
                        .icon(() -> {
                            ItemInfo info = li.cil.oc.api.API.items.get(li.cil.oc.core.Constants.BlockName.CaseTier1);
                            return info != null ? info.createItemStack(1) : ItemStack.EMPTY;
                        })
                        .displayItems((params, output) -> {
                            // Blocks
                            addToCreativeTab(output, Constants.BlockName.Adapter);
                            addToCreativeTab(output, Constants.BlockName.Assembler);
                            addToCreativeTab(output, Constants.BlockName.Cable);
                            addToCreativeTab(output, Constants.BlockName.Capacitor);
                            addToCreativeTab(output, Constants.BlockName.CaseTier1);
                            addToCreativeTab(output, Constants.BlockName.CaseTier3);
                            addToCreativeTab(output, Constants.BlockName.CaseTier2);
                            addToCreativeTab(output, Constants.BlockName.ChameliumBlock);
                            addToCreativeTab(output, Constants.BlockName.Charger);
                            addToCreativeTab(output, Constants.BlockName.Disassembler);
                            addToCreativeTab(output, Constants.BlockName.DiskDrive);
                            addToCreativeTab(output, Constants.BlockName.Geolyzer);
                            addToCreativeTab(output, Constants.BlockName.HologramTier1);
                            addToCreativeTab(output, Constants.BlockName.HologramTier2);
                            addToCreativeTab(output, Constants.BlockName.Keyboard);
                            addToCreativeTab(output, Constants.BlockName.MotionSensor);
                            addToCreativeTab(output, Constants.BlockName.PowerConverter);
                            addToCreativeTab(output, Constants.BlockName.PowerDistributor);
                            addToCreativeTab(output, Constants.BlockName.Printer);
                            addToCreativeTab(output, Constants.BlockName.Raid);
                            addToCreativeTab(output, Constants.BlockName.Redstone);
                            addToCreativeTab(output, Constants.BlockName.Relay);
                            addToCreativeTab(output, Constants.BlockName.ScreenTier1);
                            addToCreativeTab(output, Constants.BlockName.ScreenTier3);
                            addToCreativeTab(output, Constants.BlockName.ScreenTier2);
                            addToCreativeTab(output, Constants.BlockName.Rack);
                            addToCreativeTab(output, Constants.BlockName.Waypoint);
                            addToCreativeTab(output, Constants.BlockName.CaseCreative);
                            addToCreativeTab(output, Constants.BlockName.Endstone);
                            addToCreativeTab(output, Constants.BlockName.NetSplitter);
                            addToCreativeTab(output, Constants.BlockName.Transposer);
                            addToCreativeTab(output, Constants.BlockName.CarpetedCapacitor);

                            // Items
                            addToCreativeTab(output, Constants.ItemName.Acid);
                            addToCreativeTab(output, Constants.ItemName.Alu);
                            addToCreativeTab(output, Constants.ItemName.ArrowKeys);
                            addToCreativeTab(output, Constants.ItemName.ButtonGroup);
                            addToCreativeTab(output, Constants.ItemName.Card);
                            addToCreativeTab(output, Constants.ItemName.Chamelium);
                            addToCreativeTab(output, Constants.ItemName.ControlUnit);
                            addToCreativeTab(output, Constants.ItemName.CuttingWire);
                            addToCreativeTab(output, Constants.ItemName.DiamondChip);
                            addToCreativeTab(output, Constants.ItemName.Disk);
                            addToCreativeTab(output, Constants.ItemName.DroneCaseTier1);
                            addToCreativeTab(output, Constants.ItemName.DroneCaseTier2);
                            addToCreativeTab(output, Constants.ItemName.DroneCaseCreative);
                            addToCreativeTab(output, Constants.ItemName.InkCartridge);
                            addToCreativeTab(output, Constants.ItemName.InkCartridgeEmpty);
                            addToCreativeTab(output, Constants.ItemName.Interweb);
                            addToCreativeTab(output, Constants.ItemName.ChipTier1);
                            addToCreativeTab(output, Constants.ItemName.ChipTier2);
                            addToCreativeTab(output, Constants.ItemName.ChipTier3);
                            addToCreativeTab(output, Constants.ItemName.MicrocontrollerCaseTier1);
                            addToCreativeTab(output, Constants.ItemName.MicrocontrollerCaseTier2);
                            addToCreativeTab(output, Constants.ItemName.MicrocontrollerCaseCreative);
                            addToCreativeTab(output, Constants.ItemName.NumPad);
                            addToCreativeTab(output, Constants.ItemName.PrintedCircuitBoard);
                            addToCreativeTab(output, Constants.ItemName.RawCircuitBoard);
                            addToCreativeTab(output, Constants.ItemName.TabletCaseTier1);
                            addToCreativeTab(output, Constants.ItemName.TabletCaseTier2);
                            addToCreativeTab(output, Constants.ItemName.TabletCaseCreative);
                            addToCreativeTab(output, Constants.ItemName.Transistor);
                            addToCreativeTab(output, Constants.ItemName.Analyzer);
                            addToCreativeTab(output, Constants.ItemName.Debugger);
                            addToCreativeTab(output, Constants.ItemName.Manual);
                            addToCreativeTab(output, Constants.ItemName.Nanomachines);
                            addToCreativeTab(output, Constants.ItemName.Terminal);
                            addToCreativeTab(output, Constants.ItemName.TexturePicker);
                            addToCreativeTab(output, Constants.ItemName.Wrench);
                            addToCreativeTab(output, Constants.ItemName.HoverBoots);
                            addToCreativeTab(output, Constants.ItemName.APUTier1);
                            addToCreativeTab(output, Constants.ItemName.APUTier2);
                            addToCreativeTab(output, Constants.ItemName.APUCreative);
                            addToCreativeTab(output, Constants.ItemName.ComponentBusTier1);
                            addToCreativeTab(output, Constants.ItemName.ComponentBusTier2);
                            addToCreativeTab(output, Constants.ItemName.ComponentBusTier3);
                            addToCreativeTab(output, Constants.ItemName.CPUTier1);
                            addToCreativeTab(output, Constants.ItemName.CPUTier2);
                            addToCreativeTab(output, Constants.ItemName.CPUTier3);
                            addToCreativeTab(output, Constants.ItemName.DiskDriveMountable);
                            addToCreativeTab(output, Constants.ItemName.RAMTier1);
                            addToCreativeTab(output, Constants.ItemName.RAMTier2);
                            addToCreativeTab(output, Constants.ItemName.RAMTier3);
                            addToCreativeTab(output, Constants.ItemName.RAMTier4);
                            addToCreativeTab(output, Constants.ItemName.RAMTier5);
                            addToCreativeTab(output, Constants.ItemName.RAMTier6);
                            addToCreativeTab(output, Constants.ItemName.ServerTier1);
                            addToCreativeTab(output, Constants.ItemName.ServerTier2);
                            addToCreativeTab(output, Constants.ItemName.ServerTier3);
                            addToCreativeTab(output, Constants.ItemName.ServerCreative);
                            addToCreativeTab(output, Constants.ItemName.TerminalServer);
                            addToCreativeTab(output, Constants.ItemName.DataCardTier1);
                            addToCreativeTab(output, Constants.ItemName.DataCardTier2);
                            addToCreativeTab(output, Constants.ItemName.DataCardTier3);
                            addToCreativeTab(output, Constants.ItemName.DebugCard);
                            addToCreativeTab(output, Constants.ItemName.GraphicsCardTier1);
                            addToCreativeTab(output, Constants.ItemName.GraphicsCardTier2);
                            addToCreativeTab(output, Constants.ItemName.GraphicsCardTier3);
                            addToCreativeTab(output, Constants.ItemName.InternetCard);
                            addToCreativeTab(output, Constants.ItemName.LinkedCard);
                            addToCreativeTab(output, Constants.ItemName.NetworkCard);
                            addToCreativeTab(output, Constants.ItemName.RedstoneCardTier1);
                            if (li.cil.oc.core.impl.integration.util.BundledRedstone.isAvailable()) {
                                addToCreativeTab(output, Constants.ItemName.RedstoneCardTier2);
                            }
                            addToCreativeTab(output, Constants.ItemName.WirelessNetworkCardTier2);
                            addToCreativeTab(output, Constants.ItemName.ComponentBusCreative);
                            addToCreativeTab(output, Constants.ItemName.AngelUpgrade);
                            addToCreativeTab(output, Constants.ItemName.BatteryUpgradeTier1);
                            addToCreativeTab(output, Constants.ItemName.BatteryUpgradeTier2);
                            addToCreativeTab(output, Constants.ItemName.BatteryUpgradeTier3);
                            addToCreativeTab(output, Constants.ItemName.ChunkloaderUpgrade);
                            addToCreativeTab(output, Constants.ItemName.CardContainerTier1);
                            addToCreativeTab(output, Constants.ItemName.CardContainerTier2);
                            addToCreativeTab(output, Constants.ItemName.CardContainerTier3);
                            addToCreativeTab(output, Constants.ItemName.UpgradeContainerTier1);
                            addToCreativeTab(output, Constants.ItemName.UpgradeContainerTier2);
                            addToCreativeTab(output, Constants.ItemName.UpgradeContainerTier3);
                            addToCreativeTab(output, Constants.ItemName.CraftingUpgrade);
                            addToCreativeTab(output, Constants.ItemName.DatabaseUpgradeTier1);
                            addToCreativeTab(output, Constants.ItemName.DatabaseUpgradeTier2);
                            addToCreativeTab(output, Constants.ItemName.DatabaseUpgradeTier3);
                            addToCreativeTab(output, Constants.ItemName.ExperienceUpgrade);
                            addToCreativeTab(output, Constants.ItemName.GeneratorUpgrade);
                            addToCreativeTab(output, Constants.ItemName.HoverUpgradeTier1);
                            addToCreativeTab(output, Constants.ItemName.HoverUpgradeTier2);
                            addToCreativeTab(output, Constants.ItemName.InventoryUpgrade);
                            addToCreativeTab(output, Constants.ItemName.InventoryControllerUpgrade);
                            addToCreativeTab(output, Constants.ItemName.LeashUpgrade);
                            addToCreativeTab(output, Constants.ItemName.MFU);
                            addToCreativeTab(output, Constants.ItemName.NavigationUpgrade);
                            addToCreativeTab(output, Constants.ItemName.PistonUpgrade);
                            addToCreativeTab(output, Constants.ItemName.SignUpgrade);
                            addToCreativeTab(output, Constants.ItemName.SolarGeneratorUpgrade);
                            addToCreativeTab(output, Constants.ItemName.StickyPistonUpgrade);
                            addToCreativeTab(output, Constants.ItemName.TankUpgrade);
                            addToCreativeTab(output, Constants.ItemName.TankControllerUpgrade);
                            addToCreativeTab(output, Constants.ItemName.TractorBeamUpgrade);
                            addToCreativeTab(output, Constants.ItemName.TradingUpgrade);
                            addToCreativeTab(output, Constants.ItemName.WirelessNetworkCardTier1);
                            addToCreativeTab(output, Constants.ItemName.EEPROM);
                            addToCreativeTab(output, Constants.ItemName.Floppy);
                            addToCreativeTab(output, Constants.ItemName.HDDTier1);
                            addToCreativeTab(output, Constants.ItemName.HDDTier2);
                            addToCreativeTab(output, Constants.ItemName.HDDTier3);

                            // Special items
                            try {
                                var stack = Items.createConfiguredDrone();
                                output.accept(stack);
                                li.cil.oc.fabric.integration.util.JEI.hide(stack);
                            } catch (Exception ignored) {
                            }
                            try {
                                var stack = Items.createConfiguredMicrocontroller();
                                output.accept(stack);
                                li.cil.oc.fabric.integration.util.JEI.hide(stack);
                            } catch (Exception ignored) {
                            }
                            try {
                                var stack = Items.createConfiguredRobot();
                                output.accept(stack);
                                li.cil.oc.fabric.integration.util.JEI.hide(stack);
                            } catch (Exception ignored) {
                            }
                            try {
                                var stack = Items.createConfiguredTablet();
                                output.accept(stack);
                                li.cil.oc.fabric.integration.util.JEI.hide(stack);
                            } catch (Exception ignored) {
                            }
                            try {
                                output.accept(Items.createChargedHoverBoots());
                            } catch (Exception ignored) {
                            }
                            for (ItemStack stack : li.cil.oc.core.impl.common.LootManager.disksForClient) {
                                if (stack != null && !stack.isEmpty()) {
                                    output.accept(stack.copyWithCount(1));
                                }
                            }
                            for (ItemStack stack : Items.registeredItems) {
                                if (stack != null && !stack.isEmpty()) {
                                    output.accept(stack.copyWithCount(1));
                                }
                            }
                            addToCreativeTab(output, Constants.ItemName.LuaBios);
                        })
                        .build()
        );

        li.cil.oc.api.CreativeTab.instance = CREATIVE_TAB;
        li.cil.oc.api.API.network = li.cil.oc.core.impl.server.network.Network.INSTANCE;
        li.cil.oc.api.API.driver = li.cil.oc.core.impl.server.driver.Registry.INSTANCE;
        li.cil.oc.api.API.fileSystem = li.cil.oc.core.impl.server.fs.FileSystem.INSTANCE;
        li.cil.oc.api.API.items = Items.INSTANCE;
        li.cil.oc.api.API.machine = new li.cil.oc.fabric.server.machine.Machine.API();
        li.cil.oc.api.API.nanomachines = new li.cil.oc.core.impl.common.nanomachines.Nanomachines();
        li.cil.oc.api.API.config = OCSettings.get().config;

        li.cil.oc.api.API.isPowerEnabled = !OCSettings.get().ignorePower;

        Loot.init();

        if (LuaStateFactory.isAvailable()) {
            if (LuaStateFactory.include53()) {
                Machine.add(NativeLua53Architecture.class);
            }
            if (LuaStateFactory.include54()) {
                Machine.add(NativeLua54Architecture.class);
            }
            if (LuaStateFactory.include52()) {
                Machine.add(NativeLua52Architecture.class);
            }
        }
        if (LuaStateFactory.includeLuaJ()) {
            Machine.add(LuaJLuaArchitecture.class);
        }
        Machine.LuaArchitecture =
                OCSettings.get().forceLuaJ ? LuaJLuaArchitecture.class : Machine.architectures().iterator().next();

        DroneTemplate.register();
        MicrocontrollerTemplate.register();
        NavigationUpgradeTemplate.register();
        RobotTemplate.register();
        ServerTemplate.register();
        TabletTemplate.register();
        TemplateBlacklist.register();

        li.cil.oc.core.impl.util.ComponentDriverHelper.setRedstoneCardCheck(d -> d instanceof li.cil.oc.fabric.integration.opencomputers.DriverRedstoneCard);

        li.cil.oc.fabric.integration.Mods.setLogger(msg -> OpenComputers.log().info(msg));
        li.cil.oc.fabric.integration.Mods.init();
		
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.vanilla.DriverItemHandler());

        Registrar.registerWrenchTool("li.cil.oc.fabric.OpenComputers.useWrench");
        Registrar.registerWrenchToolCheck("li.cil.oc.fabric.OpenComputers.isWrench");
        Registrar.registerItemCharge(
                "OpenComputers",
                "li.cil.oc.fabric.OpenComputers.canCharge",
                "li.cil.oc.fabric.OpenComputers.charge");
        Registrar.registerItemCharge(
                "TeamRebornEnergy",
                "li.cil.oc.fabric.integration.vanilla.DriverEnergy.canCharge",
                "li.cil.oc.fabric.integration.vanilla.DriverEnergy.charge");
        Registrar.registerInkProvider("li.cil.oc.fabric.OpenComputers.inkCartridgeInkProvider");
        Registrar.registerInkProvider("li.cil.oc.fabric.OpenComputers.dyeInkProvider");

        Registrar.registerProgramDiskLabel("build", "builder", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("dig", "dig", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("base64", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("deflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("gpg", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("inflate", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("md5sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("sha256sum", "data", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("refuel", "generator", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("irc", "irc", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("maze", "maze", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("arp", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("ifconfig", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("ping", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("route", "network", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("opl-flash", "openloader", "Lua 5.2", "Lua 5.3", "LuaJ");
        Registrar.registerProgramDiskLabel("oppm", "oppm", "Lua 5.2", "Lua 5.3", "LuaJ");

        li.cil.oc.api.Driver.add(new ConverterNanomachines());
        li.cil.oc.api.Driver.add(new ConverterLinkedCard());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverAPU());
        li.cil.oc.api.Driver.add(new DriverComponentBus());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverCPU());
        li.cil.oc.api.Driver.add(new DriverDataCard());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverDebugCard());
        li.cil.oc.api.Driver.add(new DriverEEPROM());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverFileSystem());
        li.cil.oc.api.Driver.add(new DriverGraphicsCard());
        li.cil.oc.api.Driver.add(new DriverInternetCard());
        li.cil.oc.api.Driver.add(new DriverLinkedCard());
        li.cil.oc.api.Driver.add(new DriverMemory());
        li.cil.oc.api.Driver.add(new DriverNetworkCard());
        li.cil.oc.api.Driver.add(new DriverKeyboard());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverRedstoneCard());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverTablet());
        li.cil.oc.api.Driver.add(new DriverWirelessNetworkCard());
        li.cil.oc.api.Driver.add(new DriverContainerCard());
        li.cil.oc.api.Driver.add(new DriverContainerFloppy());
        li.cil.oc.api.Driver.add(new DriverContainerUpgrade());
        li.cil.oc.api.Driver.add(new DriverGeolyzer());
        li.cil.oc.api.Driver.add(new DriverMotionSensor());
        li.cil.oc.api.Driver.add(new DriverScreen());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverTransposer());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverDiskDriveMountable());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverServer());
        li.cil.oc.api.Driver.add(new DriverTerminalServer());
        li.cil.oc.api.Driver.add(new DriverUpgradeAngel());
        li.cil.oc.api.Driver.add(new DriverUpgradeBarcodeReader());
        li.cil.oc.api.Driver.add(new DriverUpgradeBattery());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeChunkloader());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeCrafting());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase());
        li.cil.oc.api.Driver.add(new DriverUpgradeExperience());
        li.cil.oc.api.Driver.add(new DriverUpgradeGenerator());
        li.cil.oc.api.Driver.add(new DriverUpgradeHover());
        li.cil.oc.api.Driver.add(new DriverUpgradeInventory());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeInventoryController());
        li.cil.oc.api.Driver.add(new DriverUpgradeLeash());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeNavigation());
        li.cil.oc.api.Driver.add(new DriverUpgradePiston());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeSign());
        li.cil.oc.api.Driver.add(new DriverUpgradeSolarGenerator());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTank());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTankController());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTractorBeam());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTrading());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeMF());

        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverAPU.Provider());
        li.cil.oc.api.Driver.add(new DriverDataCard.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverDebugCard.Provider());
        li.cil.oc.api.Driver.add(new DriverEEPROM.Provider());
        li.cil.oc.api.Driver.add(new DriverGraphicsCard.Provider());
        li.cil.oc.api.Driver.add(new DriverInternetCard.Provider());
        li.cil.oc.api.Driver.add(new DriverLinkedCard.Provider());
        li.cil.oc.api.Driver.add(new DriverNetworkCard.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverRedstoneCard.Provider());
        li.cil.oc.api.Driver.add(new DriverWirelessNetworkCard.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase.Provider());
        li.cil.oc.api.Driver.add(new DriverGeolyzer.Provider());
        li.cil.oc.api.Driver.add(new DriverMotionSensor.Provider());
        li.cil.oc.api.Driver.add(new DriverScreen.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverTransposer.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeChunkloader.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeCrafting.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeExperience.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeGenerator.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeInventoryController.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradeLeash.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeNavigation.Provider());
        li.cil.oc.api.Driver.add(new DriverUpgradePiston.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeSign.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTankController.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeTractorBeam.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.DriverUpgradeMF.Provider());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.EnvironmentProviderBlocks());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.InventoryProviderDatabase());
        li.cil.oc.api.Driver.add(new li.cil.oc.fabric.integration.opencomputers.InventoryProviderServer());

        li.cil.oc.api.Nanomachines.addProvider(new HungryProvider());
        li.cil.oc.api.Nanomachines.addProvider(new ParticleProvider());
        li.cil.oc.api.Nanomachines.addProvider(new PotionProvider());
        li.cil.oc.api.Nanomachines.addProvider(new MagnetProvider());
        li.cil.oc.api.Nanomachines.addProvider(new DisintegrationProvider());

        blacklistHost(li.cil.oc.api.internal.Adapter.class,
                Constants.BlockName.Geolyzer,
                Constants.BlockName.MotionSensor,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.BatteryUpgradeTier1,
                Constants.ItemName.BatteryUpgradeTier2,
                Constants.ItemName.BatteryUpgradeTier3,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.PistonUpgrade,
                Constants.ItemName.StickyPistonUpgrade,
                Constants.ItemName.SolarGeneratorUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);
        blacklistHost(li.cil.oc.api.internal.Drone.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2);
        blacklistHost(li.cil.oc.api.internal.Microcontroller.class,
                Constants.BlockName.Keyboard,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.APUTier1,
                Constants.ItemName.APUTier2,
                Constants.ItemName.GraphicsCardTier1,
                Constants.ItemName.GraphicsCardTier2,
                Constants.ItemName.GraphicsCardTier3,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.NavigationUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.TractorBeamUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);
        blacklistHost(li.cil.oc.api.internal.Robot.class,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.Analyzer,
                Constants.ItemName.LeashUpgrade);
        blacklistHost(li.cil.oc.api.internal.Tablet.class,
                Constants.BlockName.ScreenTier1,
                Constants.BlockName.Transposer,
                Constants.BlockName.CarpetedCapacitor,
                Constants.ItemName.NetworkCard,
                Constants.ItemName.RedstoneCardTier1,
                Constants.ItemName.AngelUpgrade,
                Constants.ItemName.ChunkloaderUpgrade,
                Constants.ItemName.CraftingUpgrade,
                Constants.ItemName.DatabaseUpgradeTier1,
                Constants.ItemName.DatabaseUpgradeTier2,
                Constants.ItemName.DatabaseUpgradeTier3,
                Constants.ItemName.ExperienceUpgrade,
                Constants.ItemName.GeneratorUpgrade,
                Constants.ItemName.HoverUpgradeTier1,
                Constants.ItemName.HoverUpgradeTier2,
                Constants.ItemName.InventoryUpgrade,
                Constants.ItemName.InventoryControllerUpgrade,
                Constants.ItemName.TankUpgrade,
                Constants.ItemName.TankControllerUpgrade,
                Constants.ItemName.LeashUpgrade,
                Constants.ItemName.TradingUpgrade);

        if (!li.cil.oc.core.impl.integration.util.BundledRedstone.isAvailable() && !li.cil.oc.core.impl.integration.util.WirelessRedstone.isAvailable()) {
            blacklistHost(li.cil.oc.api.internal.Drone.class, Constants.ItemName.RedstoneCardTier2);
            blacklistHost(li.cil.oc.api.internal.Tablet.class, Constants.ItemName.RedstoneCardTier2);
        }

        Registrar.lock();
        li.cil.oc.core.impl.server.driver.Registry.INSTANCE.setLocked(true);

        li.cil.oc.fabric.common.event.NanomachinesHandler.init();
        li.cil.oc.fabric.common.EventHandler.init();
        li.cil.oc.fabric.common.SaveHandler.init();
        li.cil.oc.fabric.common.event.AnalyzerEventHandler.init();
        li.cil.oc.fabric.common.event.HoverBootsHandler.init();
        li.cil.oc.fabric.server.ComponentTracker.init();
        li.cil.oc.fabric.server.network.Waypoints.init();
        li.cil.oc.fabric.server.network.WirelessNetwork.init();

        li.cil.oc.api.event.RobotPlaceInAirEvent.EVENT.register(AngelUpgradeHandler::onPlaceInAir);
        li.cil.oc.api.event.GeolyzerEvent.Scan.EVENT.register(EventHandlerVanilla::onGeolyzerScan);
        li.cil.oc.api.event.GeolyzerEvent.Analyze.EVENT.register(EventHandlerVanilla::onGeolyzerAnalyze);
        li.cil.oc.api.event.RobotMoveEvent.Post.EVENT.register(ChunkloaderUpgradeHandler::onMove);
        li.cil.oc.api.event.RobotAnalyzeEvent.EVENT.register(ExperienceUpgradeHandler::onRobotAnalyze);
        li.cil.oc.api.event.RobotUsedToolEvent.ComputeDamageRate.EVENT.register(ExperienceUpgradeHandler::onRobotComputeDamageRate);
        li.cil.oc.api.event.RobotBreakBlockEvent.Pre.EVENT.register(ExperienceUpgradeHandler::onRobotBreakBlockPre);
        li.cil.oc.api.event.RobotAttackEntityEvent.Post.EVENT.register(ExperienceUpgradeHandler::onRobotAttackEntityPost);
        li.cil.oc.api.event.RobotBreakBlockEvent.Post.EVENT.register(ExperienceUpgradeHandler::onRobotBreakBlockPost);
        li.cil.oc.api.event.RobotPlaceBlockEvent.Post.EVENT.register(ExperienceUpgradeHandler::onRobotPlaceBlockPost);
        li.cil.oc.api.event.RobotMoveEvent.Post.EVENT.register(ExperienceUpgradeHandler::onRobotMovePost);
        li.cil.oc.api.event.RobotExhaustionEvent.EVENT.register(ExperienceUpgradeHandler::onRobotExhaustion);
        li.cil.oc.api.event.RobotUsedToolEvent.ApplyDamageRate.EVENT.register(RobotCommonHandler::onRobotApplyDamageRate);
        li.cil.oc.api.event.RobotMoveEvent.Pre.EVENT.register(RobotCommonHandler::onRobotMove);
        li.cil.oc.api.event.RobotMoveEvent.Post.EVENT.register(WirelessNetworkCardHandler::onMove);
        li.cil.oc.api.event.FileSystemAccessEvent.Server.EVENT.register(FileSystemAccessHandler::onFileSystemAccessServer);
        li.cil.oc.api.event.FileSystemAccessEvent.Client.EVENT.register(FileSystemAccessHandler::onFileSystemAccessClient);
        li.cil.oc.api.event.NetworkActivityEvent.Server.EVENT.register(NetworkActivityHandler::onNetworkActivityServer);
    }

    @SuppressWarnings("unused")
    public static boolean useWrench(Player player, int x, int y, int z, boolean changeDurability) {
        if (player.getMainHandItem().getItem() instanceof Wrench wrench) {
            return wrench.useWrenchOnBlock(player, player.level(), new BlockPos(x, y, z), !changeDurability);
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static boolean isWrench(ItemStack stack) {
        return stack.getItem() instanceof Wrench;
    }

    @SuppressWarnings("unused")
    public static boolean canCharge(ItemStack stack) {
        if (stack.getItem() instanceof Chargeable chargeable) {
            return chargeable.canCharge(stack);
        }
        return false;
    }

    @SuppressWarnings("unused")
    public static double charge(ItemStack stack, double amount, boolean simulate) {
        if (stack.getItem() instanceof Chargeable chargeable) {
            return chargeable.charge(stack, amount, simulate);
        }
        return amount;
    }

    @SuppressWarnings("unused")
    public static int inkCartridgeInkProvider(ItemStack stack) {
        if (li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.InkCartridge)) {
            return OCSettings.get().printInkValue;
        }
        return 0;
    }

    @SuppressWarnings("unused")
    public static int dyeInkProvider(ItemStack stack) {
        if (Color.isDye(stack)) {
            return OCSettings.get().printInkValue / 10;
        }
        return 0;
    }

    private static void blacklistHost(Class<?> host, String... itemNames) {
        for (String itemName : itemNames) {
            ItemInfo itemInfo = li.cil.oc.api.Items.get(itemName);
            if (itemInfo != null) {
                Registrar.blacklistHost(itemName, host, itemInfo.createItemStack(1));
            }
        }
    }

    public static Logger log() {
        return li.cil.oc.core.impl.util.Log.get();
    }

    private static void addToCreativeTab(net.minecraft.world.item.CreativeModeTab.Output output, String itemName) {
        try {
            var info = li.cil.oc.api.API.items.get(itemName);
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
