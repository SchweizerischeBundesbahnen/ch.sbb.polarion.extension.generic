package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;
import com.polarion.platform.security.PermissionDeniedException;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Polarion marks this exception as user-friendly, so its message is passed on.
 */
@Provider
public class PermissionDeniedExceptionMapper implements ExceptionMapper<PermissionDeniedException> {
    private static final Logger logger = Logger.getLogger(PermissionDeniedExceptionMapper.class);

    public Response toResponse(PermissionDeniedException e) {
        logger.warn("Permission denied in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.FORBIDDEN.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
