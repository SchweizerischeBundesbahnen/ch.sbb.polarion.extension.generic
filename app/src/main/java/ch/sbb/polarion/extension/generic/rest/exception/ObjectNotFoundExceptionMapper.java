package ch.sbb.polarion.extension.generic.rest.exception;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectNotFoundExceptionMapper implements ExceptionMapper<ObjectNotFoundException> {
    private static final Logger logger = Logger.getLogger(ObjectNotFoundExceptionMapper.class);

    public Response toResponse(ObjectNotFoundException e) {
        logger.error("Not found error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(e.getMessage())
                .build();
    }
}
