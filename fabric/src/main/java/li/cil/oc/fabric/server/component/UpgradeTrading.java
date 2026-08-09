package li.cil.oc.fabric.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeTradingBase;
import net.minecraft.world.item.trading.Merchant;
import org.jetbrains.annotations.NotNull;

public class UpgradeTrading extends UpgradeTradingBase {
    @SuppressWarnings("unused")
    public UpgradeTrading(EnvironmentHost host) {
        super(host);
    }

    @SuppressWarnings("unused")
    @Override
    protected @NotNull Object createTradeObject(@NotNull Merchant merchant, int recipeID, int merchantID) {
        return new Trade(this, merchant, recipeID, merchantID);
    }
}
