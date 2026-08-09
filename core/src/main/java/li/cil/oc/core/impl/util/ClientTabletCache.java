package li.cil.oc.core.impl.util;

import li.cil.oc.core.impl.server.component.TabletHost;
import li.cil.oc.core.impl.server.component.TabletHostBase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ClientTabletCache extends TabletCache {
    public ClientTabletCache() {
        super(5L);
    }

    @SuppressWarnings("unused")
    @Override
    protected TabletHostBase createHost(ItemStack stack, Player player) {
        return new TabletHost(stack, player);
    }
}
