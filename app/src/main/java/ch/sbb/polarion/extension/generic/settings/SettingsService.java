package ch.sbb.polarion.extension.generic.settings;

import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.shared.api.transaction.ReadOnlyTransaction;
import com.polarion.alm.shared.api.transaction.TransactionalExecutor;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.core.util.StringUtils;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.core.PlatformContext;
import com.polarion.platform.service.repository.IExtendedRevisionMetaData;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Contains utility methods for load/save settings data and query revision information.
 */
@SuppressWarnings("squid:S1200") // Ignore dependencies on other classes count limitation
public class SettingsService {
    private static final Logger logger = Logger.getLogger((Object) SettingsService.class);
    private static final String REVISION_DATE_TIME_FORMAT = "yyyy-MM-dd HH:mm";

    private final IRepositoryService repositoryService;
    private final IProjectService projectService;
    private final ITrackerService trackerService;

    public SettingsService() {
        repositoryService = PlatformContext.getPlatform().lookupService(IRepositoryService.class);
        projectService = PlatformContext.getPlatform().lookupService(IProjectService.class);
        trackerService = PlatformContext.getPlatform().lookupService(ITrackerService.class);
    }

    public SettingsService(IRepositoryService repositoryService, IProjectService projectService, ITrackerService trackerService) {
        this.repositoryService = repositoryService;
        this.projectService = projectService;
        this.trackerService = trackerService;
    }

    public void save(@NotNull ILocation location, @NotNull String content) {
        save(location, content.getBytes(StandardCharsets.UTF_8));
    }

    public void save(@NotNull ILocation location, @NotNull byte[] content) {
        Runnable runnable = () -> {
            IRepositoryConnection connection = repositoryService.getConnection(location);
            try (InputStream inputStream = new ByteArrayInputStream(content)) {
                if (connection.exists(location)) {
                    connection.setContent(location, inputStream);
                } else {
                    connection.create(location, inputStream);
                }
            } catch (IOException e) {
                logger.error("Cannot save content to location '" + location + "':" + e.getMessage(), e);
            }
        };

        if (TransactionalExecutor.currentTransaction() == null) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                runnable.run();
                return null;
            });
        } else {
            runnable.run();
        }
    }

    public void delete(@NotNull ILocation location) {
        Runnable runnable = () -> {
            IRepositoryConnection connection = repositoryService.getConnection(location);
            if (connection.exists(location)) {
                connection.delete(location);
            }
        };

        ReadOnlyTransaction existingTransaction = TransactionalExecutor.currentTransaction();
        if (existingTransaction == null || existingTransaction.isReadOnly()) {
            TransactionalExecutor.executeInWriteTransaction(transaction -> {
                runnable.run();
                return null;
            });
        } else {
            runnable.run();
        }
    }

    @Nullable
    public String read(@NotNull ILocation location, String revisionName) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IRepositoryReadOnlyConnection readOnlyConnection = repositoryService.getReadOnlyConnection(location);
            if (!readOnlyConnection.exists(location)) {
                logNotExistingLocation(location);
                return null;
            }

            try (InputStream inputStream = readOnlyConnection.getContent(StringUtils.isEmpty(revisionName) ? location : Location.getLocationWithRevision(location.getLocationPath(), revisionName))) {
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                logger.error("Error reading content from location '" + location.getLocationPath() + "', revision '" + revisionName + "': " + e.getMessage(), e);
                return null;
            }
        });
    }

    public boolean exists(@NotNull ILocation location) {
        return Boolean.TRUE.equals(
                TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
                    IRepositoryReadOnlyConnection readOnlyConnection = repositoryService.getReadOnlyConnection(location);
                    return readOnlyConnection.exists(location);
                }));
    }

    @SuppressWarnings("unchecked")
    public List<Revision> listRevisions(@NotNull ILocation location, @Nullable String projectId) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IRepositoryReadOnlyConnection readOnlyConnection = repositoryService.getReadOnlyConnection(location);
            if (!readOnlyConnection.exists(location)) {
                logNotExistingLocation(location);
                return new ArrayList<>();
            }

            List<IExtendedRevisionMetaData> metaDataList = readOnlyConnection.getRevisionsMetaData(location, true);
            Map<String, String> userNamesMap = projectService.getUsers().stream()
                    .collect(Collectors.toMap(IUser::getId, user -> user.getName() == null ? user.getId() : user.getName()));

            SimpleDateFormat dateFormat = new SimpleDateFormat(REVISION_DATE_TIME_FORMAT);
            return metaDataList.stream()
                    .filter(metaData -> metaData.getChangeLocationTo() == null || metaData.getChangeLocationTo().getLastComponent().equals(location.getLastComponent()))
                    .map(metaData -> Revision.builder()
                            .name(metaData.getName())
                            .baseline(getRevisionBaseline(projectId, metaData.getName()))
                            .date(dateFormat.format(metaData.getDate()))
                            .author(userNamesMap.getOrDefault(metaData.getAuthor(), metaData.getAuthor()))
                            .description(metaData.getDescription())
                            .build())
                    .sorted()
                    .toList();
        });
    }

    @SuppressWarnings("unchecked")
    public Collection<String> getPersistedSettingFileNames(ILocation settingsFolderLocation) {
        final IRepositoryReadOnlyConnection readOnlyConnection = repositoryService.getReadOnlyConnection(settingsFolderLocation);
        List<Location> subLocations = readOnlyConnection.getSubLocations(settingsFolderLocation, false);
        return subLocations.stream()
                .map(Location::getLastComponent)
                .filter(name -> name.endsWith(GenericNamedSettings.SETTINGS_FILE_EXTENSION))
                .map(name -> name.replace(GenericNamedSettings.SETTINGS_FILE_EXTENSION, ""))
                .toList();
    }

    /**
     * Returns last revision. If the {@code location} param pointed to the folder then it will return a new revision number after any modification inside it (e.g. file edited, removed etc.)
     */
    public String getLastRevision(@NotNull ILocation location) {
        return TransactionalExecutor.executeSafelyInReadOnlyTransaction(transaction -> {
            IRepositoryReadOnlyConnection readOnlyConnection = repositoryService.getReadOnlyConnection(location);
            if (!readOnlyConnection.exists(location)) {
                logNotExistingLocation(location);
                return null;
            }
            return readOnlyConnection.getLastRevision(location);
        });
    }

    private void logNotExistingLocation(ILocation location) {
        logger.warn("Location does not exist: " + location.getLocationPath());
    }

    @Nullable
    private String getRevisionBaseline(@Nullable String projectId, @NotNull String revision) {
        IInternalBaselinesManager baselinesManager = (IInternalBaselinesManager) trackerService.getTrackerProject(projectId == null ? IRepositoryService.DEFAULT : projectId).getBaselinesManager();
        IBaseline baseline = baselinesManager.getRevisionBaseline(revision);
        return baseline != null ? baseline.getName() : null;
    }
}
