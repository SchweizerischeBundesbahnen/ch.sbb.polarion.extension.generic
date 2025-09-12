package ch.sbb.polarion.extension.generic.rest;

import ch.sbb.polarion.extension.generic.test_extensions.PlatformContextMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith({MockitoExtension.class, PlatformContextMockExtension.class})
class GenericRestApplicationTest {

    @Test
    void getSingletons() {
        GenericRestApplication app = new GenericRestApplication();
        assertFalse(app.getSingletons().isEmpty());
    }
}
