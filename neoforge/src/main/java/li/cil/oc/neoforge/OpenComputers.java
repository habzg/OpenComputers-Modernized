package li.cil.oc.neoforge;

import java.util.List;
import java.util.stream.Collectors;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.Tags;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.CraftHandler;
import li.cil.oc.core.impl.common.Registrar;
import li.cil.oc.core.impl.common.network.OCPayload;
import li.cil.oc.core.impl.server.command.CommandHandler;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.SafeThreadPool;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.impl.util.ThreadPoolFactory;
import li.cil.oc.neoforge.common.EventHandler;
import li.cil.oc.neoforge.common.Proxy;
import li.cil.oc.neoforge.common.asm.SimpleComponentTickHandler;
import li.cil.oc.neoforge.common.asm.template.StaticSimpleEnvironment;
import li.cil.oc.neoforge.common.init.Blocks;
import li.cil.oc.neoforge.common.init.Entities;
import li.cil.oc.neoforge.common.init.Items;
import li.cil.oc.neoforge.common.init.LootModifiers;
import li.cil.oc.neoforge.common.init.Menus;
import li.cil.oc.neoforge.common.init.Recipes;
import li.cil.oc.neoforge.common.init.BlockEntities;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

@Mod(OpenComputers.ID)
public final class OpenComputers {
    public static final String ID = Tags.MOD_ID;

    public static final String Name = Tags.MOD_NAME;

    public static Proxy proxy;

    public static IEventBus getModEventBus() {
        return modEventBus;
    }

    private static IEventBus modEventBus;

    public OpenComputers(IEventBus modEventBus) {
        OpenComputers.modEventBus = modEventBus;
        if (net.neoforged.fml.ModList.get().isLoaded(li.cil.oc.core.integration.ModIDs.TIS3D)) {
            // Must be done in the constructor: the RegisterEvent for TIS-3D's
            // serial interface provider registry fires during mod loading,
            // before common setup, when the regular integration init runs.
            li.cil.oc.neoforge.integration.tis3d.ModTIS3D proxy = new li.cil.oc.neoforge.integration.tis3d.ModTIS3D();
            proxy.initialize();
        }
        SideTracker.setDedicatedServer(net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER);
        SideTracker.setCurrentServer(ServerLifecycleHooks::getCurrentServer);
        li.cil.oc.core.impl.server.fs.FileSystem.setEnvironmentFactory(li.cil.oc.core.impl.server.component.FileSystem::new);
        li.cil.oc.core.impl.server.component.traits.WorldAction.setHandler(new li.cil.oc.neoforge.util.BlockInteractionHandlerImpl());
        li.cil.oc.core.util.Tasks.setScheduler(EventHandler::scheduleServer);
        li.cil.oc.core.impl.util.FluidUtils.setHandler(new li.cil.oc.neoforge.util.FluidTransferHandlerImpl());
        li.cil.oc.core.util.ServerNetwork.setInstance(new li.cil.oc.core.impl.util.NeoServerNetwork());
        li.cil.oc.core.impl.util.SaveHandlerDelegate.setInstance(new li.cil.oc.core.impl.util.SaveHandlerDelegateImpl());
        li.cil.oc.core.util.MachineStateHelper.setInstance(new li.cil.oc.core.util.MachineStateHelper() {
            @Override
            public boolean isInSynchronizedCall(li.cil.oc.api.machine.Machine machine) {
                var state = ((li.cil.oc.neoforge.server.machine.Machine) machine).state();
                for (var s : state) {
                    if (s.id == STATE_SYNCHRONIZED_CALL || s.id == STATE_SYNCHRONIZED_RETURN) return true;
                }
                return false;
            }
        });
        li.cil.oc.core.util.ConverterRegistry.setInstance(new li.cil.oc.core.impl.util.NeoConverterRegistry());
        EventHandlerDelegate.setInstance(new li.cil.oc.neoforge.util.EventHandlerDelegate());
        li.cil.oc.core.impl.util.PlayerUtils.setDataProvider(li.cil.oc.neoforge.common.PlayerDataProvider.INSTANCE);
        ContainerProviderDelegate.setInstance(new ContainerProviderDelegate() {
            @Override
            public MenuProvider getContainerProvider(int guiType, Level world, int x, int y, int z) {
                return OpenComputers.getContainerProvider(guiType, world, x, y, z);
            }

            @Override
            public void openMenu(Player player, int guiType, Level world, int x, int y, int z) {
                if (player instanceof ServerPlayer serverPlayer) {
                    serverPlayer.openMenu(getContainerProvider(guiType, world, x, y, z), buf -> {
                        buf.writeInt(guiType);
                        buf.writeInt(x);
                        buf.writeInt(y);
                        buf.writeInt(z);
                        String address = "";
                        if (guiType == li.cil.oc.core.common.GuiType.Robot) {
                            var be = world.getBlockEntity(new net.minecraft.core.BlockPos(x, li.cil.oc.core.common.GuiType.extractY(y), z));
                            if (be instanceof li.cil.oc.core.impl.common.blockentity.RobotProxy rproxy) {
                                var addr = rproxy.robot.computerAddress();
                                if (addr != null) address = addr;
                            }
                        }
                        buf.writeUtf(address);
                    });
                } else {
                    player.openMenu(getContainerProvider(guiType, world, x, y, z));
                }
            }
        });
        li.cil.oc.core.impl.server.component.UpgradeGenerator.setFuelProvider(li.cil.oc.neoforge.common.FuelProvider.INSTANCE);
        li.cil.oc.core.impl.common.ComponentTracker.setServerTracker(li.cil.oc.neoforge.server.ComponentTracker.INSTANCE);
        li.cil.oc.core.util.PacketBuilderFactory.setInstance(new li.cil.oc.core.util.PacketBuilderFactory() {
            @Override
            public Object createCompressed(li.cil.oc.core.common.PacketType type) {
                return new li.cil.oc.neoforge.common.PacketBuilder.Compressed(type);
            }
        });
        li.cil.oc.core.util.ClientPacketSenderDelegate.setInstance(new li.cil.oc.neoforge.util.ClientPacketSenderDelegate());
        li.cil.oc.core.impl.common.PacketSender.setInstance(new li.cil.oc.neoforge.server.PacketSender());

        li.cil.oc.core.impl.server.network.Network.cbMultipartAvailable = Mods.CBMultipart.isAvailable();
        li.cil.oc.core.impl.server.network.Network.simpleComponentHandler = (be) -> {
            var level = be.getLevel();
            if (level != null) {
                var provider = level.getCapability(li.cil.oc.neoforge.common.capability.OCBlockCapabilities.SIMPLE_COMPONENT_PROVIDER, be.getBlockPos(), null);
                if (provider != null) return provider.node();
            }
            return li.cil.oc.neoforge.common.asm.template.StaticSimpleEnvironment.node(be, (li.cil.oc.api.network.SimpleComponent) be);
        };

        li.cil.oc.neoforge.CreativeTab.TABS.register(modEventBus);
        Menus.MENU_TYPES.register(modEventBus);
        Blocks.BLOCKS.register(modEventBus);
        Items.ITEMS.register(modEventBus);
        BlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        Entities.ENTITY_TYPES.register(modEventBus);
        Recipes.RECIPE_SERIALIZERS.register(modEventBus);
        LootModifiers.GLM_CODECS.register(modEventBus);
        li.cil.oc.neoforge.common.init.Conditions.CONDITION_SERIALIZERS.register(modEventBus);

        if (Mods.CBMultipart.isModAvailable()) {
            li.cil.oc.neoforge.integration.cbmultipart.MultipartRegistrations.init(modEventBus);
        }

        li.cil.oc.core.impl.common.item.RedstoneCard.setProjectRedAvailable(Mods.ProjectRedTransmission::isAvailable);
        li.cil.oc.core.impl.common.item.Debugger.setFakePlayerCheck((player) -> player instanceof net.neoforged.neoforge.common.util.FakePlayer);
        li.cil.oc.core.impl.common.item.FloppyDisk.setOpenDriveScreen(() -> li.cil.oc.neoforge.client.GuiHandler.openScreen(GuiType.Drive, 0, 0, 0));
        li.cil.oc.core.impl.common.item.HardDiskDrive.setOpenDriveScreen(() -> li.cil.oc.neoforge.client.GuiHandler.openScreen(GuiType.Drive, 0, 0, 0));
        li.cil.oc.core.impl.common.item.Drone.setAgentOwnerProvider((player) -> {
            if (player instanceof li.cil.oc.neoforge.server.agent.Player fakePlayer) {
                return new li.cil.oc.core.impl.common.item.Drone.PlayerOwner(fakePlayer.agent.ownerName(), fakePlayer.agent.ownerUUID());
            }
            return null;
        });
        li.cil.oc.core.impl.common.item.Analyzer.setFakePlayerCheck((player) -> player instanceof net.neoforged.neoforge.common.util.FakePlayer);
        li.cil.oc.core.impl.common.item.Tablet.setTerminalPacketSender((address, player) -> {
            try (var pb = new li.cil.oc.neoforge.common.SimplePacketBuilder(li.cil.oc.core.common.PacketType.OpenTabletTerminal)) {
                pb.writeUTF(address);
                pb.writeNBT(new net.minecraft.nbt.CompoundTag());
                pb.sendToPlayer(player);
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        });
        li.cil.oc.core.impl.common.item.UpgradeTank.setFluidTooltipProvider((registries, data) -> {
            var fluidStack = net.neoforged.neoforge.fluids.FluidStack.parse(registries, data).orElse(null);
            if (fluidStack != null && !fluidStack.isEmpty()) {
                return fluidStack.getHoverName().getString() + ": " + fluidStack.getAmount() + "/16000";
            }
            return null;
        });

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(OpenComputers::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        NeoForge.EVENT_BUS.addListener(this::serverStop);

        NeoForge.EVENT_BUS.register(SimpleComponentTickHandler.Instance);

        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {

            event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        if (blockEntity instanceof li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor acceptor
                                && side != null && acceptor.canConnectPower(side)) {
                            return new li.cil.oc.neoforge.common.capability.InternalEnergyStorage(acceptor, side);
                        }
                        return null;
                    },
                    Blocks.BLOCKS.getEntries().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            );

            event.registerBlock(Capabilities.ItemHandler.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        if (blockEntity instanceof net.minecraft.world.Container container && side != null) {
                            return new net.neoforged.neoforge.items.wrapper.InvWrapper(container);
                        }
                        return null;
                    },
                    Blocks.BLOCKS.getEntries().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            );

            event.registerBlock(Capabilities.FluidHandler.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        if (blockEntity instanceof net.neoforged.neoforge.fluids.capability.IFluidHandler handler) {
                            return handler;
                        }
                        return null;
                    },
                    Blocks.BLOCKS.getEntries().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            );

            List<Item> chargeableList = Items.ITEMS.getEntries().stream().map(DeferredHolder::get).filter(item -> item instanceof Chargeable).collect(Collectors.toList());
            if (!chargeableList.isEmpty()) {
                net.minecraft.world.item.Item[] chargeableItems = chargeableList.toArray(new net.minecraft.world.item.Item[0]);
                event.registerItem(Capabilities.EnergyStorage.ITEM,
                        (itemStack, ctx) -> {
                            var item = itemStack.getItem();
                            if (item instanceof Chargeable chargeable) {
                                return new li.cil.oc.neoforge.common.capability.ChargeableEnergyStorage(itemStack, chargeable);
                            }
                            return null;
                        },
                        chargeableItems);
            }
            if (Mods.ComputerCraft.isModAvailable()) {
                li.cil.oc.neoforge.integration.computercraft.PeripheralProvider.registerCapabilities(event);
            }

            final net.minecraft.world.level.block.Block[] allBlocks =
                    Blocks.BLOCKS.getEntries().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new);
            li.cil.oc.neoforge.common.capability.OCBlockCapabilities.register(event, allBlocks);

            event.registerBlock(li.cil.oc.neoforge.common.capability.OCBlockCapabilities.SIMPLE_COMPONENT_PROVIDER,
                    (level, pos, state, blockEntity, side) -> {
                        if (blockEntity instanceof li.cil.oc.api.network.SimpleComponent sc) {
                            return new li.cil.oc.neoforge.common.capability.SimpleComponentCapability(blockEntity, sc);
                        }
                        return null;
                    },
                    allBlocks);

            li.cil.oc.core.impl.server.network.Network.capabilityNodeHandler = (blockEntity, side) -> {
                final net.minecraft.world.level.Level level = blockEntity.getLevel();
                final net.minecraft.core.BlockPos pos = blockEntity.getBlockPos();
                if (level != null) {
                    final li.cil.oc.api.network.SidedEnvironment sided = level.getCapability(
                            li.cil.oc.neoforge.common.capability.OCBlockCapabilities.SIDED_ENVIRONMENT, pos, side);
                    if (sided != null) return sided.sidedNode(side);
                    final li.cil.oc.api.network.Environment environment = level.getCapability(
                            li.cil.oc.neoforge.common.capability.OCBlockCapabilities.ENVIRONMENT, pos, side);
                    if (environment != null) return environment.node();
                }
                return null;
            };

            if (Mods.AppliedEnergistics2.isModAvailable()) {
                List<net.minecraft.world.level.block.Block> powerBlockList = Blocks.BLOCKS.getEntries().stream()
                        .map(DeferredHolder::get)
                        .filter(b -> b instanceof li.cil.oc.core.impl.common.block.traits.PowerAcceptor)
                        .collect(Collectors.toList());
                if (!powerBlockList.isEmpty()) {
                    net.minecraft.world.level.block.Block[] powerBlocks = powerBlockList.toArray(new net.minecraft.world.level.block.Block[0]);
                    li.cil.oc.neoforge.integration.appeng.ModAppEng.registerCapabilities(event, powerBlocks);
                }
            }

            if (Mods.MoreRed.isModAvailable()) {
                li.cil.oc.neoforge.integration.morered.ModMoreRed.registerCapabilities(event);
            }

            });
    }

    public static Logger log() {
        return li.cil.oc.core.impl.util.Log.get();
    }

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

    public static MenuProvider getContainerProvider(int guiType, Level world, int x, int y, int z) {
        return new MenuProvider() {
            @Override
            public @NotNull Component getDisplayName() {
                String key = CONTAINER_TITLES.get(guiType);
                if (key != null) return Component.translatable(key);
                return Component.translatable("gui.opencomputers." + guiType);
            }

            @Override
            public AbstractContainerMenu createMenu(int containerId, @NotNull Inventory inv, @NotNull Player player) {
                return li.cil.oc.neoforge.common.GuiHandler.getServerGuiElement(containerId, guiType, player, world, x, y, z, "");
            }
        };
    }

    private void commonSetup(final FMLCommonSetupEvent e) {
        CraftHandler.isFakePlayer = player -> player instanceof FakePlayer;

        if (proxy == null) {
            proxy = new li.cil.oc.neoforge.common.Proxy();
        }
        proxy.preInit(e);
        proxy.init(e);
        proxy.postInit();
        Registrar.lock();
        log().info("Done with pre init phase.");
    }

    public void serverStarting(final RegisterCommandsEvent e) {
        CommandHandler.register(e.getDispatcher());
        ThreadPoolFactory.safePools.forEach(SafeThreadPool::newThreadPool);

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
            } else if (!OCSettings.get().internetFilteringRulesObserved && e.getCommandSelection() == Commands.CommandSelection.DEDICATED) {
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
    }

    public void serverStop(final ServerStoppedEvent ignoredE) {
        ThreadPoolFactory.safePools.forEach(SafeThreadPool::waitForCompletion);
        StaticSimpleEnvironment.onServerStopped();
    }

    public static void onRegisterPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        final var registrar = event.registrar(OpenComputers.ID);
        registrar.playBidirectional(
                OCPayload.TYPE,
                OCPayload.STREAM_CODEC,
                new DirectionalPayloadHandler<>(
                        (OCPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) -> {
                            var buf = io.netty.buffer.Unpooled.wrappedBuffer(payload.data());
                            li.cil.oc.neoforge.client.ClientPacketHandler.INSTANCE.onPacket(buf);
                        },
                        (OCPayload payload, net.neoforged.neoforge.network.handling.IPayloadContext context) -> {
                            var player = context.player();
                            var buf = io.netty.buffer.Unpooled.wrappedBuffer(payload.data());
                            li.cil.oc.neoforge.server.PacketHandler.INSTANCE.onPacketData(buf, player);
                        }
                )
        );
    }
}
