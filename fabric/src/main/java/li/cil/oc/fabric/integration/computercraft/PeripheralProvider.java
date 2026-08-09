package li.cil.oc.fabric.integration.computercraft;

import dan200.computercraft.api.peripheral.PeripheralLookup;
import li.cil.oc.core.impl.common.blockentity.Relay;
import li.cil.oc.core.impl.integration.computercraft.RelayPeripheral;
import li.cil.oc.fabric.common.init.BlockEntities;

public final class PeripheralProvider {
    private PeripheralProvider() {
    }

    public static void register() {
        try {
            PeripheralLookup.get().registerForBlockEntities(
                    (blockEntity, side) -> {
                        if (blockEntity instanceof Relay relay) {
                            return new RelayPeripheral(relay);
                        }
                        return null;
                    },
                    BlockEntities.RELAY
            );
        } catch (Exception | NoClassDefFoundError ignored) {
        }
    }
}
