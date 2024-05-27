package ch.sbb.polarion.extension.generic.fields;

import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.subterra.base.data.identification.IContextId;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static org.mockito.Mockito.mock;

public class ConverterTestUtils {

    public static Object process(@NotNull Object value, @NotNull FieldMetadata fieldMetadata) {
        return process(value, createContext(null, null), fieldMetadata);
    }

    @SuppressWarnings("unchecked")
    public static Object process(@NotNull Object value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return IConverter.getSuitableConverter(value, fieldMetadata.getType()).convert(value, context, fieldMetadata);
    }

    @SuppressWarnings("rawtypes")
    public static ConverterContext createContext(Map<String, Map<String, String>> enumsMapping, Class preferredReturnType) {
        return ConverterContext.builder().contextId(mock(IContextId.class)).enumsMapping(enumsMapping).preferredReturnType(preferredReturnType).build();
    }

    public static FieldMetadata getWorkItemCustomField() {
        return FieldMetadata.builder()
                .id("someCustomField")
                .type(FieldType.ENUM.getType())
                .custom(true)
                .required(false)
                .build();
    }

    public static FieldMetadata getWorkItemPriorityField() {
        return FieldMetadata.builder()
                .id(IWorkItem.ENUM_ID_PRIORITY)
                .type(FieldType.ENUM.getType())
                .custom(false)
                .required(false)
                .build();
    }

}
