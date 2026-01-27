package ch.sbb.polarion.extension.generic.rest.exception;

import org.junit.jupiter.api.Test;

import javax.ws.rs.NotAuthorizedException;

import static org.junit.jupiter.api.Assertions.*;

class UnauthorizedExceptionTest {

    @Test
    void testConstructor() {
        String message = "test unauthorized message";
        UnauthorizedException exception = new UnauthorizedException(message);

        assertInstanceOf(NotAuthorizedException.class, exception);
        assertEquals(message, exception.getMessage());
    }

}
