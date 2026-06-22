package li.cil.oc.core.impl.server.command;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class CommandUtils {
    private CommandUtils() {
    }

    public static int getOpLevel(CommandSourceStack source) {
        MinecraftServer srv = source.getServer();
        var profile = srv.getSingleplayerProfile();
        if (srv.isSingleplayer() && srv.getWorldData().isAllowCommands() &&
                profile != null && profile.getName() != null &&
                profile.getName().equalsIgnoreCase(source.getTextName())) {
            return 4;
        }

        if (source.getEntity() == null) {
            return 4;
        } else if (source.getEntity() instanceof ServerPlayer player) {
            var opEntry = srv.getPlayerList().getOps().get(player.getGameProfile());
            if (opEntry == null) return 0;
            return opEntry.getLevel();
        } else {
            return 0;
        }
    }
}
