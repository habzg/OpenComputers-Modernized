package li.cil.oc.core.impl.common.item;

import java.util.List;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import li.cil.oc.core.impl.common.item.data.DroneData;
import net.minecraft.core.BlockPos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

public class Drone extends DelegateItem {
    public record PlayerOwner(String name, UUID uuid) {}

    private static BooleanSupplier extendedTooltips = () -> false;
    private static Function<Player, PlayerOwner> agentOwnerProvider = (p) -> null;

    public static void setExtendedTooltips(BooleanSupplier supplier) {
        extendedTooltips = supplier;
    }

    public static void setAgentOwnerProvider(Function<Player, PlayerOwner> provider) {
        agentOwnerProvider = provider;
    }

    public Drone(Properties properties) {
        super(properties);
    }

    @Override
    public void tooltipExtended(ItemStack stack, List<Component> tooltip) {
        if (extendedTooltips.getAsBoolean()) {
            var info = new DroneData(stack);
            var components = info.components.stream().filter(c -> c != null && !c.isEmpty()).toList();
            if (!components.isEmpty()) {
                tooltip.addAll(li.cil.oc.core.impl.util.Tooltip.extended("server.Components"));
                for (var component : components) {
                    tooltip.add(Component.literal("- ").append(component.getHoverName()));
                }
            }
        } else {
            var shiftKey = Component.translatable("key.keyboard.left.shift").getString();
            var tooLong = Component.translatable("tooltip.opencomputers.toolong", shiftKey).getString();
            if (tooltip.stream().noneMatch(line -> line.getString().equals(tooLong))) {
                tooltip.add(Component.literal(tooLong).withStyle(ChatFormatting.GRAY));
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
            var owner = agentOwnerProvider.apply(player);
            if (owner != null) {
                drone.ownerName = owner.name();
                drone.ownerUUID = owner.uuid();
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
