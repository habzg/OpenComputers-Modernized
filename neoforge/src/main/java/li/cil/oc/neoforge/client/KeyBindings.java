package li.cil.oc.neoforge.client;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import li.cil.oc.neoforge.OpenComputers;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

@SuppressWarnings("unused")
public final class KeyBindings {
    public static final List<Function<KeyMapping, Boolean>> keyBindingChecks = new ArrayList<>();
    public static final List<Function<KeyMapping, String>> keyBindingNameGetters = new ArrayList<>();

    public static final KeyMapping clipboardPaste;

    static {
        keyBindingChecks.add(KeyBindings::isKeyBindingPressedVanilla);
        keyBindingNameGetters.add(KeyBindings::getKeyBindingNameVanilla);

        clipboardPaste = new KeyMapping("key.opencomputers.clipboardpaste", GLFW.GLFW_KEY_INSERT, OpenComputers.Name);
    }

    public static void registerBindings(RegisterKeyMappingsEvent event) {
        event.register(clipboardPaste);
    }

    public static boolean showExtendedTooltips() {
        var window = Minecraft.getInstance().getWindow().getWindow();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @SuppressWarnings("unused")
    public static boolean isPastingClipboard() {
        return isKeyBindingPressed(clipboardPaste);
    }

    public static boolean isKeyBindingPressed(KeyMapping keyBinding) {
        if (keyBinding == null) return false;
        for (Function<KeyMapping, Boolean> check : keyBindingChecks) {
            if (!check.apply(keyBinding)) return false;
        }
        return true;
    }

    private static String getKeyBindingNameVanilla(KeyMapping keyBinding) {
        try {
            return keyBinding.getTranslatedKeyMessage().getString();
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean isKeyBindingPressedVanilla(KeyMapping keyBinding) {
        try {
            return keyBinding.isDown();
        } catch (Throwable t) {
            return false;
        }
    }
}
