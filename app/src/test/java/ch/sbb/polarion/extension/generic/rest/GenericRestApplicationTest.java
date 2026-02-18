package ch.sbb.polarion.extension.generic.rest;

import ch.sbb.polarion.extension.generic.rest.controller.settings.NamedSettingsApiController;
import ch.sbb.polarion.extension.generic.rest.controller.settings.NamedSettingsInternalController;
import ch.sbb.polarion.extension.generic.rest.feature.LifecycleBindingFeature;
import ch.sbb.polarion.extension.generic.settings.GenericNamedSettings;
import ch.sbb.polarion.extension.generic.settings.NamedSettingsRegistry;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class GenericRestApplicationTest {

    @BeforeEach
    @AfterEach
    void clearRegistry() {
        NamedSettingsRegistry.INSTANCE.getAll().clear();
    }

    @Test
    void getSingletons() {
        GenericRestApplication app = new GenericRestApplication();
        assertFalse(app.getSingletons().isEmpty());
    }

    @Test
    void getSingletonsContainsLifecycleBindingFeature() {
        GenericRestApplication app = new GenericRestApplication();
        assertTrue(app.getSingletons().stream().anyMatch(LifecycleBindingFeature.class::isInstance));
    }

    @Test
    void getClassesContainsGenericControllers() {
        GenericRestApplication app = new GenericRestApplication();
        Set<Class<?>> classes = app.getClasses();
        assertFalse(classes.isEmpty());
        assertFalse(classes.contains(NamedSettingsApiController.class));
        assertFalse(classes.contains(NamedSettingsInternalController.class));
    }

    @Test
    void getClassesContainsNamedSettingsControllersWhenRegistryNotEmpty() {
        GenericNamedSettings<?> mockSettings = mock(GenericNamedSettings.class);
        NamedSettingsRegistry.INSTANCE.register(List.of(mockSettings));

        GenericRestApplication app = new GenericRestApplication();
        Set<Class<?>> classes = app.getClasses();
        assertTrue(classes.contains(NamedSettingsApiController.class));
        assertTrue(classes.contains(NamedSettingsInternalController.class));
    }
}
