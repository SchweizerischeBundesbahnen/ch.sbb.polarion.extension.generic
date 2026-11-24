package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class StringToFloatConverter implements IConverter<String, Float> {
    @Override
    public Float convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        try {
            return Float.valueOf(initialValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Cannot convert value '%s' of field '%s' to a number".formatted(initialValue, fieldMetadata.getId()), e);
        }
    }

    @Override
    public String convertBack(@NotNull Float value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
