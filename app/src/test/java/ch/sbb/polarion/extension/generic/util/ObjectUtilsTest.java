package ch.sbb.polarion.extension.generic.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilsTest {

    @Test
    void requireNotNull_withNonNullObject_shouldReturnObject() {
        // Arrange
        String expected = "test";

        // Act
        String result = ObjectUtils.requireNotNull(expected, "Message");

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void requireNotNull_withNullObject_shouldThrowException() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> {
            ObjectUtils.requireNotNull(null, "Message");
        });
    }
}