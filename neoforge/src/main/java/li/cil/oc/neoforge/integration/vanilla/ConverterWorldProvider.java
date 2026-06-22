package li.cil.oc.neoforge.integration.vanilla;

import li.cil.oc.api.driver.Converter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("unused")
public final class ConverterWorldProvider implements Converter {
    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if (value instanceof ResourceKey<?> key && key.registryKey().equals(Registries.DIMENSION)) {
            output.put("id", UUID.nameUUIDFromBytes(
                    key.location().toString().getBytes(StandardCharsets.UTF_8)
            ).toString());
            output.put("name", key.location().toString());
        } else if (value instanceof DimensionType provider) {
            try {
                output.put("id", UUID.nameUUIDFromBytes(
                        java.security.MessageDigest.getInstance("MD5").digest(
                                (Long.toString(provider.fixedTime().orElse(0L)) + provider).getBytes(StandardCharsets.UTF_8)
                        )
                ).toString());
            } catch (java.security.NoSuchAlgorithmException ignored) {
            }
            output.put("name", provider.toString());
        }
    }
}
