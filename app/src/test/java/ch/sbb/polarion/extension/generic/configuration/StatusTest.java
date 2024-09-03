package ch.sbb.polarion.extension.generic.configuration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatusTest {
    @Test
    void testColors() {
        assertEquals("<span style='color: green;'>OK</span>", Status.OK.toHtml());
        assertEquals("<span style='color: orange;'>WARNING</span>", Status.WARNING.toHtml());
        assertEquals("<span style='color: red;'>ERROR</span>", Status.ERROR.toHtml());
    }
}
