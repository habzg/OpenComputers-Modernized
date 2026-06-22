package li.cil.oc.neoforge.common.item;

import li.cil.oc.core.common.GuiType;
import li.cil.oc.core.common.item.traits.ItemTier;
import li.cil.oc.core.impl.Settings;
import li.cil.oc.neoforge.OpenComputers;
import li.cil.oc.neoforge.common.item.traits.DelegateItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class UpgradeDatabase extends DelegateItem implements ItemTier {

    private final int tier;

    public UpgradeDatabase(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
    }

    public int tier() {
        return tier;
    }

    @Override
    protected List<Object> tooltipData() {
        return List.of(Settings.get().databaseEntriesPerTier[tier]);
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return li.cil.oc.core.impl.util.Rarity.byTier(tier);
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isShiftKeyDown()) {
            if (!world.isClientSide) {
                player.openMenu(OpenComputers.getContainerProvider(GuiType.Database, world, 0, 0, 0),
                        (net.minecraft.network.RegistryFriendlyByteBuf buf) -> {
                            buf.writeInt(GuiType.Database);
                            buf.writeInt(0);
                            buf.writeInt(0);
                            buf.writeInt(0);
                        });
            }
            player.swing(hand);
        } else {
            CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
            if (cd != null && !cd.isEmpty() && cd.copyTag().contains(Settings.namespace + "items")) {
                stack.set(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                player.swing(hand);
            }
        }
        return InteractionResultHolder.success(stack);
    }
}
