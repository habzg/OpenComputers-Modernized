package li.cil.oc.core.impl.common.component;

import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.core.impl.common.PacketSender;
import li.cil.oc.core.util.ResultWrapper;


public class Screen extends TextBuffer {
    public final li.cil.oc.core.impl.common.blockentity.Screen screen;

    public Screen(li.cil.oc.core.impl.common.blockentity.Screen screen) {
        super(screen);
        this.screen = screen;
    }

    @Callback(direct = true, doc = "function():boolean -- Whether touch mode is inverted (sneak-activate opens GUI, instead of normal activate).")
    public Object[] isTouchModeInverted(Context computer, Arguments args) {
        return ResultWrapper.result(screen.invertTouchMode);
    }

    @Callback(doc = "function(value:boolean):boolean -- Sets whether to invert touch mode (sneak-activate opens GUI, instead of normal activate).")
    public Object[] setTouchModeInverted(Context computer, Arguments args) {
        boolean newValue = args.checkBoolean(0);
        boolean oldValue = screen.invertTouchMode;
        if (newValue != oldValue) {
            screen.invertTouchMode = newValue;
            PacketSender.sendScreenTouchMode(screen, newValue);
        }
        return ResultWrapper.result(oldValue);
    }
}
