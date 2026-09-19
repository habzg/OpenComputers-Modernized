package li.cil.oc.core.impl.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.inventory.InventoryMenu;

// Credit: https://github.com/akki697222/OpenComputers-CE/blob/dev-MC1.20/src/main/scala/li/cil/oc/client/renderer/RenderTypes.java (MIT)

public class RenderHelper {
  private static class ConcreteRenderType extends RenderType {
    ConcreteRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setupState, Runnable clearState) {
      super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setupState, clearState);
    }
  }

  public static final RenderType BLOCK_OVERLAY = new ConcreteRenderType(
    "opencomputers:overlay_block",
    DefaultVertexFormat.POSITION_TEX, VertexFormat.Mode.QUADS, 1024, false, false,
    () -> {
      RenderSystem.disableBlend();
      RenderSystem.enableDepthTest();
      RenderSystem.depthFunc(515);
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.setShader(GameRenderer::getPositionTexShader);
      RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
    },
    () -> {
    });
}
