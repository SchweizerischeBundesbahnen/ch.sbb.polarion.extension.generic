package ch.sbb.polarion.extension.generic.rest.exception.mapper;

import ch.sbb.polarion.extension.generic.rest.model.ErrorEntity;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.Response;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class UncaughtExceptionMapperTest {

    private static final Pattern ERROR_ID_PATTERN = Pattern.compile(
            "^" + Pattern.quote(UncaughtExceptionMapper.GENERIC_MESSAGE)
                    + " \\(Error ID: [0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\)$");

    @Test
    void testResponse() {
        String leakyMessage = "com.polarion.internal.Foo failed at /repo/.polarion/records.xml";
        assertDoesNotThrow(() -> {
            try (Response response = new UncaughtExceptionMapper().toResponse(new IllegalAccessException(leakyMessage))) {
                assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
                ErrorEntity entity = (ErrorEntity) response.getEntity();
                assertNotNull(entity);
                assertTrue(ERROR_ID_PATTERN.matcher(entity.getMessage()).matches(),
                        "message must be the generic message followed by a correlation error id, but was: " + entity.getMessage());
                assertFalse(entity.getMessage().contains("com.polarion"), "must not leak class names");
                assertFalse(entity.getMessage().contains("/repo/"), "must not leak repository paths");
            }
        });
    }

    @Test
    void testResponseWithoutMessage() {
        try (Response response = new UncaughtExceptionMapper().toResponse(new NullPointerException())) {
            assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response.getStatus());
            ErrorEntity entity = (ErrorEntity) response.getEntity();
            assertNotNull(entity);
            assertTrue(ERROR_ID_PATTERN.matcher(entity.getMessage()).matches(),
                    "message must be the generic message followed by a correlation error id, but was: " + entity.getMessage());
        }
    }

    @Test
    void testFormatLogMessageUsesExceptionMessage() {
        String logMessage = UncaughtExceptionMapper.formatLogMessage("the-error-id", new IllegalStateException("boom"));
        assertEquals("Error ID: the-error-id - Error message: boom", logMessage);
    }

    @Test
    void testFormatLogMessageFallsBackWhenMessageIsNull() {
        String logMessage = UncaughtExceptionMapper.formatLogMessage("the-error-id", new NullPointerException());
        assertEquals("Error ID: the-error-id - Error message: <no message>", logMessage);
    }

    @Test
    void testEachResponseGetsUniqueErrorId() {
        try (Response first = new UncaughtExceptionMapper().toResponse(new IllegalStateException("boom"));
             Response second = new UncaughtExceptionMapper().toResponse(new IllegalStateException("boom"))) {
            String firstMessage = ((ErrorEntity) first.getEntity()).getMessage();
            String secondMessage = ((ErrorEntity) second.getEntity()).getMessage();
            assertNotEquals(firstMessage, secondMessage, "each error must carry its own correlation id");
        }
    }

}
