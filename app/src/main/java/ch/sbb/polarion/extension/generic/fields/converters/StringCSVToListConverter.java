package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.google.common.base.Joiner;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringCSVToListConverter implements IConverter<String, List<Object>> {

    public static final String SEPARATOR = ",";

    @Override
    public List<Object> convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        String[] values = initialValue.split(Pattern.quote(SEPARATOR));
        List<Object> list = Stream.of(values)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        // at least in case of required enums this will attempt to set default value, other cases can require this too (probably)
        return list.isEmpty() && fieldMetadata.isRequired() ? List.of("") : list;
    }

    @Override
    public String convertBack(@NotNull List<Object> value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Joiner.on(SEPARATOR).join(value);
    }
}
