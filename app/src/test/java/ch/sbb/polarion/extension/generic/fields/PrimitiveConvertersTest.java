package ch.sbb.polarion.extension.generic.fields;

import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.Currency;
import com.polarion.core.util.types.DateOnly;
import com.polarion.core.util.types.Text;
import com.polarion.core.util.types.TimeOnly;
import com.polarion.core.util.types.duration.DurationTime;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class PrimitiveConvertersTest {

    @Test
    void testDoubleToStringConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.STRING.getType());

        assertEquals("1.11", ConverterTestUtils.process(1.11, fieldMetadata));
    }

    @Test
    void testDoubleToTextConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.TEXT.getType());

        assertEquals(Text.plain("12.3"), ConverterTestUtils.process(12.3, fieldMetadata)); //uses chain converter
    }

    @Test
    void testDoubleToIntegerConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.INTEGER.getType());

        assertEquals(115, ConverterTestUtils.process(115.0, fieldMetadata));
    }

    @Test
    @SneakyThrows
    void testDateToDateOnlyConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.DATE_ONLY.getType());
        String strDate = "2015-06-22 14:55";
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(strDate);

        assertEquals(DateOnly.parse("2015-06-22 14:55"), ConverterTestUtils.process(date, fieldMetadata));
    }

    @Test
    @SneakyThrows
    void testDateToTimeOnlyConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.TIME_ONLY.getType());
        String strDate = "2015-06-22 14:55";
        Date date = new SimpleDateFormat("yyyy-MM-dd hh:mm").parse(strDate);

        assertEquals(new TimeOnly(date), ConverterTestUtils.process(date, fieldMetadata));
    }

    @Test
    void testDoubleToCurrencyConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.CURRENCY.getType());

        assertEquals(Currency.parse("123.44"), ConverterTestUtils.process(123.44, fieldMetadata));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "123.44     | 123.44",
            "-100       | -100",
            "-0.5       | -0.5",
            "CHF -12.30 | -12.30",
            "1,000.50   | 1000.50",
            "1'000.50   | 1000.50",
            "1,000      | 1000",
            "1,5        | 15",
    })
    void testStringToCurrencyConversion(String value, String expected) {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.CURRENCY.getType());

        assertEquals(Currency.parse(expected), ConverterTestUtils.process(value, fieldMetadata));
    }

    @Test
    void testStringToCurrencyConversionIgnoresServerLocale() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.CURRENCY.getType());
        Locale defaultLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            assertEquals(Currency.parse("1000.50"), ConverterTestUtils.process("1,000.50", fieldMetadata));
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "CHF", "1.2.3", "100-"})
    void testStringToCurrencyConversionOfInvalidValue(String value) {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.CURRENCY.getType());

        assertNull(ConverterTestUtils.process(value, fieldMetadata));
    }

    @Test
    void testStringToDurationConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.DURATION.getType());

        assertEquals(DurationTime.fromString("2 1/2h"), ConverterTestUtils.process("2 1/2h", fieldMetadata));
    }

    @Test
    void testDoubleToRichConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.RICH.getType());

        assertEquals(Text.html("7.5"), ConverterTestUtils.process(7.5, fieldMetadata)); //uses chain converter
    }

    @Test
    void testStringToBooleanConversion() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();
        fieldMetadata.setType(FieldType.BOOLEAN.getType());

        assertEquals(Boolean.FALSE, ConverterTestUtils.process("false", fieldMetadata));
        assertEquals(Boolean.FALSE, ConverterTestUtils.process("False", fieldMetadata));
        assertEquals(Boolean.FALSE, ConverterTestUtils.process("0", fieldMetadata));
        assertEquals(Boolean.FALSE, ConverterTestUtils.process("1", fieldMetadata));
        assertEquals(Boolean.FALSE, ConverterTestUtils.process("anyValue", fieldMetadata));
        assertEquals(Boolean.FALSE, ConverterTestUtils.process("", fieldMetadata));
        assertEquals(Boolean.TRUE, ConverterTestUtils.process("true", fieldMetadata));
        assertEquals(Boolean.TRUE, ConverterTestUtils.process("True", fieldMetadata));
    }

    @Test
    void testNoSuitableConverterFound() {
        FieldMetadata fieldMetadata = ConverterTestUtils.getWorkItemCustomField();

        assertThrows(NullPointerException.class,
                () -> ConverterTestUtils.process(true, fieldMetadata),
                "Expected NullPointerException thrown, but it didn't");
    }
}
