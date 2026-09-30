package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.jobs.JobsRegistryShutDownException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Answers a job refused because its extension is stopping with 503: the service is unavailable, not overloaded.
 */
@Provider
public class JobsRegistryShutDownExceptionMapper implements ExceptionMapper<JobsRegistryShutDownException> {
    private final Logger logger = Logger.getLogger(JobsRegistryShutDownExceptionMapper.class);

    @Override
    public Response toResponse(JobsRegistryShutDownException e) {
        logger.warn("Job refused: " + e.getMessage());
        return Response.status(Response.Status.SERVICE_UNAVAILABLE.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
