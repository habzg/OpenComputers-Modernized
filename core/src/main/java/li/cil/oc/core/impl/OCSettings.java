package li.cil.oc.core.impl;

import com.mojang.authlib.GameProfile;
import li.cil.repack.com.typesafe.config.Config;
import li.cil.repack.com.typesafe.config.ConfigFactory;
import li.cil.repack.com.typesafe.config.ConfigRenderOptions;
import li.cil.repack.com.typesafe.config.ConfigValue;
import li.cil.repack.com.typesafe.config.ConfigValueFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import li.cil.oc.core.impl.util.InternetFilteringRule;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OCSettings {
    private static final Logger LOGGER = LoggerFactory.getLogger(OCSettings.class);
    public static final String resourceDomain = "opencomputers";
    public static final String namespace = "oc:";
    public static final String savePath = "opencomputers/";
    public static final String scriptPath = "/assets/" + resourceDomain + "/lua/";
    public static final int[][] screenResolutionsByTier = {{50, 16}, {80, 25}, {160, 50}};
    public static final li.cil.oc.api.internal.TextBuffer.ColorDepth[] screenDepthsByTier = {
            li.cil.oc.api.internal.TextBuffer.ColorDepth.OneBit,
            li.cil.oc.api.internal.TextBuffer.ColorDepth.FourBit,
            li.cil.oc.api.internal.TextBuffer.ColorDepth.EightBit
    };
    public static final int[] deviceComplexityByTier = {12, 24, 32, 9001};
    private static final java.util.List<String> forbiddenConfigLists = Arrays.asList(
            "internet.blacklist", "internet.whitelist"
    );
    private static final String prefix = "opencomputers.";
    @SuppressWarnings("unchecked")
    private static final java.util.AbstractMap.SimpleEntry<String, String[]>[] configPatches = new java.util.AbstractMap.SimpleEntry[]{
            new java.util.AbstractMap.SimpleEntry<>("1.4.7", new String[]{"misc.geolyzerNoise"}),
            new java.util.AbstractMap.SimpleEntry<>("1.4.8", new String[]{"power.value.AppliedEnergistics2", "power.value.RedstoneFlux"}),
            new java.util.AbstractMap.SimpleEntry<>("1.5.20", new String[]{"switch.relayDelayUpgrade"}),
            new java.util.AbstractMap.SimpleEntry<>("1.7.2", new String[]{"power.cost.wirelessCostPerRange", "misc.maxWirelessRange", "misc.maxOpenPorts", "computer.cpuComponentCount"}),
            new java.util.AbstractMap.SimpleEntry<>("1.8.0", new String[]{"computer.robot.limitFlightHeight"})
    };
    private static final String filteringRulesPatchVersion = "1.8.3";
    public static boolean rTreeDebugRenderer = false;
    private static OCSettings settings;
    public final Config config;
    // client
    public final double screenTextFadeStartDistance;
    public final double maxScreenTextRenderDistance;
    public final boolean textLinearFiltering;
    public final boolean textAntiAlias;
    public final boolean robotLabels;
    public final float soundVolume;
    public final double fontCharScale;
    public final double hologramFadeStartDistance;
    public final double hologramRenderDistance;
    public final double hologramFlickerFrequency;
    public final int monochromeColor;
    public final String fontRenderer;
    public final int beepSampleRate;
    public final int beepAmplitude;
    public final float beepRadius;
    public final double[] nanomachineHudPos;
    public final boolean enableNanomachinePfx;
    public final int transposerFluidTransferRate;
    // computer
    public final int threads;
    public final double timeout;
    public final double startupDelay;
    public final int eepromSize;
    public final int eepromDataSize;
    public final int[] cpuComponentSupport;
    public final double[] callBudgets;
    public final boolean canComputersBeOwned;
    public final int maxUsers;
    public final int maxUsernameLength;
    public final boolean eraseTmpOnReboot;
    public final int executionDelay;
    // computer.lua
    public final boolean allowBytecode;
    public final boolean allowGC;
    public final boolean enableLua53;
    public final boolean defaultLua53;
    public final boolean enableLua54;
    public final int[] ramSizes;
    public final double ramScaleFor64Bit;
    public final int maxTotalRam;
    // robot
    public final boolean allowActivateBlocks;
    public final boolean allowUseItemsWithDuration;
    public final boolean canAttackPlayers;
    public final int limitFlightHeight;
    public final boolean screwCobwebs;
    public final double swingRange;
    public final double useAndPlaceRange;
    public final double itemDamageRate;
    public final String nameFormat;
    public final String uuidFormat;
    public final int[] upgradeFlightHeight;
    // robot.xp
    public final double baseXpToLevel;
    public final double constantXpGrowth;
    public final double exponentialXpGrowth;
    public final double robotActionXp;
    public final double robotExhaustionXpRate;
    public final double robotOreXpRate;
    public final double bufferPerLevel;
    public final double toolEfficiencyPerLevel;
    public final double harvestSpeedBoostPerLevel;
    // robot.delays
    public final double turnDelay;
    public final double moveDelay;
    public final double swingDelay;
    public final double useDelay;
    public final double placeDelay;
    public final double dropDelay;
    public final double suckDelay;
    public final double harvestRatio;
    // power
    public final boolean ignorePower;
    public final double tickFrequency;
    public final double chargeRateExternal;
    public final double chargeRateTablet;
    public final double generatorEfficiency;
    public final double solarGeneratorEfficiency;
    public final double assemblerTickAmount;
    public final double disassemblerTickAmount;
    public final double printerTickAmount;
    // power.carpetedCapacitors
    public final double sheepPower;
    public final double ocelotPower;
    public final double carpetDamageChance;
    // power.buffer
    public final double bufferCapacitor;
    public final double bufferCapacitorAdjacencyBonus;
    public final double bufferComputer;
    public final double bufferRobot;
    public final double bufferConverter;
    public final double bufferDistributor;
    public final double[] bufferCapacitorUpgrades;
    public final double bufferTablet;
    public final double bufferAccessPoint;
    public final double bufferDrone;
    public final double bufferMicrocontroller;
    public final double bufferHoverBoots;
    public final double bufferNanomachines;
    // power.cost
    public final double computerCost;
    public final double microcontrollerCost;
    public final double robotCost;
    public final double droneCost;
    public final double sleepCostFactor;
    public final double screenCost;
    public final double hologramCost;
    public final double hddReadCost;
    public final double hddWriteCost;
    public final double gpuSetCost;
    public final double gpuFillCost;
    public final double gpuClearCost;
    public final double gpuCopyCost;
    public final double robotTurnCost;
    public final double robotMoveCost;
    public final double robotExhaustionCost;
    public final double[] wirelessCostPerRange;
    public final double geolyzerScanCost;
    public final double robotBaseCost;
    public final double robotComplexityCost;
    public final double microcontrollerBaseCost;
    public final double microcontrollerComplexityCost;
    public final double tabletBaseCost;
    public final double tabletComplexityCost;
    public final double droneBaseCost;
    public final double droneComplexityCost;
    public final double disassemblerItemCost;
    public final double chunkloaderCost;
    public final double pistonCost;
    public final double eepromWriteCost;
    public final double printCost;
    public final double hoverBootJump;
    public final double hoverBootAbsorb;
    public final double hoverBootMove;
    public final double dataCardTrivial;
    public final double dataCardTrivialByte;
    public final double dataCardSimple;
    public final double dataCardSimpleByte;
    public final double dataCardComplex;
    public final double dataCardComplexByte;
    public final double dataCardAsymmetric;
    public final double transposerCost;
    public final double nanomachineCost;
    public final double nanomachineReconfigureCost;
    public final double mfuCost;
    // power.rate
    public final double accessPointRate;
    public final double assemblerRate;
    public final double[] caseRate;
    public final double chargerRate;
    public final double disassemblerRate;
    public final double powerConverterRate;
    public final double serverRackRate;
    public final double capacitorRate;
    public final double powerDistributorRate;
    public final double relayRate;
    // filesystem
    public final int fileCost;
    public final boolean bufferChanges;
    public final int[] hddSizes;
    public final int[] hddPlatterCounts;
    public final int floppySize;
    public final int tmpSize;
    public final int maxHandles;
    public final int maxReadBuffer;
    public final int sectorSeekThreshold;
    public final double sectorSeekTime;
    // internet
    public final boolean httpEnabled;
    public final boolean httpHeadersEnabled;
    public final boolean tcpEnabled;
    public final InternetFilteringRule[] internetFilteringRules;
    public final boolean internetFilteringRulesObserved;
    public final int httpTimeout;
    public final int maxConnections;
    public final int internetThreads;
    public final String httpUserAgent;
    // switch
    public final int switchDefaultMaxQueueSize;
    public final int switchQueueSizeUpgrade;
    public final int switchDefaultRelayDelay;
    public final double switchRelayDelayUpgrade;
    public final int switchDefaultRelayAmount;
    public final int switchRelayAmountUpgrade;
    // hologram
    public final double[] hologramMaxScaleByTier;
    public final double[] hologramMaxTranslationByTier;
    public final double hologramSetRawDelay;
    public final boolean hologramLight;
    // misc
    public final int maxScreenWidth;
    public final int maxScreenHeight;
    public final boolean inputUsername;
    public final int initialNetworkPacketTTL;
    public final int maxNetworkPacketSize;
    public final int maxNetworkPacketParts;
    public final int[] maxOpenPorts;
    public final double[] maxWirelessRange;
    public final int rTreeMaxEntries = 10;
    public final int terminalsPerServer = 4;
    public final int lootProbability;
    public final boolean lootRecrafting;
    public final int geolyzerRange;
    public final float geolyzerNoise;
    public final boolean disassembleAllTheThings;
    public final double disassemblerBreakChance;
    public final java.util.List<String> disassemblerInputBlacklist;
    public final boolean hideOwnPet;
    public final boolean allowItemStackInspection;
    public final int[] databaseEntriesPerTier = {9, 25, 81};
    public final double presentChance;
    public final java.util.List<String> assemblerBlacklist;
    public final int threadPriority;
    public final boolean giveManualToNewPlayers;
    public final int dataCardSoftLimit;
    public final int dataCardHardLimit;
    public final double dataCardTimeout;
    public final double redstoneDelay;
    public final double tradingRange;
    public final int mfuRange;
    public final int maxClipboard;
    // nanomachines
    public final double nanomachineTriggerQuota;
    public final double nanomachineConnectorQuota;
    public final int nanomachineMaxInputs;
    public final int nanomachineMaxOutputs;
    public final int nanomachinesSafeInputsActive;
    public final int nanomachinesMaxInputsActive;
    public final double nanomachinesCommandDelay;
    public final double nanomachinesCommandRange;
    public final double nanomachineMagnetRange;
    public final int nanomachineDisintegrationRange;
    public final List<?> nanomachinePotionWhitelist;
    public final float nanomachinesHungryDamage;
    public final double nanomachinesHungryEnergyRestored;
    // printer
    public final int maxPrintComplexity;
    public final double printRecycleRate;
    public final boolean chameliumEdible;
    public final int maxPrintLightLevel;
    public final int printCustomRedstone;
    public final int printMaterialValue;
    public final int printInkValue;
    public final boolean printsHaveOpacity;
    public final double noclipMultiplier;
    // chunkloader
    public final java.util.List<Integer> chunkloadDimensionBlacklist;
    public final java.util.List<Integer> chunkloadDimensionWhitelist;
    // integration
    public final java.util.List<String> modBlacklist;
    public final java.util.List<String> peripheralBlacklist;
    public final String fakePlayerUuid;
    public final String fakePlayerName;
    public final GameProfile fakePlayerProfile;
    // integration.vanilla
    public final boolean enableInventoryDriver;
    public final boolean enableTankDriver;
    public final boolean enableCommandBlockDriver;
    public final boolean allowItemStackNBTTags;
    // debug
    public final boolean logLuaCallbackErrors;
    public final boolean forceLuaJ;
    public final boolean allowUserdata;
    public final boolean allowPersistence;
    public final boolean limitMemory;
    public final boolean forceCaseInsensitive;
    public final boolean logFullLibLoadErrors;
    public final String forceNativeLibPlatform;
    public final String forceNativeLibPathFirst;
    public final boolean logHexFontErrors;
    public final boolean alwaysTryNative;
    public final boolean debugPersistence;
    public final boolean nativeInTmpDir;
    public final boolean periodicallyForceLightUpdate;
    public final boolean insertIdsInConverters;
    public final DebugCardAccess debugCardAccess;
    public final boolean registerLuaJArchitecture;
    public final boolean disableLocaleChanging;
    // >= 1.7.4
    public final int maxSignalQueueSize;
    // >= 1.7.6
    public final double[] vramSizes;
    public final double bitbltCost;
    // >= 1.8.2
    public final int diskActivitySoundDelay;
    public final double maxNetworkClientPacketDistance;

    public final double maxNetworkClientEffectPacketDistance;
    public final double maxNetworkClientSoundPacketDistance;

    public OCSettings(Config config, File gameDir) {
        this.config = config;

        // client
        screenTextFadeStartDistance = config.getDouble("client.screenTextFadeStartDistance");
        maxScreenTextRenderDistance = config.getDouble("client.maxScreenTextRenderDistance");
        textLinearFiltering = config.getBoolean("client.textLinearFiltering");
        textAntiAlias = config.getBoolean("client.textAntiAlias");
        robotLabels = config.getBoolean("client.robotLabels");
        soundVolume = Math.clamp((float) config.getDouble("client.soundVolume"), 0, 2);
        fontCharScale = Math.clamp(config.getDouble("client.fontCharScale"), 0.5, 2);
        hologramFadeStartDistance = Math.max(0, config.getDouble("client.hologramFadeStartDistance"));
        hologramRenderDistance = Math.max(0, config.getDouble("client.hologramRenderDistance"));
        hologramFlickerFrequency = Math.max(0, config.getDouble("client.hologramFlickerFrequency"));
        monochromeColor = Integer.decode(config.getString("client.monochromeColor"));
        fontRenderer = config.getString("client.fontRenderer");
        beepSampleRate = config.getInt("client.beepSampleRate");
        beepAmplitude = Math.clamp(config.getInt("client.beepVolume"), 0, Byte.MAX_VALUE);
        beepRadius = Math.clamp((float) config.getDouble("client.beepRadius"), 1, 32);
        {
            java.util.List<Double> hudPosList = config.getDoubleList("client.nanomachineHudPos");
            if (hudPosList.size() >= 2) {
                nanomachineHudPos = new double[]{hudPosList.get(0), hudPosList.get(1)};
            } else {
                LOGGER.warn("Bad number of HUD coordinates, ignoring.");
                nanomachineHudPos = new double[]{-1.0, -1.0};
            }
        }
        enableNanomachinePfx = config.getBoolean("client.enableNanomachinePfx");
        transposerFluidTransferRate = config.getInt("misc.transposerFluidTransferRate");

        // computer
        threads = Math.max(1, config.getInt("computer.threads"));
        timeout = Math.max(0, config.getDouble("computer.timeout"));
        startupDelay = Math.max(0.05, config.getDouble("computer.startupDelay"));
        eepromSize = Math.max(0, config.getInt("computer.eepromSize"));
        eepromDataSize = Math.max(0, config.getInt("computer.eepromDataSize"));
        {
            java.util.List<Integer> cpuList = config.getIntList("computer.cpuComponentCount");
            if (cpuList.size() >= 4) {
                cpuComponentSupport = new int[]{cpuList.get(0), cpuList.get(1), cpuList.get(2), cpuList.get(3)};
            } else {
                LOGGER.warn("Bad number of CPU component counts, ignoring.");
                cpuComponentSupport = new int[]{8, 12, 16, 1024};
            }
        }
        {
            java.util.List<Double> budgetList = config.getDoubleList("computer.callBudgets");
            if (budgetList.size() >= 3) {
                callBudgets = new double[]{budgetList.get(0), budgetList.get(1), budgetList.get(2)};
            } else {
                LOGGER.warn("Bad number of call budgets, ignoring.");
                callBudgets = new double[]{0.5, 1.0, 1.5};
            }
        }
        canComputersBeOwned = config.getBoolean("computer.canComputersBeOwned");
        maxUsers = Math.max(0, config.getInt("computer.maxUsers"));
        maxUsernameLength = Math.max(0, config.getInt("computer.maxUsernameLength"));
        eraseTmpOnReboot = config.getBoolean("computer.eraseTmpOnReboot");
        executionDelay = Math.max(0, config.getInt("computer.executionDelay"));

        // computer.lua
        allowBytecode = config.getBoolean("computer.lua.allowBytecode");
        allowGC = config.getBoolean("computer.lua.allowGC");
        enableLua53 = config.getBoolean("computer.lua.enableLua53");
        defaultLua53 = config.getBoolean("computer.lua.defaultLua53");
        enableLua54 = config.getBoolean("computer.lua.enableLua54");
        {
            java.util.List<Integer> ramList = config.getIntList("computer.lua.ramSizes");
            if (ramList.size() >= 6) {
                ramSizes = new int[]{ramList.get(0), ramList.get(1), ramList.get(2), ramList.get(3), ramList.get(4), ramList.get(5)};
            } else {
                LOGGER.warn("Bad number of RAM sizes, ignoring.");
                ramSizes = new int[]{192, 256, 384, 512, 768, 1024};
            }
        }
        ramScaleFor64Bit = Math.max(1, config.getDouble("computer.lua.ramScaleFor64Bit"));
        maxTotalRam = Math.max(0, config.getInt("computer.lua.maxTotalRam"));

        // robot
        allowActivateBlocks = config.getBoolean("robot.allowActivateBlocks");
        allowUseItemsWithDuration = config.getBoolean("robot.allowUseItemsWithDuration");
        canAttackPlayers = config.getBoolean("robot.canAttackPlayers");
        limitFlightHeight = Math.max(-1, config.getInt("robot.limitFlightHeight"));
        screwCobwebs = config.getBoolean("robot.notAfraidOfSpiders");
        swingRange = config.getDouble("robot.swingRange");
        useAndPlaceRange = config.getDouble("robot.useAndPlaceRange");
        itemDamageRate = Math.clamp(config.getDouble("robot.itemDamageRate"), 0, 1);
        nameFormat = config.getString("robot.nameFormat");
        uuidFormat = config.getString("robot.uuidFormat");
        {
            java.util.List<Integer> flightList = config.getIntList("robot.upgradeFlightHeight");
            if (flightList.size() >= 2) {
                upgradeFlightHeight = new int[]{flightList.get(0), flightList.get(1)};
            } else {
                LOGGER.warn("Bad number of hover flight height counts, ignoring.");
                upgradeFlightHeight = new int[]{64, 256};
            }
        }

        // robot.xp
        baseXpToLevel = Math.max(0, config.getDouble("robot.xp.baseValue"));
        constantXpGrowth = Math.max(1, config.getDouble("robot.xp.constantGrowth"));
        exponentialXpGrowth = Math.max(1, config.getDouble("robot.xp.exponentialGrowth"));
        robotActionXp = Math.max(0, config.getDouble("robot.xp.actionXp"));
        robotExhaustionXpRate = Math.max(0, config.getDouble("robot.xp.exhaustionXpRate"));
        robotOreXpRate = Math.max(0, config.getDouble("robot.xp.oreXpRate"));
        bufferPerLevel = Math.max(0, config.getDouble("robot.xp.bufferPerLevel"));
        toolEfficiencyPerLevel = Math.max(0, config.getDouble("robot.xp.toolEfficiencyPerLevel"));
        harvestSpeedBoostPerLevel = Math.max(0, config.getDouble("robot.xp.harvestSpeedBoostPerLevel"));

        // robot.delays
        turnDelay = Math.max(0.05, config.getDouble("robot.delays.turn") - 0.06);
        moveDelay = Math.max(0.05, config.getDouble("robot.delays.move") - 0.06);
        swingDelay = Math.max(0, config.getDouble("robot.delays.swing") - 0.06);
        useDelay = Math.max(0, config.getDouble("robot.delays.use") - 0.06);
        placeDelay = Math.max(0, config.getDouble("robot.delays.place") - 0.06);
        dropDelay = Math.max(0, config.getDouble("robot.delays.drop") - 0.06);
        suckDelay = Math.max(0, config.getDouble("robot.delays.suck") - 0.06);
        harvestRatio = Math.max(0, config.getDouble("robot.delays.harvestRatio"));

        // power
        ignorePower = config.getBoolean("power.ignorePower");
        tickFrequency = Math.max(1, config.getDouble("power.tickFrequency"));
        chargeRateExternal = config.getDouble("power.chargerChargeRate");
        chargeRateTablet = config.getDouble("power.chargerChargeRateTablet");
        generatorEfficiency = config.getDouble("power.generatorEfficiency");
        solarGeneratorEfficiency = config.getDouble("power.solarGeneratorEfficiency");
        assemblerTickAmount = Math.max(1, config.getDouble("power.assemblerTickAmount"));
        disassemblerTickAmount = Math.max(1, config.getDouble("power.disassemblerTickAmount"));
        printerTickAmount = Math.max(1, config.getDouble("power.printerTickAmount"));

        // power.carpetedCapacitors
        sheepPower = Math.max(0, config.getDouble("power.carpetedCapacitors.sheepPower"));
        ocelotPower = Math.max(0, config.getDouble("power.carpetedCapacitors.ocelotPower"));
        carpetDamageChance = Math.clamp(config.getDouble("power.carpetedCapacitors.damageChance"), 0, 1.0);

        // power.buffer
        bufferCapacitor = Math.max(0, config.getDouble("power.buffer.capacitor"));
        bufferCapacitorAdjacencyBonus = Math.max(0, config.getDouble("power.buffer.capacitorAdjacencyBonus"));
        bufferComputer = Math.max(0, config.getDouble("power.buffer.computer"));
        bufferRobot = Math.max(0, config.getDouble("power.buffer.robot"));
        bufferConverter = Math.max(0, config.getDouble("power.buffer.converter"));
        bufferDistributor = Math.max(0, config.getDouble("power.buffer.distributor"));
        {
            java.util.List<Double> batteryList = config.getDoubleList("power.buffer.batteryUpgrades");
            if (batteryList.size() >= 3) {
                bufferCapacitorUpgrades = new double[]{batteryList.get(0), batteryList.get(1), batteryList.get(2)};
            } else {
                LOGGER.warn("Bad number of battery upgrade buffer sizes, ignoring.");
                bufferCapacitorUpgrades = new double[]{10000.0, 15000.0, 20000.0};
            }
        }
        bufferTablet = Math.max(0, config.getDouble("power.buffer.tablet"));
        bufferAccessPoint = Math.max(0, config.getDouble("power.buffer.accessPoint"));
        bufferDrone = Math.max(0, config.getDouble("power.buffer.drone"));
        bufferMicrocontroller = Math.max(0, config.getDouble("power.buffer.mcu"));
        bufferHoverBoots = Math.max(1, config.getDouble("power.buffer.hoverBoots"));
        bufferNanomachines = Math.max(0, config.getDouble("power.buffer.nanomachines"));

        // power.cost
        computerCost = Math.max(0, config.getDouble("power.cost.computer"));
        microcontrollerCost = Math.max(0, config.getDouble("power.cost.microcontroller"));
        robotCost = Math.max(0, config.getDouble("power.cost.robot"));
        droneCost = Math.max(0, config.getDouble("power.cost.drone"));
        sleepCostFactor = Math.max(0, config.getDouble("power.cost.sleepFactor"));
        screenCost = Math.max(0, config.getDouble("power.cost.screen"));
        hologramCost = Math.max(0, config.getDouble("power.cost.hologram"));
        hddReadCost = Math.max(0, config.getDouble("power.cost.hddRead")) / 1024;
        hddWriteCost = Math.max(0, config.getDouble("power.cost.hddWrite")) / 1024;
        gpuSetCost = Math.max(0, config.getDouble("power.cost.gpuSet")) / basicScreenPixels();
        gpuFillCost = Math.max(0, config.getDouble("power.cost.gpuFill")) / basicScreenPixels();
        gpuClearCost = Math.max(0, config.getDouble("power.cost.gpuClear")) / basicScreenPixels();
        gpuCopyCost = Math.max(0, config.getDouble("power.cost.gpuCopy")) / basicScreenPixels();
        robotTurnCost = Math.max(0, config.getDouble("power.cost.robotTurn"));
        robotMoveCost = Math.max(0, config.getDouble("power.cost.robotMove"));
        robotExhaustionCost = Math.max(0, config.getDouble("power.cost.robotExhaustion"));
        {
            java.util.List<Double> wirelessList = config.getDoubleList("power.cost.wirelessCostPerRange");
            if (wirelessList.size() >= 2) {
                wirelessCostPerRange = new double[]{Math.max(0, wirelessList.get(0)), Math.max(0, wirelessList.get(1))};
            } else {
                LOGGER.warn("Bad number of wireless card energy costs, ignoring.");
                wirelessCostPerRange = new double[]{0.05, 0.05};
            }
        }
        geolyzerScanCost = Math.max(0, config.getDouble("power.cost.geolyzerScan"));
        robotBaseCost = Math.max(0, config.getDouble("power.cost.robotAssemblyBase"));
        robotComplexityCost = Math.max(0, config.getDouble("power.cost.robotAssemblyComplexity"));
        microcontrollerBaseCost = Math.max(0, config.getDouble("power.cost.microcontrollerAssemblyBase"));
        microcontrollerComplexityCost = Math.max(0, config.getDouble("power.cost.microcontrollerAssemblyComplexity"));
        tabletBaseCost = Math.max(0, config.getDouble("power.cost.tabletAssemblyBase"));
        tabletComplexityCost = Math.max(0, config.getDouble("power.cost.tabletAssemblyComplexity"));
        droneBaseCost = Math.max(0, config.getDouble("power.cost.droneAssemblyBase"));
        droneComplexityCost = Math.max(0, config.getDouble("power.cost.droneAssemblyComplexity"));
        disassemblerItemCost = Math.max(0, config.getDouble("power.cost.disassemblerPerItem"));
        chunkloaderCost = Math.max(0, config.getDouble("power.cost.chunkloaderCost"));
        pistonCost = Math.max(0, config.getDouble("power.cost.pistonPush"));
        eepromWriteCost = Math.max(0, config.getDouble("power.cost.eepromWrite"));
        printCost = Math.max(0, config.getDouble("power.cost.printerModel"));
        hoverBootJump = Math.max(0, config.getDouble("power.cost.hoverBootJump"));
        hoverBootAbsorb = Math.max(0, config.getDouble("power.cost.hoverBootAbsorb"));
        hoverBootMove = Math.max(0, config.getDouble("power.cost.hoverBootMove"));
        dataCardTrivial = Math.max(0, config.getDouble("power.cost.dataCardTrivial"));
        dataCardTrivialByte = Math.max(0, config.getDouble("power.cost.dataCardTrivialByte"));
        dataCardSimple = Math.max(0, config.getDouble("power.cost.dataCardSimple"));
        dataCardSimpleByte = Math.max(0, config.getDouble("power.cost.dataCardSimpleByte"));
        dataCardComplex = Math.max(0, config.getDouble("power.cost.dataCardComplex"));
        dataCardComplexByte = Math.max(0, config.getDouble("power.cost.dataCardComplexByte"));
        dataCardAsymmetric = Math.max(0, config.getDouble("power.cost.dataCardAsymmetric"));
        transposerCost = Math.max(0, config.getDouble("power.cost.transposer"));
        nanomachineCost = Math.max(0, config.getDouble("power.cost.nanomachineInput"));
        nanomachineReconfigureCost = Math.max(0, config.getDouble("power.cost.nanomachinesReconfigure"));
        mfuCost = Math.max(0, config.getDouble("power.cost.mfuRelay"));

        // power.rate
        accessPointRate = Math.max(0, config.getDouble("power.rate.accessPoint"));
        assemblerRate = Math.max(0, config.getDouble("power.rate.assembler"));
        {
            java.util.List<Double> caseRateList = config.getDoubleList("power.rate.case");
            if (caseRateList.size() >= 3) {
                caseRate = new double[]{caseRateList.get(0), caseRateList.get(1), caseRateList.get(2), 9001.0};
            } else {
                LOGGER.warn("Bad number of computer case conversion rates, ignoring.");
                caseRate = new double[]{5.0, 10.0, 20.0, 9001.0};
            }
        }
        chargerRate = Math.max(0, config.getDouble("power.rate.charger"));
        disassemblerRate = Math.max(0, config.getDouble("power.rate.disassembler"));
        powerConverterRate = Math.max(0, config.getDouble("power.rate.powerConverter"));
        serverRackRate = Math.max(0, config.getDouble("power.rate.serverRack"));
        capacitorRate = Math.max(0, config.getDouble("power.rate.capacitor"));
        powerDistributorRate = Math.max(0, config.getDouble("power.rate.powerDistributor"));
        relayRate = Math.max(0, config.getDouble("power.rate.relay"));

        // filesystem
        fileCost = Math.max(0, config.getInt("filesystem.fileCost"));
        bufferChanges = config.getBoolean("filesystem.bufferChanges");
        {
            java.util.List<Integer> hddList = config.getIntList("filesystem.hddSizes");
            if (hddList.size() >= 3) {
                hddSizes = new int[]{hddList.get(0), hddList.get(1), hddList.get(2)};
            } else {
                LOGGER.warn("Bad number of HDD sizes, ignoring.");
                hddSizes = new int[]{1024, 2048, 4096};
            }
        }
        {
            java.util.List<Integer> platterList = config.getIntList("filesystem.hddPlatterCounts");
            if (platterList.size() >= 3) {
                hddPlatterCounts = new int[]{platterList.get(0), platterList.get(1), platterList.get(2)};
            } else {
                LOGGER.warn("Bad number of HDD platter counts, ignoring.");
                hddPlatterCounts = new int[]{2, 4, 6};
            }
        }
        floppySize = Math.max(0, config.getInt("filesystem.floppySize"));
        tmpSize = Math.max(0, config.getInt("filesystem.tmpSize"));
        maxHandles = Math.max(0, config.getInt("filesystem.maxHandles"));
        maxReadBuffer = Math.max(0, config.getInt("filesystem.maxReadBuffer"));
        sectorSeekThreshold = config.getInt("filesystem.sectorSeekThreshold");
        sectorSeekTime = config.getDouble("filesystem.sectorSeekTime");

        // internet
        httpEnabled = config.getBoolean("internet.enableHttp");
        httpHeadersEnabled = config.getBoolean("internet.enableHttpHeaders");
        tcpEnabled = config.getBoolean("internet.enableTcp");
        {
            java.util.List<String> filterList = config.getStringList("internet.filteringRules");
            java.util.List<InternetFilteringRule> rules = new ArrayList<>();
            boolean observed = true;
            for (String p : filterList) {
                if ("removeme".equals(p)) {
                    observed = false;
                } else {
                    rules.add(new InternetFilteringRule(p));
                }
            }
            internetFilteringRules = rules.toArray(new InternetFilteringRule[0]);
            internetFilteringRulesObserved = observed;
        }
        httpTimeout = Math.max(0, config.getInt("internet.requestTimeout")) * 1000;
        maxConnections = Math.max(0, config.getInt("internet.maxTcpConnections"));
        internetThreads = Math.max(1, config.getInt("internet.threads"));
        httpUserAgent = config.getString("internet.httpUserAgent");

        // switch
        switchDefaultMaxQueueSize = Math.max(1, config.getInt("switch.defaultMaxQueueSize"));
        switchQueueSizeUpgrade = Math.max(0, config.getInt("switch.queueSizeUpgrade"));
        switchDefaultRelayDelay = Math.max(1, config.getInt("switch.defaultRelayDelay"));
        switchRelayDelayUpgrade = Math.max(0, config.getDouble("switch.relayDelayUpgrade"));
        switchDefaultRelayAmount = Math.max(1, config.getInt("switch.defaultRelayAmount"));
        switchRelayAmountUpgrade = Math.max(0, config.getInt("switch.relayAmountUpgrade"));

        // hologram
        {
            java.util.List<Double> scaleList = config.getDoubleList("hologram.maxScale");
            if (scaleList.size() >= 2) {
                hologramMaxScaleByTier = new double[]{Math.max(1.0, scaleList.get(0)), Math.max(1.0, scaleList.get(1))};
            } else {
                LOGGER.warn("Bad number of hologram max scales, ignoring.");
                hologramMaxScaleByTier = new double[]{3.0, 4.0};
            }
        }
        {
            java.util.List<Double> transList = config.getDoubleList("hologram.maxTranslation");
            if (transList.size() >= 2) {
                hologramMaxTranslationByTier = new double[]{Math.max(0.0, transList.get(0)), Math.max(0.0, transList.get(1))};
            } else {
                LOGGER.warn("Bad number of hologram max translations, ignoring.");
                hologramMaxTranslationByTier = new double[]{0.25, 0.5};
            }
        }
        hologramSetRawDelay = Math.max(0, config.getDouble("hologram.setRawDelay"));
        hologramLight = config.getBoolean("hologram.emitLight");

        // misc
        maxScreenWidth = Math.max(1, config.getInt("misc.maxScreenWidth"));
        maxScreenHeight = Math.max(1, config.getInt("misc.maxScreenHeight"));
        inputUsername = config.getBoolean("misc.inputUsername");
        initialNetworkPacketTTL = Math.max(5, config.getInt("misc.initialNetworkPacketTTL"));
        maxNetworkPacketSize = Math.max(0, config.getInt("misc.maxNetworkPacketSize"));
        maxNetworkPacketParts = Math.max(4, config.getInt("misc.maxNetworkPacketParts"));
        {
            java.util.List<Integer> portsList = config.getIntList("misc.maxOpenPorts");
            if (portsList.size() >= 3) {
                maxOpenPorts = new int[]{Math.max(0, portsList.get(0)), Math.max(0, portsList.get(1)), Math.max(0, portsList.get(2))};
            } else {
                LOGGER.warn("Bad number of max open ports, ignoring.");
                maxOpenPorts = new int[]{16, 1, 16};
            }
        }
        {
            java.util.List<Double> rangeList = config.getDoubleList("misc.maxWirelessRange");
            if (rangeList.size() >= 2) {
                maxWirelessRange = new double[]{Math.max(0.0, rangeList.get(0)), Math.max(0.0, rangeList.get(1))};
            } else {
                LOGGER.warn("Bad number of wireless card max ranges, ignoring.");
                maxWirelessRange = new double[]{16.0, 400.0};
            }
        }
        lootProbability = config.getInt("misc.lootProbability");
        lootRecrafting = config.getBoolean("misc.lootRecrafting");
        geolyzerRange = config.getInt("misc.geolyzerRange");
        geolyzerNoise = Math.max(0, (float) config.getDouble("misc.geolyzerNoise"));
        disassembleAllTheThings = config.getBoolean("misc.disassembleAllTheThings");
        disassemblerBreakChance = Math.clamp(config.getDouble("misc.disassemblerBreakChance"), 0, 1);
        disassemblerInputBlacklist = config.getStringList("misc.disassemblerInputBlacklist");
        hideOwnPet = config.getBoolean("misc.hideOwnSpecial");
        allowItemStackInspection = config.getBoolean("misc.allowItemStackInspection");
        presentChance = Math.clamp(config.getDouble("misc.presentChance"), 0, 1);
        assemblerBlacklist = config.getStringList("misc.assemblerBlacklist");
        threadPriority = config.getInt("misc.threadPriority");
        giveManualToNewPlayers = config.getBoolean("misc.giveManualToNewPlayers");
        dataCardSoftLimit = Math.max(0, config.getInt("misc.dataCardSoftLimit"));
        dataCardHardLimit = Math.max(0, config.getInt("misc.dataCardHardLimit"));
        dataCardTimeout = Math.max(0, config.getDouble("misc.dataCardTimeout"));
        redstoneDelay = Math.max(0, config.getDouble("misc.redstoneDelay"));
        tradingRange = Math.max(0, config.getDouble("misc.tradingRange"));
        mfuRange = Math.clamp(config.getInt("misc.mfuRange"), 0, 128);
        maxClipboard = Math.max(0, config.getInt("misc.maxClipboard"));

        // nanomachines
        nanomachineTriggerQuota = Math.max(0, config.getDouble("nanomachines.triggerQuota"));
        nanomachineConnectorQuota = Math.max(0, config.getDouble("nanomachines.connectorQuota"));
        nanomachineMaxInputs = Math.max(1, config.getInt("nanomachines.maxInputs"));
        nanomachineMaxOutputs = Math.max(1, config.getInt("nanomachines.maxOutputs"));
        nanomachinesSafeInputsActive = Math.max(0, config.getInt("nanomachines.safeInputsActive"));
        nanomachinesMaxInputsActive = Math.max(0, config.getInt("nanomachines.maxInputsActive"));
        nanomachinesCommandDelay = Math.max(0, config.getDouble("nanomachines.commandDelay"));
        nanomachinesCommandRange = Math.max(0, config.getDouble("nanomachines.commandRange"));
        nanomachineMagnetRange = Math.max(0, config.getDouble("nanomachines.magnetRange"));
        nanomachineDisintegrationRange = Math.max(0, config.getInt("nanomachines.disintegrationRange"));
        nanomachinePotionWhitelist = config.getAnyRefList("nanomachines.potionWhitelist");
        nanomachinesHungryDamage = Math.max(0, (float) config.getDouble("nanomachines.hungryDamage"));
        nanomachinesHungryEnergyRestored = Math.max(0, config.getDouble("nanomachines.hungryEnergyRestored"));

        // printer
        maxPrintComplexity = config.getInt("printer.maxShapes");
        printRecycleRate = config.getDouble("printer.recycleRate");
        chameliumEdible = config.getBoolean("printer.chameliumEdible");
        maxPrintLightLevel = Math.clamp(config.getInt("printer.maxBaseLightLevel"), 0, 15);
        printCustomRedstone = Math.max(0, config.getInt("printer.customRedstoneCost"));
        printMaterialValue = Math.max(0, config.getInt("printer.materialValue"));
        printInkValue = Math.max(0, config.getInt("printer.inkValue"));
        printsHaveOpacity = config.getBoolean("printer.printsHaveOpacity");
        noclipMultiplier = Math.max(0, config.getDouble("printer.noclipMultiplier"));

        // chunkloader
        chunkloadDimensionBlacklist = getIntList(config, "chunkloader.dimBlacklist", null);
        chunkloadDimensionWhitelist = getIntList(config, "chunkloader.dimWhitelist", null);

        // integration
        modBlacklist = config.getStringList("integration.modBlacklist");
        peripheralBlacklist = config.getStringList("integration.peripheralBlacklist");
        fakePlayerUuid = config.getString("integration.fakePlayerUuid");
        fakePlayerName = config.getString("integration.fakePlayerName");
        fakePlayerProfile = new GameProfile(UUID.fromString(fakePlayerUuid), fakePlayerName);

        // integration.vanilla
        enableInventoryDriver = config.getBoolean("integration.vanilla.enableInventoryDriver");
        enableTankDriver = config.getBoolean("integration.vanilla.enableTankDriver");
        enableCommandBlockDriver = config.getBoolean("integration.vanilla.enableCommandBlockDriver");
        allowItemStackNBTTags = config.getBoolean("integration.vanilla.allowItemStackNBTTags");

        // debug
        logLuaCallbackErrors = config.getBoolean("debug.logCallbackErrors");
        forceLuaJ = config.getBoolean("debug.forceLuaJ");
        allowUserdata = !config.getBoolean("debug.disableUserdata");
        allowPersistence = !config.getBoolean("debug.disablePersistence");
        limitMemory = !config.getBoolean("debug.disableMemoryLimit");
        forceCaseInsensitive = config.getBoolean("debug.forceCaseInsensitiveFS");
        logFullLibLoadErrors = config.getBoolean("debug.logFullNativeLibLoadErrors");
        forceNativeLibPlatform = config.getString("debug.forceNativeLibPlatform");
        forceNativeLibPathFirst = config.getString("debug.forceNativeLibPathFirst");
        logHexFontErrors = config.getBoolean("debug.logHexFontErrors");
        alwaysTryNative = config.getBoolean("debug.alwaysTryNative");
        debugPersistence = config.getBoolean("debug.verbosePersistenceErrors");
        nativeInTmpDir = config.getBoolean("debug.nativeInTmpDir");
        periodicallyForceLightUpdate = config.getBoolean("debug.periodicallyForceLightUpdate");
        insertIdsInConverters = config.getBoolean("debug.insertIdsInConverters");

        {
            Object unwrapped;
            try {
                unwrapped = config.getValue("debug.debugCardAccess").unwrapped();
            } catch (Exception e) {
                unwrapped = "deny";
            }
            if ("true".equals(unwrapped) || "allow".equals(unwrapped) || Boolean.TRUE.equals(unwrapped)) {
                debugCardAccess = DebugCardAccess.Allowed;
            } else if ("false".equals(unwrapped) || "deny".equals(unwrapped) || Boolean.FALSE.equals(unwrapped)) {
                debugCardAccess = DebugCardAccess.Forbidden;
            } else if ("whitelist".equals(unwrapped)) {
                File wlFile = new File(gameDir, "config" + File.separator + "opencomputers" + File.separator + "debug_card_whitelist.txt");
                debugCardAccess = new DebugCardAccess.Whitelist(wlFile);
            } else {
                LOGGER.warn("Unknown debug card access type, falling back to `deny`. Allowed values: `allow`, `deny`, `whitelist`.");
                debugCardAccess = DebugCardAccess.Forbidden;
            }
        }

        registerLuaJArchitecture = config.getBoolean("debug.registerLuaJArchitecture");
        disableLocaleChanging = config.getBoolean("debug.disableLocaleChanging");

        // >= 1.7.4
        maxSignalQueueSize = Math.max(256, config.hasPath("computer.maxSignalQueueSize") ? config.getInt("computer.maxSignalQueueSize") : 256);

        // >= 1.7.6
        {
            java.util.List<Double> vramList = config.getDoubleList("gpu.vramSizes");
            if (vramList.size() >= 3) {
                vramSizes = new double[]{vramList.get(0), vramList.get(1), vramList.get(2)};
            } else {
                LOGGER.warn("Bad number of VRAM sizes (expected 3), ignoring.");
                vramSizes = new double[]{1, 2, 3};
            }
        }
        bitbltCost = config.hasPath("gpu.bitbltCost") ? config.getDouble("gpu.bitbltCost") : 0.5;

        // >= 1.8.2
        diskActivitySoundDelay = Math.max(-1, config.getInt("misc.diskActivitySoundDelay"));
        maxNetworkClientPacketDistance = Math.max(0, config.getDouble("misc.maxNetworkClientPacketDistance"));
        maxNetworkClientEffectPacketDistance = Math.max(0, config.getDouble("misc.maxNetworkClientEffectPacketDistance"));
        maxNetworkClientSoundPacketDistance = Math.max(0, config.getDouble("misc.maxNetworkClientSoundPacketDistance"));
    }

    public static int basicScreenPixels() {
        return screenResolutionsByTier[0][0] * screenResolutionsByTier[0][1];
    }

    public double ratioAppliedEnergistics2() {
        return (config.hasPath("power.value.AppliedEnergistics2") ? config.getDouble("power.value.AppliedEnergistics2") : 0.5) / 1000.0;
    }

    public double ratioRedstoneFlux() {
        return (config.hasPath("power.value.RedstoneFlux") ? config.getDouble("power.value.RedstoneFlux") : 1.0) / 1000.0;
    }

    public static OCSettings get() {
        return settings;
    }

    public static void load(File file, File gameDir, String modVersion) {
        String eol = "\n";
        Config defaults;
        try {
            InputStream in = OCSettings.class.getResourceAsStream("/application.conf");
            if (in == null) throw new RuntimeException("Failed to load default config: resource not found");
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(eol);
            }
            sb.append(eol);
            reader.close();
            defaults = ConfigFactory.parseString(sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to load default config", e);
        }
        Config config;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(eol);
            }
            reader.close();
            String plain = sb.toString();
            config = patchConfig(ConfigFactory.parseString(plain), defaults, modVersion).withFallback(defaults);
            settings = new OCSettings(config.getConfig("opencomputers"), gameDir);
        } catch (Exception e) {
            if (file.exists()) {
                throw new RuntimeException("Error parsing configuration file. To restore defaults, delete '" + file.getName() + "' and restart the game.", e);
            }
            settings = new OCSettings(defaults.getConfig("opencomputers"), gameDir);
            config = defaults;
        }
        for (String key : forbiddenConfigLists) {
            if (config.hasPath(prefix + key)) {
                if (!config.getStringList(prefix + key).isEmpty()) {
                    throw new RuntimeException("Error parsing configuration file: removed configuration option '" + key + "' is not empty. This option should no longer be used.");
                }
            }
        }
        try {
            ConfigRenderOptions renderSettings = ConfigRenderOptions.defaults().setJson(false).setOriginComments(false);
            String nl = System.lineSeparator();
            String rendered = config.root().render(renderSettings);
            rendered = rendered.replace("\r", "");
            rendered = java.util.regex.Pattern.compile("^(\\s+)", java.util.regex.Pattern.MULTILINE).matcher(rendered).replaceAll(m -> {
                String spaces = m.group(1);
                return spaces.replace("  ", " ");
            });
            String[] lines = rendered.split("\n");
            StringBuilder out = new StringBuilder();
            for (String l : lines) {
                String trimmed = java.util.regex.Pattern.compile("^(\\s*)").matcher(l).replaceAll(m -> m.group(1).replace("  ", " "));
                if (!trimmed.isEmpty()) {
                    out.append(trimmed).append(nl);
                }
            }
            String result = out.toString();
            String nleRegex = nl.replace("\r", "\\r").replace("\n", "\\n");
            result = result.replaceAll("((\\s*#.*" + nleRegex + ")(\\s*[^#\\s].*" + nleRegex + ")+)", "$1" + nl);
            try (PrintWriter pw = new PrintWriter(file)) {
                pw.write(result);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed saving config.", e);
        }
    }

    private static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        for (int i = 0; i < Math.max(parts1.length, parts2.length); i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return p1 - p2;
        }
        return 0;
    }

    private static Config patchConfig(Config config, Config defaults, String modVersion) {
        if (modVersion == null) return config;

        String cfgVersion = config.hasPath(prefix + "version") ? config.getString(prefix + "version") : "0.0.0";

        var patched = config;
        if (!cfgVersion.equals(modVersion)) {
            LOGGER.info("Updating config from version '{}' to '{}'.", cfgVersion, defaults.getString(prefix + "version"));
            patched = patched.withValue(prefix + "version", defaults.getValue(prefix + "version"));

            for (var entry : configPatches) {
                String versionBound = entry.getKey();
                String[] paths = entry.getValue();
                boolean contains = false;
                try {
                    contains = compareVersions(cfgVersion, versionBound) < 0;
                } catch (Exception e) {
                    // ignore
                }
                if (contains) {
                    for (String path : paths) {
                        String fullPath = prefix + path;
                        LOGGER.info("=> Updating setting '{}'.", fullPath);
                        if (defaults.hasPath(fullPath)) {
                            patched = patched.withValue(fullPath, defaults.getValue(fullPath));
                        } else {
                            patched = patched.withoutPath(fullPath);
                        }
                    }
                }
            }

            try {
                if (compareVersions(cfgVersion, filteringRulesPatchVersion) < 0) {
                    LOGGER.info("=> Migrating Internet Card filtering rules. ");
                    Pattern cidrPattern = Pattern.compile("(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})/(\\d{1,2})");
                    java.util.List<String> httpHostWhitelist = patched.hasPath(prefix + "internet.whitelist") ? patched.getStringList(prefix + "internet.whitelist") : new ArrayList<>();
                    java.util.List<String> httpHostBlacklist = patched.hasPath(prefix + "internet.blacklist") ? patched.getStringList(prefix + "internet.blacklist") : new ArrayList<>();
                    java.util.List<String> internetFilteringRules = new ArrayList<>();

                    for (String blockedAddress : httpHostBlacklist) {
                        if (cidrPattern.matcher(blockedAddress).find()) {
                            internetFilteringRules.add("deny ip:" + blockedAddress);
                        } else {
                            internetFilteringRules.add("deny domain:" + blockedAddress);
                        }
                    }
                    for (String allowedAddress : httpHostWhitelist) {
                        if (cidrPattern.matcher(allowedAddress).find()) {
                            internetFilteringRules.add("allow ip:" + allowedAddress);
                        } else {
                            internetFilteringRules.add("allow domain:" + allowedAddress);
                        }
                    }
                    if (!httpHostWhitelist.isEmpty()) {
                        internetFilteringRules.add("deny all");
                    }
                    internetFilteringRules.addAll(defaults.getStringList(prefix + "internet.filteringRules"));

                    ConfigValue patchedRules = ConfigValueFactory.fromIterable(new ArrayList<>(internetFilteringRules));
                    try {
                        for (String key : Arrays.asList("internet.whitelist", "internet.blacklist")) {
                            if (patched.hasPath(prefix + key)) {
                                ConfigValue originalValue = patched.getValue(prefix + key);
                                ConfigValue deprecatedValue = ConfigValueFactory.fromIterable(new ArrayList<String>(), originalValue.origin().description());
                                patched = patched.withValue(prefix + key, deprecatedValue);
                            }
                        }
                        defaults.getValue(prefix + "internet.filteringRules").origin().comments();
                    } catch (Throwable t) {
                        // pass
                    }
                    patched = patched.withValue(prefix + "internet.filteringRules", patchedRules);
                }
            } catch (Exception e) {
                // pass
            }
        }
        return patched;
    }

    public static java.util.List<Integer> getIntList(Config config, String path, java.util.List<Integer> defaultVal) {
        if (config.hasPath(path)) return config.getIntList(path);
        if (defaultVal != null) return defaultVal;
        return new LinkedList<>();
    }

    public boolean internetFilteringRulesInvalid() {
        for (InternetFilteringRule rule : internetFilteringRules) {
            if (rule.invalid()) return true;
        }
        return false;
    }

    public boolean internetAccessConfigured() {
        return httpEnabled || tcpEnabled;
    }

    // DebugCardAccess

    public boolean internetAccessDenied() {
        return !internetAccessConfigured() || internetFilteringRulesInvalid();
    }

    public static class AccessContext {
        private final String player;
        private final String nonce;

        public AccessContext(String player, String nonce) {
            this.player = player;
            this.nonce = nonce;
        }

        public String player() {
            return player;
        }

        public String nonce() {
            return nonce;
        }
    }

    public abstract static class DebugCardAccess {
        public static final DebugCardAccess Forbidden = new DebugCardAccess() {
            @SuppressWarnings("SameReturnValue")
            @Override
            public String checkAccess(AccessContext ctx) {
                return "debug card is disabled";
            }
        };
        public static final DebugCardAccess Allowed = new DebugCardAccess() {
            @SuppressWarnings("SameReturnValue")
            @Override
            public String checkAccess(AccessContext ctx) {
                return null;
            }
        };

        public abstract String checkAccess(AccessContext ctx);

        public static class Whitelist extends DebugCardAccess {
            private final File noncesFile;
            private final java.util.Map<String, String> values = new java.util.HashMap<>();
            private final java.security.SecureRandom rng;

            public Whitelist(File noncesFile) {
                this.noncesFile = noncesFile;
                try {
                    this.rng = java.security.SecureRandom.getInstance("SHA1PRNG");
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                load();
            }

            public void save() {
                File noncesDir = noncesFile.getParentFile();
                if (!noncesDir.exists() && !noncesDir.mkdirs()) {
                    try {
                        throw new RuntimeException(new IOException("Cannot create nonces directory: " + noncesDir.getCanonicalPath()));
                    } catch (IOException e) {
                        LOGGER.error(e.getMessage(), e);
                        return;
                    }
                }
                try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(noncesFile), StandardCharsets.UTF_8), false)) {
                    for (java.util.Map.Entry<String, String> e : values.entrySet()) {
                        writer.println(e.getKey() + " " + e.getValue());
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to save nonces file", e);
                }
            }

            public void load() {
                values.clear();
                if (!noncesFile.exists()) return;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(noncesFile), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split(" ", 2);
                        if (parts.length == 2) {
                            values.put(parts[0], parts[1]);
                        }
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to load nonces file", e);
                }
            }

            private String generateNonce() {
                byte[] buf = new byte[16];
                rng.nextBytes(buf);
                return new String(Hex.encodeHex(buf, true));
            }

            public String nonce(String player) {
                return values.get(player.toLowerCase());
            }

            public boolean isWhitelisted(String player) {
                return values.containsKey(player.toLowerCase());
            }

            public java.util.Set<String> whitelist() {
                return values.keySet();
            }

            public void add(String player) {
                if (!values.containsKey(player.toLowerCase())) {
                    values.put(player.toLowerCase(), generateNonce());
                    save();
                }
            }

            public void remove(String player) {
                if (values.remove(player.toLowerCase()) != null) {
                    save();
                }
            }

            public void invalidate(String player) {
                if (values.containsKey(player.toLowerCase())) {
                    values.put(player.toLowerCase(), generateNonce());
                    save();
                }
            }

            @Override
            public String checkAccess(AccessContext ctx) {
                if (ctx != null) {
                    String x = values.get(ctx.player().toLowerCase());
                    if (x != null) {
                        if (x.equals(ctx.nonce())) return null;
                        else return "debug card is invalidated, please re-bind it to yourself";
                    }
                    return "you are not whitelisted to use debug card";
                }
                return "debug card is whitelisted, Shift+Click with it to bind card to yourself";
            }
        }
    }
}
