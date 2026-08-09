package li.cil.oc.fabric.integration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.integration.ModIDs;
import net.fabricmc.loader.api.FabricLoader;

@SuppressWarnings("unused")
public final class Mods {
    private static final List<ModBase> knownMods = new ArrayList<>();
    private static Consumer<String> logger = s -> {};

    public static final ModBase AppliedEnergistics2 = new SimpleMod(ModIDs.AppliedEnergistics2);
    public static final ModBase AppliedMekanistics = new SimpleMod(ModIDs.AppliedMekanistics);
    public static final ModBase ComputerCraft = new SimpleMod(ModIDs.ComputerCraft);
    public static final ModBase Create = new SimpleMod(ModIDs.Create);
    public static final ModBase Sable = new SimpleMod(ModIDs.Sable);
    public static final ModBase EnderIO = new SimpleMod(ModIDs.EnderIO);
    public static final ModBase EnderStorage = new SimpleMod(ModIDs.EnderStorage);
    public static final ModBase CBMultipart = new SimpleMod(ModIDs.CBMultipart);
    public static final ModBase Mekanism = new SimpleMod(ModIDs.Mekanism);
    public static final ModBase TIS3D = new SimpleMod(ModIDs.TIS3D);
    public static final ModBase TeamRebornEnergy = new SimpleMod(ModIDs.TeamRebornEnergy);
    public static final ModBase Minecraft = new SimpleMod(ModIDs.Minecraft);
    public static final ModBase OpenComputers = new SimpleMod(ModIDs.OpenComputers);
    public static final ModBase ProjectRedTransmission = new SimpleMod(ModIDs.ProjectRedTransmission);
    public static final ModBase RefinedStorage2 = new SimpleMod(ModIDs.RefinedStorage2);
    public static final ModBase RFTools = new SimpleMod(ModIDs.RFTools);
    public static final ModBase Jade = new SimpleMod(ModIDs.Jade);
    public static final ModBase TheOneProbe = new SimpleMod(ModIDs.TheOneProbe);
    public static final li.cil.oc.core.integration.ModProxy[] Proxies = new li.cil.oc.core.integration.ModProxy[]{
            proxyOrNull("li.cil.oc.fabric.integration.computercraft.ModComputerCraft"),
            proxyOrNull("li.cil.oc.fabric.integration.refinedstorage2.ModRefinedStorage2"),
            proxyOrNull("li.cil.oc.fabric.integration.sable.ModSable"),
            proxyOrNull("li.cil.oc.fabric.integration.jade.ModJade"),
            proxyOrNull("li.cil.oc.fabric.integration.tis3d.ModTIS3D"),
            proxyOrNull("li.cil.oc.fabric.integration.energy.ModTeamRebornEnergy"),
            new li.cil.oc.fabric.integration.vanilla.ModVanilla(),
            new li.cil.oc.fabric.integration.opencomputers.ModOpenComputers(),
    };
    private static final Set<li.cil.oc.core.integration.ModProxy> handlers = new HashSet<>();

    private Mods() {
    }

    public static void setLogger(Consumer<String> log) {
        logger = log;
    }

    public static List<ModBase> getAll() {
        return new ArrayList<>(knownMods);
    }

    public static void init() {
        for (var proxy : Proxies) {
            tryInit(proxy);
        }
    }

    public static li.cil.oc.core.integration.ModProxy proxyOrNull(String className) {
        try {
            return (li.cil.oc.core.integration.ModProxy) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    private static void tryInit(li.cil.oc.core.integration.ModProxy mod) {
        if (mod == null) return;
        var modInfo = mod.getMod();
        boolean isBlacklisted = OCSettings.get().modBlacklist.contains(modInfo.id());
        boolean alwaysEnabled = modInfo == Mods.Minecraft || modInfo == Mods.OpenComputers;
        if (!isBlacklisted && (alwaysEnabled || modInfo.isModAvailable()) && handlers.add(mod)) {
            logger.accept("Initializing mod integration for '" + modInfo.id() + "'.");
            try {
                mod.initialize();
            } catch (Throwable e) {
                logger.accept("Error initializing integration for '" + modInfo.id() + "': " + e.getMessage());
            }
        }
    }

    public abstract static class ModBase implements li.cil.oc.core.integration.Mod {
        protected ModBase() {
            knownMods.add(this);
        }

        @Override
        public abstract boolean isModAvailable();

        @Override
        public abstract String id();

        public String version() {
            var modContainer = FabricLoader.getInstance().getModContainer(id());
            return modContainer.map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse(null);
        }
    }

    public static class SimpleMod extends ModBase {
        private final String id;
        private final boolean isModAvailable_;

        public SimpleMod(String id) {
            this.id = id;
            this.isModAvailable_ = computeAvailability();
        }

        private boolean computeAvailability() {
            try {
                return FabricLoader.getInstance().isModLoaded(id);
            } catch (Throwable e) {
                return false;
            }
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isModAvailable() {
            return isModAvailable_;
        }
    }

}
