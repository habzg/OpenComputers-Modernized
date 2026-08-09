package li.cil.oc.neoforge.integration.appeng;

import li.cil.oc.core.impl.common.blockentity.traits.power.AE2Power;
import li.cil.oc.core.impl.common.blockentity.traits.power.AppliedEnergistics2;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@SuppressWarnings("unused")
public final class ModAppEng implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.AppliedEnergistics2;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event, Block[] powerBlocks) {
        event.registerBlock(appeng.api.AECapabilities.IN_WORLD_GRID_NODE_HOST,
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof AppliedEnergistics2 ae2) {
                        return new OCGridNodeHost(ae2);
                    }
                    return null;
                },
                powerBlocks
        );
    }

    @Override
    public void initialize() {
        AE2Power.setDelegate(new AE2GridDelegate());

        li.cil.oc.core.impl.common.Registrar.registerWrenchTool("li.cil.oc.neoforge.integration.appeng.EventHandlerAE2.useWrench");
        li.cil.oc.core.impl.common.Registrar.registerWrenchToolCheck("li.cil.oc.neoforge.integration.appeng.EventHandlerAE2.isWrench");

        li.cil.oc.api.API.driver.add(new DriverController());
        li.cil.oc.api.API.driver.add(new DriverExportBus());
        li.cil.oc.api.API.driver.add(new DriverImportBus());
        li.cil.oc.api.API.driver.add(new DriverPartInterface());
        li.cil.oc.api.API.driver.add(new DriverBlockInterface());

        li.cil.oc.api.API.driver.add(new ConverterCellInventory());

        li.cil.oc.api.API.driver.add(new DriverController.Provider());
        li.cil.oc.api.API.driver.add(new DriverExportBus.Provider());
        li.cil.oc.api.API.driver.add(new DriverImportBus.Provider());
        li.cil.oc.api.API.driver.add(new DriverPartInterface.Provider());
        li.cil.oc.api.API.driver.add(new DriverBlockInterface.Provider());
    }
}
