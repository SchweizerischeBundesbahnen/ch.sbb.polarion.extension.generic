package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import org.jetbrains.annotations.NotNull;

public class DoubleToIntegerConverter implements IConverter<Double, Integer> {
    @Override
    public Integer convert(@NotNull Double initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return initialValue.intValue();
    }

    @Override
    public Double convertBack(@NotNull Integer value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.doubleValue();
    }
}
