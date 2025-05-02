package ch.sbb.polarion.extension.generic;

import com.polarion.alm.ui.server.forms.extensions.IFormExtension;
import com.polarion.alm.ui.server.forms.extensions.impl.FormExtensionsRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GenericBundleActivatorTest {

    @Mock
    private FormExtensionsRegistry registry;

    @Test
    void testBundleActivator() {
        try (MockedStatic<FormExtensionsRegistry> extensionsRegistryStatic = mockStatic(FormExtensionsRegistry.class)) {
            extensionsRegistryStatic.when(FormExtensionsRegistry::getInstance).thenReturn(registry);

            new TestBundleActivator(Map.of()).start(null);
            verify(registry, times(0)).registerExtension(anyString(), any());

            new TestBundleActivator(Map.of("first", mock(IFormExtension.class), "second", mock(IFormExtension.class))).start(null);
            verify(registry, times(1)).registerExtension(eq("first"), any());
            verify(registry, times(1)).registerExtension(eq("second"), any());
        }
    }


    private static class TestBundleActivator extends GenericBundleActivator {

        Map<String, IFormExtension> extensions;

        public TestBundleActivator(Map<String, IFormExtension> extensions) {
            this.extensions = extensions;
        }

        @Override
        protected Map<String, IFormExtension> getExtensions() {
            return extensions;
        }
    }
}
