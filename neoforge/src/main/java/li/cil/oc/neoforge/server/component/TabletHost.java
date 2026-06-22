package li.cil.oc.neoforge.server.component;

import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.component.TabletHostBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import org.jetbrains.annotations.NotNull;

public class TabletHost extends TabletHostBase {
    public final ItemStack stack;
    public Player player;
    private final ItemStack[] mirror;

    public TabletHost(ItemStack stack, Player player) {
        var data = new TabletData(stack);
        this.stack = stack;
        this.player = player;
        this.mirror = new ItemStack[data.items.size()];
        for (int i = 0; i < data.items.size(); i++) {
            mirror[i] = data.items.get(i);
        }
        creationLevel = player.level();
    }

    @Override
    public void player(@NotNull Player p) {
        player = p;
    }

    @Override
    public Player player() {
        return player;
    }

    @Override
    public @NotNull ItemStack getStack() {
        return stack;
    }

    @Override
    public ItemStack[] items() {
        return mirror;
    }

    @Override
    public void updateItems(int slot, ItemStack s) {
        if (slot >= 0 && slot < mirror.length) {
            mirror[slot] = (s == null || s.isEmpty()) ? null : s;
        }
    }

    @Override
    public int getContainerSize() {
        return mirror.length;
    }

    @Override
    public void setChanged() {
        var data = new TabletData(stack);
        if (data.items.size() != mirror.length) {
            data.items.clear();
            for (int i = 0; i < mirror.length; i++) data.items.add(null);
        }
        for (int i = 0; i < mirror.length; i++) {
            data.items.set(i, (mirror[i] == null || mirror[i].isEmpty()) ? null : mirror[i]);
        }
        data.save(stack);
    }

    @Override
    public @NotNull String containerSlotType() {
        return li.cil.oc.core.common.Slot.Tablet;
    }

    @Override
    public int containerSlotTier() {
        return new TabletData(stack).tier;
    }

    @Override
    protected @NotNull CompoundTag loadMachineTag() {
        var tag = stack.get(DataComponents.CUSTOM_DATA);
        if (tag != null && !tag.isEmpty()) {
            var t = tag.copyTag();
            if (t.contains(li.cil.oc.core.impl.Settings.namespace + "data")) {
                return t.getCompound(li.cil.oc.core.impl.Settings.namespace + "data");
            }
        }
        return new CompoundTag();
    }

    @Override
    protected void saveMachineTag(@NotNull CompoundTag nbt) {
        var tag = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag t;
        if (tag != null && !tag.isEmpty()) {
            t = tag.copyTag();
        } else {
            t = new CompoundTag();
        }
        t.put(li.cil.oc.core.impl.Settings.namespace + "data", nbt);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(t));
    }
}
