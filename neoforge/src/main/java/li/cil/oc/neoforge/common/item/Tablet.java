package li.cil.oc.neoforge.common.item;

import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.PacketType;
import li.cil.oc.core.impl.common.item.data.TabletData;
import li.cil.oc.core.impl.server.component.TabletHostBase;
import li.cil.oc.core.impl.util.BlockPosition;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.SimplePacketBuilder;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import li.cil.oc.neoforge.server.component.TabletHost;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class Tablet extends DelegateItem implements Chargeable {
    private static final int TimeToAnalyze = 10;
    private static BlockPosition currentBlockPos;
    private static Direction currentSide;
    private static float currentHitX;
    private static float currentHitY;
    private static float currentHitZ;

    public Tablet(Item.Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, Player player, @NotNull InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                var tabletHost = get(stack, player);
                tabletHost.stopMachine();
                var data = new TabletData(stack);
                if (data.tier > li.cil.oc.core.common.Tier.One) {
                    player.openMenu(OpenComputers.getContainerProvider(GuiType.TabletInner, level, 0, 0, 0), (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
                        buf.writeInt(GuiType.TabletInner);
                        buf.writeInt(0);
                        buf.writeInt(0);
                        buf.writeInt(0);
                    });
                }
            }
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public @NotNull InteractionResult onItemUseFirst(@NotNull ItemStack stack, @NotNull UseOnContext context) {
        currentBlockPos = new BlockPosition(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getLevel());
        currentSide = context.getClickedFace();
        currentHitX = (float) (context.getClickLocation().x - context.getClickedPos().getX());
        currentHitY = (float) (context.getClickLocation().y - context.getClickedPos().getY());
        currentHitZ = (float) (context.getClickLocation().z - context.getClickedPos().getZ());
        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult useOn(@NotNull UseOnContext context) {
        currentBlockPos = new BlockPosition(context.getClickedPos().getX(), context.getClickedPos().getY(), context.getClickedPos().getZ(), context.getLevel());
        currentSide = context.getClickedFace();
        currentHitX = (float) (context.getClickLocation().x - context.getClickedPos().getX());
        currentHitY = (float) (context.getClickLocation().y - context.getClickedPos().getY());
        currentHitZ = (float) (context.getClickLocation().z - context.getClickedPos().getZ());
        net.minecraft.world.entity.player.Player player = context.getPlayer();
        if (player != null) {
            player.startUsingItem(context.getHand());
        }
        return InteractionResult.SUCCESS;
    }


    @Override
    public void releaseUsing(@NotNull ItemStack stack, @NotNull Level level, @NotNull LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player)) return;
        int duration = getUseDuration(stack, entity) - timeLeft;
        boolean didAnalyze = duration >= TimeToAnalyze;
        if (didAnalyze) {
            if (!level.isClientSide) {
                var tabletHost = get(stack, player);
                tabletHost.machine();
                if (tabletHost.machine().isRunning() && currentBlockPos != null && currentSide != null) {
                    try {
                        var data = new net.minecraft.nbt.CompoundTag();
                        tabletHost.machine().node().sendToReachable("tablet.use", data, stack, player, currentBlockPos, currentSide, currentHitX, currentHitY, currentHitZ);
                        if (!data.isEmpty()) {
                            tabletHost.machine().signal("tablet_use", data);
                        }
                    } catch (Throwable t) {
                        OpenComputers.log().warn("Block analysis on tablet right click failed gloriously!", t);
                    }
                }
            }
        } else if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                var tabletHost = get(stack, player);
                tabletHost.startMachine();
                sendOpenTabletTerminal((ServerPlayer) player, tabletHost);
            }
        }
    }

    @Override
    public int getUseDuration(@NotNull ItemStack stack, @NotNull LivingEntity entity) {
        return 72000;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack stack) {
        return UseAnim.NONE;
    }

    @Override
    public net.minecraft.world.item.Rarity getRarity(ItemStack stack) {
        var data = new TabletData(stack);
        return li.cil.oc.core.impl.util.Rarity.byTier(data.tier);
    }

    @Override
    public int getMaxStackSize(@NotNull ItemStack stack) {
        return 1;
    }

    @Override
    public void inventoryTick(@NotNull ItemStack stack, @NotNull Level level, @NotNull Entity entity, int slot, boolean selected) {
        if (entity instanceof Player player) {
            var host = get(stack, player);
            if (host instanceof TabletHost th) {
                th.player(player);
            }
            host.update();
            if (level.isClientSide && player.isUsingItem() && player.getUseItem().getItem() == this && player.getTicksUsingItem() == TimeToAnalyze) {
                li.cil.oc.neoforge.util.Audio.play(player.getX(), player.getY() + 2, player.getZ(), ".");
            }
        }
    }

    @Override
    public boolean canCharge(ItemStack stack) {
        return true;
    }

    @Override
    public double charge(ItemStack stack, double amount, boolean simulate) {
        if (amount < 0) return amount;
        var data = new TabletData(stack);
        var charge = Math.min(data.maxEnergy - data.energy, amount);
        if (!simulate) {
            data.energy += charge;
            data.save(stack);
        }
        return amount - charge;
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        var data = new TabletData(stack);
        return data.maxEnergy > 0 ? (int) Math.round(13.0 * data.energy / data.maxEnergy) : 0;
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        var data = new TabletData(stack);
        var fill = data.maxEnergy > 0 ? data.energy / data.maxEnergy : 0;
        int r = (int) (255 - fill * 255);
        int g = (int) (fill * 255);
        return (r << 16) | (g << 8);
    }

    private static void sendOpenTabletTerminal(ServerPlayer player, TabletHostBase tabletHost) {
        try {
            for (ManagedEnvironment env : tabletHost.componentEnvironments()) {
                if (env instanceof TextBuffer buffer) {
                    var address = buffer.node() != null ? buffer.node().address() : "";
                    if (!address.isEmpty()) {
                        try (var pb = new SimplePacketBuilder(PacketType.OpenTabletTerminal)) {
                            pb.writeUTF(address);
                            pb.writeNBT(new net.minecraft.nbt.CompoundTag());
                            pb.sendToPlayer(player);
                        }
                    }
                    return;
                }
            }
        } catch (java.io.IOException e) {
            org.slf4j.LoggerFactory.getLogger("OpenComputers-Tablet").warn("Failed to send tablet terminal packet.", e);
        }
    }

    public static TabletHostBase get(ItemStack stack, Player player) {
        var cache = TabletCache.forSide(player.level().isClientSide());
        if (cache != null) {
            return cache.get(stack, player);
        }
        return new li.cil.oc.neoforge.server.component.TabletHost(stack, player);
    }
}
