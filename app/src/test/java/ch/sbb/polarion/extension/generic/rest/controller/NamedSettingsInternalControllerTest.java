package ch.sbb.polarion.extension.generic.rest.controller;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.settings.Revision;
import ch.sbb.polarion.extension.generic.settings.SettingId;
import ch.sbb.polarion.extension.generic.settings.SettingName;
import ch.sbb.polarion.extension.generic.settings.SettingsModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"rawtypes", "unchecked", "UnusedReturnValue"})
class NamedSettingsInternalControllerTest {

    private NamedSettingsInternalController controller;

    @BeforeEach
    void setup() {
        controller = new NamedSettingsInternalController(mock(PolarionService.class));
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void testReadFeaturesList() {
        assertEquals(0, controller.readFeaturesList().size());

        NamedSettingsRegistry.INSTANCE.register(List.of(mockSettings("feature1"), mockSettings("feature2")));
        assertTrue(controller.readFeaturesList().containsAll(List.of("feature1", "feature2")));
    }

    @Test
    void testReadSettingNames() {
        GenericNamedSettings settings = mockSettings("feature1");
        when(settings.readNames(any())).thenReturn(
                List.of(generateSettingName(1), generateSettingName(3), generateSettingName(8)));
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        assertTrue(controller.readSettingNames("feature1", "testScope").containsAll(
                List.of(generateSettingName(8), generateSettingName(1), generateSettingName(3))));

        assertThrows(ObjectNotFoundException.class, () -> controller.readSettingNames("feature2", "testScope"));
    }

    @Test
    void testRenameSetting() {
        GenericNamedSettings settings = mockSettings("feature1");
        when(settings.readNames(any())).thenReturn(
                List.of(generateSettingName(1), generateSettingName(3), generateSettingName(8), generateSettingName(42, "testScope2")));
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        ArgumentCaptor<TestModel> modelCaptor = ArgumentCaptor.forClass(TestModel.class);
        controller.renameSetting("feature1", "settingName1", "testScope", "newName1");
        verify(settings, times(1)).save(eq("testScope"), any(), modelCaptor.capture());
        assertEquals("newName1", modelCaptor.getValue().getName());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.renameSetting("feature1", "settingName1", "testScope", ""));
        assertEquals("Setting name required", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> controller.renameSetting("feature1", "settingName1", "testScope", "#qwe"));
        assertEquals("Setting name: only alphanumeric characters, hyphens and spaces are allowed", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> controller.renameSetting("feature1", "settingName1", "testScope", "settingName3"));
        assertEquals("Setting with the specified name already exists", exception.getMessage());

        assertDoesNotThrow(() -> controller.renameSetting("feature1", "settingName1", "testScope2", "settingName3"));

        exception = assertThrows(IllegalArgumentException.class,
                () -> controller.renameSetting("feature1", "settingName1", "testScope2", "settingName42"));
        assertEquals("Setting with the specified name already exists", exception.getMessage());
    }

    @Test
    void testReadRevisionsList() {
        GenericNamedSettings settings = mockSettings("feature1");
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));

        List<Revision> testList = List.of();
        when(settings.listRevisions(any(), any())).thenReturn(testList);
        assertEquals(testList, controller.readRevisionsList("feature1", "someName", "testScope"));

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> controller.readRevisionsList("feature2", "someName", "testScope"));
        assertEquals("No settings found by featureName: feature2", exception.getMessage());
    }

    @Test
    void testDeleteSetting() {
        GenericNamedSettings settings = mockSettings("feature1");
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));
        controller.deleteSetting("feature1", "name1", "testScope");

        ArgumentCaptor<SettingId> settingIdCaptor = ArgumentCaptor.forClass(SettingId.class);
        verify(settings, times(1)).delete(eq("testScope"), settingIdCaptor.capture());
        assertEquals("name1", settingIdCaptor.getValue().getIdentifier());

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> controller.deleteSetting("feature2", "someName", "testScope"));
        assertEquals("No settings found by featureName: feature2", exception.getMessage());
    }

    @Test
    void testReadSetting() {
        GenericNamedSettings settings = mockSettings("feature1");
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));
        SettingsModel settingsModel = controller.readSetting("feature1", "name1", "testScope", "1");
        assertEquals("feature1Model", settingsModel.getName());

        when(settings.read(any(), any(), any())).thenReturn(null);
        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> controller.readSetting("feature1", "name1", "testScope", "1"));
        assertEquals("Cannot find data using specified parameters", exception.getMessage());
    }

    @Test
    void testSaveSetting() {
        GenericNamedSettings settings = mockSettings("feature1");
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));
        when(settings.readNames(any())).thenReturn(
                List.of(generateSettingName(1), generateSettingName(3)));

        TestModel newModel = new TestModel();
        newModel.setName("someArbitraryName");
        when(settings.fromJson(any())).thenReturn(newModel);

        // existing setting
        controller.saveSetting("feature1", "settingName1", "testScope", "jsonContent");
        assertEquals("settingName1", newModel.getName()); // ensure that name was rewritten

        ArgumentCaptor<SettingsModel> modelCaptor = ArgumentCaptor.forClass(SettingsModel.class);
        verify(settings, times(1)).save(eq("testScope"), any(), modelCaptor.capture());
        assertEquals(newModel, modelCaptor.getValue());

        // new setting
        when(settings.getIdByName(any(), eq(true), any())).thenReturn(null);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> controller.saveSetting("feature1", "", "testScope", "jsonContent"));
        assertEquals("Setting name required", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> controller.saveSetting("feature1", "#qwe", "testScope", "jsonContent"));
        assertEquals("Setting name: only alphanumeric characters, hyphens and spaces are allowed", exception.getMessage());

        exception = assertThrows(IllegalArgumentException.class,
                () -> controller.saveSetting("feature1", "settingName3", "testScope", "jsonContent"));
        assertEquals("Setting with the specified name already exists", exception.getMessage());

        // use provided content
        controller.saveSetting("feature1", "settingName1", "testScope5", "jsonContent");

        modelCaptor = ArgumentCaptor.forClass(SettingsModel.class);
        verify(settings, times(1)).save(eq("testScope5"), any(), modelCaptor.capture());
        assertEquals(newModel, modelCaptor.getValue());

        // use default values
        TestModel modelFromDefaultValues = new TestModel();
        when(settings.defaultValues()).thenReturn(modelFromDefaultValues);
        controller.saveSetting("feature1", "settingName1", "testScope7", "");

        modelCaptor = ArgumentCaptor.forClass(SettingsModel.class);
        verify(settings, times(1)).save(eq("testScope7"), any(), modelCaptor.capture());
        assertEquals(modelFromDefaultValues, modelCaptor.getValue());
    }

    @Test
    void testGetDefaultValues() {
        GenericNamedSettings settings = mockSettings("feature1");
        NamedSettingsRegistry.INSTANCE.register(List.of(settings));
        TestModel model = new TestModel();
        when(settings.defaultValues()).thenReturn(model);

        assertEquals(model, controller.getDefaultValues("feature1"));

        ObjectNotFoundException exception = assertThrows(ObjectNotFoundException.class,
                () -> controller.getDefaultValues("feature2"));
        assertEquals("No settings found by featureName: feature2", exception.getMessage());
    }

    private SettingName generateSettingName(int index) {
        return generateSettingName(index, "testScope");
    }

    private SettingName generateSettingName(int index, String scope) {
        return SettingName.builder().id("settingId" + index).name("settingName" + index).scope(scope).build();
    }

    private GenericNamedSettings mockSettings(String featureName) {
        GenericNamedSettings settings = mock(GenericNamedSettings.class);
        lenient().when(settings.getFeatureName()).thenReturn(featureName);
        lenient().when(settings.getIdByName(any(), eq(true), any())).thenReturn(featureName + "Id");
        TestModel model = new TestModel();
        model.setName(featureName + "Model");
        model.setBundleTimestamp("TS");
        lenient().when(settings.read(any(), any(), any())).thenReturn(model);
        return settings;
    }

    private static class TestModel extends SettingsModel {

        @Override
        protected String serializeModelData() {
            return "";
        }

        @Override
        protected void deserializeModelData(String serializedString) {
            // nothing
        }
    }
}