package li.cil.oc.core.impl.server.command;

import li.cil.oc.core.impl.OCSettings;
import li.cil.oc.core.impl.OCSettings.DebugCardAccess;
import li.cil.oc.core.impl.common.command.SimpleCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class DebugWhitelistCommand extends SimpleCommand {
    public static final DebugWhitelistCommand INSTANCE = new DebugWhitelistCommand();

    private DebugWhitelistCommand() {
        super("oc_debugWhitelist");
    }

    @Override
    public boolean checkPermission(CommandSourceStack source) {
        return true;
    }

    public boolean isOp(CommandSourceStack source) {
        return CommandUtils.getOpLevel(source) >= 2;
    }

    @Override
    protected int execute(CommandSourceStack source, String[] args) {
        if (source.getEntity() != null && !(source.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            source.sendFailure(Component.literal("§cThis command can only be used by players."));
            return 0;
        }
        if (!(OCSettings.get().debugCardAccess instanceof DebugCardAccess.Whitelist wl)) {
            source.sendFailure(Component.literal("§cDebug card whitelisting is not enabled."));
            return 0;
        }

        java.util.function.Consumer<String> revokeUser = player -> {
            if (wl.isWhitelisted(player)) {
                wl.invalidate(player);
                source.sendSuccess(() -> Component.literal("§aAll your debug cards were invalidated."), false);
            } else {
                source.sendSuccess(() -> Component.literal("§cYou are not whitelisted to use debug card."), false);
            }
        };

        if (args.length == 1 && "revoke".equals(args[0])) {
            revokeUser.accept(source.getTextName());
        } else if (args.length == 2 && "revoke".equals(args[0]) && isOp(source)) {
            if (isValidPlayerName(args[1])) {
                revokeUser.accept(args[1]);
            } else {
                source.sendFailure(Component.literal("§cInvalid player name."));
                return 0;
            }
        } else if (args.length == 1 && "list".equals(args[0]) && isOp(source)) {
            java.util.Set<String> players = wl.whitelist();
            if (!players.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§aCurrently whitelisted players: §e" + String.join(", ", players)), false);
            } else {
                source.sendSuccess(() -> Component.literal("§cThere is no currently whitelisted players."), false);
            }
        } else if (args.length == 2 && "add".equals(args[0]) && isOp(source)) {
            if (isValidPlayerName(args[1])) {
                wl.add(args[1]);
                source.sendSuccess(() -> Component.literal("§aPlayer was added to whitelist."), false);
            } else {
                source.sendFailure(Component.literal("§cInvalid player name."));
                return 0;
            }
        } else if (args.length == 2 && "remove".equals(args[0]) && isOp(source)) {
            if (isValidPlayerName(args[1])) {
                wl.remove(args[1]);
                source.sendSuccess(() -> Component.literal("§aPlayer was removed from whitelist"), false);
            } else {
                source.sendFailure(Component.literal("§cInvalid player name."));
                return 0;
            }
        } else {
            if (isOp(source))
                source.sendSuccess(() -> Component.literal("§e" + getName() + " [revoke|add|remove] <player> OR " + getName() + " [revoke|list]"), false);
            else
                source.sendSuccess(() -> Component.literal("§e" + getName() + " revoke"), false);
        }
        return 0;
    }

    private static boolean isValidPlayerName(String name) {
        if (name == null || name.isEmpty() || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9') && c != '_') return false;
        }
        return true;
    }
}
