package li.cil.oc.fabric.integration.vanilla;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.integration.vanilla.ConverterItemStack;
import li.cil.oc.core.impl.integration.vanilla.ConverterNBT;
import li.cil.oc.core.impl.integration.vanilla.ConverterWorld;
import li.cil.oc.core.impl.integration.vanilla.ConverterWorldProvider;
import li.cil.oc.core.impl.integration.vanilla.DriverCommandBlock;
import li.cil.oc.core.impl.integration.vanilla.DriverComparator;
import li.cil.oc.core.impl.integration.vanilla.DriverMobSpawner;
import li.cil.oc.core.impl.integration.vanilla.DriverNoteBlock;
import li.cil.oc.core.impl.integration.vanilla.DriverRecordPlayer;
import li.cil.oc.core.impl.util.BlockPosition;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

@SuppressWarnings("unused")
public final class ModVanilla implements li.cil.oc.core.integration.ModProxy, BundledRedstone.RedstoneProvider {
    @Override
    public li.cil.oc.core.integration.Mod getMod() {
        return li.cil.oc.fabric.integration.Mods.Minecraft;
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

        if (OCSettings.get().enableInventoryDriver) {
            li.cil.oc.api.API.driver.add(new DriverInventory());
        }
        if (OCSettings.get().enableTankDriver) {
            li.cil.oc.api.API.driver.add(new DriverFluidHandler());
            li.cil.oc.api.API.driver.add(new DriverFluidTank());
        }
        if (OCSettings.get().enableCommandBlockDriver) {
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
