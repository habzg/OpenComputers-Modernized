package li.cil.oc.neoforge.common;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.machine.luac.LuaStateFactory;
import li.cil.oc.core.impl.server.machine.luac.NativeLua52Architecture;
import li.cil.oc.core.impl.server.machine.luac.NativeLua53Architecture;
import li.cil.oc.core.impl.server.machine.luac.NativeLua54Architecture;
import li.cil.oc.core.impl.server.machine.luaj.LuaJLuaArchitecture;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.init.Blocks;
import li.cil.oc.neoforge.common.init.Items;
import li.cil.oc.neoforge.integration.Mods;
import net.neoforged.fml.ModList;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.loading.FMLPaths;

public class Proxy {
    public void preInit(FMLCommonSetupEvent e) {
        OCSettings.load(
                FMLPaths.CONFIGDIR.get().resolve("OpenComputers.cfg").toFile(),
                FMLPaths.CONFIGDIR.get().toFile(),
                ModList.get().getModContainerById("opencomputers")
                        .orElseThrow(() -> new RuntimeException("OpenComputers mod container not found"))
                        .getModInfo().getVersion().toString()
        );

        OpenComputers.log().debug("Initializing blocks and items.");

        Blocks.init();

        li.cil.oc.api.API.network = li.cil.oc.core.impl.server.network.Network.INSTANCE;

        Items.init();

        OpenComputers.log().info("Initializing OpenComputers API.");

        li.cil.oc.api.CreativeTab.instance = li.cil.oc.neoforge.CreativeTab.TAB.get();
        li.cil.oc.api.API.driver = li.cil.oc.core.impl.server.driver.Registry.INSTANCE;
        li.cil.oc.api.API.fileSystem = li.cil.oc.core.impl.server.fs.FileSystem.INSTANCE;
        li.cil.oc.api.API.items = Items.INSTANCE;
        li.cil.oc.api.API.machine = new li.cil.oc.neoforge.server.machine.Machine.API();
        li.cil.oc.api.API.nanomachines = new li.cil.oc.core.impl.common.nanomachines.Nanomachines();

        li.cil.oc.api.API.config = OCSettings.get().config;

        OpenComputers.log().info("Initializing loot disks.");
        Loot.init();

        if (LuaStateFactory.isAvailable()) {
            if (LuaStateFactory.include53()) {
                li.cil.oc.api.Machine.add(NativeLua53Architecture.class);
            }
            if (LuaStateFactory.include54()) {
                li.cil.oc.api.Machine.add(NativeLua54Architecture.class);
            }
            if (LuaStateFactory.include52()) {
                li.cil.oc.api.Machine.add(NativeLua52Architecture.class);
            }
        }
        if (LuaStateFactory.includeLuaJ()) {
            li.cil.oc.api.Machine.add(LuaJLuaArchitecture.class);
        }

        li.cil.oc.api.Machine.LuaArchitecture =
                OCSettings.get().forceLuaJ ? LuaJLuaArchitecture.class : li.cil.oc.api.Machine.architectures().iterator().next();
    }

    public void init(FMLCommonSetupEvent e) {
        li.cil.oc.neoforge.OpenComputers.log().debug("Initializing mod integration.");
        li.cil.oc.core.integration.ModIDs.setModResolver(net.neoforged.fml.ModList.get()::isLoaded);
        li.cil.oc.neoforge.integration.Mods.setLogger(msg -> li.cil.oc.neoforge.OpenComputers.log().info(msg));
        Mods.init();
        li.cil.oc.api.API.isPowerEnabled = !OCSettings.get().ignorePower;
    }

    public void postInit() {
        li.cil.oc.core.impl.server.driver.Registry.INSTANCE.setLocked(true);
    }
}
