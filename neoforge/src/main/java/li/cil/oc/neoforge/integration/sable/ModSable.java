package li.cil.oc.neoforge.integration.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import li.cil.oc.core.impl.client.ClientDistanceHelper;
import li.cil.oc.core.impl.common.PacketBuilderBase;
import li.cil.oc.neoforge.integration.Mod;
import li.cil.oc.neoforge.integration.ModProxy;
import li.cil.oc.neoforge.integration.Mods;
import net.minecraft.core.Position;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

@SuppressWarnings("unused")
public final class ModSable implements ModProxy {
    @Override
    public Mod getMod() {
        return Mods.Sable;
    }

    @Override
    public void initialize() {
        PacketBuilderBase.setDistanceHelper(ModSable::distanceSquared);
        ClientDistanceHelper.setDistanceHelper(ModSable::distanceSquaredTo);
        ClientDistanceHelper.setProjectHelper(ModSable::project);
    }

    private static double distanceSquared(Level level, double x, double y, double z, Player player) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, x, y, z, player.getX(), player.getY(), player.getZ());
    }

    private static double distanceSquaredTo(Level level, double x, double y, double z, double px, double py, double pz) {
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(level, x, y, z, px, py, pz);
    }

    private static Vec3 project(Level level, Vec3 pos) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) pos);
    }
}
