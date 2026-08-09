package li.cil.oc.fabric.integration;

public interface ModProxy extends li.cil.oc.core.integration.ModProxy {
    @Override
    li.cil.oc.core.integration.Mod getMod();

    @Override
    void initialize();
}
