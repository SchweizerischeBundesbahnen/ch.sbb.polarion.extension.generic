package ch.sbb.polarion.extension.generic.properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GetterFinderTest {

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