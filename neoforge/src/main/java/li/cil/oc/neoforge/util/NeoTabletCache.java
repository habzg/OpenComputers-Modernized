package li.cil.oc.neoforge.util;

import li.cil.oc.core.impl.server.component.TabletHostBase;
import li.cil.oc.core.impl.util.TabletCache;
import li.cil.oc.neoforge.server.component.TabletHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class NeoTabletCache extends TabletCache {
    public NeoTabletCache() {
        super(10L);
    }

    @Override
    protected TabletHostBase createHost(ItemStack stack, Player player) {
        return new TabletHost(stack, player);
    }
}
