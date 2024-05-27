package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StringToDateConverterTest {

    @Test
    void testConvert() {
        StringToDateConverter converter = new StringToDateConverter();
        ConverterContext context = ConverterContext.builder().build();
        FieldMetadata metadata = FieldMetadata.builder().build();
        // basic date
        assertEquals(getDate(LocalDate.of(1985, 1, 24)),
                converter.convert("1985-01-24", context, metadata));

        // acceptable basic dates are yyyy-MM-dd only
        assertThrows(DateTimeParseException.class, () -> converter.convert("24.01.1985", context, metadata));

        // parse time
        assertEquals(getDate(LocalDateTime.now().with(LocalTime.of(14, 25))),
                converter.convert("14:25", context, metadata));
        assertEquals(getDate(LocalDateTime.now().with(LocalTime.of(14, 25, 38))),
                converter.convert("14:25:38", context, metadata));

        // acceptable time values: HH:mm & HH:mm:ss
        assertThrows(DateTimeParseException.class, () -> converter.convert("14 25", context, metadata));

        // ISO dateTime format
        Date fullDate = getDate(LocalDateTime.of(1985, 1, 24, 10, 27, 45));
        assertEquals(fullDate, converter.convert("1985-01-24T10:27:45", context, metadata));

        // yyyy-MM-ddTHH:mm:ss is the only ISO dateTime format supported
        assertNull(converter.convert("1985-01-24T10:27:45.000-05:00", context, metadata));

        assertEquals("1985-01-24 10:27", converter.convertBack(fullDate, context, metadata));

        assertEquals("1985-01-24", converter.convertBack(fullDate, context, metadata.setType(FieldType.DATE_ONLY.getType())));

        assertEquals("10:27:45", converter.convertBack(fullDate, context, metadata.setType(FieldType.TIME_ONLY.getType())));
    }

    private Date getDate(LocalDate localDate) {
        return Date.from(localDate.atStartOfDay().atZone(ZoneOffset.systemDefault()).toInstant());
    }

    private Date getDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneOffset.systemDefault()).toInstant());
    }
}
