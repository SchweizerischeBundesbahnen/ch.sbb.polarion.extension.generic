package ch.sbb.polarion.extension.generic.settings;

import ch.sbb.polarion.extension.generic.util.VersionUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.ParameterizedType;

public interface NamedSettings<T extends SettingsModel> {

    String DEFAULT_NAME = "Default";

    @NotNull T defaultValues();

    T read(@NotNull String scope, @NotNull SettingId id, String revisionName);

    T save(@NotNull String scope, @NotNull SettingId id, @NotNull T what);

    void delete(@NotNull String scope, @NotNull SettingId id);

    default @NotNull String currentBundleTimestamp() {
        return VersionUtils.getVersion().getBundleBuildTimestamp();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    @NotNull default T fromString(String content) {
        T model = ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]).getConstructor().newInstance();
        model.deserialize(content);
        return model;
    }

    @NotNull
    default String toString(T what) {
        return what.serialize();
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    default T fromJson(String jsonString) {
        return new ObjectMapper().readValue(jsonString, ((Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0]));
    }

}
