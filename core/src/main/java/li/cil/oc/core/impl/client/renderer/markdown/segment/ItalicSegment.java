package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import net.minecraft.network.chat.Style;

public class ItalicSegment extends TextSegment {
    @SuppressWarnings("unused")
    public ItalicSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    protected Style style() {
        return Style.EMPTY.withItalic(true);
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "*" + text() + "*";
        };
    }
}
