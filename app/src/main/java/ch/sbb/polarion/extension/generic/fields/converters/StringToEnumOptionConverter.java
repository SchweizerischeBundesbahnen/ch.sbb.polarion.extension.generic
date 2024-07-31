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
import com.polarion.alm.shared.util.StringUtils;
import com.polarion.alm.tracker.model.IWorkItem;
import com.polarion.platform.persistence.IEnumOption;
import com.polarion.platform.persistence.IEnumeration;
import com.polarion.subterra.base.data.model.IEnumType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class StringToEnumOptionConverter implements IConverter<String, IEnumOption> {

    @Override
    public IEnumOption convert(@NotNull String initialValue, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return getEnumerationOptionForField(fieldMetadata, initialValue, context);
    }

    @Override
    public String convertBack(@NotNull IEnumOption value, @NotNull ConverterContext context, @NotNull FieldMetadata fieldMetadata) {
        return value.getName();
    }

    @Nullable
    @SuppressWarnings({"squid:S5852", "squid:S3776"}) // regex is safe here, ignore cognitive complexity warning
    private IEnumOption getEnumerationOptionForField(@NotNull FieldMetadata fieldMetadata, @NotNull String initialValue, @NotNull ConverterContext context) {
        String value = initialValue.trim();
        if (value.isEmpty() && !fieldMetadata.isRequired()) { // default enum value should be used if the field is required
            return null;
        }
        if (FieldType.unwrapIfListType(fieldMetadata.getType()) instanceof IEnumType enumType) {//attempt to unwrap type coz this converter may be used from ListConverter
            IEnumeration<IEnumOption> enumeration = new PolarionService().getEnumeration(enumType, context.getContextId());

            Map<String, String> enumMapping = Optional.ofNullable(context.getEnumsMapping()) //mapping may be null
                    .orElse(new HashMap<>())
                    .getOrDefault(fieldMetadata.getId(), new HashMap<>())
                    .entrySet().stream()
                    .filter(entry -> !StringUtils.isEmptyTrimmed(entry.getValue())) //remove entries with null/empty/blank values
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            List<String> existingOptionIds = enumeration.getAllOptions().stream().map(EnumUtils::getEnumId).toList();
            for (Map.Entry<String, String> entry : enumMapping.entrySet()) {
                //enumMapping may contain non-actual data coz enum elements may be changed after enumMapping definition
                //therefore we have to check whether this particular option still exists or not
                if (existingOptionIds.contains(entry.getKey()) && Arrays.asList(StringUtils.getNotNull(entry.getValue()).trim().split("\\s*,\\s*")).contains(value) || entry.getKey().equals(value)) {
                    return enumeration.getAllOptions().stream()
                            .filter(o -> entry.getKey().equals(EnumUtils.getEnumId(o)))
                            .findFirst()
                            .orElseThrow();
                }
            }

            try {
                //the rest unmapped options will be processed using keys/values
                List<IEnumOption> unmappedOptions = enumeration.getAllOptions().stream().filter(o -> !enumMapping.containsKey(EnumUtils.getEnumId(o))).toList();
                return findEnumOption(value, enumType, unmappedOptions);
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
    private static IEnumOption findEnumOption(@NotNull String value, @NotNull IEnumType enumType, @NotNull List<IEnumOption> options) throws EnumOptionNotFoundException {
        try {
            //first we try to find value by ID
            return options.stream()
                    .filter(option -> EnumUtils.getEnumId(option).equals(value))
                    .findFirst()
                    .orElseThrow(() -> new EnumOptionNotFoundByIdException(String.format("EnumOption with id '%s' not found", value)));
        } catch (EnumOptionNotFoundByIdException e) {
            //if nothing found then we peek first option by name ignore case
            return options.stream()
                    .filter(option -> option.getName().trim().equalsIgnoreCase(value)) // enum names in Polarion can have spaces at the end
                    .findFirst()
                    .orElseThrow(() -> new EnumOptionNotFoundException(String.format("Unsupported value '%s' for enum '%s'", value, enumType.getEnumerationId())));
        }
    }
}
