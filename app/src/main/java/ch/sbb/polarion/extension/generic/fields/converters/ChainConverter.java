package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ChainConverter implements IConverter<Object, Object> {

    private final List<IConverter> converterChain;

    public ChainConverter(IConverter... converters) {
        converterChain = Arrays.stream(converters).toList();
    }

    @Override
    public Object convert(@NotNull Object initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        for (IConverter converter : converterChain) {
            initialValue = converter.convert(initialValue, context, fieldMetadata);
        }
        return initialValue;
    }

    @Override
    public Object convertBack(@NotNull Object value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        for (IConverter converter : Lists.reverse(converterChain)) {
            value = converter.convertBack(value, context, fieldMetadata);
        }
        return value;
    }
}
