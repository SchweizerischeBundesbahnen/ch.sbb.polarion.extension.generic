package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.Text;
import org.jetbrains.annotations.NotNull;

public class StringToRichConverter implements IConverter<String, Text> {

    @Override
    public Text convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return Text.html(initialValue);
    }

    @Override
    public String convertBack(@NotNull Text value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getContent();
    }
}
