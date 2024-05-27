package ch.sbb.polarion.extension.generic.rest.exception;

import com.polarion.core.util.logging.Logger;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class NotFoundExceptionMapper implements ExceptionMapper<NotFoundException> {
    private static final Logger logger = Logger.getLogger(NotFoundExceptionMapper.class);

    public Response toResponse(NotFoundException e) {
        logger.error("Not found error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
