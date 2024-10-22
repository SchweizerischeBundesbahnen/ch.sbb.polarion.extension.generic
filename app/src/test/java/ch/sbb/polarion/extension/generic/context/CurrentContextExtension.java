package ch.sbb.polarion.extension.generic.context;

import ch.sbb.polarion.extension.generic.properties.ExtensionConfiguration;
import ch.sbb.polarion.extension.generic.util.ContextUtils;
import ch.sbb.polarion.extension.generic.util.ManifestUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;

import java.util.Set;
import java.util.jar.Attributes;

import static org.mockito.Mockito.mockStatic;

public class CurrentContextExtension implements BeforeEachCallback, AfterEachCallback {

    public static final String TEST_EXTENSION = "test-extension";

    private MockedStatic<ContextUtils> contextUtilsMockedStatic;
    private MockedStatic<ManifestUtils> manifestUtilsMockedStatic;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        contextUtilsMockedStatic = mockStatic(ContextUtils.class);

        String contextName = TEST_EXTENSION;
        CurrentContextConfig annotation = extensionContext.getRequiredTestClass().getAnnotation(CurrentContextConfig.class);
        if (annotation != null) {
            contextName = annotation.value();
            Class<?> testExtensionConfigurationClass = annotation.extensionConfiguration();
            contextUtilsMockedStatic.when(() -> ContextUtils.findSubTypes(ExtensionConfiguration.class)).thenReturn(Set.of(testExtensionConfigurationClass));
        }

        manifestUtilsMockedStatic = mockStatic(ManifestUtils.class);
        Attributes attributes = new Attributes();
        attributes.put(new Attributes.Name(ContextUtils.EXTENSION_CONTEXT), contextName);
        attributes.put(new Attributes.Name(ContextUtils.DISCOVER_BASE_PACKAGE), ContextUtils.CH_SBB_POLARION_EXTENSION + contextName.replace("-", "_"));
        attributes.put(new Attributes.Name(ContextUtils.CONFIGURATION_PROPERTIES_PREFIX), ContextUtils.CH_SBB_POLARION_EXTENSION + contextName.replace("-", "_") + ".");
        manifestUtilsMockedStatic.when(ManifestUtils::getManifestAttributes).thenReturn(attributes);

        contextUtilsMockedStatic.when(ContextUtils::getContext).thenCallRealMethod();
        contextUtilsMockedStatic.when(ContextUtils::getConfigurationPropertiesPrefix).thenCallRealMethod();
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (manifestUtilsMockedStatic != null) {
            manifestUtilsMockedStatic.close();
        }
        if (contextUtilsMockedStatic != null) {
            contextUtilsMockedStatic.close();
        }
    }

}
