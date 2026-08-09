package li.cil.oc.fabric.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;

@SuppressWarnings("unused")
public final class CommandHandler {
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(ClientCommandManager.literal("oc_setclipboard")
                .then(ClientCommandManager.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(StringArgumentType.getString(ctx, "text"));
                            return Command.SINGLE_SUCCESS;
                        })
                )
        ));
    }
}
