package li.cil.oc.core.impl.common.tileentity.traits;

public interface RotationAware {
    default net.minecraft.core.Direction toLocal(net.minecraft.core.Direction global) {
        return global;
    }

    default net.minecraft.core.Direction toGlobal(net.minecraft.core.Direction local) {
        return local;
    }
}
