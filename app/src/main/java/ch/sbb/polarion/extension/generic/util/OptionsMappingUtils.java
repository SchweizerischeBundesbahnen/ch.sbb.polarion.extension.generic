package ch.sbb.polarion.extension.generic.util;

import com.polarion.alm.shared.util.StringUtils;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@UtilityClass
public class OptionsMappingUtils {

    String EMPTY_VALUE = "(empty)";

    @NotNull
    public Map<String, String> getMappingForFieldId(String fieldId, Map<String, Map<String, String>> commonMapping) {
        return Optional.ofNullable(commonMapping)
                .orElse(new HashMap<>())
                .getOrDefault(fieldId, new HashMap<>())
                .entrySet().stream()
                .filter(entry -> !StringUtils.isEmptyTrimmed(entry.getValue())) //remove entries with null/empty/blank values
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Nullable
    public String getMappedOptionKey(String fieldId, @Nullable Object initialValue, @Nullable Map<String, Map<String, String>> commonMapping) {
        if (initialValue == null || initialValue instanceof String) {
            String value = StringUtils.isEmptyTrimmed((String) initialValue) ? EMPTY_VALUE : ((String) initialValue);
            for (Map.Entry<String, String> entry : getMappingForFieldId(fieldId, commonMapping).entrySet()) {
                if (Stream.of(StringUtils.getNotNull(entry.getValue()).split(",")).anyMatch(v -> StringUtils.areEqualTrimmed(v, value))) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}
