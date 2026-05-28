package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalStateExceptionMapper implements ExceptionMapper<IllegalStateException> {
    private final Logger logger = Logger.getLogger(IllegalStateExceptionMapper.class);

    public Response toResponse(IllegalStateException e) {
        logger.error("Illegal state error: " + e.getMessage(), e);
        return Response.status(Response.Status.CONFLICT.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
