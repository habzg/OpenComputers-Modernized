package li.cil.oc.core.integration;

public interface Mod {
    String id();

    boolean isModAvailable();

    default boolean isAvailable() {
        return isModAvailable();
    }
}
