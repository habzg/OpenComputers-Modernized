package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.impl.client.renderer.markdown.Document;
import net.minecraft.client.gui.Font;

import java.util.Set;

public abstract class BasicTextSegment extends Segment {
    protected static final Set<Character> BREAKS = Set.of(' ', '.', ',', ':', ';', '!', '?', '_', '=', '-', '+', '*', '/', '\\');
    private static final Set<String> LISTS = Set.of("- ", "* ");

    @SuppressWarnings("unused")
    public BasicTextSegment(Segment parent) {
        super(parent);
    }

    @SuppressWarnings("unused")
    public abstract String text();

    @SuppressWarnings("unused")
    protected int lineHeight(Font renderer) {
        return Document.lineHeight(renderer);
    }

    protected int stringWidth(String s, Font renderer) {
        return renderer.width(s);
    }

    protected int maxChars(String s, int maxWidth, int maxLineWidth, Font renderer) {
        int pos = -1;
        int lastBreak = -1;
        int fullWidth = stringWidth(s, renderer);
        while (pos < s.length()) {
            pos++;
            int width = stringWidth(s.substring(0, pos), renderer);
            boolean exceedsLineLength = width >= maxWidth;
            if (exceedsLineLength) {
                boolean mayUseFullLine = maxWidth == maxLineWidth;
                boolean canFitInLine = fullWidth <= maxLineWidth;
                boolean matchesFullLine = fullWidth == maxLineWidth;
                if (lastBreak >= 0) {
                    return lastBreak + 1;
                }
                if (mayUseFullLine && matchesFullLine) {
                    return s.length();
                }
                if (canFitInLine && !mayUseFullLine) {
                    return 0;
                }
                return Math.max(0, pos - 1);
            }
            if (pos < s.length() && BREAKS.contains(s.charAt(pos))) lastBreak = pos;
        }
        return pos;
    }

    protected int computeWrapIndent(Font renderer) {
        String prefix = computeRootPrefix();
        if (prefix != null) return stringWidth(prefix, renderer);
        return 0;
    }

    @SuppressWarnings("SameReturnValue")
    protected boolean ignoreLeadingWhitespace() {
        return true;
    }

    private String computeRootPrefix() {
        Segment root = root();
        if (root instanceof TextSegment ts) {
            String t = ts.text();
            if (t.length() >= 2) {
                String prefix = t.substring(0, 2);
                if (LISTS.contains(prefix)) return prefix;
            }
        }
        return null;
    }
}
