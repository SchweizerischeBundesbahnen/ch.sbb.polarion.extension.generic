package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.duration.DurationTime;
import org.jetbrains.annotations.NotNull;

public class StringToDurationConverter implements IConverter<String, DurationTime> {
    @Override
    public DurationTime convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return DurationTime.fromString(initialValue);
    }

    @Override
    public String convertBack(@NotNull DurationTime value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return String.valueOf(value);
    }
}
