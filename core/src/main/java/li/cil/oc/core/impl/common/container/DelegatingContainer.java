package li.cil.oc.core.impl.common.container;

import java.util.function.Supplier;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class DelegatingContainer implements Container {
    private final Supplier<Container> delegate;

    public DelegatingContainer(Supplier<Container> delegate) {
        this.delegate = delegate;
    }

    private Container current() {
        return delegate.get();
    }

    @Override
    public int getContainerSize() {
        var c = current();
        return c != null ? c.getContainerSize() : 0;
    }

    @Override
    public boolean isEmpty() {
        var c = current();
        return c == null || c.isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem(int index) {
        var c = current();
        return c != null ? c.getItem(index) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItem(int index, int count) {
        var c = current();
        return c != null ? c.removeItem(index, count) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack removeItemNoUpdate(int index) {
        var c = current();
        return c != null ? c.removeItemNoUpdate(index) : ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, @NotNull ItemStack stack) {
        var c = current();
        if (c != null) c.setItem(index, stack);
    }

    @Override
    public int getMaxStackSize() {
        var c = current();
        return c != null ? c.getMaxStackSize() : 64;
    }

    @Override
    public void setChanged() {
        var c = current();
        if (c != null) c.setChanged();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        var c = current();
        return c != null && c.stillValid(player);
    }

    @Override
    public boolean canPlaceItem(int index, @NotNull ItemStack stack) {
        var c = current();
        return c != null && c.canPlaceItem(index, stack);
    }

    @Override
    public void clearContent() {
        var c = current();
        if (c != null) c.clearContent();
    }
}
