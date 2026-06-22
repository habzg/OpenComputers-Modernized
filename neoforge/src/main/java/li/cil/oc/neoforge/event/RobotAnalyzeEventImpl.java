package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotAnalyzeEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotAnalyzeEventImpl extends Event implements RobotAnalyzeEvent, ICancellableEvent {
    @SuppressWarnings("NonExtendableApiUsage")
    @Override
    public boolean isCanceled() {
        return ICancellableEvent.super.isCanceled();
    }

    @Override
    public void setCanceled(boolean c) {
        ICancellableEvent.super.setCanceled(c);
    }

    protected final Agent agent;
    protected final Player player;

    public RobotAnalyzeEventImpl(Agent agent, Player player) {
        this.agent = agent;
        this.player = player;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public Player player() {
        return player;
    }
}
