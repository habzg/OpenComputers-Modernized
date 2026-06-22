package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.NeoForge;

@SuppressWarnings("unused")
public final class ModVanilla implements ModProxy, BundledRedstone.RedstoneProvider {
    @Override
    public Mods.ModBase getMod() {
        return Mods.Minecraft;
    }

    @Override
    public void initialize() {
        li.cil.oc.api.API.driver.add(new DriverBeacon());
        li.cil.oc.api.API.driver.add(new DriverBrewingStand());
        li.cil.oc.api.API.driver.add(new DriverComparator());
        li.cil.oc.api.API.driver.add(new DriverFurnace());
        li.cil.oc.api.API.driver.add(new DriverMobSpawner());
        li.cil.oc.api.API.driver.add(new DriverNoteBlock());
        li.cil.oc.api.API.driver.add(new DriverRecordPlayer());

        li.cil.oc.api.API.driver.add(new DriverBeacon.Provider());
        li.cil.oc.api.API.driver.add(new DriverBrewingStand.Provider());
        li.cil.oc.api.API.driver.add(new DriverComparator.Provider());
        li.cil.oc.api.API.driver.add(new DriverFurnace.Provider());
        li.cil.oc.api.API.driver.add(new DriverMobSpawner.Provider());
        li.cil.oc.api.API.driver.add(new DriverNoteBlock.Provider());
        li.cil.oc.api.API.driver.add(new DriverRecordPlayer.Provider());

        if (Settings.get().enableInventoryDriver) {
            li.cil.oc.api.API.driver.add(new DriverInventory());
        }
        if (Settings.get().enableTankDriver) {
            li.cil.oc.api.API.driver.add(new DriverFluidHandler());
            li.cil.oc.api.API.driver.add(new DriverFluidTank());
        }
        if (Settings.get().enableCommandBlockDriver) {
            li.cil.oc.api.API.driver.add(new DriverCommandBlock());
        }

        li.cil.oc.api.API.driver.add(new ConverterFluidContainerItem());
        li.cil.oc.api.API.driver.add(new ConverterFluidStack());
        li.cil.oc.api.API.driver.add(new ConverterFluidTankInfo());
        li.cil.oc.api.API.driver.add(new ConverterItemStack());
        li.cil.oc.api.API.driver.add(new ConverterNBT());
        li.cil.oc.api.API.driver.add(new ConverterWorld());
        li.cil.oc.api.API.driver.add(new ConverterWorldProvider());

        BundledRedstone.addProvider(this);

        NeoForge.EVENT_BUS.register(EventHandlerVanilla.class);
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        var world = pos.level();
        if (world == null) return 0;
        var offsetPos = new BlockPos(
                pos.x() + side.getStepX(),
                pos.y() + side.getStepY(),
                pos.z() + side.getStepZ()
        );
        return Math.max(
                world.getSignal(offsetPos, side),
                world.getBlockState(offsetPos).getBlock() == Blocks.REDSTONE_WIRE ?
                        world.getBlockState(offsetPos).getValue(net.minecraft.world.level.block.RedStoneWireBlock.POWER) : 0
        );
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        return null;
    }
}
