package ch.sbb.polarion.extension.generic.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {
    private static final Logger logger = Logger.getLogger(IllegalArgumentExceptionMapper.class);

    public Response toResponse(IllegalArgumentException e) {
        logger.error("Illegal argument error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
