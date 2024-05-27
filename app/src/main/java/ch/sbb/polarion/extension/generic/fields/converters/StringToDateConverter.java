package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class StringToDateConverter implements IConverter<String, Date> {

    private static final String ISO_DATE_PATTERN = "yyyy-MM-dd";
    private static final String ISO_TIME_PATTERN = "HH:mm";
    private static final String ISO_TIME_PATTERN_WITH_SECONDS = "HH:mm:ss";

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
        String format = ISO_DATE_PATTERN + " " + ISO_TIME_PATTERN;
        if (Objects.equals(fieldMetadata.getType(), FieldType.DATE_ONLY.getType())) {
            format = ISO_DATE_PATTERN;
        } else if (Objects.equals(fieldMetadata.getType(), FieldType.TIME_ONLY.getType())) {
            format = value.toInstant().atZone(ZoneOffset.UTC).getSecond() == 0 ? ISO_TIME_PATTERN : ISO_TIME_PATTERN_WITH_SECONDS;
        }
        return new SimpleDateFormat(format).format(value);
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
        LocalDateTime dateTime = LocalDateTime.now();
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
