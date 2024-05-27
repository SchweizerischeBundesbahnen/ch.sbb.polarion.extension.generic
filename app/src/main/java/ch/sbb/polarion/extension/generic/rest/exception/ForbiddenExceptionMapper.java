package ch.sbb.polarion.extension.generic.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.ForbiddenException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {
    private static final Logger logger = Logger.getLogger(ForbiddenExceptionMapper.class);

    public Response toResponse(ForbiddenException e) {
        logger.error("Forbidden error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.FORBIDDEN.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
