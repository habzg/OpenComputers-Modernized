package li.cil.oc.neoforge.client;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

public final class CommandHandler {
    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void register(RegisterClientCommandsEvent event) {
        var dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("oc_setclipboard")
                .then(Commands.argument("text", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            Minecraft.getInstance().keyboardHandler.setClipboard(StringArgumentType.getString(ctx, "text"));
                            return Command.SINGLE_SUCCESS;
                        })
                )
        );
    }
}
