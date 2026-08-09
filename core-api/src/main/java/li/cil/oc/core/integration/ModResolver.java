package li.cil.oc.core.integration;

@FunctionalInterface
public interface ModResolver {
    boolean isModLoaded(String modId);
}
