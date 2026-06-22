package li.cil.oc.neoforge.integration.mekanism.gas;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.core.impl.Settings;
import mekanism.api.chemical.ChemicalStack;

import java.util.Map;

@SuppressWarnings("unused")
public final class ConverterChemicalStack implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ChemicalStack stack) {
            var key = stack.getChemicalHolder().getKey();
            if (key != null) {
                if (Settings.get().insertIdsInConverters) {
                    output.put("id", key.location().toString());
                }
                output.put("name", key.location().getPath());
                output.put("label", stack.getChemical().getTextComponent().getString());
            }
            output.put("amount", stack.getAmount());
        }
    }
}
