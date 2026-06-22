package li.cil.oc.core.impl.client.renderer.markdown.segment;

import li.cil.oc.core.client.renderer.markdown.MarkupFormat;

public class CodeSegment extends TextSegment {
    private static final int CODE_COLOR = 0xBFCCFF;

    @SuppressWarnings("unused")
    public CodeSegment(Segment parent, String text) {
        super(parent, text);
    }

    @Override
    protected int color() {
        return CODE_COLOR;
    }

    @Override
    public String toString(MarkupFormat format) {
        return switch (format) {
            case Markdown -> "`" + text() + "`";
        };
    }
}
