package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class InternalServerErrorExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new InternalServerErrorExceptionMapper().toResponse(new InternalServerErrorException("test message"))) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        }
    }

}
