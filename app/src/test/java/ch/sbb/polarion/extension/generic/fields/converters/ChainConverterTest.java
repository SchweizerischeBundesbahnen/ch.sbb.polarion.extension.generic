package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChainConverterTest {

    @Mock
    private ConverterContext context;

    @Mock
    private FieldMetadata fieldMetadata;

    @Mock
    private IConverter<String, Integer> firstConverter;

    @Mock
    private IConverter<Integer, Double> secondConverter;

    private ChainConverter chainConverter;

    @BeforeEach
    void setUp() {
        // Reset the mocks before each test
        reset(context, fieldMetadata, firstConverter, secondConverter);
    }

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create a chain converter with multiple converters")
        void shouldCreateChainConverterWithMultipleConverters() {
            chainConverter = new ChainConverter(firstConverter, secondConverter);
            assertNotNull(chainConverter);
        }

        @Test
        @DisplayName("Should create a chain converter with a single converter")
        void shouldCreateChainConverterWithSingleConverter() {
            chainConverter = new ChainConverter(firstConverter);
            assertNotNull(chainConverter);
        }

        @Test
        @DisplayName("Should create a chain converter with no converters")
        void shouldCreateChainConverterWithNoConverters() {
            chainConverter = new ChainConverter();
            assertNotNull(chainConverter);
        }
    }

    @Nested
    @DisplayName("Convert Method Tests")
    class ConvertMethodTests {

        @Test
        @DisplayName("Should apply converters in sequence")
        void shouldApplyConvertersInSequence() {
            // Arrange
            String initialValue = "42";
            Integer intermediateValue = 42;
            Double finalValue = 42.0;

            when(firstConverter.convert(initialValue, context, fieldMetadata)).thenReturn(intermediateValue);
            when(secondConverter.convert(intermediateValue, context, fieldMetadata)).thenReturn(finalValue);

            chainConverter = new ChainConverter(firstConverter, secondConverter);

            // Act
            Object result = chainConverter.convert(initialValue, context, fieldMetadata);

            // Assert
            assertEquals(finalValue, result);
            verify(firstConverter).convert(initialValue, context, fieldMetadata);
            verify(secondConverter).convert(intermediateValue, context, fieldMetadata);
        }

        @Test
        @DisplayName("Should handle a single converter")
        void shouldHandleSingleConverter() {
            // Arrange
            String initialValue = "42";
            Integer finalValue = 42;

            when(firstConverter.convert(initialValue, context, fieldMetadata)).thenReturn(finalValue);

            chainConverter = new ChainConverter(firstConverter);

            // Act
            Object result = chainConverter.convert(initialValue, context, fieldMetadata);

            // Assert
            assertEquals(finalValue, result);
            verify(firstConverter).convert(initialValue, context, fieldMetadata);
        }

        @Test
        @DisplayName("Should return initial value with no converters")
        void shouldReturnInitialValueWithNoConverters() {
            // Arrange
            String initialValue = "42";

            chainConverter = new ChainConverter();

            // Act
            Object result = chainConverter.convert(initialValue, context, fieldMetadata);

            // Assert
            assertEquals(initialValue, result);
        }

        @Nested
        @DisplayName("ConvertBack Method Tests")
        class ConvertBackMethodTests {

            @Test
            @DisplayName("Should apply converters in reverse sequence")
            void shouldApplyConvertersInReverseSequence() {
                // Arrange
                Double initialValue = 42.0;
                Integer intermediateValue = 42;
                String finalValue = "42";

                when(secondConverter.convertBack(initialValue, context, fieldMetadata)).thenReturn(intermediateValue);
                when(firstConverter.convertBack(intermediateValue, context, fieldMetadata)).thenReturn(finalValue);

                chainConverter = new ChainConverter(firstConverter, secondConverter);

                // Act
                Object result = chainConverter.convertBack(initialValue, context, fieldMetadata);

                // Assert
                assertEquals(finalValue, result);
                verify(secondConverter).convertBack(initialValue, context, fieldMetadata);
                verify(firstConverter).convertBack(intermediateValue, context, fieldMetadata);
            }

            @Test
            @DisplayName("Should handle a single converter in convertBack")
            void shouldHandleSingleConverterInConvertBack() {
                // Arrange
                Integer initialValue = 42;
                String finalValue = "42";

                when(firstConverter.convertBack(initialValue, context, fieldMetadata)).thenReturn(finalValue);

                chainConverter = new ChainConverter(firstConverter);

                // Act
                Object result = chainConverter.convertBack(initialValue, context, fieldMetadata);

                // Assert
                assertEquals(finalValue, result);
                verify(firstConverter).convertBack(initialValue, context, fieldMetadata);
            }

            @Test
            @DisplayName("Should return initial value with no converters in convertBack")
            void shouldReturnInitialValueWithNoConvertersInConvertBack() {
                // Arrange
                Double initialValue = 42.0;

                chainConverter = new ChainConverter();

                // Act
                Object result = chainConverter.convertBack(initialValue, context, fieldMetadata);

                // Assert
                assertEquals(initialValue, result);
            }

            /**
             * Helper converter for testing
             */
            private static class StringToDoubleConverter implements IConverter<String, Double> {
                @Override
                public Double convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return Double.parseDouble(initialValue);
                }

                @Override
                public String convertBack(@NotNull Double value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return value.toString();
                }
            }

            /**
             * Helper converter for testing
             */
            private static class DoubleToIntegerConverter implements IConverter<Double, Integer> {
                @Override
                public Integer convert(@NotNull Double initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return initialValue.intValue();
                }

                @Override
                public Double convertBack(@NotNull Integer value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return value.doubleValue();
                }
            }

            /**
             * Helper converter for testing three-converter chains
             */
            private static class IntegerToStringConverter implements IConverter<Integer, String> {
                @Override
                public String convert(@NotNull Integer initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return initialValue.toString();
                }

                @Override
                public Integer convertBack(@NotNull String value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
                    return Integer.parseInt(value);
                }
            }

            @Nested
            @DisplayName("Integration Tests")
            class IntegrationTests {

                @Test
                @DisplayName("Should chain converters correctly")
                void shouldChainConvertersCorrectly() {
                    // Use actual converter implementations
                    StringToDoubleConverter stringToDoubleConverter = new StringToDoubleConverter();
                    DoubleToIntegerConverter doubleToIntegerConverter = new DoubleToIntegerConverter();

                    chainConverter = new ChainConverter(stringToDoubleConverter, doubleToIntegerConverter);

                    // Create a real ConverterContext and FieldMetadata
                    ConverterContext realContext = mock(ConverterContext.class);
                    FieldMetadata realFieldMetadata = mock(FieldMetadata.class);

                    // Test convert
                    Object result = chainConverter.convert("42.7", realContext, realFieldMetadata);
                    assertEquals(42, result); // Double 42.7 should be converted to Integer 42

                    // Test convertBack
                    Object backResult = chainConverter.convertBack(42, realContext, realFieldMetadata);
                    assertEquals("42.0", backResult); // Integer 42 should be converted back to String "42.0"
                }

                @Test
                @DisplayName("Should handle chaining three converters")
                void shouldHandleThreeConverters() {
                    // Create three converters
                    StringToDoubleConverter stringToDoubleConverter = new StringToDoubleConverter();
                    DoubleToIntegerConverter doubleToIntegerConverter = new DoubleToIntegerConverter();
                    IntegerToStringConverter integerToStringConverter = new IntegerToStringConverter();

                    chainConverter = new ChainConverter(stringToDoubleConverter, doubleToIntegerConverter, integerToStringConverter);

                    ConverterContext realContext = mock(ConverterContext.class);
                    FieldMetadata realFieldMetadata = mock(FieldMetadata.class);

                    // Test convert: String -> Double -> Integer -> String
                    Object result = chainConverter.convert("42.7", realContext, realFieldMetadata);
                    assertEquals("42", result); // "42.7" -> 42.7 -> 42 -> "42"

                    // Test convertBack: String -> Integer -> Double -> String
                    Object backResult = chainConverter.convertBack("42", realContext, realFieldMetadata);
                    assertEquals("42.0", backResult); // "42" -> 42 -> 42.0 -> "42.0"
                }
            }
        }
    }
}
