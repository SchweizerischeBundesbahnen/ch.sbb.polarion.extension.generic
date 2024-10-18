package ch.sbb.polarion.extension.generic.properties;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Properties;

public class ExtendedProperties extends Properties {

    private final HashMap<Object, String> defaultValues = new HashMap<>();
    private final HashMap<Object, String> descriptions = new HashMap<>();

    public ExtendedProperties() {
        super();
    }

    public ExtendedProperties(int size) {
        super(size);
    }

    @Override
    public synchronized Object setProperty(@NotNull String key, @NotNull String value) {
        return this.setProperty(key, value, null, null);
    }

    public synchronized Object setProperty(@NotNull String key, @NotNull String value, @Nullable String defaultValue, @Nullable String description) {
        return this.put(key, value, defaultValue, description);
    }

    public synchronized Object put(@NotNull Object key, @NotNull Object value, @Nullable String defaultValue, @Nullable String description) {
        defaultValues.put(key, defaultValue);
        descriptions.put(key, description);
        return super.put(key, value);
    }

    public synchronized @Nullable String getDescription(@NotNull Object key) {
        return descriptions.get(key);
    }

    public synchronized @Nullable String getDefaultValue(@NotNull Object key) {
        return defaultValues.get(key);
    }

    @Override
    public synchronized Object put(@NotNull Object key, @NotNull Object value) {
        return this.put(key, value, null, null);
    }

    @Override
    public synchronized boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public synchronized int hashCode() {
        return super.hashCode();
    }
}
