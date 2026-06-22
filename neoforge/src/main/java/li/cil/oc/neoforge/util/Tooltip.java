package li.cil.oc.neoforge.util;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class Tooltip {
    private static final int maxWidth = 220;

    private static Supplier<Font> fontSupplier = () -> null;
    private static BooleanSupplier extendedTooltips = () -> false;

    public static void setFont(Font font) {
        fontSupplier = () -> font;
    }

    public static void setExtendedTooltips(BooleanSupplier supplier) {
        extendedTooltips = supplier;
    }

    private Tooltip() {
    }

    private static Font font() {
        return fontSupplier.get();
    }

    public static List<Component> get(String name, Object... args) {
        String keyName = name.replace("_", "").toLowerCase(Locale.ROOT);
        String fullKey = "tooltip.opencomputers." + keyName;
        String tooltip = net.minecraft.network.chat.Component.translatable(fullKey, args).getString().replaceAll("\\[nl]", "\n").trim();

        if (font() == null) {
            String[] lines = tooltip.split("\n");
            return Arrays.stream(lines).<Component>map(Component::literal).toList();
        }

        boolean isSubTooltip = name.contains(".");
        boolean shouldShorten = (isSubTooltip || font().width(tooltip) > maxWidth) && !extendedTooltips.getAsBoolean();
        if (shouldShorten) {
            if (isSubTooltip) return List.of();
            return List.of(Component.literal("§7" + net.minecraft.network.chat.Component.translatable("tooltip.opencomputers.toolong", net.minecraft.network.chat.Component.translatable("key.keyboard.left.shift").getString()).getString().replaceAll("\\[nl]", "\n").trim()));
        }

        List<Component> result = new ArrayList<>();
        String[] lines = tooltip.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("§")) {
                line = "§7" + line;
            }
            result.add(Component.literal(line));
        }
        return result;
    }

    public static List<Component> extended(String name, Object... args) {
        if (extendedTooltips.getAsBoolean()) {
            String keyName = name.replace("_", "").toLowerCase(Locale.ROOT);
            String tooltip = net.minecraft.network.chat.Component.translatable("tooltip.opencomputers." + keyName, args).getString().replaceAll("\\[nl]", "\n").trim();
            List<Component> result = new ArrayList<>();
            String[] lines = tooltip.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("§")) {
                    line = "§7" + line;
                }
                result.add(Component.literal(line));
            }
            return result;
        }
        return List.of();
    }
}
