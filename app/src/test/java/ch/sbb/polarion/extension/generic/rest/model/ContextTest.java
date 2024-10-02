package ch.sbb.polarion.extension.generic.rest.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContextTest {

    @Test
    void testContextCreation() {
        assertEquals("test-extension", new Context("test-extension").getExtensionContext());

        assertThrows(IllegalArgumentException.class, () -> new Context(""));
        assertThrows(IllegalArgumentException.class, () -> new Context("   "));
    }
}