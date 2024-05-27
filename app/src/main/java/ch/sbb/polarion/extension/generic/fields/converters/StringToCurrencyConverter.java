package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.Currency;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class StringToCurrencyConverter implements IConverter<String, Currency> {
    @Override
    public Currency convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        final NumberFormat format = NumberFormat.getNumberInstance(Locale.getDefault());
        if (format instanceof DecimalFormat decimalFormat) {
            decimalFormat.setParseBigDecimal(true);
            try {
                return new Currency((BigDecimal) format.parse(initialValue.replaceAll("[^\\d.,]", "")));
            } catch (ParseException e) {
                return null;
            }
        }
        return null;
    }

    @Override
    public String convertBack(@NotNull Currency value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
