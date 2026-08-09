package li.cil.oc.fabric.integration.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import li.cil.oc.fabric.client.PacketSender;
import li.cil.oc.fabric.client.gui.Database;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.common.entry.type.VanillaEntryTypes;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;

public class DatabaseDragHandler<T extends Screen> implements DraggableStackVisitor<Screen> {
    @Override
    public <R extends Screen> boolean isHandingScreen(R screen) {
        return screen instanceof Database;
    }

    @Override
    public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
        if (stack.getStack().getType() != VanillaEntryTypes.ITEM) {
            return DraggedAcceptorResult.PASS;
        }
        var db = (Database) context.getScreen();
        var pos = context.getCurrentPosition();
        if (pos == null) {
            return DraggedAcceptorResult.PASS;
        }
        int slot = getSlotAt(db, pos.x, pos.y);
        if (slot < 0) {
            return DraggedAcceptorResult.PASS;
        }
        ItemStack item = stack.getStack().castValue();
        PacketSender.sendDatabaseSetSlot(slot, item.copyWithCount(1));
        return DraggedAcceptorResult.CONSUMED;
    }

    @Override
    public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
        if (stack.getStack().getType() != VanillaEntryTypes.ITEM) {
            return Stream.empty();
        }
        if (!(context.getScreen() instanceof Database db)) {
            return Stream.empty();
        }
        List<BoundsProvider> bounds = new ArrayList<>();
        for (var rect : getSlotBounds(db)) {
            bounds.add(BoundsProvider.ofRectangle(rect));
        }
        return bounds.stream();
    }

    private static List<Rectangle> getSlotBounds(Database gui) {
        List<Rectangle> bounds = new ArrayList<>();
        int slotSize = 18;
        int tier = gui.databaseInventory.tier();
        int offset = 8 + new int[]{3, 2, 0}[Math.clamp(tier, 0, 2)] * slotSize;
        int size = (int) Math.ceil(Math.sqrt(gui.databaseInventory.getContainerSize()));
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = gui.windowX() + offset + col * slotSize;
                int y = gui.windowY() + offset + row * slotSize;
                bounds.add(new Rectangle(x, y, slotSize, slotSize));
            }
        }
        return bounds;
    }

    private static int getSlotAt(Database gui, double mouseX, double mouseY) {
        int slotSize = 18;
        int tier = gui.databaseInventory.tier();
        int offset = 8 + new int[]{3, 2, 0}[Math.clamp(tier, 0, 2)] * slotSize;
        int size = (int) Math.ceil(Math.sqrt(gui.databaseInventory.getContainerSize()));
        int startX = gui.windowX() + offset;
        int startY = gui.windowY() + offset;
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = startX + col * slotSize;
                int y = startY + row * slotSize;
                if (mouseX >= x && mouseX < x + slotSize && mouseY >= y && mouseY < y + slotSize) {
                    return row * size + col;
                }
            }
        }
        return -1;
    }
}
