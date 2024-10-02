package ch.sbb.polarion.extension.generic.properties;

import ch.sbb.polarion.extension.generic.rest.model.Context;
import ch.sbb.polarion.extension.generic.util.ContextUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class GetterFinderTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    MockedStatic<ContextUtils> contextUtils;

    @BeforeEach
    void setUp() {
        Context context = new Context("test-extension");
        contextUtils.when(ContextUtils::getContext).thenReturn(context);
    }

    @AfterEach
    void tearDown() {
        contextUtils.close();
    }

    @Test
    void testGetValue() {
        TestExtensionConfiguration configuration = new TestExtensionConfiguration();

        // simple getter
        assertEquals("stringGetterValue", GetterFinder.getValue(configuration, "stringGetter"));

        // string 'is' getter
        assertNull(GetterFinder.getValue(configuration, "inconvenientStringGetter"));

        // string 'is' getter
        assertNull(GetterFinder.getValue(configuration, "inconvenientStringGetter"));

        // boolean wrapper getter
        assertEquals(String.valueOf(true), GetterFinder.getValue(configuration, "booleanWrapperGetter"));

        // boolean primitive getter
        assertEquals(String.valueOf(true), GetterFinder.getValue(configuration, "booleanPrimitiveGetter"));

        // boolean wrapper getter, is this expected behavior?
        assertNull(GetterFinder.getValue(configuration, "booleanWrapperIsGetter"));

        // boolean primitive getter
        assertEquals(String.valueOf(true), GetterFinder.getValue(configuration, "booleanPrimitiveIsGetter"));

        // int wrapper getter
        assertEquals("42", GetterFinder.getValue(configuration, "intWrapperGetter"));

        // int primitive getter
        assertEquals("42", GetterFinder.getValue(configuration, "intPrimitiveGetter"));
    }

    @SuppressWarnings("unused")
    private static final class TestExtensionConfiguration extends ExtensionConfiguration {

        public String getStringGetter() {
            return "stringGetterValue";
        }

        public String isInconvenientStringGetter() {
            return "testValue1Value";
        }

        public Boolean getBooleanWrapperGetter() {
            return Boolean.TRUE;
        }

        public boolean getBooleanPrimitiveGetter() {
            return Boolean.TRUE;
        }

        public Boolean isBooleanWrapperIsGetter() {
            return Boolean.TRUE;
        }

        public boolean isBooleanPrimitiveIsGetter() {
            return Boolean.TRUE;
        }

        public Integer getIntWrapperGetter() {
            return 42;
        }

        public int getIntPrimitiveGetter() {
            return 42;
        }
    }
}