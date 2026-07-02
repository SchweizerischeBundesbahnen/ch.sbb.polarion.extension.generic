package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class NotFoundExceptionMapperTest {

    @Test
    void testResponse() {
        assertDoesNotThrow(() -> {
            try (Response response = new NotFoundExceptionMapper().toResponse(new NotFoundException("test message"))) {
                assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
                ErrorEntity entity = (ErrorEntity) response.getEntity();
                assertNotNull(entity);
                assertEquals("test message", entity.getMessage());
            }
        });
    }

}
