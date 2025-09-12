package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class UncaughtExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new UncaughtExceptionMapper().toResponse(new IllegalAccessException("test message"))) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

}
