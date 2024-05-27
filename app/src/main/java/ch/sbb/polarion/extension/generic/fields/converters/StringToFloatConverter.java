package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class StringToFloatConverter implements IConverter<String, Float> {
    @Override
    public Float convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Float.valueOf(initialValue);
    }

    @Override
    public String convertBack(@NotNull Float value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
