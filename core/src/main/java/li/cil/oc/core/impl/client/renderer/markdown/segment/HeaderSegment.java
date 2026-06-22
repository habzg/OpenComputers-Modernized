package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import net.minecraft.network.chat.Style;

public class HeaderSegment extends TextSegment {
    public final int level;

    @SuppressWarnings("unused")
    public HeaderSegment(Segment parent, String text, int level) {
        super(parent, text);
        this.level = level;
    }

    @Override
    protected float scale() {
        return Math.max(2, 5 - level) / 2f;
    }

    @Override
    protected Style style() {
        return Style.EMPTY.withUnderlined(true);
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "#".repeat(level) + " " + text();
        };
    }
}
