package li.cil.oc.fabric.client.renderer;

import li.cil.oc.fabric.client.renderer.blockentity.HologramRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Minecraft;

@SuppressWarnings("unused")
public final class HologramDeferredEventHandler {

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(context -> {
            var bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
            HologramRenderer.drawPending(bufferSource);
        });
    }
}
