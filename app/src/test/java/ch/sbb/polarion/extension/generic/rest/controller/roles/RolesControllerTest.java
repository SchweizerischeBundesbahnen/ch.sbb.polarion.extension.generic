package ch.sbb.polarion.extension.generic.rest.controller.roles;

import ch.sbb.polarion.extension.generic.rest.model.RolesInfo;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class RolesControllerTest {

    private static final String SCOPE = "project/project_id/";

    @Mock
    private PolarionService polarionService;

    @BeforeEach
    void init() {
        // lenient: the constructor test uses neither the service nor this stub.
        lenient().when(polarionService.getGlobalRoles()).thenReturn(List.of("admin"));
    }

    @Test
    void testGetRoles() {
        when(polarionService.getProjectRoles(SCOPE)).thenReturn(Set.of("project_admin"));

        RolesInfo roles = new RolesInternalController(polarionService).getRoles(SCOPE);

        assertEquals(List.of("admin"), roles.getGlobalRoles());
        assertEquals(List.of("project_admin"), roles.getProjectRoles());
    }

    @Test
    void testGetRolesWithoutProjectScope() {
        when(polarionService.getProjectRoles(null)).thenReturn(Set.of());

        RolesInfo roles = new RolesInternalController(polarionService).getRoles(null);

        assertEquals(List.of("admin"), roles.getGlobalRoles());
        assertTrue(roles.getProjectRoles().isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetRolesThroughTheApiVariant() {
        // A token caller has no Polarion session, so the api variant elevates before reading the
        // security service - the same pairing the settings controllers use.
        when(polarionService.getProjectRoles(SCOPE)).thenReturn(Set.of("project_admin"));
        when(polarionService.callPrivileged(any(Callable.class)))
                .thenAnswer(invocation -> ((Callable<Object>) invocation.getArgument(0)).call());

        RolesInfo roles = new RolesApiController(polarionService).getRoles(SCOPE);

        assertEquals(List.of("admin"), roles.getGlobalRoles());
        assertEquals(List.of("project_admin"), roles.getProjectRoles());
        verify(polarionService).callPrivileged(any(Callable.class));
    }

    @Test
    void testDefaultConstructors() {
        // The constructors Polarion itself uses: they build their own PolarionService from the platform.
        assertNotNull(new RolesInternalController());
        assertNotNull(new RolesApiController());
    }
}
