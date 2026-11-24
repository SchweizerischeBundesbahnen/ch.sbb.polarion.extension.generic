package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class DoubleToStringConverter implements IConverter<Double, String> {

    @Override
    public String convert(@NotNull Double initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(initialValue);
    }

    @Override
    public Double convertBack(@NotNull String value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert value '%s' of field '%s' to a number".formatted(value, fieldMetadata.getId()), e);
        }
    }
}
