package ch.sbb.polarion.extension.generic.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class InternalServerErrorExceptionMapper implements ExceptionMapper<InternalServerErrorException> {
    private static final Logger logger = Logger.getLogger(InternalServerErrorExceptionMapper.class);

    public Response toResponse(InternalServerErrorException e) {
        logger.error("Internal server error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
