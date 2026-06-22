package li.cil.oc.neoforge.integration;

import li.cil.oc.core.impl.Settings;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("unused")
public final class Mods {
    private static final List<ModBase> knownMods = new ArrayList<>();

    public static final ModBase AppliedEnergistics2 = new SimpleMod(IDs.AppliedEnergistics2, "@[rv1,)");
    public static final ModBase AppliedMekanistics = new SimpleMod(IDs.AppliedMekanistics);
    public static final ModBase ComputerCraft = new SimpleMod(IDs.ComputerCraft, "@[1.73,)");
    public static final ModBase Create = new SimpleMod(IDs.Create);
    public static final ModBase EnderIO = new SimpleMod(IDs.EnderIO, "@[2.2,)");
    public static final ModBase EnderStorage = new SimpleMod(IDs.EnderStorage);
    public static final ModBase CBMultipart = new SimpleMod(IDs.CBMultipart);
    public static final ModBase Mekanism = new SimpleMod(IDs.Mekanism);
    public static final ModBase Minecraft = new SimpleMod(IDs.Minecraft);
    public static final ModBase OpenComputers = new SimpleMod(IDs.OpenComputers);
    public static final ModBase PortalGun = new SimpleMod(IDs.PortalGun);
    public static final ModBase ProjectRedTransmission = new SimpleMod(IDs.ProjectRedTransmission);
    public static final ModBase Railcraft = new SimpleMod(IDs.Railcraft);
    public static final ModBase RFTools = new SimpleMod(IDs.RFTools);
    public static final ModBase Jade = new SimpleMod(IDs.Jade);
    public static final ModBase TheOneProbe = new SimpleMod(IDs.TheOneProbe);
    public static final ModProxy[] Proxies = new ModProxy[]{
            proxyOrNull("li.cil.oc.neoforge.integration.appeng.ModAppEng"),
            proxyOrNull("li.cil.oc.neoforge.integration.appmek.ModAppliedMekanistics"),
            proxyOrNull("li.cil.oc.neoforge.integration.computercraft.ModComputerCraft"),
            proxyOrNull("li.cil.oc.neoforge.integration.enderio.ModEnderIO"),
            proxyOrNull("li.cil.oc.neoforge.integration.enderstorage.ModEnderStorage"),
            proxyOrNull("li.cil.oc.neoforge.integration.cbmultipart.ModCBMultipart"),
            proxyOrNull("li.cil.oc.neoforge.integration.create.ModCreate"),
            proxyOrNull("li.cil.oc.neoforge.integration.rftools.ModRFTools"),
            proxyOrNull("li.cil.oc.neoforge.integration.mekanism.ModMekanism"),
            proxyOrNull("li.cil.oc.neoforge.integration.projectred.ModProjectRed"),
            proxyOrNull("li.cil.oc.neoforge.integration.railcraft.ModRailcraft"),
            new li.cil.oc.neoforge.integration.vanilla.ModVanilla(),
            new li.cil.oc.neoforge.integration.neoforge.ModNeoForge(),
            proxyOrNull("li.cil.oc.neoforge.integration.jade.ModJade"),
            proxyOrNull("li.cil.oc.neoforge.integration.top.ModTop"),
            proxyOrNull("li.cil.oc.neoforge.integration.computercraft.ModComputerCraft"),
            new li.cil.oc.neoforge.integration.opencomputers.ModOpenComputers(),
    };
    private static final Set<ModProxy> handlers = new HashSet<>();

    private Mods() {
    }

    public static List<ModBase> getAll() {
        return new ArrayList<>(knownMods);
    }

    public static void init() {
        for (ModProxy proxy : Proxies) {
            tryInit(proxy);
        }
    }

    private static ModProxy proxyOrNull(String className) {
        try {
            return (ModProxy) Class.forName(className).getDeclaredConstructor().newInstance();
        } catch (Throwable e) {
            return null;
        }
    }

    private static void tryInit(ModProxy mod) {
        if (mod == null) return;
        boolean isBlacklisted = Settings.get().modBlacklist.contains(mod.getMod().id());
        boolean alwaysEnabled = mod.getMod() == null || mod.getMod() == Mods.Minecraft || mod.getMod() == Mods.OpenComputers;
        if (!isBlacklisted && (alwaysEnabled || mod.getMod().isModAvailable()) && handlers.add(mod)) {
            li.cil.oc.neoforge.OpenComputers.log().debug("Initializing mod integration for '{}'.", mod.getMod().id());
            try {
                mod.initialize();
            } catch (Throwable e) {
                li.cil.oc.neoforge.OpenComputers.log().warn("Error initializing integration for '{}'", mod.getMod().id(), e);
            }
        }
    }

    public static final class IDs {
        public static final String AppliedEnergistics2 = "ae2";
        public static final String AppliedMekanistics = "appmek";
        public static final String ComputerCraft = "computercraft";
        public static final String Create = "create";
        public static final String EnderIO = "enderio";
        public static final String EnderStorage = "EnderStorage";
        public static final String CBMultipart = "cb_multipart";
        public static final String Mekanism = "Mekanism";
        public static final String Minecraft = "Minecraft";
        public static final String OpenComputers = "OpenComputers";
        public static final String PortalGun = "PortalGun";
        public static final String ProjectRedTransmission = "projectred_transmission";
        public static final String Railcraft = "railcraft";
        public static final String RFTools = "rftoolsutility";
        public static final String Jade = "jade";
        public static final String TheOneProbe = "theoneprobe";

        private IDs() {
        }
    }

    public abstract static class ModBase implements Mod {
        protected ModBase() {
            knownMods.add(this);
        }

        public abstract boolean isModAvailable();

        public abstract String id();

        public boolean isAvailable() {
            return isModAvailable();
        }

        public Object container() {
            return ModList.get().getModContainerById(id()).orElse(null);
        }

        public String version() {
            var c = container();
            if (c instanceof ModContainer mc) {
                IModInfo modInfo = mc.getModInfo();
                return modInfo.getVersion().toString();
            }
            return null;
        }
    }

    public static class SimpleMod extends ModBase {
        private final String id;
        private final String version;
        private final boolean isModAvailable_;

        public SimpleMod(String id) {
            this(id, "");
        }

        public SimpleMod(String id, String version) {
            this.id = id;
            this.version = version;
            this.isModAvailable_ = computeAvailability();
        }

        private boolean computeAvailability() {
            try {
                if (version.isEmpty()) {
                    return ModList.get().isLoaded(id);
                }
                return ModList.get().isLoaded(id);
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
