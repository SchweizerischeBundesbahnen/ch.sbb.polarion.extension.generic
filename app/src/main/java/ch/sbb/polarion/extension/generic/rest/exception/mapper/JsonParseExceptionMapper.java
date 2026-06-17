package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.core.JsonParseException;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps malformed JSON in the request body to a 400 Bad Request.
 *
 * <p>A {@link JsonParseException} means the request bytes are not well-formed JSON, which is a
 * client error. Without this mapper it falls through to {@link UncaughtExceptionMapper} and
 * surfaces as 500.
 *
 * <p>Deliberately scoped to the parse exception rather than its parent
 * {@code JsonProcessingException}, so that server-side serialization failures
 * ({@code JsonGenerationException}) keep producing 500 instead of being masked as client errors.
 *
 * <p>The parser message is returned to the client to aid diagnosis. It may reference class names
 * and package paths, but these extensions are open-source, so that detail is not sensitive.
 */
@Provider
public class JsonParseExceptionMapper implements ExceptionMapper<JsonParseException> {

    private static final Logger logger = Logger.getLogger(JsonParseExceptionMapper.class);

    @Override
    public Response toResponse(JsonParseException exception) {
        logger.warn("Malformed JSON in request body: " + exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .entity(new ErrorEntity(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
