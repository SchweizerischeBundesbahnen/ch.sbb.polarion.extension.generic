package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.context.CurrentContextConfig;
import ch.sbb.polarion.extension.generic.context.CurrentContextExtension;
import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.settings.named_settings.TestModel;
import ch.sbb.polarion.extension.generic.settings.named_settings.TestSettings;
import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static ch.sbb.polarion.extension.generic.settings.NamedSettings.DEFAULT_NAME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith({MockitoExtension.class, CurrentContextExtension.class})
@CurrentContextConfig(GenericNamedSettingsTest.POLARION_TEXT_EXTENSION)
@SuppressWarnings("unused")
class GenericNamedSettingsTest {

    public static final String POLARION_TEXT_EXTENSION = "polarion-text-extension";

    @Test
    void testBeforeAndAfterSave() {
        try (MockedConstruction<SettingsService> mockSettingsService = Mockito.mockConstruction(SettingsService.class);
             MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            TestSettings settingsSpy = spy(TestSettings.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectFolderLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/some_project/")).thenReturn(mockProjectLocation);

            TestModel model = mock(TestModel.class);
            when(model.serialize()).thenCallRealMethod();

            settingsSpy.save("project/some_project/", SettingId.fromId("some_setting"), model);

            verify(settingsSpy).beforeSave(model);
            verify(settingsSpy).afterSave(model);

            SettingId emptySettingId = SettingId.fromId("");
            assertThrows(IllegalArgumentException.class, () -> settingsSpy.save("project/some_project/", emptySettingId, model));
        }
    }

    @Test
    void testRead() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectSettingsFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockProjectSettingsFolderLocation);
            ILocation mockProjectTest1Location = mock(ILocation.class);
            ILocation mockProjectTest2Location = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/project_test1.settings")).thenReturn(mockProjectTest1Location);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/project_test2.settings")).thenReturn(mockProjectTest2Location);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/some_project/")).thenReturn(mockProjectLocation);
            when(settingsService.getLastRevision(mockProjectSettingsFolderLocation)).thenReturn("34");
            when(settingsService.getPersistedSettingFileNames(mockProjectSettingsFolderLocation)).thenReturn(List.of("project_test1", "project_test2"));

            ILocation mockDefaultLocation = mock(ILocation.class);
            ILocation mockDefaultSettingsFolderLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockDefaultSettingsFolderLocation);
            ILocation mockDefaultTest1Location = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/default_test1.settings")).thenReturn(mockDefaultTest1Location);
            ILocation mockDefaultDefaultLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/Default.settings")).thenReturn(mockDefaultDefaultLocation);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(settingsService.getLastRevision(mockDefaultSettingsFolderLocation)).thenReturn("42");
            when(settingsService.getPersistedSettingFileNames(mockDefaultSettingsFolderLocation)).thenReturn(List.of("default_test1"));

            TestSettings testSettings = new TestSettings(settingsService);
            when(settingsService.read(eq(mockProjectTest1Location), any())).thenReturn(getModelContent("project_test1"));
            when(settingsService.read(eq(mockProjectTest2Location), any())).thenReturn(getModelContent("project_test2"));
            when(settingsService.read(eq(mockDefaultTest1Location), any())).thenReturn(getModelContent("default_test1"));

            TestModel testModelProjectTest1 = testSettings.read("project/some_project/", SettingId.fromName("project_test1"), null);
            assertEquals("project_test1", testModelProjectTest1.getName());

            TestModel testModelProjectTest2 = testSettings.read("project/some_project/", SettingId.fromName("project_test2"), null);
            assertEquals("project_test2", testModelProjectTest2.getName());

            TestModel testModelDefaultTest1 = testSettings.read("project/some_project/", SettingId.fromName("default_test1"), null);
            assertEquals("default_test1", testModelDefaultTest1.getName());

            TestModel testModelGlobalDefaultTest1 = testSettings.read("", SettingId.fromName("default_test1"), null);
            assertEquals("default_test1", testModelGlobalDefaultTest1.getName());

            SettingId defaultTest1SettingId = SettingId.fromName("unknown");
            assertThrows(ObjectNotFoundException.class, () -> testSettings.read("project/some_project/", defaultTest1SettingId, "55"));

            SettingId unknownSettingId = SettingId.fromName("unknown");
            assertThrows(ObjectNotFoundException.class, () -> testSettings.read("project/some_project/", unknownSettingId, null));

            TestModel testModelDefaultSaved = testSettings.read("project/some_project/", SettingId.fromName(DEFAULT_NAME), null);
            assertEquals("Default", testModelDefaultSaved.getName());

            TestModel testModelDefaultNotSaved = testSettings.read("project/some_project/", SettingId.fromName(DEFAULT_NAME), null);
            assertEquals("Default", testModelDefaultNotSaved.getName());
        }
    }

    @Test
    void testReadNames() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {

            SettingsService settingsService = mock(SettingsService.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectFolderLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/some_project/")).thenReturn(mockProjectLocation);
            when(settingsService.getPersistedSettingFileNames(mockProjectFolderLocation)).thenReturn(Arrays.asList("test1", "test2"));
            when(settingsService.getLastRevision(mockProjectFolderLocation)).thenReturn("34");

            ILocation mockDefaultLocation = mock(ILocation.class);
            ILocation mockDefaultFolderLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(anyString())).thenReturn(mockDefaultFolderLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(settingsService.getPersistedSettingFileNames(mockDefaultFolderLocation)).thenReturn(Arrays.asList("default1", "default2"));
            when(settingsService.getLastRevision(mockDefaultFolderLocation)).thenReturn("42");

            TestSettings testSettings = new TestSettings(settingsService);
            when(settingsService.read(any(), any())).thenReturn(getModelContent("test1"), getModelContent("test2"), getModelContent("default1"), getModelContent("default2"));

            Collection<SettingName> names = testSettings.readNames("project/some_project/");
            assertEquals(Stream.of("Default", "test1", "test2", "default1", "default2").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //folder revisions remain the same = used cached values
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("test3"), getModelContent("test4"), getModelContent("default3"), getModelContent("default4"));
            names = testSettings.readNames("project/some_project/");
            assertEquals(Stream.of("Default", "test1", "test2", "default1", "default2").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //default folder revision updated = results partially changed
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("default3"), getModelContent("default4"));
            when(settingsService.getLastRevision(mockDefaultFolderLocation)).thenReturn("43");
            names = testSettings.readNames("project/some_project/");
            assertEquals(Stream.of("Default", "test1", "test2", "default3", "default4").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //project folder revision updated = results changed again
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("test3"), getModelContent("test4"));
            when(settingsService.getLastRevision(mockProjectFolderLocation)).thenReturn("35");
            names = testSettings.readNames("project/some_project/");
            assertEquals(Stream.of("Default", "test3", "test4", "default3", "default4").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());
        }
    }

    private String getModelContent(String name) {
        TestModel testModel = new TestModel();
        testModel.setName(name);
        return testModel.serialize();
    }

    @Test
    void testDelete() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            SettingsService settingsService = mock(SettingsService.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectSettingsFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockProjectSettingsFolderLocation);
            ILocation mockProjectTest1Location = mock(ILocation.class);
            ILocation mockProjectTest2Location = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/project_delete1.settings")).thenReturn(mockProjectTest1Location);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/delete_project/")).thenReturn(mockProjectLocation);
            when(settingsService.getLastRevision(mockProjectSettingsFolderLocation)).thenReturn("11");
            when(settingsService.getPersistedSettingFileNames(mockProjectSettingsFolderLocation)).thenReturn(List.of("project_delete1"));

            ILocation mockDefaultLocation = mock(ILocation.class);
            ILocation mockDefaultSettingsFolderLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockDefaultSettingsFolderLocation);
            ILocation mockDefaultTest1Location = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/default_delete1.settings")).thenReturn(mockDefaultTest1Location);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(settingsService.getLastRevision(mockDefaultSettingsFolderLocation)).thenReturn("66");
            when(settingsService.getPersistedSettingFileNames(mockDefaultSettingsFolderLocation)).thenReturn(List.of("default_delete1"));

            TestSettings testSettings = new TestSettings(settingsService);
            when(settingsService.read(eq(mockProjectTest1Location), any())).thenReturn(getModelContent("project_delete1"));
            when(settingsService.read(eq(mockDefaultTest1Location), any())).thenReturn(getModelContent("default_delete1"));

            SettingId unknownSettingId = SettingId.fromName("unknown");
            assertThrows(ObjectNotFoundException.class, () -> testSettings.delete("project/delete_project/", unknownSettingId));

            testSettings.delete("project/delete_project/", SettingId.fromName("project_delete1"));
            verify(settingsService).delete(mockProjectTest1Location);

            SettingId defaultDelete1SettingId = SettingId.fromName("default_delete1");
            assertThrows(ObjectNotFoundException.class, () -> testSettings.delete("project/delete_project/", defaultDelete1SettingId));
        }
    }

    @Test
    void testListRevisions() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {
            mockScopeUtils.when(() -> ScopeUtils.getProjectFromScope(anyString())).thenCallRealMethod();

            SettingsService settingsService = mock(SettingsService.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectSettingsFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockProjectSettingsFolderLocation);
            ILocation mockProjectTest1Location = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/project_list_revisions1.settings")).thenReturn(mockProjectTest1Location);
            ILocation mockProjectDefault1Location = mock(ILocation.class);
            when(mockProjectLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/default_list_revisions1.settings")).thenReturn(mockProjectDefault1Location);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("project/list_revisions_project/")).thenReturn(mockProjectLocation);
            when(settingsService.getLastRevision(mockProjectSettingsFolderLocation)).thenReturn("11");
            when(settingsService.getPersistedSettingFileNames(mockProjectSettingsFolderLocation)).thenReturn(List.of("project_list_revisions1"));

            ILocation mockDefaultLocation = mock(ILocation.class);
            ILocation mockDefaultSettingsFolderLocation = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test")).thenReturn(mockDefaultSettingsFolderLocation);
            ILocation mockDefaultTest1Location = mock(ILocation.class);
            when(mockDefaultLocation.append(".polarion/extensions/" + POLARION_TEXT_EXTENSION + "/Test/default_list_revisions1.settings")).thenReturn(mockDefaultTest1Location);

            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("")).thenReturn(mockDefaultLocation);
            when(settingsService.getLastRevision(mockDefaultSettingsFolderLocation)).thenReturn("66");
            when(settingsService.getPersistedSettingFileNames(mockDefaultSettingsFolderLocation)).thenReturn(List.of("default_list_revisions1"));

            TestSettings testSettings = new TestSettings(settingsService);
            when(settingsService.read(eq(mockProjectTest1Location), any())).thenReturn(getModelContent("project_list_revisions1"));
            when(settingsService.listRevisions(mockProjectTest1Location, "list_revisions_project")).thenReturn(
                    List.of(
                            Revision.builder().name("3").build(),
                            Revision.builder().name("2").build(),
                            Revision.builder().name("1").build()
                    )
            );
            when(settingsService.listRevisions(mockProjectDefault1Location, "list_revisions_project")).thenReturn(Collections.emptyList());
            when(settingsService.listRevisions(mockDefaultTest1Location, null)).thenReturn(
                    List.of(
                            Revision.builder().name("5").build(),
                            Revision.builder().name("4").build()
                    )
            );

            when(settingsService.read(eq(mockDefaultTest1Location), any())).thenReturn(getModelContent("default_list_revisions1"));

            SettingId unknownSettingId = SettingId.fromName("unknown");
            assertThrows(ObjectNotFoundException.class, () -> testSettings.listRevisions("project/list_revisions_project/", unknownSettingId));

            List<Revision> projectListRevisions1 = testSettings.listRevisions("project/list_revisions_project/", SettingId.fromName("project_list_revisions1"));
            assertThat(projectListRevisions1).extracting(Revision::getName).containsExactly("3", "2", "1");

            List<Revision> defaultListRevisions1 = testSettings.listRevisions("", SettingId.fromName("default_list_revisions1"));
            assertThat(defaultListRevisions1).extracting(Revision::getName).containsExactly("5", "4");

            List<Revision> projectDefaultListRevisions1 = testSettings.listRevisions("project/list_revisions_project/", SettingId.fromName("default_list_revisions1"));
            assertThat(projectDefaultListRevisions1).isEmpty();
        }
    }

}
