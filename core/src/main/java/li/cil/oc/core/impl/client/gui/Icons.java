package li.cil.oc.core.impl.client.gui;

import java.util.HashMap;
import java.util.Map;
import li.cil.oc.core.common.Slot;
import li.cil.oc.core.common.Tier;
import li.cil.oc.core.impl.OCSettings;
import net.minecraft.resources.ResourceLocation;

public final class Icons {
    private static final Map<String, ResourceLocation> bySlotType = new HashMap<>();
    private static final Map<Integer, ResourceLocation> byTier = new HashMap<>();

    public static void init() {
        for (String name : Slot.All) {
            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/icons/" + name + ".png");
            bySlotType.put(name, rl);
        }
        byTier.put(Tier.None, ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/icons/na.png"));
        for (int tier = Tier.One; tier <= Tier.Three; tier++) {
            byTier.put(tier, ResourceLocation.fromNamespaceAndPath(OCSettings.resourceDomain, "textures/icons/tier" + tier + ".png"));
        }
    }

    public static ResourceLocation get(String slotType) {
        return bySlotType.get(slotType);
    }

    public static ResourceLocation get(int tier) {
        return byTier.get(tier);
    }
}
