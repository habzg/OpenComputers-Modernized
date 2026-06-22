package li.cil.oc.neoforge.event;

import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.api.internal.Agent;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

public class RobotUsedToolEventImpl extends Event implements RobotUsedToolEvent, ICancellableEvent {
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
    protected final ItemStack toolBeforeUse;
    protected final ItemStack toolAfterUse;
    protected double damageRate;

    public RobotUsedToolEventImpl(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
        this.agent = agent;
        this.toolBeforeUse = toolBeforeUse;
        this.toolAfterUse = toolAfterUse;
        this.damageRate = damageRate;
    }

    @Override
    public Agent agent() {
        return agent;
    }

    @Override
    public ItemStack toolBeforeUse() {
        return toolBeforeUse;
    }

    @Override
    public ItemStack toolAfterUse() {
        return toolAfterUse;
    }

    @Override
    public double getDamageRate() {
        return damageRate;
    }

    public static class ComputeDamageRate extends RobotUsedToolEventImpl implements RobotUsedToolEvent.ComputeDamageRate {
        public ComputeDamageRate(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }

        @Override
        public void setDamageRate(double damageRate) {
            this.damageRate = Math.clamp(damageRate, 0, 1);
        }
    }

    public static class ApplyDamageRate extends RobotUsedToolEventImpl implements RobotUsedToolEvent.ApplyDamageRate {
        public ApplyDamageRate(Agent agent, ItemStack toolBeforeUse, ItemStack toolAfterUse, double damageRate) {
            super(agent, toolBeforeUse, toolAfterUse, damageRate);
        }
    }
}
