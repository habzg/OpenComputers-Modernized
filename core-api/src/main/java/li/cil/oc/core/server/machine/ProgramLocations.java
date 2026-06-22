package li.cil.oc.core.server.machine;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProgramLocations {
    private static final Map<String, Map<String, String>> architectureLocations = new LinkedHashMap<>();
    private static final Map<String, String> globalLocations = new LinkedHashMap<>();

    private ProgramLocations() {
    }

    public static void addMapping(String program, String label, String... architectures) {
        if (architectures == null || architectures.length == 0) {
            globalLocations.put(program, label);
        } else {
            for (String arch : architectures) {
                architectureLocations.computeIfAbsent(arch, k -> new LinkedHashMap<>()).put(program, label);
            }
        }
    }

    public static Map<String, String> getMappings(String architecture) {
        Map<String, String> result = new LinkedHashMap<>();
        Map<String, String> archMap = architectureLocations.get(architecture);
        if (archMap != null) result.putAll(archMap);
        result.putAll(globalLocations);
        return result;
    }
}
