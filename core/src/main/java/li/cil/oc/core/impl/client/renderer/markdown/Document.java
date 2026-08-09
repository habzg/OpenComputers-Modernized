package li.cil.oc.core.impl.client.renderer.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import li.cil.oc.api.Manual;
import li.cil.oc.core.impl.client.renderer.markdown.segment.BoldSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.CodeSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.HeaderSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.InteractiveSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.ItalicSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.LinkSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.RenderSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.Segment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.StrikethroughSegment;
import li.cil.oc.core.impl.client.renderer.markdown.segment.TextSegment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

@SuppressWarnings("unused")
public final class Document {
    private static String currentPagePath = "";

    public static String getCurrentPagePath() {
        return currentPagePath;
    }

    public static void setCurrentPagePath(String path) {
        currentPagePath = path != null ? path : "";
    }

    public static Segment parse(Iterable<String> document) {
        var segments = new ArrayList<Segment>();
        for (String line : document) {
            String text = line != null ? trimTrailing(line) : "";
            segments.add(new TextSegment(null, text));
        }
        for (var entry : SEGMENT_TYPES) {
            var pattern = entry.getKey();
            var factory = entry.getValue();
            var refined = new ArrayList<Segment>();
            for (var seg : segments) {
                refined.addAll(seg.refine(pattern, factory));
            }
            segments = refined;
        }
        for (int i = 0; i < segments.size() - 1; i++) {
            segments.get(i).next = segments.get(i + 1);
        }
        return segments.isEmpty() ? null : segments.getFirst();
    }

    public static int height(Segment document, int maxWidth, Font renderer) {
        int currentX = 0;
        int currentY = 0;
        Segment segment = document;
        while (segment != null) {
            currentY += segment.nextY(currentX, maxWidth, renderer);
            currentX = segment.nextX(currentX, maxWidth, renderer);
            segment = segment.next;
        }
        return currentY;
    }

    public static int lineHeight(Font renderer) {
        return renderer.lineHeight + 1;
    }

    public static InteractiveSegment render(Segment document, int x, int y, int maxWidth, int maxHeight, int yOffset, Font renderer, GuiGraphics graphics, int mouseX, int mouseY) {
        InteractiveSegment hovered = null;
        int indent = 0;
        int currentY = y - yOffset;
        int minY = y - lineHeight(renderer);
        int maxYLimit = y + maxHeight + lineHeight(renderer);
        Segment segment = document;
        while (segment != null) {
            int segmentHeight = segment.nextY(indent, maxWidth, renderer);
            if (currentY + segmentHeight >= minY && currentY <= maxYLimit) {
                InteractiveSegment result = segment.render(x, currentY, indent, maxWidth, renderer, graphics, mouseX, mouseY);
                if (result != null) hovered = result;
            }
            currentY += segmentHeight;
            indent = segment.nextX(indent, maxWidth, renderer);
            segment = segment.next;
        }
        if (mouseX < x || mouseX > x + maxWidth || mouseY < y || mouseY > y + maxHeight) hovered = null;
        if (hovered != null) hovered.notifyHover();
        return hovered;
    }

    private static String trimTrailing(String s) {
        int i = s.length();
        while (i > 0 && Character.isWhitespace(s.charAt(i - 1))) i--;
        return s.substring(0, i);
    }

    private static final List<java.util.Map.Entry<Pattern, java.util.function.BiFunction<Segment, java.util.regex.MatchResult, Segment>>> SEGMENT_TYPES = List.of(
            java.util.Map.entry(Pattern.compile("^(#+)\\s(.*)"), (s, m) -> new HeaderSegment(s, m.group(2), m.group(1).length())),
            java.util.Map.entry(Pattern.compile("(`)(.*?)\\1"), (s, m) -> new CodeSegment(s, m.group(2))),
            java.util.Map.entry(Pattern.compile("!\\[([^\\[]*)]\\(([^)]+)\\)"), (s, m) -> {
                try {
                    var renderer = Manual.imageFor(m.group(2));
                    if (renderer != null) return new RenderSegment(s, m.group(1), renderer);
                    return new TextSegment(s, "No renderer found for: " + m.group(2));
                } catch (Throwable t) {
                    return new TextSegment(s, t.getMessage() != null ? t.getMessage() : "Unknown error.");
                }
            }),
            java.util.Map.entry(Pattern.compile("\\[([^\\[]+)]\\(([^)]+)\\)"), (s, m) -> new LinkSegment(s, m.group(1), m.group(2))),
            java.util.Map.entry(Pattern.compile("(\\*\\*|__)(\\S.*?\\S|$)\\1"), (s, m) -> new BoldSegment(s, m.group(2))),
            java.util.Map.entry(Pattern.compile("([*_])(\\S.*?\\S|$)\\1"), (s, m) -> new ItalicSegment(s, m.group(2))),
            java.util.Map.entry(Pattern.compile("~~(\\S.*?\\S|$)~~"), (s, m) -> new StrikethroughSegment(s, m.group(1)))
    );
}
