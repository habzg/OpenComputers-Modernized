package li.cil.oc.core.impl.common.blockentity.traits.power;

public final class AE2Power {
    private static AE2PowerDelegate delegate;

    private AE2Power() {
    }

    public static void setDelegate(AE2PowerDelegate d) {
        delegate = d;
    }

    public static AE2PowerDelegate delegate() {
        return delegate;
    }
}
