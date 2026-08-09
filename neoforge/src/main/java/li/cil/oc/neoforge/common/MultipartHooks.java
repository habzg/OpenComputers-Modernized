package li.cil.oc.neoforge.common;

import li.cil.oc.core.impl.util.Color;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.HitResult;
import com.mojang.blaze3d.vertex.VertexConsumer;

public final class MultipartHooks {
    public interface Access {
        boolean isCablePart(BlockEntity te);

        int getCableColor(BlockEntity te);

        boolean hasOCPart(BlockEntity te);

        boolean canConnect(BlockEntity te, Direction side);

        boolean isCableHit(BlockEntity te, HitResult hit);

        void setSprite(VertexConsumer consumer, TextureAtlasSprite sprite);
    }

    public static Access access;

    private MultipartHooks() {
    }

    public static boolean isCablePart(BlockEntity te) {
        return access != null && access.isCablePart(te);
    }

    public static int getCableColor(BlockEntity te) {
        return access != null ? access.getCableColor(te) : Color.LightGray;
    }

    public static boolean hasOCPart(BlockEntity te) {
        return access != null && access.hasOCPart(te);
    }

    public static boolean denyConnect(BlockEntity te, Direction side) {
        return access != null && !access.canConnect(te, side);
    }

    public static boolean isCableHit(BlockEntity te, HitResult hit) {
        return access != null && access.isCableHit(te, hit);
    }

    public static void setSprite(VertexConsumer consumer, TextureAtlasSprite sprite) {
        if (access != null) access.setSprite(consumer, sprite);
    }
}
