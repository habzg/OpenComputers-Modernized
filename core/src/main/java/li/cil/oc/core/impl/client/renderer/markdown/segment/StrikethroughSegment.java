package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;
import net.minecraft.network.chat.Style;

public class StrikethroughSegment extends TextSegment {
    @SuppressWarnings("unused")
    public StrikethroughSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    protected Style style() {
        return Style.EMPTY.withStrikethrough(true);
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "~~" + text() + "~~";
        };
    }
}
