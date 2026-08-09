package li.cil.oc.fabric.common.container;

import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.common.container.DelegatingContainer;
import li.cil.oc.core.impl.common.container.Player;
import li.cil.oc.core.impl.common.container.RobotLookup;
import li.cil.oc.core.impl.common.container.StaticComponentSlot;
import li.cil.oc.fabric.common.init.Menus;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Robot extends Player {
    public final li.cil.oc.fabric.common.blockentity.Robot robot;
    public final String address;
    private final Level level;
    public final boolean hasScreen;
    public final int deltaY;
    protected final int withScreenHeight = 256;
    protected final int noScreenHeight = 108;
    protected final int factor = 100;

    public Robot(int containerId, Inventory playerInventory, li.cil.oc.fabric.common.blockentity.Robot robot) {
        this(containerId, playerInventory, robot, robot.getLevel(), robot.computerAddress() != null ? robot.computerAddress() : "");
    }

    private Robot(int containerId, Inventory playerInventory, li.cil.oc.fabric.common.blockentity.Robot robot, Level level, String address) {
        super(Menus.ROBOT, containerId, playerInventory, new DelegatingContainer(() -> {
            var resolved = RobotLookup.get(level, address);
            return resolved != null ? resolved : robot;
        }));
        this.robot = robot;
        this.address = address;
        this.level = level;

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) (Robot.this.current().globalBuffer / factor);
            }

            @Override
            public void set(int value) {
                Robot.this.current().globalBuffer = value * factor;
            }
        });
        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return (int) (Robot.this.current().globalBufferSize / factor);
            }

            @Override
            public void set(int value) {
                Robot.this.current().globalBufferSize = value * factor;
            }
        });

        hasScreen = robot.agentComponents().stream().anyMatch(c -> c instanceof li.cil.oc.api.internal.TextBuffer);
        deltaY = hasScreen ? 0 : withScreenHeight - noScreenHeight;

        addSlot(170, 232 - deltaY, Slot.Tool, Tier.Any);
        addSlot(170 + slotSize, 232 - deltaY, robot.containerSlotType(1), robot.containerSlotTier(1));
        addSlot(170 + 2 * slotSize, 232 - deltaY, robot.containerSlotType(2), robot.containerSlotTier(2));
        addSlot(170 + 3 * slotSize, 232 - deltaY, robot.containerSlotType(3), robot.containerSlotTier(3));

        for (int i = 0; i <= 3; i++) {
            int y = 156 + i * slotSize - deltaY;
            for (int j = 0; j <= 3; j++) {
                int x = 170 + j * slotSize;
                addSlot(new InventorySlot(this, otherInventory, slots.size(), x, y));
            }
        }
        for (int i = 16; i < 64; i++) {
            addSlot(new InventorySlot(this, otherInventory, slots.size(), -10000, -10000));
        }

        addPlayerInventorySlots(6, 174 - deltaY);
    }

    public li.cil.oc.fabric.common.blockentity.Robot current() {
        var resolved = RobotLookup.get(level, address);
        if (resolved instanceof li.cil.oc.fabric.common.blockentity.Robot lr) return lr;
        return robot;
    }

    public class InventorySlot extends StaticComponentSlot {
        public InventorySlot(Player container, Container inventory, int index, int x, int y) {
            super(container, inventory, index, x, y, Slot.Any, Tier.Any);
        }

        public boolean isValid() {
            return robot.isInventorySlot(getContainerSlot());
        }

        @Override
        public boolean isActive() {
            return isValid() && super.isActive();
        }

        @Override
        public net.minecraft.world.item.@NotNull ItemStack getItem() {
            if (isValid()) return super.getItem();
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
    }
}
