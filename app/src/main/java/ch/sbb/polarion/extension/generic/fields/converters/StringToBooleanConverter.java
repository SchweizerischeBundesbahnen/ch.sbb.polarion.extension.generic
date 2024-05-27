package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class StringToBooleanConverter implements IConverter<String, Boolean> {
    @Override
    public Boolean convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Boolean.valueOf(initialValue);
    }

    @Override
    public String convertBack(@NotNull Boolean value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
