package li.cil.oc.core.impl.common.nanomachines.provider;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.prefab.AbstractProvider;
import net.minecraft.world.entity.player.Player;

public abstract class ScalaProvider extends AbstractProvider {
    protected ScalaProvider(String id) {
        super(id);
    }

    protected abstract Iterable<Behavior> createScalaBehaviors(Player player);

    @Override
    public Iterable<Behavior> createBehaviors(Player player) {
        return createScalaBehaviors(player);
    }
}
