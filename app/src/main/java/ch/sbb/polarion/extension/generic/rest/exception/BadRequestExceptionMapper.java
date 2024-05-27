package ch.sbb.polarion.extension.generic.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class BadRequestExceptionMapper implements ExceptionMapper<BadRequestException> {
    private static final Logger logger = Logger.getLogger(BadRequestExceptionMapper.class);

    public Response toResponse(BadRequestException e) {
        logger.error("Bad request error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
