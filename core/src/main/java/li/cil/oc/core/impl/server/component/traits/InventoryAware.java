package li.cil.oc.core.impl.server.component.traits;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.core.impl.util.ExtendedArguments;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;


public interface InventoryAware {
    Player fakePlayer();

    Container inventory();

    int selectedSlot();

    void selectedSlot_$eq(int value) ;

    default int[] insertionSlots() {
        int size = inventory().getContainerSize();
        int[] result = new int[size];
        int sel = selectedSlot();
        int idx = 0;
        for (int i = sel; i < size; i++) result[idx++] = i;
        for (int i = 0; i < sel; i++) result[idx++] = i;
        return result;
    }

    default int optSlot(Arguments args, int n) {
        if (args.count() > 0 && args.checkAny(0) != null)
            return ExtendedArguments.checkSlot(args, inventory(), 0);
        return selectedSlot();
    }

    default ItemStack stackInSlot(int slot) {
        return inventory().getItem(slot);
    }
}
