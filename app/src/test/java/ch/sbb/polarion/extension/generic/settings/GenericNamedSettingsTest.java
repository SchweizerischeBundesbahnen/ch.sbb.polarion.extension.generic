package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.util.ScopeUtils;
import com.polarion.subterra.base.location.ILocation;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unused")
class GenericNamedSettingsTest {
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
        }
    }

    @Test
    void testReadNames() {
        try (MockedStatic<ScopeUtils> mockScopeUtils = mockStatic(ScopeUtils.class)) {

            SettingsService settingsService = mock(SettingsService.class);

            ILocation mockProjectLocation = mock(ILocation.class);
            ILocation mockProjectFolderLocation = mock(ILocation.class);
            when(mockProjectLocation.append(anyString())).thenReturn(mockProjectFolderLocation);
            mockScopeUtils.when(() -> ScopeUtils.getContextLocation("Test")).thenReturn(mockProjectLocation);
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

            Collection<SettingName> names = testSettings.readNames("Test");
            assertEquals(Stream.of("test1", "test2", "default1", "default2").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //folder revisions remain the same = used cached values
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("test3"), getModelContent("test4"), getModelContent("default3"), getModelContent("default4"));
            names = testSettings.readNames("Test");
            assertEquals(Stream.of("test1", "test2", "default1", "default2").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //default folder revision updated = results partially changed
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("default3"), getModelContent("default4"));
            when(settingsService.getLastRevision(mockDefaultFolderLocation)).thenReturn("43");
            names = testSettings.readNames("Test");
            assertEquals(Stream.of("test1", "test2", "default3", "default4").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());

            //project folder revision updated = results changed again
            lenient().when(settingsService.read(any(), any())).thenReturn(getModelContent("test3"), getModelContent("test4"));
            when(settingsService.getLastRevision(mockProjectFolderLocation)).thenReturn("35");
            names = testSettings.readNames("Test");
            assertEquals(Stream.of("test3", "test4", "default3", "default4").sorted().toList(), names.stream().map(SettingName::getName).sorted().toList());
        }
    }

    private String getModelContent(String name) {
        TestModel testModel = new TestModel();
        testModel.setName(name);
        return testModel.serialize();
    }

    private static class TestSettings extends GenericNamedSettings<TestModel> {
        public TestSettings() {
            super("Test");
        }

        public TestSettings(SettingsService settingsService) {
            super("Test", settingsService);
        }

        @Override
        public @NotNull TestModel defaultValues() {
            return new TestModel();
        }

        @Override
        public @NotNull String currentBundleTimestamp() {
            return "Something";
        }
    }

    public static class TestModel extends SettingsModel {

        @Override
        protected String serializeModelData() {
            return "Dummy implementation";
        }

        @Override
        protected void deserializeModelData(String serializedString) {
            // Dummy implementation
        }
    }
}
