package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@SuppressWarnings("rawtypes")
public enum NamedSettingsRegistry {
    INSTANCE;

    private final Set<GenericNamedSettings> settingsSet = new HashSet<>();

    @Getter
    private boolean scopeAgnostic;

    /**
     * Register a new setting. It's a good idea to call it before REST application initialization,
     * for example in the constructor of the {@link ch.sbb.polarion.extension.generic.rest.GenericRestApplication}'s subclass constructor.
     */
    public NamedSettingsRegistry register(List<GenericNamedSettings<?>> settingsList) {
        settingsSet.addAll(settingsList);
        return this;
    }

    public Set<GenericNamedSettings> getAll() {
        return settingsSet;
    }

    public GenericNamedSettings getByFeatureName(String featureName) {
        return settingsSet.stream()
                .filter(s -> s.getFeatureName().equals(featureName))
                .findFirst()
                .orElseThrow(() -> new ObjectNotFoundException("No settings found by featureName: " + featureName));
    }

    public NamedSettingsRegistry setScopeAgnostic(boolean scopeAgnostic) {
        this.scopeAgnostic = scopeAgnostic;
        return this;
    }
}
