package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.Currency;
import org.jetbrains.annotations.NotNull;

public class StringToCurrencyConverter implements IConverter<String, Currency> {
    @Override
    @SuppressWarnings("squid:S1166") // an unparsable value converts to null by design
    public Currency convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        String value = initialValue.replaceAll("[^\\d.-]", "");
        String unsigned = value.replace("-", "");
        if (unsigned.isEmpty()) {
            return null;
        }
        try {
            return Currency.parse(value.startsWith("-") ? "-" + unsigned : unsigned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public String convertBack(@NotNull Currency value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
