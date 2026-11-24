package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("rawtypes")
public enum NamedSettingsRegistry {
    INSTANCE;

    private final Set<GenericNamedSettings> settingsSet = new HashSet<>();

    /**
     * Register a new setting. It's a good idea to call it before REST application initialization,
     * for example in the constructor of the {@link ch.sbb.polarion.extension.generic.rest.GenericRestApplication}'s subclass constructor.
     */
    public NamedSettingsRegistry register(List<GenericNamedSettings<?>> settingsList) {
        settingsSet.addAll(settingsList);
        return this;
    }

    /**
     * Replaces all registered settings with the given ones. Unlike {@link #register(List)}, it also replaces
     * a registered instance which is equal to a new one (same class and feature name).
     */
    public NamedSettingsRegistry replaceAll(List<? extends GenericNamedSettings<?>> settingsList) {
        settingsSet.clear();
        settingsSet.addAll(settingsList);
        return this;
    }

    /**
     * Removes all registered settings.
     */
    public void clear() {
        settingsSet.clear();
    }

    /**
     * Returns all registered settings. Do not modify the returned set, it becomes read-only in version 17.0.0.
     * Use {@link #register(List)}, {@link #replaceAll(List)} or {@link #clear()} to change the registry.
     */
    public Set<GenericNamedSettings> getAll() {
        return settingsSet;
    }

    public GenericNamedSettings getByFeatureName(String featureName) {
        return settingsSet.stream()
                .filter(s -> s.getFeatureName().equals(featureName))
                .findFirst()
                .orElseThrow(() -> new ObjectNotFoundException("No settings found by featureName: " + featureName));
    }
}
