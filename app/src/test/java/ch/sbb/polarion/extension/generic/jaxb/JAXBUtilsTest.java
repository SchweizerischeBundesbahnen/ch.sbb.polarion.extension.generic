package ch.sbb.polarion.extension.generic.jaxb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.UnmarshalException;
import java.io.IOException;
import java.io.Reader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class JAXBUtilsTest {

    private static final String TEST_OBJECT = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" + System.lineSeparator() +
            "<TestObject>" + System.lineSeparator() +
            "    <testKey>key</testKey>" + System.lineSeparator() +
            "    <testValue>100</testValue>" + System.lineSeparator() +
            "</TestObject>" + System.lineSeparator();

    @Test
    void deserialize() throws JAXBException {
        TestObject testObject = JAXBUtils.deserialize(TestObject.class, TEST_OBJECT);

        assertEquals("key", testObject.getTestKey());
        assertEquals(100, testObject.getTestValue());
        assertNull(JAXBUtils.deserialize(TestObject.class, (String) null));
        assertNull(JAXBUtils.deserialize(TestObject.class, (Reader) null));
        assertNull(JAXBUtils.deserialize(null, mock(Reader.class)));
    }

    @Test
    void serialize() throws JAXBException, IOException {
        TestObject testObject = new TestObject("key", 100);
        String serialized = JAXBUtils.serialize(testObject);

        assertEquals(TEST_OBJECT.replace(System.lineSeparator(), "\n"), serialized);
    }

    /**
     * Samples cover each illegal range: control chars 0x0-0x8 / 0xB-0xC / 0xE-0x1F,
     * the surrogate block 0xD800-0xDFFF and the noncharacters 0xFFFE-0xFFFF.
     */
    @ParameterizedTest
    @ValueSource(ints = {0x0000, 0x0008, 0x000B, 0x001F, 0xD800, 0xFFFE, 0xFFFF})
    void serializeStripsCharsIllegalInXml(int illegalCodePoint) throws JAXBException, IOException {
        String illegalChar = String.valueOf((char) illegalCodePoint);
        TestObject testObject = new TestObject("prefix" + illegalChar + "suffix", 100);

        String serialized = JAXBUtils.serialize(testObject);
        TestObject deserialized = JAXBUtils.deserialize(TestObject.class, serialized);

        assertEquals("prefixsuffix", deserialized.getTestKey());
    }

    /**
     * Legal samples cover the singletons (TAB, LF, SPACE), both BMP bands bordering the surrogate
     * gap (0xD7FF / 0xE000 / 0xFFFD) and a supplementary code point (emoji at 0x1F600).
     */
    @ParameterizedTest
    @ValueSource(ints = {0x0009, 0x000A, 0x0020, 0xD7FF, 0xE000, 0xFFFD, 0x1F600})
    void serializeAcceptsCharsLegalInXml(int legalCodePoint) throws JAXBException, IOException {
        String testKey = new String(Character.toChars(legalCodePoint));
        TestObject testObject = new TestObject(testKey, 100);

        String serialized = JAXBUtils.serialize(testObject);
        TestObject deserialized = JAXBUtils.deserialize(TestObject.class, serialized);

        assertEquals(testKey, deserialized.getTestKey());
    }

    @Test
    void serializePreservesXmlMarkupChars() throws JAXBException, IOException {
        TestObject testObject = new TestObject("a <b> & \"c\"", 100);

        String serialized = JAXBUtils.serialize(testObject);
        TestObject deserialized = JAXBUtils.deserialize(TestObject.class, serialized);

        assertEquals("a <b> & \"c\"", deserialized.getTestKey());
    }

    /**
     * Proves the reason for stripping illegal characters in {@link JAXBUtils#serialize}: XML which contains
     * a character illegal in XML 1.0 cannot be unmarshalled back, so it must never be produced.
     * Samples cover each illegal range: control chars 0x0-0x8 / 0xB-0xC / 0xE-0x1F,
     * the surrogate block 0xD800-0xDFFF and the noncharacters 0xFFFE-0xFFFF.
     */
    @ParameterizedTest
    @ValueSource(ints = {0x0000, 0x0004, 0x0008, 0x000B, 0x000C, 0x000E, 0x0013, 0x001F, 0xD800, 0xDC00, 0xDFFF, 0xFFFE, 0xFFFF})
    void deserializeFailsOnCharsIllegalInXml(int illegalChar) {
        String xml = TEST_OBJECT.replace(">key<", ">k" + (char) illegalChar + "y<");

        UnmarshalException exception = assertThrows(UnmarshalException.class, () -> JAXBUtils.deserialize(TestObject.class, xml));
        assertTrue(exception.getLinkedException().getMessage().contains("invalid XML character"));
    }
}
