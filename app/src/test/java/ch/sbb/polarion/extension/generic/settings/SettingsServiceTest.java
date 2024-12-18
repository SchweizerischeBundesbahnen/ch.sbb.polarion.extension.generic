package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.polarion.CustomExtensionMock;
import ch.sbb.polarion.extension.generic.polarion.PlatformContextMockExtension;
import ch.sbb.polarion.extension.generic.polarion.TransactionalExecutorExtension;
import com.polarion.alm.projects.IProjectService;
import com.polarion.alm.projects.model.IUser;
import com.polarion.alm.tracker.ITrackerService;
import com.polarion.alm.tracker.model.IBaseline;
import com.polarion.alm.tracker.model.ITrackerProject;
import com.polarion.alm.tracker.model.ipi.IInternalBaselinesManager;
import com.polarion.core.util.DateUtils;
import com.polarion.platform.internal.service.repository.ExtendedRevisionMetaData;
import com.polarion.platform.internal.service.repository.LocationChangeMetaData;
import com.polarion.platform.internal.service.repository.RevisionMetaData;
import com.polarion.platform.persistence.spi.PObjectList;
import com.polarion.platform.service.repository.IRepositoryConnection;
import com.polarion.platform.service.repository.IRepositoryReadOnlyConnection;
import com.polarion.platform.service.repository.IRepositoryService;
import com.polarion.platform.service.repository.driver.DriverException;
import com.polarion.subterra.base.location.ILocation;
import com.polarion.subterra.base.location.Location;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class, TransactionalExecutorExtension.class})
@SuppressWarnings({"unused", "unchecked", "rawtypes"})
class SettingsServiceTest {
    private static final String MOCK_SETTINGS = "mock.settings";

    @CustomExtensionMock
    private IProjectService projectService;
    @CustomExtensionMock
    private ITrackerService trackerService;
    @CustomExtensionMock
    private IRepositoryService repositoryService;

    @Test
    void testSave() {
        SettingsService settingsService = new SettingsService(repositoryService, projectService, trackerService);

        IRepositoryConnection connection = mock(IRepositoryConnection.class);
        when(repositoryService.getConnection(any(ILocation.class))).thenReturn(connection);

        when(connection.exists(any(ILocation.class))).thenReturn(true);
        byte[] bytes = "someContent".getBytes(StandardCharsets.UTF_8);
        ILocation location = mock(ILocation.class);
        settingsService.save(location, bytes);
        verify(connection, times(1)).setContent(any(), any());
        verify(connection, times(0)).create(any(), any());

        // it must suppress IOException
        try (MockedConstruction<ByteArrayInputStream> mockByteArrayInputStream = Mockito.mockConstruction(ByteArrayInputStream.class,
                (mock, context) -> doThrow(new IOException("Emulated IOException")).when(mock).close())) {
            settingsService.save(location, bytes);
        }

        // but not other exceptions
        try (MockedConstruction<ByteArrayInputStream> mockByteArrayInputStream = Mockito.mockConstruction(ByteArrayInputStream.class,
                (mock, context) -> doThrow(new RuntimeException("Emulated RuntimeException")).when(mock).close())) {
            assertThrows(RuntimeException.class, () -> settingsService.save(location, bytes));
        }

        settingsService.save(location, bytes);
        verify(connection, times(4)).setContent(any(), any());
        verify(connection, times(0)).create(any(), any());

        when(connection.exists(any(ILocation.class))).thenReturn(false);
        settingsService.save(location, bytes);
        verify(connection, times(4)).setContent(any(), any());
        verify(connection, times(1)).create(any(), any());
    }

    @Test
    void testOverloadedSaveMethodCall() {
        SettingsService settingsService = mock(SettingsService.class);
        doCallRealMethod().when(settingsService).save(any(), anyString());

        settingsService.save(mock(ILocation.class), "someContent");
        verify(settingsService, times(1)).save(any(), any(byte[].class));
    }

    @Test
    void testDelete() {
        SettingsService settingsService = new SettingsService(repositoryService, projectService, trackerService);

        ILocation location = mock(ILocation.class);
        IRepositoryConnection connection = mock(IRepositoryConnection.class);
        when(repositoryService.getConnection(any(ILocation.class))).thenReturn(connection);

        when(connection.exists(any(ILocation.class))).thenReturn(false);
        settingsService.delete(location);
        verify(connection, times(0)).delete(any());

        when(connection.exists(any(ILocation.class))).thenReturn(true);
        settingsService.delete(location);
        verify(connection, times(1)).delete(any());
    }

    @Test
    void testRead() {
        IRepositoryReadOnlyConnection mockedConnection = mock(IRepositoryReadOnlyConnection.class);
        when(repositoryService.getReadOnlyConnection(any(ILocation.class))).thenReturn(mockedConnection);

        when(mockedConnection.exists(any(ILocation.class))).thenReturn(false);

        assertNull(new SettingsService(repositoryService, projectService, trackerService).read(mock(ILocation.class), null));

        when(mockedConnection.exists(any(ILocation.class))).thenReturn(true);
        when(mockedConnection.getContent(any(ILocation.class))).thenReturn(null);
        assertNull(new SettingsService(repositoryService, projectService, trackerService).read(mock(ILocation.class), null));

        when(mockedConnection.getContent(any(ILocation.class))).thenReturn(new ByteArrayInputStream(new byte[0]));
        assertNotNull(new SettingsService(repositoryService, projectService, trackerService).read(mock(ILocation.class), null));

        try (MockedStatic<Location> mockedLocation = mockStatic(Location.class)) {
            mockedLocation.when(() -> Location.getLocationWithRevision(any(), anyString())).thenReturn(mock(ILocation.class));
            assertNotNull(new SettingsService(repositoryService, projectService, trackerService).read(mock(ILocation.class), "123"));

            when(mockedConnection.getContent(any(ILocation.class))).thenThrow(new DriverException()); //DriverException is thrown when wrong revision used
            assertNull(new SettingsService(repositoryService, projectService, trackerService).read(mock(ILocation.class), "123"));
        }
    }

    @Test
    void testExists() {
        SettingsService settingsService = new SettingsService(repositoryService, projectService, trackerService);

        IRepositoryReadOnlyConnection connection = mock(IRepositoryReadOnlyConnection.class);
        when(repositoryService.getReadOnlyConnection(any(ILocation.class))).thenReturn(connection);
        ILocation location = mock(ILocation.class);

        when(connection.exists(any())).thenReturn(false);
        assertFalse(settingsService.exists(location));

        when(connection.exists(any())).thenReturn(true);
        assertTrue(settingsService.exists(location));
    }

    @Test
    void testRevisionsList() {
        IRepositoryReadOnlyConnection mockedConnection = mock(IRepositoryReadOnlyConnection.class);
        when(mockedConnection.getRevisionsMetaData(any(ILocation.class), anyBoolean())).thenReturn(Arrays.asList(
                constructExtendedRevisionMetaData("73", "author1", "desc1", DateUtils.date(2023, 1, 20, 14, 45)),
                constructExtendedRevisionMetaData("456", "author2", "desc2", DateUtils.date(2023, 5, 10, 10, 24)),
                constructExtendedRevisionMetaData("789", "author3", "desc3", DateUtils.date(2023, 2, 12, 12, 21)),
                constructExtendedRevisionMetaData("555", "author4", "desc4", DateUtils.date(2023, 7, 11, 22, 12))
        ));
        when(repositoryService.getReadOnlyConnection(any(ILocation.class))).thenReturn(mockedConnection);

        ITrackerProject trackerProject = mock(ITrackerProject.class);
        when(trackerService.getTrackerProject(anyString())).thenReturn(trackerProject);
        IInternalBaselinesManager baselinesManager = mock(IInternalBaselinesManager.class);
        when(trackerProject.getBaselinesManager()).thenReturn(baselinesManager);
        IBaseline baseline = mock(IBaseline.class);
        when(baseline.getName()).thenReturn("baselineName");
        when(baselinesManager.getRevisionBaseline(anyString())).thenReturn(baseline);

        List<IUser> users = Arrays.asList(
                mockUser("author1", "USER NAME"),
                mockUser("author2", "USER NAME 2")
        );
        when(projectService.getUsers()).thenReturn(new PObjectList(null, users));

        ILocation location = mock(ILocation.class);
        when(location.getLastComponent()).thenReturn(MOCK_SETTINGS);

        // non-existing location
        when(mockedConnection.exists(any(ILocation.class))).thenReturn(false);
        SettingsService settingsService = new SettingsService(repositoryService, projectService, trackerService);
        List<Revision> revisions = settingsService.listRevisions(location, null);
        assertTrue(revisions.isEmpty());

        // general case
        when(mockedConnection.exists(any(ILocation.class))).thenReturn(true);
        revisions = settingsService.listRevisions(location, null);
        assertEquals(4, revisions.size());
        assertEquals(Arrays.asList("789", "555", "456", "73"), revisions.stream().map(Revision::getName).toList());
        assertEquals("author3", revisions.get(0).getAuthor());
        assertEquals("USER NAME 2", revisions.get(2).getAuthor());
        assertEquals(Set.of("baselineName"), revisions.stream().map(Revision::getBaseline).collect(Collectors.toSet()));

        // missing user's name
        users = Arrays.asList(
                mockUser("author1", "USER NAME"),
                mockUser("author2", null)
        );
        when(projectService.getUsers()).thenReturn(new PObjectList(null, users));
        revisions = settingsService.listRevisions(location, null);
        assertEquals("author2", revisions.get(2).getAuthor());

        // emulate case when revision's location name not match
        when(location.getLastComponent()).thenReturn("unknown_name.settings");
        revisions = settingsService.listRevisions(location, null);
        assertTrue(revisions.isEmpty());
    }

    @Test
    void testListFilenames() {
        IRepositoryService mockedRepositoryService = mock(IRepositoryService.class);

        IRepositoryReadOnlyConnection mockedConnection = mock(IRepositoryReadOnlyConnection.class);
        List<ILocation> filesList = Arrays.asList(
                constructLocation("file1.settings"),
                constructLocation("file2.settings"),
                constructLocation("file3.txt"),
                constructLocation("file4"),
                constructLocation("file 5.settings")
        );
        when(mockedConnection.getSubLocations(any(ILocation.class), anyBoolean())).thenReturn(filesList);
        when(mockedRepositoryService.getReadOnlyConnection(any(ILocation.class))).thenReturn(mockedConnection);

        Collection<String> fileNames = new SettingsService(mockedRepositoryService, null, null).getPersistedSettingFileNames(mock(ILocation.class));
        assertEquals(Arrays.asList("file1", "file2", "file 5"), fileNames);
    }

    private IUser mockUser(String id, String name) {
        IUser userMock = mock(IUser.class);
        lenient().when(userMock.getId()).thenReturn(id);
        lenient().when(userMock.getName()).thenReturn(name);
        return userMock;
    }

    private ExtendedRevisionMetaData constructExtendedRevisionMetaData(@NotNull String name, @NotNull String author, @NotNull String description, @NotNull Date date) {
        RevisionMetaData revisionMetaData = new RevisionMetaData(name, author, description, date);
        LocationChangeMetaData locationChangeMetaData = new LocationChangeMetaData();
        locationChangeMetaData.setToLoc(Location.getLocationWithRevision(MOCK_SETTINGS, name));
        return new ExtendedRevisionMetaData(revisionMetaData, locationChangeMetaData);
    }

    private ILocation constructLocation(String fileName) {
        ILocation location = mock(Location.class);
        when(location.getLastComponent()).thenReturn(fileName);
        return location;
    }
}
