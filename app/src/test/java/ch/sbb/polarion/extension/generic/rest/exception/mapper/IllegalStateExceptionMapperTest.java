package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class IllegalStateExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new IllegalStateExceptionMapper().toResponse(new IllegalStateException("test message"))) {
            assertEquals(Response.Status.CONFLICT.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

}
