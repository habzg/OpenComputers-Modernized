package li.cil.oc.fabric.server.component;

import li.cil.oc.core.impl.server.component.TradeBase;
import li.cil.oc.core.impl.server.component.TradeInfoBase;
import li.cil.oc.fabric.common.EventHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.trading.Merchant;
import org.jetbrains.annotations.NotNull;

public class Trade extends TradeBase {
    @SuppressWarnings("unused")
    public Trade() {
        this(new TradeInfo());
    }

    public Trade(UpgradeTrading upgrade, Merchant merchant, int recipeID, int merchantID) {
        this(new TradeInfo(upgrade.host, merchant, recipeID, merchantID));
    }

    public Trade(TradeInfo info) {
        super(info);
    }

    @SuppressWarnings("unused")
    @Override
    protected void deferredLoad(@NotNull CompoundTag nbt, HolderLookup.@NotNull Provider provider, @NotNull TradeInfoBase target) {
        EventHandler.scheduleServer(() -> target.load(nbt, provider));
    }
}
