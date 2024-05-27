package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.projects.model.IUser;
import org.jetbrains.annotations.NotNull;

public class StringToUserConverter implements IConverter<String, IUser> {

    @Override
    public IUser convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        throw new UnsupportedOperationException("Direct user conversion isn't supported yet");
    }

    @Override
    public String convertBack(@NotNull IUser value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getName();
    }
}
