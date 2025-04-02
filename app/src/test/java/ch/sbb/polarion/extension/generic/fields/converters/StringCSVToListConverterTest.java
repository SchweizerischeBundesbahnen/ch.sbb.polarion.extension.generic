package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StringCSVToListConverterTest {

    private StringCSVToListConverter converter;
    private ConverterContext context;
    private FieldMetadata fieldMetadata;

    @BeforeEach
    void setUp() {
        converter = new StringCSVToListConverter();
        context = mock(ConverterContext.class);
        fieldMetadata = mock(FieldMetadata.class);
    }

    @Nested
    @DisplayName("Tests for convert method")
    class ConvertTests {

        @Test
        @DisplayName("Should convert a simple CSV string to a list")
        void shouldConvertSimpleCsvToList() {
            // Given
            String input = "item1,item2,item3";
            when(fieldMetadata.isRequired()).thenReturn(false);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(Arrays.asList("item1", "item2", "item3"), result);
        }

        @Test
        @DisplayName("Should trim white spaces when converting")
        void shouldTrimWhiteSpaces() {
            // Given
            String input = "item1 , item2,  item3";
            when(fieldMetadata.isRequired()).thenReturn(false);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(Arrays.asList("item1", "item2", "item3"), result);
        }

        @Test
        @DisplayName("Should filter out empty values")
        void shouldFilterEmptyValues() {
            // Given
            String input = "item1,,item2, ,item3";
            when(fieldMetadata.isRequired()).thenReturn(false);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(Arrays.asList("item1", "item2", "item3"), result);
        }

        @Test
        @DisplayName("Should handle empty string input")
        void shouldHandleEmptyString() {
            // Given
            String input = "";
            when(fieldMetadata.isRequired()).thenReturn(false);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("Should return list with empty string when input is empty and field is required")
        void shouldReturnListWithEmptyStringWhenRequiredAndEmpty() {
            // Given
            String input = "";
            when(fieldMetadata.isRequired()).thenReturn(true);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(Collections.singletonList(""), result);
        }

        @Test
        @DisplayName("Should handle input with special characters")
        void shouldHandleSpecialCharacters() {
            // Given
            String input = "item@1,item#2,item$3";
            when(fieldMetadata.isRequired()).thenReturn(false);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(Arrays.asList("item@1", "item#2", "item$3"), result);
        }
    }

    @Nested
    @DisplayName("Tests for convertBack method")
    class ConvertBackTests {

        @Test
        @DisplayName("Should convert a list back to CSV string")
        void shouldConvertListBackToString() {
            // Given
            List<Object> input = Arrays.asList("item1", "item2", "item3");

            // When
            String result = converter.convertBack(input, context, fieldMetadata);

            // Then
            assertEquals("item1,item2,item3", result);
        }

        @Test
        @DisplayName("Should handle empty list when converting back")
        void shouldHandleEmptyListWhenConvertingBack() {
            // Given
            List<Object> input = Collections.emptyList();

            // When
            String result = converter.convertBack(input, context, fieldMetadata);

            // Then
            assertEquals("", result);
        }

        @Test
        @DisplayName("Should convert list with single item correctly")
        void shouldConvertSingleItemList() {
            // Given
            List<Object> input = Collections.singletonList("singleItem");

            // When
            String result = converter.convertBack(input, context, fieldMetadata);

            // Then
            assertEquals("singleItem", result);
        }

        @Test
        @DisplayName("Should convert list with non-string objects correctly")
        void shouldConvertNonStringObjects() {
            // Given
            List<Object> input = Arrays.asList(123, true, 45.67);

            // When
            String result = converter.convertBack(input, context, fieldMetadata);

            // Then
            assertEquals("123,true,45.67", result);
        }
    }

    @Nested
    @DisplayName("Parameterized tests")
    class ParameterizedTests {

        @ParameterizedTest
        @MethodSource("csvToListTestCases")
        @DisplayName("Should convert various CSV strings to lists correctly")
        void shouldConvertVariousCsvStringsToLists(String input, List<Object> expected, boolean isRequired) {
            // Given
            when(fieldMetadata.isRequired()).thenReturn(isRequired);

            // When
            List<Object> result = converter.convert(input, context, fieldMetadata);

            // Then
            assertEquals(expected, result);
        }

        private static Stream<Arguments> csvToListTestCases() {
            return Stream.of(
                    Arguments.of("a,b,c", Arrays.asList("a", "b", "c"), false),
                    Arguments.of(" x , y , z ", Arrays.asList("x", "y", "z"), false),
                    Arguments.of(",,", Collections.emptyList(), false),
                    Arguments.of("", Collections.emptyList(), false),
                    Arguments.of("", Collections.singletonList(""), true),
                    Arguments.of("single", Collections.singletonList("single"), false)
            );
        }

        @ParameterizedTest
        @MethodSource("listToCsvTestCases")
        @DisplayName("Should convert various lists back to CSV strings correctly")
        void shouldConvertVariousListsBackToCsvStrings(List<Object> input, String expected) {
            // When
            String result = converter.convertBack(input, context, fieldMetadata);

            // Then
            assertEquals(expected, result);
        }

        private static Stream<Arguments> listToCsvTestCases() {
            return Stream.of(
                    Arguments.of(Arrays.asList("a", "b", "c"), "a,b,c"),
                    Arguments.of(Collections.emptyList(), ""),
                    Arguments.of(Collections.singletonList("single"), "single"),
                    Arguments.of(Arrays.asList("", "empty", ""), ",empty,"),
                    Arguments.of(Arrays.asList(1, 2, 3), "1,2,3")
            );
        }
    }

    @Test
    @DisplayName("Should maintain end-to-end consistency")
    void shouldMaintainEndToEndConsistency() {
        // Given
        String original = "item1,item2,item3";
        when(fieldMetadata.isRequired()).thenReturn(false);

        // When
        List<Object> converted = converter.convert(original, context, fieldMetadata);
        String reconverted = converter.convertBack(converted, context, fieldMetadata);

        // Then
        assertEquals(original, reconverted);
    }
}
