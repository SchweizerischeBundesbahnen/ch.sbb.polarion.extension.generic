package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.platform.security.PermissionDeniedException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionDeniedExceptionMapperTest {

    @Test
    void testResponse() {
        PermissionDeniedException exception = mock(PermissionDeniedException.class);
        when(exception.getMessage()).thenReturn("test message");

        try (Response response = new PermissionDeniedExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        }
    }

}
