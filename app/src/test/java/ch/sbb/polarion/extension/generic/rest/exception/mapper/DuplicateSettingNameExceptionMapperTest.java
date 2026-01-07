package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.exception.DuplicateSettingNameException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DuplicateSettingNameExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new DuplicateSettingNameExceptionMapper().toResponse(new DuplicateSettingNameException("test message"))) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        }
    }

}
