package ch.sbb.polarion.extension.generic.rest.exception;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import ch.sbb.polarion.extension.generic.rest.filter.LogoutFilter;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Default mapper for all unmapped exceptions.
 * If you delete this mapper then {@link LogoutFilter}
 * will stop working.
 */
@Provider
public class UncaughtExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger logger = Logger.getLogger(UncaughtExceptionMapper.class);

    public Response toResponse(Throwable throwable) {
         logger.error("Error in controller: " + throwable.getMessage(), throwable);
        if (throwable instanceof WebApplicationException webapplicationexception) {
            //this block covers cases when the specific WebApplicationException was thrown but
            //there is no explicit mapper for it (e.g. NotAuthorizedException)
            return webapplicationexception.getResponse();
        } else {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode())
                    .entity(new ErrorEntity(throwable.getMessage()))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }
    }
}
