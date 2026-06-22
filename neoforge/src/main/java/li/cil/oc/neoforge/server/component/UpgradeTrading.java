package li.cil.oc.neoforge.server.component;

import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.core.impl.server.component.UpgradeTradingBase;
import net.minecraft.world.item.trading.Merchant;
import org.jetbrains.annotations.NotNull;

public class UpgradeTrading extends UpgradeTradingBase {
    public UpgradeTrading(EnvironmentHost host) {
        super(host);
    }

    @Override
    protected @NotNull Object createTradeObject(@NotNull Merchant merchant, int recipeID, int merchantID) {
        return new Trade(this, merchant, recipeID, merchantID);
    }
}
