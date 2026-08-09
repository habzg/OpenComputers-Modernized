package li.cil.oc.core.impl.common.item.data;

import li.cil.oc.core.Constants;
import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.server.component.DebugCardBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public class DebugCardData extends ItemData {
    public DebugCardBase.AccessContext access;

    public DebugCardData() {
        super(Constants.ItemName.DebugCard);
    }

    public DebugCardData(ItemStack stack) {
        this();
        load(stack);
    }

    @Override
    public void load(CompoundTag nbt, HolderLookup.Provider provider) {
        access = DebugCardBase.AccessContext.load(dataTag(nbt));
    }

    public static String getAccessPlayer(ItemStack stack) {
        var cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd != null && !cd.isEmpty()) {
            CompoundTag tag = cd.copyTag();
            if (tag.contains(OCSettings.namespace + "data")) {
                CompoundTag data = tag.getCompound(OCSettings.namespace + "data");
                if (data.contains(OCSettings.namespace + "player")) {
                    return data.getString(OCSettings.namespace + "player");
                }
            }
        }
        return "";
    }

    @Override
    public void save(CompoundTag nbt, HolderLookup.Provider provider) {
        var tag = dataTag(nbt);
        DebugCardBase.AccessContext.remove(tag);
        if (access != null) {
            access.save(tag);
        }
    }

    private CompoundTag dataTag(CompoundTag nbt) {
        if (!nbt.contains(OCSettings.namespace + "data")) {
            nbt.put(OCSettings.namespace + "data", new CompoundTag());
        }
        return nbt.getCompound(OCSettings.namespace + "data");
    }
}
