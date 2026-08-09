package li.cil.oc.core.impl.util;

import li.cil.oc.core.impl.OCSettings;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class AccessContext extends OCSettings.AccessContext {
    public AccessContext(String player, String nonce) {
        super(player, nonce);
    }

    public static void remove(CompoundTag nbt) {
        nbt.remove(OCSettings.namespace + "player");
        nbt.remove(OCSettings.namespace + "accessNonce");
    }

    public static @Nullable AccessContext load(CompoundTag nbt) {
        if (nbt.contains(OCSettings.namespace + "player")) {
            return new AccessContext(
                    nbt.getString(OCSettings.namespace + "player"),
                    nbt.getString(OCSettings.namespace + "accessNonce"));
        }
        return null;
    }

    public void save(CompoundTag nbt) {
        nbt.putString(OCSettings.namespace + "player", player());
        nbt.putString(OCSettings.namespace + "accessNonce", nonce());
    }
}
