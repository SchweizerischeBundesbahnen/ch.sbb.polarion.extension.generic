package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.subterra.base.data.model.IType;
import com.polarion.subterra.base.data.model.internal.EnumType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Converts list of values using unwrapped type. Currently (Polarion v2310), it seems that only enums may be used with lists
 * but this converter is ready to support any type in the future.
 */
public class ListConverter implements IConverter<List<Object>, List<Object>> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> convert(@NotNull List<Object> list, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        IType type = FieldType.unwrapIfListType(fieldMetadata.getType());
        List<Object> resultList = list.stream().map(value -> {
            IConverter converter = IConverter.getSuitableConverter(value, type);
            Object valueToSet = converter == null ? value : converter.convert(value, context, fieldMetadata);
            //enum list accepts list of ids, so we have to extract them from options
            return (valueToSet instanceof IEnumOption enumValue) ? enumValue.getId() : valueToSet;
        }).toList();
        return type instanceof EnumType ? resultList.stream().distinct().toList() : resultList;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<Object> convertBack(@NotNull List<Object> list, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        IType type = FieldType.unwrapIfListType(fieldMetadata.getType());
        return list.stream().map(value -> {
            IConverter converter = IConverter.getSuitableConverter(context.getPreferredReturnType(), type);
            return converter == null ? value : converter.convertBack(value, context, fieldMetadata);
        }).toList();
    }
}
