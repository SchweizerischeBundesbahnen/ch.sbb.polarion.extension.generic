package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.util.logging.Logger;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.UUID;

/**
 * Default mapper for all unmapped exceptions.
 * If you delete this mapper then {@link LogoutFilter}
 * will stop working.
 *
 * <p>Unmapped exceptions carry messages that are not under the application's control
 * (class names, repository paths, parser details). To avoid leaking such internals into the
 * response body, the full exception is logged server-side while the client receives a fixed,
 * generic message.
 *
 * <p>To correlate a client-visible error with its server-side log entry, a random error id is
 * generated per failure. The id is written to the log together with the exception and embedded
 * into the generic response message, so a user can quote it to an administrator who then locates
 * the matching log entry. This mirrors how Polarion itself reports server errors to the UI.
 */
@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Throwable> {

    static final String GENERIC_MESSAGE = "Internal server error";

    private static final Logger logger = Logger.getLogger(UncaughtExceptionMapper.class);

    public Response toResponse(Throwable throwable) {
        String errorId = UUID.randomUUID().toString();
        logger.error("Error ID: " + errorId + " - Error message: " + throwable.getMessage(), throwable);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                .entity(new ErrorEntity(GENERIC_MESSAGE + " (Error ID: " + errorId + ")"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
