package li.cil.oc.core.common;

import java.util.HashMap;
import java.util.Map;

public final class GuiType {
    public static final Map<Integer, Category> Categories = new HashMap<>();
    public static final int Adapter = register(0, Category.Block, "Adapter");
    public static final int Assembler = register(1, Category.Block, "Assembler");
    public static final int Case = register(2, Category.Block, "Case");
    public static final int Charger = register(3, Category.Block, "Charger");
    public static final int Database = register(4, Category.Item, "Database");
    public static final int Disassembler = register(5, Category.Block, "Disassembler");
    public static final int DiskDrive = register(6, Category.Block, "DiskDrive");
    public static final int DiskDriveMountable = register(7, Category.Item, "DiskDriveMountable");
    public static final int DiskDriveMountableInRack = register(8, Category.Block, "DiskDriveMountableInRack");
    public static final int Drive = register(9, Category.Item, "Drive");
    public static final int Drone = register(10, Category.Entity, "Drone");
    public static final int Printer = register(12, Category.Block, "Printer");
    public static final int Rack = register(13, Category.Block, "Rack");
    public static final int Raid = register(14, Category.Block, "Raid");
    public static final int Relay = register(15, Category.Block, "Relay");
    public static final int Robot = register(16, Category.Block, "Robot");
    public static final int Screen = register(17, Category.Block, "Screen");
    public static final int Server = register(18, Category.Item, "Server");
    public static final int ServerInRack = register(19, Category.Block, "ServerInRack");
    public static final int Tablet = register(21, Category.Item, "Tablet");
    public static final int TabletInner = register(22, Category.Item, "TabletInner");
    public static final int Waypoint = register(24, Category.Block, "Waypoint");

    private GuiType() {
    }

    private static int register(int id, Category subType, String ignoredName) {
        Categories.put(id, subType);
        return id;
    }

    public static int embedSlot(int y, int slot) {
        return (y & 0x00FFFFFF) | (slot << 24);
    }

    public static int extractY(int value) {
        return (value << 8) >> 8;
    }

    public static int extractSlot(int value) {
        return (value >>> 24) & 0xFF;
    }

    public enum Category {
        Block,
        Entity,
        Item
    }

}
