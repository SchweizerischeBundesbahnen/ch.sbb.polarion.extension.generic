package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class StringToDateConverter implements IConverter<String, Date> {

    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    private static final String ISO_TIME_PATTERN = "HH:mm";
    private static final String ISO_TIME_PATTERN_WITH_SECONDS = "HH:mm:ss";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_PATTERN);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIME_PATTERN);
    private static final DateTimeFormatter TIME_WITH_SECONDS_FORMATTER = DateTimeFormatter.ofPattern(ISO_TIME_PATTERN_WITH_SECONDS);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(ISO_DATE_PATTERN + " " + ISO_TIME_PATTERN);

    @Override
    public Date convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        int length = initialValue.trim().length();
        String isoDateTimePattern = "yyyy-MM-ddTHH:mm:ss";
        if (length == ISO_DATE_PATTERN.length()) {
            return convertDateOnly(builder, initialValue);
        } else if (length == ISO_TIME_PATTERN.length() || length == ISO_TIME_PATTERN_WITH_SECONDS.length()) {
            return convertTimeOnly(builder, initialValue);
        } else if (length == isoDateTimePattern.length()) {
            return convertDateTime(builder, initialValue);
        }
        return null;
    }

    @Override
    public String convertBack(@NotNull Date value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        LocalDateTime dateTime = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DATE_TIME_FORMATTER;
        if (Objects.equals(fieldMetadata.getType(), FieldType.DATE_ONLY.getType())) {
            formatter = DATE_FORMATTER;
        } else if (Objects.equals(fieldMetadata.getType(), FieldType.TIME_ONLY.getType())) {
            formatter = dateTime.getSecond() == 0 ? TIME_FORMATTER : TIME_WITH_SECONDS_FORMATTER;
        }
        return dateTime.format(formatter);
    }

    private Date convertDateOnly(DateTimeFormatterBuilder builder, String initialValue) {
        DateTimeFormatter formatter = builder.parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE)
                .toFormatter(Locale.getDefault());
        return Date.from(LocalDate.parse(initialValue, formatter).atStartOfDay()
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    private Date convertTimeOnly(DateTimeFormatterBuilder builder, String initialValue) {
        DateTimeFormatter formatter = builder.parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_TIME)
                .toFormatter(Locale.getDefault());
        LocalDateTime dateTime = LocalDateTime.now(ZoneId.systemDefault());
        LocalTime time = LocalTime.parse(initialValue, formatter);
        return Date.from(dateTime.with(time)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    private Date convertDateTime(DateTimeFormatterBuilder builder, String initialValue) {
        DateTimeFormatter formatter = builder.parseCaseInsensitive()
                .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toFormatter(Locale.getDefault());
        return Date.from(LocalDateTime.parse(initialValue, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }
}
