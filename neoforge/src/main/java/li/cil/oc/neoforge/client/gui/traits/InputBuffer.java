package li.cil.oc.neoforge.client.gui.traits;

import li.cil.oc.core.impl.client.Textures;
import li.cil.oc.core.impl.client.gui.traits.DisplayBuffer;
import li.cil.oc.core.impl.client.renderer.TextBufferRenderCache;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public interface InputBuffer extends DisplayBuffer {
    Map<InputBuffer, Map<Integer, Character>> _pressedKeys = new WeakHashMap<>();
    Map<InputBuffer, long[]> _state = new WeakHashMap<>();
    Map<InputBuffer, int[]> _altGrLwjglCode = new WeakHashMap<>();

    li.cil.oc.api.internal.TextBuffer buffer();

    boolean hasKeyboard();

    default Map<Integer, Character> pressedKeys() {
        return _pressedKeys.computeIfAbsent(this, k -> new HashMap<>());
    }

    default long showKeyboardMissing() {
        return _state.computeIfAbsent(this, k -> new long[1])[0];
    }

    default void showKeyboardMissing(long value) {
        _state.computeIfAbsent(this, k -> new long[1])[0] = value;
    }

    @SuppressWarnings({"SameReturnValue", "unused"})
    default boolean isPauseScreen() {
        return false;
    }

    default void drawBufferLayer(GuiGraphics guiGraphics) {
        DisplayBuffer.super.drawBufferLayer(guiGraphics);
        if (System.currentTimeMillis() - showKeyboardMissing() < 1000) {
            float x = (float) (bufferX() + bufferColumns() * scale() * TextBufferRenderCache.renderer.charRenderWidth() - 16);
            float y = (float) (bufferY() + bufferRows() * scale() * TextBufferRenderCache.renderer.charRenderHeight() - 16);
            guiGraphics.blit(Textures.guiKeyboardMissing, (int) x, (int) y, 16, 16, 0, 0, 16, 16, 16, 16);
        }
    }

    default void onGuiClosed() {
        if (buffer() != null) {
            for (Map.Entry<Integer, Character> entry : pressedKeys().entrySet()) {
                buffer().keyUp(entry.getValue(), glfwToLwjglKeyCode(entry.getKey()), null);
            }
            pressedKeys().clear();
        }
    }

    default boolean handleKeyPress(int keyCode, int ignoredScanCode, int modifiers) {
        if (buffer() == null || keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_F11) return false;
        if (hasKeyboard()) {
            char c = getCharFromCode(keyCode, modifiers);
            int lwjglCode = glfwToLwjglKeyCode(keyCode);
            int ctrl = modifiers & GLFW.GLFW_MOD_CONTROL;
            int alt = modifiers & GLFW.GLFW_MOD_ALT;

            if (ctrl != 0 && alt == 0) {
                if (c >= 'a' && c <= 'z') c = (char) (c - 'a' + 1);
                else if (c >= 'A' && c <= 'Z') c = (char) (c - 'A' + 1);
                if (keyCode == GLFW.GLFW_KEY_V) {
                    buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null);
                    return true;
                }
            }

            if (keyCode == li.cil.oc.neoforge.client.KeyBindings.clipboardPaste.getKey().getValue() &&
                    (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_ALT)) == 0) {
                buffer().clipboard(Minecraft.getInstance().keyboardHandler.getClipboard(), null);
                return true;
            }

            if (ctrl != 0 && alt != 0) {
                _altGrLwjglCode.put(this, new int[]{lwjglCode});
                if (!pressedKeys().containsKey(keyCode) || shouldRepeat(keyCode, c)) {
                    buffer().keyDown((char) 0, lwjglCode, null);
                    pressedKeys().put(keyCode, (char) 0);
                }
                return true;
            }

            if (!pressedKeys().containsKey(keyCode) || shouldRepeat(keyCode, c)) {
                buffer().keyDown(c, lwjglCode, null);
                pressedKeys().put(keyCode, c);
            }
        } else {
            showKeyboardMissing(System.currentTimeMillis());
        }
        return true;
    }

    default boolean handleCharTyped(char codePoint, int modifiers) {
        if (buffer() != null && hasKeyboard() && codePoint >= 32) {
            int ctrl = modifiers & GLFW.GLFW_MOD_CONTROL;
            int alt = modifiers & GLFW.GLFW_MOD_ALT;
            if (ctrl != 0 && alt != 0) {
                int[] stored = _altGrLwjglCode.get(this);
                int lwjglCode = stored != null ? stored[0] : 0;
                buffer().keyDown(codePoint, lwjglCode, null);
                return true;
            }
            return true;
        }
        return false;
    }

    default boolean handleKeyRelease(int keyCode, int ignoredScanCode, int ignoredModifiers) {
        if (buffer() != null) {
            Character c = pressedKeys().remove(keyCode);
            if (c != null) {
                buffer().keyUp(c, glfwToLwjglKeyCode(keyCode), null);
            }
            return true;
        }
        return false;
    }

    private static char getCharFromCode(int keyCode, int modifiers) {
        boolean caps = (modifiers & GLFW.GLFW_MOD_CAPS_LOCK) != 0;
        boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode >= GLFW.GLFW_KEY_A && keyCode <= GLFW.GLFW_KEY_Z) {
            boolean upper = shift ^ caps;
            return upper ? (char) ('A' + (keyCode - GLFW.GLFW_KEY_A)) : (char) ('a' + (keyCode - GLFW.GLFW_KEY_A));
        }
        if (keyCode >= GLFW.GLFW_KEY_0 && keyCode <= GLFW.GLFW_KEY_9) {
            if (shift) return ")!@#$%^&*(".charAt(keyCode - GLFW.GLFW_KEY_0);
            return (char) ('0' + (keyCode - GLFW.GLFW_KEY_0));
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE) return ' ';
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) return '\n';
        if (keyCode == GLFW.GLFW_KEY_TAB) return '\t';
        if (keyCode == GLFW.GLFW_KEY_MINUS) return shift ? '_' : '-';
        if (keyCode == GLFW.GLFW_KEY_EQUAL) return shift ? '+' : '=';
        if (keyCode == GLFW.GLFW_KEY_LEFT_BRACKET) return shift ? '{' : '[';
        if (keyCode == GLFW.GLFW_KEY_RIGHT_BRACKET) return shift ? '}' : ']';
        if (keyCode == GLFW.GLFW_KEY_BACKSLASH) return shift ? '|' : '\\';
        if (keyCode == GLFW.GLFW_KEY_SEMICOLON) return shift ? ':' : ';';
        if (keyCode == GLFW.GLFW_KEY_APOSTROPHE) return shift ? '"' : '\'';
        if (keyCode == GLFW.GLFW_KEY_COMMA) return shift ? '<' : ',';
        if (keyCode == GLFW.GLFW_KEY_PERIOD) return shift ? '>' : '.';
        if (keyCode == GLFW.GLFW_KEY_SLASH) return shift ? '?' : '/';
        if (keyCode == GLFW.GLFW_KEY_GRAVE_ACCENT) return shift ? '~' : '`';
        return 0;
    }

    private static int glfwToLwjglKeyCode(int keyCode) {
        return switch (keyCode) {
            case GLFW.GLFW_KEY_A -> 30;
            case GLFW.GLFW_KEY_B -> 48;
            case GLFW.GLFW_KEY_C -> 46;
            case GLFW.GLFW_KEY_D -> 32;
            case GLFW.GLFW_KEY_E -> 18;
            case GLFW.GLFW_KEY_F -> 33;
            case GLFW.GLFW_KEY_G -> 34;
            case GLFW.GLFW_KEY_H -> 35;
            case GLFW.GLFW_KEY_I -> 23;
            case GLFW.GLFW_KEY_J -> 36;
            case GLFW.GLFW_KEY_K -> 37;
            case GLFW.GLFW_KEY_L -> 38;
            case GLFW.GLFW_KEY_M -> 50;
            case GLFW.GLFW_KEY_N -> 49;
            case GLFW.GLFW_KEY_O -> 24;
            case GLFW.GLFW_KEY_P -> 25;
            case GLFW.GLFW_KEY_Q -> 16;
            case GLFW.GLFW_KEY_R -> 19;
            case GLFW.GLFW_KEY_S -> 31;
            case GLFW.GLFW_KEY_T -> 20;
            case GLFW.GLFW_KEY_U -> 22;
            case GLFW.GLFW_KEY_V -> 47;
            case GLFW.GLFW_KEY_W -> 17;
            case GLFW.GLFW_KEY_X -> 45;
            case GLFW.GLFW_KEY_Y -> 21;
            case GLFW.GLFW_KEY_Z -> 44;
            case GLFW.GLFW_KEY_0 -> 11;
            case GLFW.GLFW_KEY_1 -> 2;
            case GLFW.GLFW_KEY_2 -> 3;
            case GLFW.GLFW_KEY_3 -> 4;
            case GLFW.GLFW_KEY_4 -> 5;
            case GLFW.GLFW_KEY_5 -> 6;
            case GLFW.GLFW_KEY_6 -> 7;
            case GLFW.GLFW_KEY_7 -> 8;
            case GLFW.GLFW_KEY_8 -> 9;
            case GLFW.GLFW_KEY_9 -> 10;
            case GLFW.GLFW_KEY_MINUS -> 12;
            case GLFW.GLFW_KEY_EQUAL -> 13;
            case GLFW.GLFW_KEY_LEFT_BRACKET -> 26;
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> 27;
            case GLFW.GLFW_KEY_SEMICOLON -> 39;
            case GLFW.GLFW_KEY_APOSTROPHE -> 40;
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> 41;
            case GLFW.GLFW_KEY_BACKSLASH -> 43;
            case GLFW.GLFW_KEY_COMMA -> 51;
            case GLFW.GLFW_KEY_PERIOD -> 52;
            case GLFW.GLFW_KEY_SLASH -> 53;
            case GLFW.GLFW_KEY_SPACE -> 57;
            case GLFW.GLFW_KEY_TAB -> 15;
            case GLFW.GLFW_KEY_ENTER -> 28;
            case GLFW.GLFW_KEY_KP_ENTER -> 156;
            case GLFW.GLFW_KEY_BACKSPACE -> 14;
            case GLFW.GLFW_KEY_INSERT -> 210;
            case GLFW.GLFW_KEY_DELETE -> 211;
            case GLFW.GLFW_KEY_HOME -> 199;
            case GLFW.GLFW_KEY_END -> 207;
            case GLFW.GLFW_KEY_PAGE_UP -> 201;
            case GLFW.GLFW_KEY_PAGE_DOWN -> 209;
            case GLFW.GLFW_KEY_UP -> 200;
            case GLFW.GLFW_KEY_DOWN -> 208;
            case GLFW.GLFW_KEY_LEFT -> 203;
            case GLFW.GLFW_KEY_RIGHT -> 205;
            case GLFW.GLFW_KEY_LEFT_SHIFT -> 42;
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> 54;
            case GLFW.GLFW_KEY_LEFT_CONTROL -> 29;
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> 157;
            case GLFW.GLFW_KEY_LEFT_ALT -> 56;
            case GLFW.GLFW_KEY_RIGHT_ALT -> 184;
            case GLFW.GLFW_KEY_LEFT_SUPER -> 219;
            case GLFW.GLFW_KEY_RIGHT_SUPER -> 220;
            case GLFW.GLFW_KEY_F1 -> 59;
            case GLFW.GLFW_KEY_F2 -> 60;
            case GLFW.GLFW_KEY_F3 -> 61;
            case GLFW.GLFW_KEY_F4 -> 62;
            case GLFW.GLFW_KEY_F5 -> 63;
            case GLFW.GLFW_KEY_F6 -> 64;
            case GLFW.GLFW_KEY_F7 -> 65;
            case GLFW.GLFW_KEY_F8 -> 66;
            case GLFW.GLFW_KEY_F9 -> 67;
            case GLFW.GLFW_KEY_F10 -> 68;
            case GLFW.GLFW_KEY_F11 -> 87;
            case GLFW.GLFW_KEY_F12 -> 88;
            default -> keyCode;
        };
    }

    default boolean shouldRepeat(int keyCode, char ignoredC) {
        return keyCode != GLFW.GLFW_KEY_LEFT_CONTROL && keyCode != GLFW.GLFW_KEY_RIGHT_CONTROL &&
                keyCode != GLFW.GLFW_KEY_LEFT_ALT && keyCode != GLFW.GLFW_KEY_RIGHT_ALT &&
                keyCode != GLFW.GLFW_KEY_LEFT_SHIFT && keyCode != GLFW.GLFW_KEY_RIGHT_SHIFT &&
                keyCode != GLFW.GLFW_KEY_LEFT_SUPER && keyCode != GLFW.GLFW_KEY_RIGHT_SUPER;
    }
}
