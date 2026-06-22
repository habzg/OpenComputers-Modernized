package li.cil.oc.core.impl.client.renderer.font;

public interface IFontRenderer {
    void generateChars(int[] chars);

    int charRenderWidth();

    int charRenderHeight();
}
