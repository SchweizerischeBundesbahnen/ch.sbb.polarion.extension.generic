package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.TimeOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DateToTimeOnlyConverter implements IConverter<Date, TimeOnly> {

    @Override
    public TimeOnly convert(@NotNull Date initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return new TimeOnly(initialValue);
    }

    @Override
    public Date convertBack(@NotNull TimeOnly value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getDate();
    }
}
