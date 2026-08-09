package li.cil.oc.neoforge.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import li.cil.oc.neoforge.client.renderer.blockentity.RobotRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import org.joml.Quaternionf;

public final class PetRenderer {
    public static final Set<String> hidden = new HashSet<>();
    private static final Map<String, double[]> entitledPlayers = new HashMap<>();
    private static final com.google.common.cache.Cache<Entity, PetLocation> petLocations =
            com.google.common.cache.CacheBuilder.newBuilder()
                    .expireAfterAccess(5, TimeUnit.SECONDS)
                    .build();
    public static boolean isInitialized = false;

    static {
        entitledPlayers.put("9f1f262f-0d68-4e13-9161-9eeaf4a0a1a8", new double[]{0.3, 0.9, 0.6}); // Sangar
        entitledPlayers.put("18f8bed4-f027-44af-8947-6a3a2317645a", new double[]{1.0, 0.0, 0.0}); // Jodarion
        entitledPlayers.put("36123742-2cf6-4cfc-8b65-278581b3caeb", new double[]{0.5, 0.7, 1.0}); // DaKaTotal
        entitledPlayers.put("2c0c214b-96f4-4565-b513-de90d5fbc977", new double[]{1.0, 0.0, 0.0}); // MichiRavencroft
        entitledPlayers.put("f3ba6ec8-c280-4950-bb08-1fcb2eab3a9c", new double[]{0.18, 0.95, 0.922}); // Vexatos
        entitledPlayers.put("9d636bdd-b9f4-4b80-b9ce-586ca04bd4f3", new double[]{0.8, 0.77, 0.75}); // StoneNomad
        entitledPlayers.put("23c7ed71-fb13-4abe-abe7-f355e1de6e62", new double[]{0.3, 0.3, 1.0}); // LizzyTheSiren
        entitledPlayers.put("076541f1-f10a-46de-a127-dfab8adfbb75", new double[]{0.2, 1.0, 0.1}); // vifino
        entitledPlayers.put("e7e90198-0ccf-4662-a827-192ec8f4419d", new double[]{0.0, 0.2, 0.6}); // Izaya
        entitledPlayers.put("f514ee69-7bbb-4e46-9e94-d8176324cec2", new double[]{0.098, 0.471, 0.784}); // Wobbo
        entitledPlayers.put("f812c043-78ba-4324-82ae-e8f05c52ae6e", new double[]{0.1, 0.8, 0.5}); // payonel
        entitledPlayers.put("380df991-f603-344c-a090-369bad2a924a", new double[]{0.96, 0.66, 0.72, 0.357, 0.808, 0.980, 0xFFFFFF}); // Dev
        entitledPlayers.put("a01e3843-e521-3998-958a-f459800e4d11", new double[]{1.0, 0.604, 0.337, 0.827, 0.384, 0.643, 0xFFFFFF}); // Player
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void onPlayerRender(RenderPlayerEvent.Pre e) {
        String uuid = e.getEntity().getUUID().toString();
        if (hidden.contains(uuid) || !entitledPlayers.containsKey(uuid)) return;
        double[] rendering = entitledPlayers.get(uuid);

        var player = e.getEntity();
        var worldTime = player.level().getGameTime();
        int timeJitter = player.hashCode() ^ 0xFF;
        double offset = timeJitter + worldTime / 20.0;
        float hover = (float) (Math.sin(timeJitter + (worldTime + e.getPartialTick()) / 20.0) * 0.03);

        PetLocation location;
        try {
            location = petLocations.get(player, () -> new PetLocation(player));
        } catch (ExecutionException ex) {
            throw new RuntimeException(ex);
        }

        var pose = e.getPoseStack();
        var buffer = e.getMultiBufferSource();
        int packedLight = e.getPackedLight();

        pose.pushPose();

        pose.translate(0, player.getEyeHeight(), 0);

        location.applyTransforms(pose, e.getPartialTick());
        float petScale = player.getScale() * 0.3f;
        pose.scale(petScale, petScale, petScale);
        pose.translate(0, hover, 0);

        if (rendering != null) {
            if (rendering.length > 3) {
                int petLightColor = rendering.length > 6 ? (int) rendering[6] : 0xF23030;
                RobotRenderer.renderChassis(pose, buffer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, offset, true, petLightColor,
                    (float) rendering[0], (float) rendering[1], (float) rendering[2],
                    (float) rendering[3], (float) rendering[4], (float) rendering[5]);
            } else {
                RobotRenderer.setModelTint((float) rendering[0], (float) rendering[1], (float) rendering[2]);
                RobotRenderer.renderChassis(pose, buffer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, offset, true, 0xF23030);
            }
        } else {
            RobotRenderer.renderChassis(pose, buffer, packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, offset, true, 0xF23030);
        }

        pose.popPose();
    }

    @SubscribeEvent
    @SuppressWarnings("unused")
    public static void tickStart(ClientTickEvent.Pre e) {
        petLocations.cleanUp();
        for (var pet : petLocations.asMap().values()) pet.update();
    }

    public static class PetLocation {
        public final Entity owner;
        public double x, y, z;
        public float yaw;
        public double lastX, lastY, lastZ;
        public float lastYaw;

        public PetLocation(Entity owner) {
            this.owner = owner;
            this.yaw = owner.getYRot();
            this.lastYaw = yaw;
        }

        public void update() {
            double dx = owner.xo - owner.getX();
            double dy = owner.yo - owner.getY();
            double dz = owner.zo - owner.getZ();
            float dYaw = owner.getYRot() - yaw;
            lastX = x;
            lastY = y;
            lastZ = z;
            lastYaw = yaw;
            x += dx;
            y += dy;
            z += dz;
            x *= 0.05;
            y *= 0.05;
            z *= 0.05;
            yaw += dYaw * 0.2f;
        }

        public void applyTransforms(PoseStack pose, float dt) {
            double ix = lastX + (x - lastX) * dt;
            double iy = lastY + (y - lastY) * dt;
            double iz = lastZ + (z - lastZ) * dt;
            float iYaw = lastYaw + (yaw - lastYaw) * dt;
            pose.translate(ix, iy, iz);
            if (!isForInventory()) {
                pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-iYaw)));
            } else {
                pose.mulPose(new Quaternionf().rotateY((float) Math.toRadians(-owner.getYRot())));
            }
            float s = ((net.minecraft.world.entity.LivingEntity) owner).getScale();
            pose.translate(0.3 * s, -0.1 * s, -0.2 * s);
        }

        private boolean isForInventory() {
            return Minecraft.getInstance().screen != null && owner == Minecraft.getInstance().player;
        }
    }
}
