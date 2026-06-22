package li.cil.oc.api.event;

import net.minecraft.world.item.ItemStack;

public interface RobotUsedToolEvent extends RobotEvent {
    /**
     * The tool that was used, before use.
     */
    @SuppressWarnings("unused")
    ItemStack toolBeforeUse();

    /**
     * The tool that was used, after use.
     */
    @SuppressWarnings("unused")
    ItemStack toolAfterUse();

    /**
     * The rate at which the used tool should lose durability, where one means
     * it loses durability at full speed, zero means it doesn't lose durability
     * at all.
     * <br>
     * This value is in an interval of [0, 1].
     */
    @SuppressWarnings("unused")
    double getDamageRate();

    /**
     * Fired when a robot used a tool and is about to apply the damage rate to
     * partially undo the durability loss. This step is used to compute the
     * rate at which the tool should lose durability, which is used by the
     * experience upgrade, for example.
     */
    interface ComputeDamageRate extends RobotUsedToolEvent {
        /**
         * Set the rate at which the tool actually gets damaged.
         * <br>
         * This will be clamped to an iterval of [0, 1].
         *
         * @param damageRate the new damage rate.
         */
        @SuppressWarnings("unused")
        void setDamageRate(double damageRate);
    }

    /**
     * Fired when a robot used a tool and the previously fired damage rate
     * computation returned a value smaller than one. The callbacks of this
     * method are responsible for applying the inverse damage the tool took.
     * The <code>toolAfterUse</code> item stack represents the actual tool, any
     * changes must be applied to that variable. The <code>toolBeforeUse</code>
     * item stack is passed for reference, to compute the actual amount of
     * durability that was lost. This may be required for tools where the
     * durability is stored in the item's NBT tag.
     */
    interface ApplyDamageRate extends RobotUsedToolEvent {
    }
}
