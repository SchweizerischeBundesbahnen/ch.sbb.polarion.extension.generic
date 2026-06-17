package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MismatchedInputExceptionMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testShapeMismatchMappedToBadRequest() {
        // Reproduces the ticket case: a JSON object posted to an endpoint expecting a JSON array.
        JavaType listOfIntegers = objectMapper.getTypeFactory().constructCollectionType(List.class, Integer.class);
        MismatchedInputException exception = assertThrows(MismatchedInputException.class,
                () -> objectMapper.readValue("{\"weight\":\"xxx\"}", listOfIntegers));

        try (Response response = new MismatchedInputExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals(exception.getMessage(), entity.getMessage());
        }
    }

    @Test
    void testWrongValueTypeMappedToBadRequest() {
        MismatchedInputException exception = assertThrows(MismatchedInputException.class,
                () -> objectMapper.readValue("\"abc\"", Integer.class));

        try (Response response = new MismatchedInputExceptionMapper().toResponse(exception)) {
            assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertEquals(exception.getMessage(), entity.getMessage());
        }
    }

}
