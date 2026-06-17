package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Maps a well-formed JSON request body that does not match the expected structure to a 400 Bad
 * Request.
 *
 * <p>{@link MismatchedInputException} is Jackson's input-binding failure: wrong shape (object where
 * an array is expected), unknown field ({@code UnrecognizedPropertyException}), wrong value type
 * ({@code InvalidFormatException}), and similar. These are client errors. Without this mapper they
 * fall through to {@link UncaughtExceptionMapper} and surface as 500.
 *
 * <p>Scoped to {@code MismatchedInputException} rather than its parent {@code JsonMappingException},
 * so that server-side serialization failures (e.g. {@code InvalidDefinitionException},
 * {@code JsonGenerationException}) keep producing 500 instead of being masked as client errors.
 *
 * <p>The parser message is returned to the client to aid diagnosis. It may reference DTO class
 * names and package paths, but these extensions are open-source, so that detail is not sensitive.
 */
@Provider
public class MismatchedInputExceptionMapper implements ExceptionMapper<MismatchedInputException> {

    private static final Logger logger = Logger.getLogger(MismatchedInputExceptionMapper.class);

    @Override
    public Response toResponse(MismatchedInputException exception) {
        logger.warn("Request body does not match the expected structure: " + exception.getMessage(), exception);
        return Response.status(Response.Status.BAD_REQUEST.getStatusCode())
                .entity(new ErrorEntity(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
