package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.Currency;
import org.jetbrains.annotations.NotNull;

public class DoubleToCurrencyConverter implements IConverter<Double, Currency> {
    @Override
    public Currency convert(@NotNull Double initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Currency.parse(String.valueOf(initialValue));
    }

    @Override
    public Double convertBack(@NotNull Currency value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getValue().doubleValue();
    }
}
