package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.core.util.types.DateOnly;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

public class DateToDateOnlyConverter implements IConverter<Date, DateOnly> {
    @Override
    public DateOnly convert(@NotNull Date initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return new DateOnly(initialValue);
    }

    @Override
    public Date convertBack(@NotNull DateOnly value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getDate();
    }
}
