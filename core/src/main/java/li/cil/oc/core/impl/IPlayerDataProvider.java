package li.cil.oc.core.impl;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

public interface IPlayerDataProvider {
    CompoundTag getPersistentData(Player player);
}
