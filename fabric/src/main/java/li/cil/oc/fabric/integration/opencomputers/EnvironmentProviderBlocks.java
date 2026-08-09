package li.cil.oc.fabric.integration.opencomputers;

import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.common.blockentity.Assembler;
import li.cil.oc.core.impl.common.blockentity.Hologram;
import li.cil.oc.core.impl.common.blockentity.Printer;
import li.cil.oc.core.impl.common.blockentity.Waypoint;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.fabric.server.machine.Machine;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

@SuppressWarnings("unused")
public final class EnvironmentProviderBlocks implements EnvironmentProvider {
    @Override
    public Class<?> getEnvironment(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (isOneOf(block, Constants.BlockName.Assembler)) return Assembler.class;
            if (isOneOf(block, Constants.BlockName.CaseTier1, Constants.BlockName.CaseTier2, Constants.BlockName.CaseTier3, Constants.BlockName.CaseCreative, Constants.BlockName.Microcontroller))
                return Machine.class;
            if (isOneOf(block, Constants.BlockName.HologramTier1, Constants.BlockName.HologramTier2))
                return Hologram.class;
            if (isOneOf(block, Constants.BlockName.Printer)) return Printer.class;
            if (isOneOf(block, Constants.BlockName.Redstone)) {
                if (BundledRedstone.isAvailable()) return li.cil.oc.core.impl.server.component.Redstone.Bundled.class;
                return li.cil.oc.core.impl.server.component.Redstone.Vanilla.class;
            }
            if (isOneOf(block, Constants.BlockName.ScreenTier1))
                return li.cil.oc.core.impl.common.component.TextBuffer.class;
            if (isOneOf(block, Constants.BlockName.ScreenTier2, Constants.BlockName.ScreenTier3))
                return li.cil.oc.core.impl.common.component.Screen.class;
            if (isOneOf(block, Constants.BlockName.Robot)) return li.cil.oc.core.impl.server.component.Robot.class;
            if (isOneOf(block, Constants.BlockName.Waypoint)) return Waypoint.class;
            if (isOneOf(block, Constants.BlockName.Relay)) return li.cil.oc.core.impl.common.blockentity.Relay.class;
        } else {
            if (li.cil.oc.api.Items.get(stack) == li.cil.oc.api.Items.get(Constants.ItemName.Drone))
                return li.cil.oc.fabric.server.component.Drone.class;
        }
        return null;
    }

    private boolean isOneOf(Block block, String... names) {
        for (String name : names) {
            if (li.cil.oc.api.Items.get(name).block() == block) return true;
        }
        return false;
    }
}
