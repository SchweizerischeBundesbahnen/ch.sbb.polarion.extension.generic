package ch.sbb.polarion.extension.generic.properties;

import lombok.EqualsAndHashCode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EqualsAndHashCode
public class ConfigurationProperties {

    public record Value(@NotNull String value, @Nullable String defaultValue, @Nullable String description) {
    }

    private final ConcurrentHashMap<String, Value> properties;

    public ConfigurationProperties() {
        this(8);
    }

    public ConfigurationProperties(int size) {
        properties = new ConcurrentHashMap<>(size);
    }

    public void setProperty(@NotNull String key, @NotNull ConfigurationProperties.Value value) {
        properties.put(key, value);
    }

    public @NotNull ConfigurationProperties.Value getProperty(@NotNull String key) {
        return properties.get(key);
    }

    public Set<String> keySet() {
        return properties.keySet();
    }

    public int size() {
        return properties.size();
    }

    public boolean isEmpty() {
        return properties.isEmpty();
    }

}
