package li.cil.oc.neoforge.integration;

public interface ModProxy extends li.cil.oc.core.integration.ModProxy {
    @Override
    Mod getMod();

    @Override
    void initialize();
}
