package li.cil.oc.neoforge.integration.jei;

import li.cil.oc.neoforge.client.PacketSender;
import li.cil.oc.neoforge.client.gui.Database;
import mezz.jei.api.gui.handlers.IGhostIngredientHandler;
import mezz.jei.api.ingredients.ITypedIngredient;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class DatabaseGhostIngredientHandler implements IGhostIngredientHandler<Database> {
    @Override
    public <I> @NotNull List<Target<I>> getTargetsTyped(@NotNull Database gui, ITypedIngredient<I> ingredient, boolean doStart) {
        List<Target<I>> targets = new ArrayList<>();
        if (ingredient.getType() == mezz.jei.api.constants.VanillaTypes.ITEM_STACK) {
            int slotSize = 18;
            int tier = gui.databaseInventory.tier();
            int offset = 8 + new int[]{3, 2, 0}[Math.clamp(tier, 0, 2)] * slotSize;
            int size = (int) Math.ceil(Math.sqrt(gui.databaseInventory.getContainerSize()));
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    int slot = row * size + col;
                    int x = gui.getGuiLeft() + offset + col * slotSize;
                    int y = gui.getGuiTop() + offset + row * slotSize;
                    Rect2i bounds = new Rect2i(x, y, slotSize, slotSize);
                    targets.add(new DatabaseSlotTarget<>(bounds, slot));
                }
            }
        }
        return targets;
    }

    @Override
    public void onComplete() {
    }

    private record DatabaseSlotTarget<I>(Rect2i bounds, int slot) implements Target<I> {
        @Override
        public @NotNull Rect2i getArea() {
            return bounds;
        }

        @Override
        public void accept(@NotNull I ingredient) {
            ItemStack stack = ((ItemStack) ingredient).copyWithCount(1);
            PacketSender.sendDatabaseSetSlot(slot, stack);
        }
    }
}
