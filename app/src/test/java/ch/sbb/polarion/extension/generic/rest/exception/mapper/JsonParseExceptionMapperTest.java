package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonParseExceptionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testMalformedJsonMappedToBadRequest() {
        JsonParseException exception = assertThrows(JsonParseException.class,
                () -> objectMapper.readValue("not json at all {{{", List.class));

        try (Response response = new JsonParseExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals(exception.getMessage(), entity.getMessage());
        }
    }
}
