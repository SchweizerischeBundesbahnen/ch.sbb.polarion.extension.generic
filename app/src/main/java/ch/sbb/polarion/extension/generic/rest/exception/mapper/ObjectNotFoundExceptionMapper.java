package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.exception.ObjectNotFoundException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ObjectNotFoundExceptionMapper implements ExceptionMapper<ObjectNotFoundException> {
    private static final Logger logger = Logger.getLogger(ObjectNotFoundExceptionMapper.class);

    public Response toResponse(ObjectNotFoundException e) {
        logger.error("Not found error in controller: " + e.getMessage(), e);
        return Response.status(Response.Status.NOT_FOUND.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
