package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonProcessingExceptionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testParseErrorMappedToBadRequest() {
        JsonProcessingException exception = assertThrows(JsonProcessingException.class,
                () -> objectMapper.readValue("not json at all {{{", List.class));

        try (Response response = new JsonProcessingExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals(exception.getMessage(), entity.getMessage());
        }
    }

    @Test
    void testShapeMismatchExposesParserMessage() {
        // Reproduces the ticket case: a JSON object posted to an endpoint expecting a JSON array.
        JavaType listOfIntegers = objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class);
        JsonProcessingException exception = assertThrows(JsonProcessingException.class,
                () -> objectMapper.readValue("{\"weight\":\"xxx\"}", listOfIntegers));

        try (Response response = new JsonProcessingExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals(exception.getMessage(), entity.getMessage());
        }
    }

}
