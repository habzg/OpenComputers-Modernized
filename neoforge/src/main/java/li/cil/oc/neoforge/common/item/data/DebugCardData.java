package li.cil.oc.neoforge.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.core.impl.common.item.data.ItemData;
import li.cil.oc.neoforge.server.component.DebugCard;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DebugCardData extends ItemData {
    public DebugCard.AccessContext access;

    public DebugCardData() {
        super(Constants.ItemName.DebugCard);
    }

    public DebugCardData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        access = DebugCard.AccessContext.load(dataTag(nbt));
    }

    public static String getAccessPlayer(ItemStack stack) {
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(Settings.namespace + "data")) {
                CompoundTag data = tag.getCompound(Settings.namespace + "data");
                if (data.contains(Settings.namespace + "player")) {
                    return data.getString(Settings.namespace + "player");
                }
            }
        }
        return "";
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        var tag = dataTag(nbt);
        DebugCard.AccessContext.remove(tag);
        if (access != null) {
            access.save(tag);
        }
    }

    private CompoundTag dataTag(CompoundTag nbt) {
        if (!nbt.contains(Settings.namespace + "data")) {
            nbt.put(Settings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(Settings.namespace + "data");
    }
}
