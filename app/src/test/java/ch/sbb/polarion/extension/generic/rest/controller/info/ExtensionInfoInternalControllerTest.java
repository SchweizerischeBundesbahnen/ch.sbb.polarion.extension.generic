package ch.sbb.polarion.extension.generic.rest.controller.info;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.util.ExtensionInfo;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * The help articles (about.html / user-guide.html / disclaimer.html) are generated at build time into an extension's
 * webapp resources and read back from the classpath. Extensions with a React administration UI keep
 * them under {@code <ext>-app}, the ones still on the JSP admin UI under {@code <ext>-admin}; both
 * must work. The fixtures live in src/test/resources/webapp/.
 */
class ExtensionInfoInternalControllerTest {

    private final ExtensionInfoInternalController controller = new ExtensionInfoInternalController();

    private MockedStatic<ExtensionInfo> mockExtensionContext(String extensionContext) {
        ExtensionInfo extensionInfo = mock(ExtensionInfo.class);
        when(extensionInfo.getContext()).thenReturn(new Context(extensionContext));
        MockedStatic<ExtensionInfo> mockedStatic = mockStatic(ExtensionInfo.class);
        mockedStatic.when(ExtensionInfo::getInstance).thenReturn(extensionInfo);
        return mockedStatic;
    }

    @Test
    void readsTheArticleFromTheReactAppWebapp() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("react-ext")) {
            assertThat(controller.getDocumentation()).contains("react app about");
        }
    }

    @Test
    void readsTheArticleFromTheLegacyAdminWebapp() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("legacy-ext")) {
            assertThat(controller.getDocumentation()).contains("legacy admin about");
        }
    }

    @Test
    void prefersTheReactAppWebappWhenBothArePresent() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("both-ext")) {
            assertThat(controller.getDocumentation()).contains("react app about");
        }
    }

    @Test
    void readsTheUserGuideTheSameWay() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("react-ext")) {
            assertThat(controller.getUserGuide()).contains("react app user guide");
        }
    }

    @Test
    void readsTheDisclaimerTheSameWay() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("react-ext")) {
            assertThat(controller.getDisclaimer()).contains("react app disclaimer");
        }
    }

    @Test
    void returnsEmptyWhenTheExtensionHasNoDisclaimer() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("legacy-ext")) {
            assertThat(controller.getDisclaimer()).isEmpty();
        }
    }

    @Test
    void returnsEmptyWhenTheArticleWasNotGenerated() {
        try (MockedStatic<ExtensionInfo> ignored = mockExtensionContext("no-html-ext")) {
            assertThat(controller.getDocumentation()).isEmpty();
        }
    }
}
