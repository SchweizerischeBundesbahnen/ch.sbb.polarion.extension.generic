package ch.sbb.polarion.extension.generic.fields.converters;

import ch.sbb.polarion.extension.generic.fields.ConverterContext;
import ch.sbb.polarion.extension.generic.fields.FieldType;
import ch.sbb.polarion.extension.generic.fields.IConverter;
import ch.sbb.polarion.extension.generic.fields.exception.DefaultEnumOptionNotFoundException;
import ch.sbb.polarion.extension.generic.fields.exception.EnumOptionNotFoundByIdException;
import ch.sbb.polarion.extension.generic.fields.exception.EnumOptionNotFoundException;
import ch.sbb.polarion.extension.generic.fields.model.FieldMetadata;
import ch.sbb.polarion.extension.generic.service.PolarionService;
import ch.sbb.polarion.extension.generic.util.EnumUtils;
import ch.sbb.polarion.extension.generic.util.OptionsMappingUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.subterra.base.data.model.IEnumType;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

public class StringToEnumOptionConverter implements IConverter<String, IEnumOption> {

    @Override
    public IEnumOption convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return getEnumerationOptionForField(fieldMetadata, initialValue, context);
    }

    @Override
    public String convertBack(@NotNull IEnumOption value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getName();
    }

    @SneakyThrows
    @Nullable
    @SuppressWarnings({"squid:S5852", "squid:S3776"}) // regex is safe here, ignore cognitive complexity warning
    private IEnumOption getEnumerationOptionForField(@NotNull FieldMetadata fieldMetadata, @NotNull String initialValue, @NotNull ConverterContext context) {
        String value = initialValue.trim();
        if (value.isEmpty() && !fieldMetadata.isRequired()) { // default enum value should be used if the field is required
            return null;
        }
        if (FieldType.unwrapIfListType(fieldMetadata.getType()) instanceof IEnumType enumType) {//attempt to unwrap type coz this converter may be used from ListConverter
            IEnumeration<IEnumOption> enumeration = new PolarionService().getEnumeration(enumType, context.getContextId());

            // first we attempt to find the option using custom mapping
            String mappingOptionKey = OptionsMappingUtils.getMappedOptionKey(fieldMetadata.getId(), value, context.getEnumsMapping());
            if (mappingOptionKey != null) {
                return enumeration.getAllOptions().stream().filter(o -> mappingOptionKey.equals(EnumUtils.getEnumId(o)))
                        .findFirst().orElseThrow((Supplier<Throwable>) () -> new IllegalArgumentException("Value %s mapped to unknown key %s".formatted(value, mappingOptionKey)));
            }

            try {
                // as a last stand try to find the option using keys/values
                return findEnumOption(value, enumType, enumeration);
            } catch (EnumOptionNotFoundException e) {
                if (value.isBlank()) {
                    return handleEmptyValue(fieldMetadata, enumType, enumeration.getAllOptions());
                } else if (IWorkItem.ENUM_ID_PRIORITY.equals(enumType.getEnumerationId())) {
                    return enumeration.wrapOption(value);
                } else {
                    throw new IllegalArgumentException(e);
                }
            }
        } else {
            throw new IllegalArgumentException(String.format("'%s' is not enum, but '%s'", fieldMetadata.getId(), fieldMetadata.getType()));
        }
    }

    @Nullable
    private IEnumOption handleEmptyValue(@NotNull FieldMetadata fieldMetadata, @NotNull IEnumType enumType, @NotNull List<IEnumOption> options) {
        if (fieldMetadata.isRequired()) {
            try {
                return findEnumDefaultValue(enumType, options);
            } catch (DefaultEnumOptionNotFoundException ex) {
                throw new IllegalArgumentException(ex);
            }
        } else {
            return null;
        }
    }

    @NotNull
    private IEnumOption findEnumDefaultValue(@NotNull IEnumType enumType, @NotNull List<IEnumOption> options) throws DefaultEnumOptionNotFoundException {
        return options.stream()
                .filter(IEnumOption::isDefault)
                .findFirst()
                .orElseThrow(() -> new DefaultEnumOptionNotFoundException(String.format("No default option found for enum '%s'", enumType.getEnumerationId())));
    }

    @NotNull
    @SuppressWarnings("squid:S1166") // no need to log or rethrow exception by design
    private static IEnumOption findEnumOption(@NotNull String value, @NotNull IEnumType enumType, @NotNull IEnumeration<IEnumOption> enumeration) throws EnumOptionNotFoundException {
        try {
            //first we try to find value by ID
            return enumeration.getAllOptions().stream()
                    .filter(option -> EnumUtils.getEnumId(option).equals(value))
                    .findFirst()
                    .orElseThrow(() -> new EnumOptionNotFoundByIdException(String.format("EnumOption with id '%s' not found", value)));
        } catch (EnumOptionNotFoundByIdException e) {
            // peek first option by name ignore case
            return enumeration.getAllOptions().stream()
                    .filter(option -> option.getName().trim().equalsIgnoreCase(value)) // enum names in Polarion can have spaces at the end
                    .findFirst()
                    .orElseThrow(() -> new EnumOptionNotFoundException(String.format("Unsupported value '%s' for enum '%s'", value, enumType.getEnumerationId())));
        }
    }
}
