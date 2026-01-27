package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.exception.UnauthorizedException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class NotAuthorizedExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new NotAuthorizedExceptionMapper().toResponse(new NotAuthorizedException("test message", "Bearer"))) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

    @Test
    void testUnauthorizedExceptionResponse() {
        try (Response response = new NotAuthorizedExceptionMapper().toResponse(new UnauthorizedException("unauthorized message"))) {
            assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("unauthorized message", entity.getMessage());
        } catch (Exception e) {
            fail("Exception thrown: " + e.getMessage());
        }
    }

}
