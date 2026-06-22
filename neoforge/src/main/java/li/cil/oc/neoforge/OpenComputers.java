package li.cil.oc.neoforge;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.core.Tags;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.Registrar;
import li.cil.oc.core.impl.util.ContainerProviderDelegate;
import li.cil.oc.core.impl.util.EventHandlerDelegate;
import li.cil.oc.core.impl.util.SafeThreadPool;
import li.cil.oc.core.impl.util.SideTracker;
import li.cil.oc.core.impl.util.ThreadPoolFactory;
import li.cil.oc.neoforge.common.Proxy;
import li.cil.oc.neoforge.common.asm.SimpleComponentTickHandler;

import li.cil.oc.neoforge.common.asm.template.StaticSimpleEnvironment;
import li.cil.oc.neoforge.common.init.Blocks;
import li.cil.oc.neoforge.common.init.Entities;
import li.cil.oc.neoforge.common.init.Items;
import li.cil.oc.neoforge.common.init.LootModifiers;
import li.cil.oc.neoforge.common.init.Menus;
import li.cil.oc.neoforge.common.init.Recipes;
import li.cil.oc.neoforge.common.init.TileEntities;
import li.cil.oc.neoforge.common.network.OCPayload;
import li.cil.oc.neoforge.integration.Mods;
import li.cil.oc.neoforge.server.command.CommandHandler;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
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
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.DirectionalPayloadHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.stream.Collectors;

@Mod(OpenComputers.ID)
public final class OpenComputers {
    public static final String ID = Tags.MOD_ID;

    public static final String Name = Tags.MOD_NAME;

    public static final String Version = Tags.VERSION;
    public static Proxy proxy;

    public OpenComputers(IEventBus modEventBus) {
        SideTracker.setDedicatedServer(net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.DEDICATED_SERVER);
        SideTracker.setCurrentServer(ServerLifecycleHooks::getCurrentServer);
        li.cil.oc.core.impl.server.fs.FileSystem.setEnvironmentFactory(li.cil.oc.core.impl.server.component.FileSystem::new);
        li.cil.oc.core.impl.server.component.traits.WorldAction.setHandler(new li.cil.oc.neoforge.util.BlockInteractionHandlerImpl());
        li.cil.oc.core.util.Tasks.setScheduler(new li.cil.oc.neoforge.util.TaskSchedulerImpl());
        li.cil.oc.core.impl.util.FluidUtils.setHandler(new li.cil.oc.neoforge.util.FluidTransferHandlerImpl());
        li.cil.oc.core.util.ServerNetwork.setInstance(new li.cil.oc.core.impl.util.NeoServerNetwork());
        li.cil.oc.core.impl.util.SaveHandlerDelegate.setInstance(new li.cil.oc.neoforge.util.NeoSaveHandlerDelegate());
        li.cil.oc.core.util.MachineStateHelper.setInstance(new li.cil.oc.neoforge.util.NeoMachineStateHelper());
        li.cil.oc.core.util.ConverterRegistry.setInstance(new li.cil.oc.core.impl.util.NeoConverterRegistry());
        EventHandlerDelegate.setInstance(new li.cil.oc.neoforge.util.NeoEventHandlerDelegate());
        ContainerProviderDelegate.setInstance(new li.cil.oc.neoforge.util.NeoContainerProviderDelegate());
        li.cil.oc.core.impl.common.ComponentTracker.setServerTracker(li.cil.oc.neoforge.server.ComponentTracker.INSTANCE);
        li.cil.oc.core.util.PacketBuilderFactory.setInstance(new li.cil.oc.neoforge.util.NeoPacketBuilderFactory());
        li.cil.oc.core.util.ClientPacketSenderDelegate.setInstance(new li.cil.oc.neoforge.util.NeoClientPacketSenderDelegate());
        li.cil.oc.core.impl.common.PacketSender.setInstance(new li.cil.oc.neoforge.server.NeoPacketSender());

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
        TileEntities.TILE_ENTITY_TYPES.register(modEventBus);
        Entities.ENTITY_TYPES.register(modEventBus);
        Recipes.RECIPE_SERIALIZERS.register(modEventBus);
        LootModifiers.GLM_CODECS.register(modEventBus);
        li.cil.oc.neoforge.common.init.Conditions.CONDITION_SERIALIZERS.register(modEventBus);

        if (Mods.CBMultipart.isModAvailable()) {
            li.cil.oc.neoforge.integration.cbmultipart.MultipartRegistrations.init(modEventBus);
        }

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(OpenComputers::onRegisterPayloadHandlers);

        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        NeoForge.EVENT_BUS.addListener(this::serverStop);

        NeoForge.EVENT_BUS.register(SimpleComponentTickHandler.Instance);

        modEventBus.addListener((RegisterCapabilitiesEvent event) -> {

            event.registerBlock(Capabilities.EnergyStorage.BLOCK,
                    (level, pos, state, blockEntity, side) -> {
                        if (blockEntity instanceof li.cil.oc.core.impl.common.tileentity.traits.PowerAcceptor acceptor
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
            li.cil.oc.neoforge.integration.computercraft.PeripheralProvider.registerCapabilities(event);

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
                    event.registerBlock(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
                            (level, pos, state, blockEntity, side) -> {
                                if (blockEntity instanceof li.cil.oc.core.impl.common.tileentity.traits.power.AppliedEnergistics2 ae2) {
                                    return new li.cil.oc.neoforge.integration.appeng.OCGridNodeHost(ae2);
                                }
                                return null;
                            },
                            powerBlocks
                    );
                }
            }

            if (Mods.ComputerCraft.isModAvailable()) {
                event.registerBlockEntity(
                        dan200.computercraft.api.peripheral.PeripheralCapability.get(),
                        TileEntities.ADAPTER.get(),
                        (be, side) -> new li.cil.oc.neoforge.integration.computercraft.AdapterPeripheral(be)
                );
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
        CONTAINER_TITLES.put(GuiType.Printer, "container.opencomputers.printer");
        CONTAINER_TITLES.put(GuiType.Rack, "container.opencomputers.rack");
        CONTAINER_TITLES.put(GuiType.Raid, "container.opencomputers.raid");
        CONTAINER_TITLES.put(GuiType.Relay, "container.opencomputers.relay");
        CONTAINER_TITLES.put(GuiType.Server, "container.opencomputers.server");
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
                return li.cil.oc.neoforge.common.GuiHandler.getServerGuiElement(containerId, guiType, player, world, x, y, z);
            }
        };
    }

    private void commonSetup(final FMLCommonSetupEvent e) {
        li.cil.oc.neoforge.common.event.NeoForgeEventBridge.init();
        li.cil.oc.api.event.OCEventFactory.setInstance(new li.cil.oc.neoforge.event.NeoForgeOCEventFactory());

        if (proxy == null) {
            proxy = new li.cil.oc.neoforge.server.Proxy();
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

        if (Settings.get().internetAccessConfigured()) {
            if (Settings.get().internetFilteringRulesInvalid()) {
                log().warn("####################################################");
                log().warn("#                                                  #");
                log().warn("#  Could not parse Internet Card filtering rules!  #");
                log().warn("#  Review the server log and adjust the filtering  #");
                log().warn("#  list to ensure it is appropriately configured.  #");
                log().warn("#   (config/OpenComputers.cfg => filteringRules)   #");
                log().warn("# Internet access has been automatically disabled. #");
                log().warn("#                                                  #");
                log().warn("####################################################");
            } else if (!Settings.get().internetFilteringRulesObserved && e.getCommandSelection() == Commands.CommandSelection.DEDICATED) {
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
                log().info("Successfully applied {} Internet Card filtering rules.", Settings.get().internetFilteringRules.length);
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
                        li.cil.oc.neoforge.client.ClientPayloadHandler::handle,
                        li.cil.oc.neoforge.server.network.ServerPayloadHandler::handle
                )
        );
    }
}
