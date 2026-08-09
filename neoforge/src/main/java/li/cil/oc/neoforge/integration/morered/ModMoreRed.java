package li.cil.oc.neoforge.integration.morered;

import commoble.morered.api.MoreRedAPI;
import commoble.morered.api.WireConnector;
import commoble.morered.wire_post.BundledCablePostBlockEntity;
import commoble.morered.wires.BundledCableBlockEntity;
import commoble.morered.wires.ColoredCableBlock;
import commoble.morered.wires.ColoredCableBlockEntity;
import li.cil.oc.core.impl.integration.util.BundledRedstone;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.neoforge.common.init.BlockEntities;
import li.cil.oc.neoforge.common.init.Blocks;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

@SuppressWarnings("unused")
public final class ModMoreRed implements ModProxy, BundledRedstone.RedstoneProvider {
    @Override
    public Mods.ModBase getMod() {
        return Mods.MoreRed;
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                MoreRedAPI.CHANNELED_POWER_CAPABILITY,
                BlockEntities.REDSTONE.get(),
                ChanneledPower::new
        );
        event.registerBlockEntity(
                MoreRedAPI.CHANNELED_POWER_CAPABILITY,
                BlockEntities.CASE.get(),
                ChanneledPower::new
        );
    }

    @Override
    public void initialize() {
        BundledRedstone.addProvider(this);

        WireConnector connector = (world, thisPos, thisState, wirePos, wireState, wireFace, directionToWire) -> true;
        var cables = MoreRedAPI.getCableConnectabilityRegistry();
        cables.put(Blocks.REDSTONE.get(), connector);
        cables.put(Blocks.CASE_TIER_1.get(), connector);
        cables.put(Blocks.CASE_TIER_2.get(), connector);
        cables.put(Blocks.CASE_TIER_3.get(), connector);
        cables.put(Blocks.CASE_CREATIVE.get(), connector);
    }

    @Override
    public int computeInput(BlockPosition pos, Direction side) {
        return 0;
    }

    @Override
    public int[] computeBundledInput(BlockPosition pos, Direction side) {
        var level = pos.level();
        if (level == null) return null;
        BlockEntity blockEntity = level.getBlockEntity(pos.offset(side).toBlockPos());
        if (blockEntity instanceof BundledCableBlockEntity cable) {
            int[] result = new int[16];
            int cableSide = side.getOpposite().ordinal();
            for (int channel = 0; channel < 16; channel++) {
                int power = cable.getPower(cableSide, channel);
                result[channel] = Math.min(255, (int) Math.round(power * 17 / 2.0));
            }
            return result;
        }
        if (blockEntity instanceof BundledCablePostBlockEntity post) {
            int[] result = new int[16];
            for (int channel = 0; channel < 16; channel++) {
                int power = post.getPower(channel);
                result[channel] = Math.min(255, (int) Math.round(power * 17 / 2.0));
            }
            return result;
        }
        if (blockEntity instanceof ColoredCableBlockEntity coloredCable) {
            BlockState cableState = blockEntity.getBlockState();
            if (cableState.getBlock() instanceof ColoredCableBlock cableBlock) {
                int[] result = new int[16];
                int channel = cableBlock.getDyeColor().ordinal();
                int power = coloredCable.getPower(side.getOpposite().ordinal());
                result[channel] = Math.min(255, (int) Math.round(power * 17 / 2.0));
                return result;
            }
        }
        return null;
    }
}