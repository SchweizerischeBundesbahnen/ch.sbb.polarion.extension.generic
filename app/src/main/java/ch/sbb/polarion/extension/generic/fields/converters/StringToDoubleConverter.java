package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class StringToDoubleConverter implements IConverter<String, Double> {
    @Override
    public Double convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        try {
            return Double.valueOf(initialValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert value '%s' of field '%s' to a number".formatted(initialValue, fieldMetadata.getId()), e);
        }
    }

    @Override
    public String convertBack(@NotNull Double value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
