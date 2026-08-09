package li.cil.oc.core.impl.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

public final class Tooltip {
    private static int maxWidth() {
        return Math.max(Minecraft.getInstance().getWindow().getGuiScaledWidth() / 2, 200);
    }

    private static Supplier<Font> fontSupplier = () -> null;
    private static BooleanSupplier extendedTooltips = () -> false;

    public static void setFont(Font font) {
        fontSupplier = () -> font;
    }

    public static void setExtendedTooltips(BooleanSupplier supplier) {
        extendedTooltips = supplier;
    }

    @SuppressWarnings("unused")
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
        boolean shouldShorten = (isSubTooltip || font().width(tooltip) > maxWidth()) && !extendedTooltips.getAsBoolean();
        if (shouldShorten) {
            if (isSubTooltip) return List.of();
            var tooLong = net.minecraft.network.chat.Component.translatable("tooltip.opencomputers.toolong", net.minecraft.network.chat.Component.translatable("key.keyboard.left.shift").getString()).getString().replaceAll("\\[nl]", "\n").trim();
            return List.of(Component.literal(tooLong).withStyle(ChatFormatting.GRAY));
        }

        return buildLines(tooltip);
    }

    public static List<Component> extended(String name, Object... args) {
        if (extendedTooltips.getAsBoolean()) {
            String keyName = name.replace("_", "").toLowerCase(Locale.ROOT);
            String tooltip = net.minecraft.network.chat.Component.translatable("tooltip.opencomputers." + keyName, args).getString().replaceAll("\\[nl]", "\n").trim();
            return buildLines(tooltip);
        }
        return List.of();
    }

    private static List<Component> buildLines(String tooltip) {
        List<Component> result = new ArrayList<>();
        String[] lines = tooltip.split("\n");
        var f = font();
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            addWrapped(f, line, result);
        }
        return result;
    }

    private static void addWrapped(Font f, String line, List<Component> result) {
        for (FormattedText segment : f.getSplitter().splitLines(Component.literal(line).withStyle(ChatFormatting.GRAY), maxWidth(), Style.EMPTY)) {
            segment.visit((style, text) -> {
                if (!text.isEmpty()) {
                    result.add(Component.literal(text).withStyle(style));
                }
                return Optional.empty();
            }, Style.EMPTY);
        }
    }
}
