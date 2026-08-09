package li.cil.oc.fabric.common;

import java.util.Arrays;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.common.inventory.DiskDriveMountableInventory;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import li.cil.oc.core.impl.util.ItemUtils;
import li.cil.oc.fabric.common.init.Menus;
import li.cil.oc.fabric.common.inventory.DatabaseInventory;
import li.cil.oc.fabric.integration.opencomputers.DriverUpgradeDatabase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public final class GuiHandler {
    private GuiHandler() {
    }

    public static AbstractContainerMenu getServerGuiElement(int containerId, int id, Player player, Level world, int x, int y, int z, String address) {
        BlockPos pos = new BlockPos(x, GuiType.extractY(y), z);
        var category = GuiType.Categories.get(id);
        if (category == null) return null;

        if (id == GuiType.Robot && address != null && !address.isEmpty()) {
            var robot = li.cil.oc.core.impl.common.container.RobotLookup.get(world, address);
            if (robot != null) {
                return new li.cil.oc.fabric.common.container.Robot(containerId, player.getInventory(), (li.cil.oc.fabric.common.blockentity.Robot) robot);
            }
        }

        switch (category) {
            case Block -> {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Adapter && id == GuiType.Adapter)
                    return new li.cil.oc.core.impl.common.container.Adapter(Menus.ADAPTER, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Adapter) te);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Assembler && id == GuiType.Assembler)
                    return new li.cil.oc.core.impl.common.container.Assembler(Menus.ASSEMBLER, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Assembler) te, player);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Case && id == GuiType.Case) {
                    int tier = 0;
                    var state = world.getBlockState(pos);
                    if (state.getBlock() instanceof li.cil.oc.fabric.common.block.Case b) tier = b.tier;
                    return new li.cil.oc.core.impl.common.container.Case(Menus.CASE, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Case) te, tier);
                }
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Charger && id == GuiType.Charger)
                    return new li.cil.oc.core.impl.common.container.Charger(Menus.CHARGER, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Charger) te);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Disassembler && id == GuiType.Disassembler)
                    return new li.cil.oc.core.impl.common.container.Disassembler(Menus.DISASSEMBLER, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Disassembler) te, player);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.DiskDrive && id == GuiType.DiskDrive)
                    return new li.cil.oc.core.impl.common.container.DiskDrive(Menus.DISK_DRIVE, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.DiskDrive) te);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Printer && id == GuiType.Printer)
                    return new li.cil.oc.core.impl.common.container.Printer(Menus.PRINTER, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Printer) te, player);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Raid && id == GuiType.Raid)
                    return new li.cil.oc.core.impl.common.container.Raid(Menus.RAID, containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Raid) te);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Relay && id == GuiType.Relay)
                    return new li.cil.oc.fabric.common.container.Relay(containerId, player.getInventory(), (li.cil.oc.core.impl.common.blockentity.Relay) te, player);
                if (te instanceof li.cil.oc.fabric.common.blockentity.RobotProxy proxy && id == GuiType.Robot)
                    return new li.cil.oc.fabric.common.container.Robot(containerId, player.getInventory(), (li.cil.oc.fabric.common.blockentity.Robot) proxy.robot);
                if (te instanceof li.cil.oc.core.impl.common.blockentity.Rack rack) {
                    if (id == GuiType.Rack)
                        return new li.cil.oc.core.impl.common.container.Rack(Menus.RACK, containerId, player.getInventory(), rack, player);
                    if (id == GuiType.ServerInRack) {
                        int slotNum = GuiType.extractSlot(y);
                        if (world.isClientSide) {
                            var stack = rack.getItem(slotNum);
                            var provider = player.level().registryAccess();
                            var serverInv = new ServerInventory() {
                                private final ItemStack[] items = new ItemStack[InventorySlots.server[Math.max(0, ItemUtils.caseTier(stack))].length];

                                {
                                    java.util.Arrays.fill(items, ItemStack.EMPTY);
                                    if (!stack.isEmpty()) {
                                        reinitialize(provider);
                                    }
                                }

                                @Override
                                public ItemStack container() {
                                    return stack;
                                }

                                @Override
                                public ItemStack[] items() {
                                    return items;
                                }

                                @Override
                                public boolean stillValid(@NotNull Player p) {
                                    return rack.stillValid(p);
                                }

                                @Override
                                public int getContainerSize() {
                                    return items.length;
                                }

                                @Override
                                public boolean canPlaceItem(int slot, @NotNull ItemStack s) {
                                    return ServerInventory.super.canPlaceItem(slot, s);
                                }

                                @Override
                                public void clearContent() {
                                    java.util.Arrays.fill(items, ItemStack.EMPTY);
                                }

                                @Override
                                public void updateItems(int slot, ItemStack s) {
                                    if (slot >= 0 && slot < items.length) items[slot] = s;
                                }

                                @Override
                                public void setChanged() {
                                    ServerInventory.super.setChanged(provider);
                                }
                            };
                            return new li.cil.oc.core.impl.common.container.Server(Menus.SERVER, containerId, player.getInventory(), serverInv, null, player);
                        } else {
                            var mountable = rack.getMountable(slotNum);
                            if (mountable instanceof li.cil.oc.core.impl.server.component.Server serverRef) {
                                return new li.cil.oc.core.impl.common.container.Server(Menus.SERVER, containerId, player.getInventory(), serverRef, serverRef, player);
                            }
                            return null;
                        }
                    }
                    if (id == GuiType.DiskDriveMountableInRack) {
                        int slot = GuiType.extractSlot(y);
                        var drive = (net.minecraft.world.Container) rack.getMountable(slot);
                        return new li.cil.oc.core.impl.common.container.DiskDrive(Menus.DISK_DRIVE, containerId, player.getInventory(), drive);
                    }
                }
            }
            case Entity -> {
                var entity = world.getEntity(x);
                if (entity instanceof li.cil.oc.core.impl.common.entity.Drone && id == GuiType.Drone)
                    return new li.cil.oc.core.impl.common.container.Drone(Menus.DRONE, containerId, player.getInventory(), (li.cil.oc.core.impl.common.entity.Drone) entity);
            }
            case Item -> {
                ItemStack heldItem = player.getMainHandItem();
                var item = heldItem.getItem();
                switch (item) {
                    case li.cil.oc.core.impl.common.item.UpgradeDatabase ignored when id == GuiType.Database -> {
                        var provider = player.level().registryAccess();
                        return new li.cil.oc.fabric.common.container.Database(containerId, player.getInventory(), new DatabaseInventory() {
                            private final ItemStack[] items = new ItemStack[OCSettings.get().databaseEntriesPerTier[new DriverUpgradeDatabase().tier(heldItem)]];
                            private final HolderLookup.Provider lookupProvider = provider;

                            {
                                var c = container();
                                if (c != null && !c.isEmpty()) {
                                    reinitialize(provider);
                                }
                            }

                            @Override
                            public ItemStack container() {
                                return heldItem;
                            }

                            @Override
                            public ItemStack[] items() {
                                return items;
                            }

                            @Override
                            public void setChanged() {
                                setChanged(lookupProvider);
                            }

                            @Override
                            public void clearContent() {
                                Arrays.fill(items, ItemStack.EMPTY);
                            }

                            @Override
                            public void updateItems(int slot, ItemStack stack) {
                                if (slot >= 0 && slot < items.length) {
                                    items[slot] = stack;
                                }
                            }
                        });
                    }
                    case li.cil.oc.core.impl.common.item.Server ignored when id == GuiType.Server -> {
                        var provider = player.level().registryAccess();
                        return new li.cil.oc.core.impl.common.container.Server(Menus.SERVER, containerId, player.getInventory(), new ServerInventory() {
                            private final ItemStack[] items = new ItemStack[InventorySlots.server[Math.max(0, ItemUtils.caseTier(heldItem))].length];

                            {
                                var c = container();
                                if (c != null && !c.isEmpty()) {
                                    reinitialize(provider);
                                }
                            }

                            @Override
                            public ItemStack container() {
                                return heldItem;
                            }

                            @Override
                            public ItemStack[] items() {
                                return items;
                            }

                            @Override
                            public int getContainerSize() {
                                return items.length;
                            }

                            @Override
                            public void clearContent() {
                                Arrays.fill(items, ItemStack.EMPTY);
                            }

                            @Override
                            public void updateItems(int slot, ItemStack stack) {
                                if (slot >= 0 && slot < items.length) {
                                    items[slot] = stack;
                                }
                            }

                            @Override
                            public void setChanged() {
                                ServerInventory.super.setChanged(provider);
                            }

                            @Override
                            public boolean stillValid(@NotNull Player player) {
                                return true;
                            }
                        }, null, player);
                    }
                    case li.cil.oc.core.impl.common.item.Tablet ignored when id == GuiType.Tablet || id == GuiType.TabletInner -> {
                        var customData = heldItem.get(DataComponents.CUSTOM_DATA);
                        if (customData != null && !customData.isEmpty()) {
                            return new li.cil.oc.core.impl.common.container.Tablet(Menus.TABLET, containerId, player.getInventory(), li.cil.oc.core.impl.common.item.Tablet.get(heldItem, player));
                        }
                    }
                    default -> {
                    }
                }
                if (item instanceof li.cil.oc.core.impl.common.item.DiskDriveMountable && id == GuiType.DiskDriveMountable) {
                    var provider = player.level().registryAccess();
                    return new li.cil.oc.core.impl.common.container.DiskDrive(Menus.DISK_DRIVE, containerId, player.getInventory(), new DiskDriveMountableInventory() {
                        private final ItemStack[] items = new ItemStack[1];

                        {
                            java.util.Arrays.fill(items, ItemStack.EMPTY);
                            var c = container();
                            if (c != null && !c.isEmpty()) {
                                reinitialize(provider);
                            }
                        }

                        @Override
                        public ItemStack container() {
                            return heldItem;
                        }

                        @Override
                        public ItemStack[] items() {
                            return items;
                        }

                        @Override
                        public void clearContent() {
                            java.util.Arrays.fill(items, ItemStack.EMPTY);
                        }

                        @Override
                        public void updateItems(int slot, ItemStack stack) {
                            if (slot >= 0 && slot < items.length) {
                                items[slot] = stack;
                            }
                        }

                        @Override
                        public void setChanged() {
                            setChanged(provider);
                        }

                        @Override
                        public boolean stillValid(@NotNull Player player) {
                            return true;
                        }
                    });
                }
            }
        }
        return null;
    }
}
