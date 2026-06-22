package li.cil.oc.core.client.renderer.tileentity;

public final class RenderUtil {
    public static boolean shouldShowErrorLight(int hash) {
        long time = System.currentTimeMillis() + hash;
        long timeSlice = time / 500;
        return timeSlice % 2 == 0;
    }
}
