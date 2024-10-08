package ch.sbb.polarion.extension.generic.context;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.util.ContextUtils;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

public class CurrentContextExtension implements BeforeEachCallback, AfterEachCallback {

    private MockedStatic<ContextUtils> contextUtilsMockedStatic;

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        contextUtilsMockedStatic = Mockito.mockStatic(ContextUtils.class);

        String contextName = "test-extension";
        CurrentContextConfig currentContextConfig = extensionContext.getRequiredTestClass().getAnnotation(CurrentContextConfig.class);
        if (currentContextConfig != null) {
            contextName = currentContextConfig.value();
        }

        Context context = new Context(contextName);
        contextUtilsMockedStatic.when(ContextUtils::getContext).thenReturn(context);
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) throws Exception {
        if (contextUtilsMockedStatic != null) {
            contextUtilsMockedStatic.close();
        }
    }
}
