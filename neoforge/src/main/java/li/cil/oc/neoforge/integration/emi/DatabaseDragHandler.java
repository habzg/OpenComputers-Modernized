package li.cil.oc.neoforge.integration.emi;

import dev.emi.emi.api.EmiDragDropHandler;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.widget.Bounds;
import java.util.ArrayList;
import java.util.List;
import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.client.gui.Database;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public class DatabaseDragHandler implements EmiDragDropHandler<Database> {
    @Override
    public boolean dropStack(Database screen, EmiIngredient stack, int x, int y) {
        var stacks = stack.getEmiStacks();
        if (stacks.isEmpty()) return false;
        var emiStack = stacks.getFirst();
        if (emiStack.isEmpty()) return false;
        ItemStack itemStack = emiStack.getItemStack();
        if (itemStack.isEmpty()) return false;
        int slot = getSlotAt(screen, x, y);
        if (slot < 0) return false;
        PacketSender.sendDatabaseSetSlot(slot, itemStack.copyWithCount(1));
        return true;
    }

    @Override
    public void render(Database screen, EmiIngredient dragged, GuiGraphics draw, int mouseX, int mouseY, float delta) {
        for (var bounds : getSlotBounds(screen)) {
            draw.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), 0x8822BB33);
        }
    }

    private static List<Bounds> getSlotBounds(Database gui) {
        List<Bounds> bounds = new ArrayList<>();
        int slotSize = 18;
        int tier = gui.databaseInventory.tier();
        int offset = 8 + new int[]{3, 2, 0}[Math.clamp(tier, 0, 2)] * slotSize;
        int size = (int) Math.ceil(Math.sqrt(gui.databaseInventory.getContainerSize()));
        for (int row = 0; row < size; row++) {
            for (int col = 0; col < size; col++) {
                int x = gui.windowX() + offset + col * slotSize;
                int y = gui.windowY() + offset + row * slotSize;
                bounds.add(new Bounds(x, y, slotSize, slotSize));
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
