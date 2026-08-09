package li.cil.oc.fabric.integration.energy;

import li.cil.oc.api.detail.ItemInfo;
import li.cil.oc.fabric.common.capability.ChargeableEnergyStorage;
import li.cil.oc.fabric.common.capability.InternalEnergyStorage;
import li.cil.oc.fabric.common.init.Blocks;
import li.cil.oc.fabric.common.init.Items;
import li.cil.oc.fabric.integration.ModProxy;
import li.cil.oc.fabric.integration.Mods;
import li.cil.oc.fabric.integration.vanilla.DriverEnergy;
import team.reborn.energy.api.EnergyStorage;

@SuppressWarnings("unused")
public final class ModTeamRebornEnergy implements ModProxy {
    @Override
    public Mods.ModBase getMod() {
        return Mods.TeamRebornEnergy;
    }

    @Override
    public void initialize() {
        EnergyStorage.SIDED.registerForBlocks(
                (level, pos, state, blockEntity, side) -> {
                    if (blockEntity instanceof li.cil.oc.core.impl.common.blockentity.traits.PowerAcceptor acceptor
                            && side != null && acceptor.canConnectPower(side)) {
                        return new InternalEnergyStorage(acceptor, side);
                    }
                    return null;
                },
                Blocks.ALL_BLOCKS
        );

        var chargeableItemList = Items.descriptors.values().stream()
                .map(ItemInfo::item)
                .filter(item -> item instanceof li.cil.oc.api.driver.item.Chargeable)
                .toList();
        if (!chargeableItemList.isEmpty()) {
            net.minecraft.world.item.Item[] chargeableItems = chargeableItemList.toArray(new net.minecraft.world.item.Item[0]);
            EnergyStorage.ITEM.registerForItems(
                    (stack, context) -> {
                        if (stack.getItem() instanceof li.cil.oc.api.driver.item.Chargeable chargeable) {
                            return new ChargeableEnergyStorage(context, chargeable);
                        }
                        return null;
                    },
                    chargeableItems
            );
        }

        li.cil.oc.api.Driver.add(new DriverEnergy());
    }
}