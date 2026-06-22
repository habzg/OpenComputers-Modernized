package li.cil.oc.neoforge.common;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.InventorySlots;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.inventory.DiskDriveMountableInventory;
import li.cil.oc.core.impl.common.inventory.ServerInventory;
import li.cil.oc.core.impl.util.ItemUtils;
import li.cil.oc.neoforge.common.inventory.DatabaseInventory;
import li.cil.oc.neoforge.server.component.DiskDriveMountable;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

public final class GuiHandler {
    private GuiHandler() {
    }

    @SuppressWarnings("unused")
    public static AbstractContainerMenu getServerGuiElement(int containerId, int id, Player player, Level world, int x, int y, int z) {
        net.minecraft.core.BlockPos pos = new net.minecraft.core.BlockPos(x, GuiType.extractY(y), z);
        var category = GuiType.Categories.get(id);
        if (category == null) return null;

        switch (category) {
            case Block -> {
                var te = world.getBlockEntity(pos);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Adapter && id == GuiType.Adapter)
                    return new li.cil.oc.neoforge.common.container.Adapter(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Adapter) te);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Assembler && id == GuiType.Assembler)
                    return new li.cil.oc.neoforge.common.container.Assembler(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Assembler) te, player);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Charger && id == GuiType.Charger)
                    return new li.cil.oc.neoforge.common.container.Charger(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Charger) te);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Case && id == GuiType.Case) {
                    int tier = 0;
                    var state = world.getBlockState(pos);
                    if (state.getBlock() instanceof li.cil.oc.neoforge.common.block.Case b) tier = b.tier;
                    return new li.cil.oc.neoforge.common.container.Case(li.cil.oc.neoforge.common.init.Menus.CASE.get(), containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Case) te, tier);
                }
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Disassembler && id == GuiType.Disassembler)
                    return new li.cil.oc.neoforge.common.container.Disassembler(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Disassembler) te, player);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.DiskDrive && id == GuiType.DiskDrive)
                    return new li.cil.oc.neoforge.common.container.DiskDrive(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.DiskDrive) te);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Printer && id == GuiType.Printer)
                    return new li.cil.oc.neoforge.common.container.Printer(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Printer) te, player);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Raid && id == GuiType.Raid)
                    return new li.cil.oc.neoforge.common.container.Raid(containerId, player.getInventory(), (li.cil.oc.core.impl.common.tileentity.Raid) te);
                if (te instanceof li.cil.oc.neoforge.common.tileentity.Relay && id == GuiType.Relay)
                    return new li.cil.oc.neoforge.common.container.Relay(containerId, player.getInventory(), (li.cil.oc.neoforge.common.tileentity.Relay) te, player);
                if (te instanceof li.cil.oc.neoforge.common.tileentity.RobotProxy proxy && id == li.cil.oc.core.common.GuiType.Robot)
                    return new li.cil.oc.neoforge.common.container.Robot(containerId, player.getInventory(), proxy.robot);
                if (te instanceof li.cil.oc.core.impl.common.tileentity.Rack rack) {
                    if (id == GuiType.Rack)
                        return new li.cil.oc.neoforge.common.container.Rack(containerId, player.getInventory(), rack, player);
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
                            return new li.cil.oc.neoforge.common.container.Server(containerId, player.getInventory(), serverInv, null, player);
                        } else {
                            var mountable = rack.getMountable(slotNum);
                            if (mountable instanceof li.cil.oc.neoforge.server.component.Server serverRef) {
                                return new li.cil.oc.neoforge.common.container.Server(containerId, player.getInventory(), serverRef, serverRef, player);
                            }
                            return null;
                        }
                    }
                    if (id == GuiType.DiskDriveMountableInRack) {
                        int slot = GuiType.extractSlot(y);
                        var drive = (DiskDriveMountable) rack.getMountable(slot);
                        return new li.cil.oc.neoforge.common.container.DiskDrive(containerId, player.getInventory(), drive);
                    }
                }
            }
            case Entity -> {
                var entity = world.getEntity(x);
                if (entity instanceof li.cil.oc.core.impl.common.entity.Drone && id == GuiType.Drone)
                    return new li.cil.oc.neoforge.common.container.Drone(containerId, player.getInventory(), (li.cil.oc.core.impl.common.entity.Drone) entity);
            }
            case Item -> {
                ItemStack heldItem = player.getMainHandItem();
                var item = heldItem.getItem();
                switch (item) {
                    case li.cil.oc.neoforge.common.item.UpgradeDatabase upgradeDatabase when id == GuiType.Database -> {
                        var provider = player.level().registryAccess();
                        return new li.cil.oc.neoforge.common.container.Database(containerId, player.getInventory(), new DatabaseInventory() {
                            private final ItemStack[] items = new ItemStack[Settings.get().databaseEntriesPerTier[new li.cil.oc.neoforge.integration.opencomputers.DriverUpgradeDatabase().tier(heldItem)]];
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
                    case li.cil.oc.neoforge.common.item.Server server when id == GuiType.Server -> {
                        var provider = player.level().registryAccess();
                        return new li.cil.oc.neoforge.common.container.Server(containerId, player.getInventory(), new ServerInventory() {
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
                    case li.cil.oc.neoforge.common.item.Tablet tablet when id == GuiType.Tablet || id == GuiType.TabletInner -> {
                        var customData = heldItem.get(DataComponents.CUSTOM_DATA);
                        if (customData != null && !customData.isEmpty()) {
                            return new li.cil.oc.neoforge.common.container.Tablet(containerId, player.getInventory(), li.cil.oc.neoforge.common.item.Tablet.get(heldItem, player));
                        }
                    }
                    default -> {
                    }
                }
                if (item instanceof li.cil.oc.neoforge.common.item.DiskDriveMountable && id == GuiType.DiskDriveMountable) {
                    return new li.cil.oc.neoforge.common.container.DiskDrive(containerId, player.getInventory(), new DiskDriveMountableInventory() {
                        @Override
                        public ItemStack container() {
                            return heldItem;
                        }

                        @Override
                        public void clearContent() {
                        }

                        @Override
                        public void updateItems(int slot, ItemStack stack) {
                        }
                    });
                }
            }
        }
        return null;
    }
}
