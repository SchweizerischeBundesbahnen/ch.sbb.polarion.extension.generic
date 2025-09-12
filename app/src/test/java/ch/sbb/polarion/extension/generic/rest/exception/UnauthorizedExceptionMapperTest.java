package ch.sbb.polarion.extension.generic.rest.exception;

import ch.sbb.polarion.extension.generic.rest.exception.mapper.UnauthorizedExceptionMapper;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new UnauthorizedExceptionMapper().toResponse(new UnauthorizedException("test message"))) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

}
