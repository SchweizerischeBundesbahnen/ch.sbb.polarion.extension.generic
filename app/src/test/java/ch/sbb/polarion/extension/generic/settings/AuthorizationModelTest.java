package ch.sbb.polarion.extension.generic.settings;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorizationModelTest {

    @Test
    void testSerializeAndDeserialize() {
        AuthorizationModel model = new AuthorizationModel();
        model.setGlobalRoles("admin", "user");
        model.setProjectRoles("project_admin");

        String serialized = model.serialize();
        assertTrue(serialized.contains(AuthorizationModel.GLOBAL_ROLES));
        assertTrue(serialized.contains(AuthorizationModel.PROJECT_ROLES));

        AuthorizationModel deserialized = new AuthorizationModel();
        deserialized.deserialize(serialized);

        assertEquals(List.of("admin", "user"), deserialized.getGlobalRoles());
        assertEquals(List.of("project_admin"), deserialized.getProjectRoles());
    }

    @Test
    void testDeserializeEmptyRoles() {
        AuthorizationModel model = new AuthorizationModel();
        model.setGlobalRoles();
        model.setProjectRoles();

        AuthorizationModel deserialized = new AuthorizationModel();
        deserialized.deserialize(model.serialize());

        assertTrue(deserialized.getGlobalRoles().isEmpty());
        assertTrue(deserialized.getProjectRoles().isEmpty());
    }

    @Test
    void testSerializeRolesNotSet() {
        // A model that was never populated must still serialize; the entries are simply left out.
        AuthorizationModel model = new AuthorizationModel();

        String serialized = model.serialize();

        assertTrue(serialized.contains(AuthorizationModel.GLOBAL_ROLES));
        assertTrue(serialized.contains(AuthorizationModel.PROJECT_ROLES));
    }

    @Test
    void testGetAllRoles() {
        AuthorizationModel model = new AuthorizationModel();
        model.setGlobalRoles("admin");
        model.setProjectRoles("project_admin", "lead");

        assertEquals(List.of("admin", "project_admin", "lead"), model.getAllRoles());
    }

    @Test
    void testDeserializeIgnoresBlankEntries() {
        AuthorizationModel model = new AuthorizationModel();

        model.deserializeModelData(model.serializeEntry(AuthorizationModel.GLOBAL_ROLES, "admin, ,user")
                + model.serializeEntry(AuthorizationModel.PROJECT_ROLES, ""));

        assertEquals(List.of("admin", "user"), model.getGlobalRoles());
        assertTrue(model.getProjectRoles().isEmpty());
    }

    @Test
    void testDeserializeMissingEntries() {
        // A setting written before the roles existed, or edited by hand, has no such block at all; it
        // must read as "nothing granted" rather than failing the whole load.
        AuthorizationModel model = new AuthorizationModel();

        model.deserialize(model.serializeEntry(SettingsModel.NAME, "Default"));

        assertTrue(model.getGlobalRoles().isEmpty());
        assertTrue(model.getProjectRoles().isEmpty());
        assertTrue(model.getAllRoles().isEmpty());
    }
}
