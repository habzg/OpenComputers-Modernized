package li.cil.oc.core.impl.client.renderer.font;

import li.cil.oc.core.impl.util.TextBuffer;

public interface TextBufferRenderData {
    boolean dirty();

    void setDirty(boolean value);

    TextBuffer data();

    int[] viewport();
}
