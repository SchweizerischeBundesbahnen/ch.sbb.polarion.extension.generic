package ch.sbb.polarion.extension.generic.settings;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarion.core.util.logging.Logger;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Optional;

@Data
@Schema(description = "Settings model")
public abstract class SettingsModel {

    private static final Logger logger = Logger.getLogger(SettingsModel.class);

    public static final String BEGIN_ENTRY = "-----BEGIN %s-----";
    public static final String END_ENTRY = "-----END %s-----";

    public static final String BUNDLE_TIMESTAMP = "BUNDLE TIMESTAMP";
    public static final String NAME = "NAME";

    @JsonIgnore
    @Schema(description = "The name of the setting", hidden = true)
    @SuppressWarnings("squid:S1845") // field has same name as a constant intentionally
    protected String name;

    @Schema(description = "The bundle timestamp of the setting")
    protected String bundleTimestamp;

    public String serialize() {
        return serializeEntry(NAME, name) + serializeEntry(BUNDLE_TIMESTAMP, bundleTimestamp) + serializeModelData();
    }

    protected String serializeEntry(@NotNull String entryName, String entryContent) {
        return entryContent == null ? "" : String.format(BEGIN_ENTRY, entryName) +
                System.lineSeparator() +
                entryContent +
                System.lineSeparator() +
                String.format(END_ENTRY, entryName) +
                System.lineSeparator();
    }

    protected <T> String serializeEntry(@NotNull String entryName, T entryContent) {
        if (entryContent == null) {
            return "";
        }
        String content;
        try {
            content = new ObjectMapper().writeValueAsString(entryContent);
        } catch (JsonProcessingException e) {
            logger.error(String.format("Error serializing '%s' of %s class to String", entryContent, entryContent.getClass()), e);
            content = null;
        }
        return content == null ? "" : String.format(BEGIN_ENTRY, entryName) +
                System.lineSeparator() +
                content +
                System.lineSeparator() +
                String.format(END_ENTRY, entryName) +
                System.lineSeparator();
    }

    public void deserialize(String serializedString) {
        name = deserializeEntry(NAME, serializedString);
        bundleTimestamp = deserializeEntry(BUNDLE_TIMESTAMP, serializedString);
        deserializeModelData(serializedString);
    }

    protected String deserializeEntry(String entryName, String serializedString, String defaultValue) {
        String value = deserializeEntry(entryName, serializedString);
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    protected String deserializeEntry(String entryName, String serializedString) {
        if (serializedString == null) {
            return null;
        }
        String beginEntry = String.format(BEGIN_ENTRY, entryName);
        String endEntry = String.format(END_ENTRY, entryName);
        int begin = serializedString.indexOf(beginEntry);
        int end = serializedString.indexOf(endEntry);
        if (begin != -1 && end > begin) {
            int beginPos = begin + beginEntry.length();
            return beginPos >= end ? "" : serializedString.substring(beginPos, end).trim();
        }
        return null;
    }

    protected <T> T deserializeEntry(String entryName, String serializedString, Class<T> aClass, T defaultValue) {
        T value = deserializeEntry(entryName, serializedString, aClass);
        return Optional.ofNullable(value).orElse(defaultValue);
    }

    @SuppressWarnings("unchecked")
    protected <T> T deserializeEntry(String entryName, String serializedString, Class<T> aClass) {
        String deserializedEntry = deserializeEntry(entryName, serializedString);
        if (deserializedEntry == null) {
            return null;
        }
        if (String.class.equals(aClass)) {
            return (T) deserializedEntry;
        } else {
            try {
                return new ObjectMapper().readValue(deserializedEntry, aClass);
            } catch (JsonProcessingException e) {
                logger.error(String.format("Error deserializing '%s' to %s class", deserializedEntry, aClass), e);
                return null;
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected <T> List<T> deserializeListEntry(String entryName, String serializedString, Class<T> aClass) {
        Class<T[]> arrayClass = (Class<T[]>) Array.newInstance(aClass, 0).getClass();
        T[] array = deserializeEntry(entryName, serializedString, arrayClass);
        return array == null ? List.of() : List.of(array);
    }

    protected abstract String serializeModelData();

    protected abstract void deserializeModelData(String serializedString);
}
