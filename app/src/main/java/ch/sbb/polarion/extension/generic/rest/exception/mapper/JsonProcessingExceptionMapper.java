package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps Jackson deserialization failures of the request body to a 400 Bad Request.
 *
 * <p>A malformed or structurally invalid JSON body is a client error, not a server fault, so it
 * must surface as 400 rather than the 500 that {@link UncaughtExceptionMapper} would otherwise
 * produce for an unmapped exception. This mapper is more specific than the {@code Throwable}
 * fallback, so JAX-RS selects it for body-deserialization failures. It covers
 * {@code JsonParseException}, {@code MismatchedInputException} and
 * {@code UnrecognizedPropertyException}, which all extend {@link JsonProcessingException}.
 *
 * <p>The parser message is returned to the client to help diagnose the malformed request. It may
 * reference DTO class names and package paths, but these extensions are open-source, so that detail
 * is not sensitive.
 */
@Provider
public class JsonProcessingExceptionMapper implements ExceptionMapper<JsonProcessingException> {

    private static final Logger logger = Logger.getLogger(JsonProcessingExceptionMapper.class);

    @Override
    public Response toResponse(JsonProcessingException exception) {
        logger.warn("Invalid JSON in request body: " + exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .entity(new ErrorEntity(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
