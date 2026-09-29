package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.jobs.JobsRegistryShutDownException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import static org.junit.jupiter.api.Assertions.*;

class JobsRegistryShutDownExceptionMapperTest {

    @Test
    void testResponse() {
        try (Response response = new JobsRegistryShutDownExceptionMapper().toResponse(new JobsRegistryShutDownException("test message"))) {
            assertEquals(Response.Status.SERVICE_UNAVAILABLE.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals("test message", entity.getMessage());
        }
    }

}
