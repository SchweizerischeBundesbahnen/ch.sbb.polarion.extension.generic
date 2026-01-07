package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.exception.DuplicateSettingNameException;
import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.polarion.core.util.logging.Logger;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class DuplicateSettingNameExceptionMapper implements ExceptionMapper<DuplicateSettingNameException> {
    private static final Logger logger = Logger.getLogger(DuplicateSettingNameExceptionMapper.class);

    @Override
    public Response toResponse(DuplicateSettingNameException e) {
        logger.error("Duplicate setting name conflict: " + e.getMessage(), e);
        return Response.status(Response.Status.CONFLICT.getStatusCode())
                .entity(new ErrorEntity(e.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
