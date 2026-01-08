package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.exception.DuplicateSettingNameException;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.util.ContextUtils;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.alm.shared.util.Pair;
import com.polarion.core.util.StringUtils;
import com.polarion.subterra.base.location.ILocation;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
    public @NotNull T load(@Nullable String project, @NotNull SettingId id) {
        try {
            return read(ScopeUtils.getScopeFromProject(project), id, null);
        } catch (ObjectNotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load saved model for project '" + project + "': " + e.getMessage(), e);
        }
    }

    public Collection<SettingName> readNames(@NotNull String scope) {

        Set<SettingName> names = new HashSet<>(getSettingNamesFromLocation(scope));

        if (!DEFAULT_SCOPE.equals(scope)) {
            // If requested scope is project - add names from global scope.
            // As names collection is Set, names from global scope will only then be added if differ from project scope names
            names.addAll(getSettingNamesFromLocation(DEFAULT_SCOPE));
        }

        names.add(SettingName.builder().id(DEFAULT_NAME).name(DEFAULT_NAME).scope(DEFAULT_SCOPE).build());
        return names.stream().sorted(namesComparator).toList();
    }

    @Override
    public @NotNull T read(@NotNull String scope, @NotNull SettingId id, @Nullable String revisionName) {
        @Nullable String fileName = getFileName(scope, false, id);
        @Nullable String value = readFileContent(scope, fileName, revisionName);

        if (value == null && StringUtils.isEmpty(revisionName)) {
            value = readFileContent(DEFAULT_SCOPE, fileName, null);
        }

        return value == null ? handleMissingValue(id) : fromString(value);
    }

    private @Nullable String readFileContent(@NotNull String scope, @Nullable String fileName, @Nullable String revisionName) {
        if (fileName == null) {
            return null;
        }
        String settingPath = String.format(LOCATION_MASK, settingsFolder, fileName, SETTINGS_FILE_EXTENSION);
        ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        return settingsService.read(location, revisionName);
    }

    protected @NotNull T handleMissingValue(@NotNull SettingId id) {
        if (DEFAULT_NAME.equals(id.getIdentifier())) {
            return defaultValues();
        } else {
            throw new ObjectNotFoundException("Setting '%s' not found".formatted(id.getIdentifier()));
        }
    }

    @Override
    public @NotNull T save(@NotNull String scope, @NotNull SettingId id, @NotNull T what) {
        @Nullable String fileName = getFileName(scope, true, id);
        if (StringUtils.isEmpty(fileName)) {
            throw new IllegalArgumentException("Provided filename is empty");
        }

        String settingPath = String.format(LOCATION_MASK, settingsFolder, fileName, SETTINGS_FILE_EXTENSION);
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
        @Nullable String fileName = getFileName(scope, true, id);
        if (StringUtils.isEmpty(fileName)) {
            throw new ObjectNotFoundException("Setting '%s' not found and that's why can not be deleted".formatted(id.getIdentifier()));
        }
        String settingPath = String.format(LOCATION_MASK, settingsFolder, fileName, SETTINGS_FILE_EXTENSION);
        final ILocation location = ScopeUtils.getContextLocation(scope).append(settingPath);
        settingsService.delete(location);
    }

    public @NotNull List<Revision> listRevisions(String scope, SettingId id) {
        @Nullable String fileName = getFileName(scope, false, id);
        if (StringUtils.isEmpty(fileName)) {
            throw new ObjectNotFoundException("Setting '%s' not found".formatted(id.getIdentifier()));
        }
        String settingPath = String.format(LOCATION_MASK, settingsFolder, fileName, SETTINGS_FILE_EXTENSION);
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

    public @Nullable String getIdByName(String scope, boolean limitToScope, String settingName) {
        return readNames(scope).stream()
                .filter(n -> Objects.equals(n.getName(), settingName) && (!limitToScope || Objects.equals(n.getScope(), scope)))
                .map(SettingName::getId)
                .findFirst()
                .orElse(null);
    }

    private @Nullable String getFileName(String scope, boolean limitToScope, SettingId settingId) {
        return settingId.isUseName() ? getIdByName(scope, limitToScope, settingId.getIdentifier()) : settingId.getIdentifier();
    }

    private Set<SettingName> getSettingNamesFromLocation(String scope) {
        ILocation targetLocation = ScopeUtils.getContextLocation(scope).append(settingsFolder);
        String lastRevision = settingsService.getLastRevision(targetLocation);
        if (lastRevision == null) { //folder doesn't exist
            return new HashSet<>();
        }
        Pair<String, Set<SettingName>> cached = SETTING_NAMES_CACHE.get(targetLocation);
        if (cached == null || !Objects.equals(lastRevision, cached.left())) {
            List<SettingName> allSettingNames = settingsService.getPersistedSettingFileNames(targetLocation).stream()
                    .map(fileNameWithoutExtension -> buildSettingName(scope, fileNameWithoutExtension))
                    .toList();
            checkForDuplicateNames(allSettingNames);
            Set<SettingName> settingNames = new HashSet<>(allSettingNames);
            cached = Pair.of(lastRevision, settingNames);
            SETTING_NAMES_CACHE.put(targetLocation, cached);
        }
        return cached.right();
    }

    private void checkForDuplicateNames(List<SettingName> settingNames) {
        Map<String, List<SettingName>> groupedByName = settingNames.stream()
                .collect(Collectors.groupingBy(SettingName::getName));
        List<String> duplicateEntries = groupedByName.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String ids = entry.getValue().stream()
                            .map(SettingName::getId)
                            .sorted()
                            .collect(Collectors.joining(", "));
                    return entry.getKey() + " (" + ids + ")";
                })
                .toList();
        if (!duplicateEntries.isEmpty()) {
            throw new DuplicateSettingNameException("Multiple settings files contain the same name: " + String.join(", ", duplicateEntries));
        }
    }

    private SettingName buildSettingName(String scope, String id) {
        //Legacy items (which were created before 'id' introduction) don't have 'name' model field - in this case we reuse their file names.
        //Proper 'name' field value will appear only after explicit settings save by user.
        T settingsModel = read(scope, SettingId.fromId(id), null);
        String name = Optional.ofNullable(settingsModel.getName()).orElse(id);

        return SettingName.builder()
                .id(id)
                .name(name)
                .scope(scope)
                .build();
    }

}
