package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.util.ContextUtils;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.shared.util.Pair;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.subterra.base.location.ILocation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class GenericNamedSettings<T extends SettingsModel> implements NamedSettings<T> {
    public static final String LOCATION_PREFIX = ".polarion/extensions";
    public static final String SETTINGS_FILE_EXTENSION = ".settings";
    public static final String DEFAULT_SCOPE = "";
    private static final Map<ILocation, Pair<String, Set<SettingName>>> SETTING_NAMES_CACHE = new HashMap<>();
    private static final Logger logger = Logger.getLogger(GenericNamedSettings.class);
    private static final String LOCATION_MASK = "%s/%s%s";

    private final Comparator<SettingName> namesComparator = (o1, o2) -> {
        if (DEFAULT_NAME.equals(o1.getName())) {
            return -1;
        } else if (DEFAULT_NAME.equals(o2.getName())) {
            return 1;
        } else {
            return o1.getName().compareTo(o2.getName());
        }
    };

    @Getter
    private final String featureName;
    @Getter
    private final String settingsFolder;
    @Getter
    private final SettingsService settingsService;

    protected GenericNamedSettings(String featureName) {
        this(featureName, new SettingsService());
    }

    protected GenericNamedSettings(String featureName, SettingsService settingsService) {
        this.featureName = featureName;
        settingsFolder = String.format("%s/%s/%s", LOCATION_PREFIX, ContextUtils.getContext().getExtensionContext(), featureName);
        this.settingsService = settingsService;
    }

    @SuppressWarnings("unused")
    public T load(String project, SettingId id) {
        try {
            return read(ScopeUtils.getScopeFromProject(project), id, null);
        } catch (Exception e) {
            logger.error("Cannot load saved model for project '" + project + "': " + e.getMessage(), e);
            return fromString("");
        }
    }

    public Collection<SettingName> readNames(@NotNull String scope) {

        Set<SettingName> names = new HashSet<>(getSettingNamesFromLocation(scope));

        if (!DEFAULT_SCOPE.equals(scope)) {
            // If requested scope is project - add names from global scope.
            // As names collection is Set, names from global scope will only then be added if differ from project scope names
            names.addAll(getSettingNamesFromLocation(DEFAULT_SCOPE));
        }

        if (names.isEmpty()) {
            // If there are no settings persisted - try to create default one
            try {
                createDefaultSettings();
            } catch (Exception e) { // If it's not possible to create the settings in read only transaction, so just ignore it
                logger.warn("Cannot create the settings in read only transaction, creation will be skipped: " + e.getMessage(), e);
            }

            names.add(SettingName.builder().id(DEFAULT_NAME).name(DEFAULT_NAME).scope(DEFAULT_SCOPE).build());
        }

        return names.stream().sorted(namesComparator).toList();
    }

    @Override
    public T read(@NotNull String scope, @NotNull SettingId id, String revisionName) {
        String settingPath = String.format(LOCATION_MASK, settingsFolder, getFileName(scope, false, id), SETTINGS_FILE_EXTENSION);
        final ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        String value = settingsService.read(location, revisionName);
        if (value == null) {
            if (!StringUtils.isEmpty(revisionName)) {
                return null;
            }
            ILocation defaultLocation = ScopeUtils.getDefaultLocation().append(settingPath);
            if (!settingsService.exists(defaultLocation)) {
                try {
                    return createDefaultSettings();
                } catch (Exception e) {
                    logger.warn("Cannot create the settings in read only transaction, default values will be used: " + e.getMessage(), e);
                    return defaultValues();
                }
            }
            value = settingsService.read(defaultLocation, null);
        }
        return fromString(value);
    }

    @Override
    public T save(@NotNull String scope, @NotNull SettingId id, @NotNull T what) {
        String settingPath = String.format(LOCATION_MASK, settingsFolder, getFileName(scope, true, id), SETTINGS_FILE_EXTENSION);
        final ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        what.setBundleTimestamp(currentBundleTimestamp());
        beforeSave(what);
        String content = toString(what);
        settingsService.save(location, content);
        afterSave(what);
        return what;
    }

    @SuppressWarnings("unused")
    public void beforeSave(@NotNull T what) {
        // To be overriden if needed
    }

    @SuppressWarnings("unused")
    public void afterSave(@NotNull T what) {
        // To be overriden if needed
    }

    @Override
    public void delete(@NotNull String scope, @NotNull SettingId id) {
        String settingPath = String.format(LOCATION_MASK, settingsFolder, getFileName(scope, true, id), SETTINGS_FILE_EXTENSION);
        final ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        settingsService.delete(location);
    }

    public @NotNull List<Revision> listRevisions(String scope, SettingId id) {
        String settingPath = String.format(LOCATION_MASK, settingsFolder, getFileName(scope, false, id), SETTINGS_FILE_EXTENSION);
        final ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        final String projectId = ScopeUtils.getProjectFromScope(scope);
        return settingsService.listRevisions(location, projectId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equals(getFeatureName(), ((GenericNamedSettings<?>) o).getFeatureName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFeatureName());
    }

    public String getIdByName(String scope, boolean limitToScope, String settingName) {
        return readNames(scope).stream().filter(
                n -> Objects.equals(n.getName(), settingName) && (!limitToScope || Objects.equals(n.getScope(), scope))
        ).map(SettingName::getId).findFirst().orElse(null);
    }

    private String getFileName(String scope, boolean limitToScope, SettingId settingId) {
        return settingId.useName ? getIdByName(scope, limitToScope, settingId.identifier) : settingId.identifier;
    }

    private Set<SettingName> getSettingNamesFromLocation(String scope) {
        ILocation targetLocation = ScopeUtils.getContextLocation(scope).append(settingsFolder);
        String lastRevision = settingsService.getLastRevision(targetLocation);
        if (lastRevision == null) { //folder doesn't exist
            return new HashSet<>();
        }
        Pair<String, Set<SettingName>> cached = SETTING_NAMES_CACHE.get(targetLocation);
        if (cached == null || !Objects.equals(lastRevision, cached.left())) {
            Set<SettingName> settingNames = settingsService.getPersistedSettingFileNames(targetLocation).stream()
                    .map(fileName -> SettingName.builder()
                            .id(fileName)
                            //Legacy items (which were created before 'id' introduction) don't have 'name' model field - in this case we reuse their file names.
                            //Proper 'name' field value will appear only after explicit settings save by user.
                            .name(Optional.ofNullable(read(scope, SettingId.fromId(fileName), null).getName()).orElse(fileName))
                            .scope(scope)
                            .build())
                    .collect(Collectors.toSet());
            cached = Pair.of(lastRevision, settingNames);
            SETTING_NAMES_CACHE.put(targetLocation, cached);
        }
        return cached.right();
    }

    private T createDefaultSettings() {
        T defaultModel = defaultValues();
        defaultModel.setName(DEFAULT_NAME);
        return save(DEFAULT_SCOPE, SettingId.fromId(DEFAULT_NAME), defaultModel);
    }
}
