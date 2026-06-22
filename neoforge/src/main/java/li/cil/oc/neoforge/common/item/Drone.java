package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.impl.common.item.data.DroneData;
import li.cil.oc.core.impl.util.Rarity;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BooleanSupplier;

public class Drone extends DelegateItem {
    private static BooleanSupplier extendedTooltips = () -> false;

    public static void setExtendedTooltips(BooleanSupplier supplier) {
        extendedTooltips = supplier;
    }

    public Drone(Properties properties) {
        super(properties);
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        var data = new DroneData(stack);
        return Rarity.byTier(data.tier);
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (extendedTooltips.getAsBoolean()) {
            var info = new DroneData(stack);
            for (var component : info.components) {
                if (component != null && !component.isEmpty()) {
                    tooltip.add(Component.literal("- " + component.getHoverName().getString()));
                }
            }
        }
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level world = context.getLevel();
        ItemStack stack = context.getItemInHand();
        if (!world.isClientSide) {
            var drone = new li.cil.oc.core.impl.common.entity.Drone(li.cil.oc.core.impl.common.entity.Drone.getEntityType(), world);
            if (player instanceof li.cil.oc.neoforge.server.agent.Player fakePlayer) {
                drone.ownerName = fakePlayer.agent.ownerName();
                drone.ownerUUID = fakePlayer.agent.ownerUUID();
            } else if (player != null) {
                drone.ownerName = player.getName().getString();
                drone.ownerUUID = player.getGameProfile().getId();
            }
            Vec3 clickLoc = context.getClickLocation();
            BlockPos pos = context.getClickedPos();
            Vec3 spawnPos = new Vec3(
                    pos.getX() + (clickLoc.x - pos.getX()) * 1.1,
                    pos.getY() + (clickLoc.y - pos.getY()) * 1.1,
                    pos.getZ() + (clickLoc.z - pos.getZ()) * 1.1
            );
            drone.initializeAfterPlacement(stack, player, spawnPos);
            world.addFreshEntity(drone);
        }
        stack.shrink(1);
        return InteractionResult.SUCCESS;
    }
}
