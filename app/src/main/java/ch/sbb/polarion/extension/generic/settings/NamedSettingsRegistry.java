package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registry of named settings. It is safe for concurrent use: each change atomically replaces an immutable snapshot,
 * so a reader never sees a partial change.
 */
@SuppressWarnings("rawtypes")
public enum NamedSettingsRegistry {
    INSTANCE;

    private final AtomicReference<Set<GenericNamedSettings>> settingsSnapshot = new AtomicReference<>(Set.of());

    /**
     * Register a new setting. It's a good idea to call it before REST application initialization,
     * for example in the constructor of the {@link ch.sbb.polarion.extension.generic.rest.GenericRestApplication}'s subclass constructor.
     */
    public NamedSettingsRegistry register(List<GenericNamedSettings<?>> settingsList) {
        settingsSnapshot.updateAndGet(current -> {
            Set<GenericNamedSettings> updated = new HashSet<>(current);
            updated.addAll(settingsList);
            return Collections.unmodifiableSet(updated);
        });
        return this;
    }

    /**
     * Replaces all registered settings with the given ones. Unlike {@link #register(List)}, it also replaces
     * a registered instance which is equal to a new one (same class and feature name).
     */
    public NamedSettingsRegistry replaceAll(List<? extends GenericNamedSettings<?>> settingsList) {
        settingsSnapshot.set(Collections.unmodifiableSet(new HashSet<>(settingsList)));
        return this;
    }

    /**
     * Removes all registered settings.
     */
    public void clear() {
        settingsSnapshot.set(Set.of());
    }

    /**
     * Returns a read-only snapshot of all registered settings. The snapshot does not reflect later changes of the registry.
     * Use {@link #register(List)}, {@link #replaceAll(List)} or {@link #clear()} to change the registry.
     */
    public Set<GenericNamedSettings> getAll() {
        return settingsSnapshot.get();
    }

    public GenericNamedSettings getByFeatureName(String featureName) {
        return settingsSnapshot.get().stream()
                .filter(s -> s.getFeatureName().equals(featureName))
                .findFirst()
                .orElseThrow(() -> new ObjectNotFoundException("No settings found by featureName: " + featureName));
    }
}
