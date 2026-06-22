package li.cil.oc.core.common;

public final class InventorySlots {
    public static final InventorySlot[][] computer = {
            {
                    new InventorySlot(Slot.Card, Tier.One),
                    new InventorySlot(Slot.Card, Tier.One),
                    new InventorySlot(Slot.Memory, Tier.One),
                    new InventorySlot(Slot.HDD, Tier.One),
                    new InventorySlot(Slot.CPU, Tier.One),
                    new InventorySlot(Slot.Memory, Tier.One),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.Card, Tier.One),
                    new InventorySlot(Slot.Memory, Tier.Two),
                    new InventorySlot(Slot.Memory, Tier.Two),
                    new InventorySlot(Slot.HDD, Tier.Two),
                    new InventorySlot(Slot.HDD, Tier.One),
                    new InventorySlot(Slot.CPU, Tier.Two),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Two),
                    new InventorySlot(Slot.Floppy, Tier.One),
                    new InventorySlot(Slot.CPU, Tier.Three),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.Floppy, Tier.One),
                    new InventorySlot(Slot.CPU, Tier.Three),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            }
    };

    public static final InventorySlot[][] server = {
            {
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.CPU, Tier.Two),
                    new InventorySlot(Slot.ComponentBus, Tier.Two),
                    new InventorySlot(Slot.Memory, Tier.Two),
                    new InventorySlot(Slot.Memory, Tier.Two),
                    new InventorySlot(Slot.HDD, Tier.Two),
                    new InventorySlot(Slot.HDD, Tier.Two),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.CPU, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.CPU, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.Card, Tier.Two),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            },
            {
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.CPU, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.ComponentBus, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.Memory, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.HDD, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.Card, Tier.Three),
                    new InventorySlot(Slot.EEPROM, Tier.Any)
            }
    };

    public static final InventorySlot[] relay = {
            new InventorySlot(Slot.CPU, Tier.Three),
            new InventorySlot(Slot.Memory, Tier.Three),
            new InventorySlot(Slot.HDD, Tier.Three),
            new InventorySlot(Slot.Card, Tier.Three)
    };

    private InventorySlots() {
    }

    public record InventorySlot(String slot, int tier) {
    }

}
