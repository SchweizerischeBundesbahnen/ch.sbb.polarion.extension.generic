package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.google.common.base.Joiner;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class StringCSVToListConverter implements IConverter<String, List<Object>> {

    public static final String SEPARATOR = ",";

    @Override
    public List<Object> convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        String[] values = initialValue.split(Pattern.quote(SEPARATOR));
        return Arrays.asList(values);
    }

    @Override
    public String convertBack(@NotNull List<Object> value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Joiner.on(SEPARATOR).join(value);
    }
}
