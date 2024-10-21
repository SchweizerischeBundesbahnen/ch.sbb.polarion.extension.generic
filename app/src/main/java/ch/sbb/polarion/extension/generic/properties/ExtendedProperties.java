package ch.sbb.polarion.extension.generic.properties;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Set;

@EqualsAndHashCode
public class ExtendedProperties {

    public record Value(@NotNull String value, @Nullable String defaultValue, @Nullable String description) {
    }

    private final HashMap<String, Value> properties;

    public ExtendedProperties() {
        this(8);
    }

    public ExtendedProperties(int size) {
        properties = new HashMap<>(size);
    }

    public void setProperty(@NotNull String key, @NotNull ExtendedProperties.Value value) {
        properties.put(key, value);
    }

    public @NotNull ExtendedProperties.Value getProperty(@NotNull String key) {
        return properties.get(key);
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public int size() {
        return properties.size();
    }

}
