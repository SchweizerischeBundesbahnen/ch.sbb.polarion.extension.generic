package ch.sbb.polarion.extension.generic.rest.controller.roles;

import ch.sbb.polarion.extension.generic.rest.model.RolesInfo;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * The api twin, {@link RolesApiController}, is four lines of delegation and has no test of its own -
 * neither does the settings pair it follows, and it offers no injecting constructor to build one with.
 */
@ExtendWith(MockitoExtension.class)
class RolesControllerTest {

    private static final String SCOPE = "project/project_id/";

    @Mock
    private PolarionService polarionService;

    @BeforeEach
    void init() {
        when(polarionService.getGlobalRoles()).thenReturn(List.of("admin"));
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
}
