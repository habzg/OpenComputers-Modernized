package li.cil.oc.core.impl.util;

import li.cil.oc.core.impl.Settings;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class AccessContext extends Settings.AccessContext {
    public AccessContext(String player, String nonce) {
        super(player, nonce);
    }

    public static void remove(CompoundTag nbt) {
        nbt.remove(Settings.namespace + "player");
        nbt.remove(Settings.namespace + "accessNonce");
    }

    public static @Nullable AccessContext load(CompoundTag nbt) {
        if (nbt.contains(Settings.namespace + "player")) {
            return new AccessContext(
                    nbt.getString(Settings.namespace + "player"),
                    nbt.getString(Settings.namespace + "accessNonce"));
        }
        return null;
    }

    public void save(CompoundTag nbt) {
        nbt.putString(Settings.namespace + "player", player());
        nbt.putString(Settings.namespace + "accessNonce", nonce());
    }
}
