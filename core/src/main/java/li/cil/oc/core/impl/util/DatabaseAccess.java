package li.cil.oc.core.impl.util;

import li.cil.oc.api.internal.Database;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Node;
import net.minecraft.world.item.ItemStack;

public final class DatabaseAccess {
    public static Database database(Node node, String address) {
        return withDatabaseGeneric(node, address, db -> db);
    }

    public static java.util.List<Database> databases(Node node) {
        var result = new java.util.ArrayList<Database>();
        if (node.network() != null) {
            for (var n : node.network().nodes(node)) {
                if (n instanceof li.cil.oc.api.network.Component component) {
                    if (component.host() instanceof Database db) {
                        result.add(db);
                    }
                }
            }
        }
        return result;
    }

    public static Object[] withDatabase(Node node, String address, java.util.function.Function<Database, Object[]> f) {
        return withDatabaseGeneric(node, address, f);
    }

    public static <T> T withDatabaseGeneric(Node node, String address, java.util.function.Function<Database, T> f) {
        Component component = (Component) node.network().node(address);
        if (component != null) {
            if (component.host() instanceof Database database) {
                return f.apply(database);
            }
            throw new IllegalArgumentException("not a database");
        }
        throw new IllegalArgumentException("no such component");
    }

    public static ItemStack getStackFromDatabase(Node node, Arguments args, int offset) {
        if (args.isString(offset)) {
            return DatabaseAccess.withDatabaseGeneric(node, args.checkString(offset), database -> {
                int entry = args.checkInteger(offset + 1);
                int size = args.optInteger(offset + 2, 1);
                ItemStack dbStack = database.getStackInSlot(entry - 1);
                if (dbStack == null || dbStack.isEmpty() || size < 1) return null;
                dbStack.setCount(Math.min(size, dbStack.getMaxStackSize()));
                return dbStack;
            });
        }
        return null;
    }
}
